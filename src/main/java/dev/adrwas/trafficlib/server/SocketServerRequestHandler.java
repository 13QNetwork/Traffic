package dev.adrwas.trafficlib.server;

import dev.adrwas.trafficlib.packet.*;
import dev.adrwas.trafficlib.packet.PendingPacket.PendingPacketStatus;
import dev.adrwas.trafficlib.packet.player.Player;
import dev.adrwas.trafficlib.util.EncryptionManager;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SocketServerRequestHandler extends Thread {

    public Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    public final SocketServer server;

    private String password;

    public HashMap<Long, PendingPacket> transitPackets = new HashMap<Long, PendingPacket>();

    public List<GlobalPacketListener> globalPacketListeners = new ArrayList<GlobalPacketListener>();

    public ArrayList<Player> players = new ArrayList<Player>();

    public String serverId;
    public String serverType;

    public String trafficLibVersion;

    public String[] plugins;

    public boolean recievedHandshake = false;

    public SocketServerRequestHandler(SocketServer server, Socket socket, String password) {
        this.socket = socket;
        this.server = server;
        this.password = password;
    }

    @Override
    public void run() {
        try {
            System.out.println("Recieved a connection from " + socket.getInetAddress().toString() + "...");
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            byte[] bytes;
            int length;

            try {
                while((length = in.readInt()) > -1) {
                    bytes = new byte[length];
                    in.readFully(bytes, 0, length);

                    bytes = EncryptionManager.decrypt(bytes, this.password);
                    try {

                        ClientPacket packet = (ClientPacket) Packet.fromByte(bytes);

                        if(recievedHandshake || packet instanceof PacketClientHandshake) {
                            if (!(packet instanceof NoTransitUpdates)) {
                                sendPacket(new PacketServerPacketStatus(packet.packetId, PendingPacketStatus.PROCESSING));
                            }

                            final SocketServerRequestHandler me = this;

                            new Thread(() -> {
                                Iterator<GlobalPacketListener> iterator = globalPacketListeners.iterator();
                                while (iterator.hasNext()) {
                                    GlobalPacketListener listener = iterator.next();
                                    if (!listener.runBeforeProcessing) break;
                                    try {
                                        if (listener.fn.apply(packet)) iterator.remove();
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                }


                                packet.onRecievedByThisServer(me);

                                iterator = globalPacketListeners.iterator();
                                while (iterator.hasNext()) {
                                    GlobalPacketListener listener = iterator.next();
                                    if (listener.runBeforeProcessing) break;
                                    try {
                                        if (listener.fn.apply(packet)) iterator.remove();
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                }

                                try {
                                    sendPacket(new PacketServerPacketStatus(packet.packetId, PendingPacketStatus.DONE));
                                } catch (PacketTransmissionException e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (EOFException e) {
                in.close();
                out.close();
                socket.close();

                System.out.println("Connection closed");
            }
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }

    public Packet sendPacketWaitOtherPacket(ServerPacket serverPacket, GlobalPacketListener listener) throws PacketTransmissionException {
        PendingPacket pendingPacket = new PendingPacket(serverPacket, PendingPacketStatus.SENDING);

        final Thread thread = Thread.currentThread();
        
        AtomicReference<Packet> packetAtomicReference = new AtomicReference<Packet>();
        
        globalPacketListeners.add(new GlobalPacketListener(listener.runBeforeProcessing, (packet) -> {
           if(listener.fn.apply(packet)) {
               synchronized (thread) {
                   packetAtomicReference.set(new PacketServerRequestPlayers(Packet.generateId()));
                   thread.notify();
               }
               return true;
           } else {
               return false;
           }
        }));

        sendPacket(pendingPacket);

        synchronized (thread) {
            try {
                thread.wait();
            } catch (InterruptedException e) {
                throw new PacketTransmissionException("Synchronization error: " + e.getMessage(), e);
            }
        }

        return packetAtomicReference.get();
    }

    public void sendPacket(ServerPacket serverPacket, Packet.PacketOperationTiming packetOperationTiming) throws PacketTransmissionException {
        PendingPacket pendingPacket = new PendingPacket(serverPacket, PendingPacketStatus.SENDING);

        if(packetOperationTiming.equals(Packet.PacketOperationTiming.ASYNC)) {
            new Thread(() -> {
                try {
                    sendPacket(pendingPacket);
                } catch (PacketTransmissionException e) {
                    e.printStackTrace();
                }
            }).start();
        } else if(packetOperationTiming.equals(Packet.PacketOperationTiming.SYNC_FINISH_WHEN_SENT)) {
            sendPacket(pendingPacket);
        } else {
            PendingPacket.PendingPacketEvent packetEvent;
            switch (packetOperationTiming) {
                case SYNC_FINISH_WHEN_RECIEVED:
                    packetEvent = PendingPacket.PendingPacketEvent.PRE_RECEIVED;
                    break;
                case SYNC_FINISH_AFTER_RECIEVED:
                    packetEvent = PendingPacket.PendingPacketEvent.POST_RECEIVED;
                    break;
                case SYNC_FINISH_WHEN_PROCESSED:
                    packetEvent = PendingPacket.PendingPacketEvent.PRE_PROCESSED;
                    break;
                default:
                    packetEvent = PendingPacket.PendingPacketEvent.POST_PROCESSED;
                    break;
            }

            final Thread thread = Thread.currentThread();
            pendingPacket.addEventListener(packetEvent, () -> {
                synchronized (thread) {
                    thread.notify();
                }
                return null;
            });

            sendPacket(pendingPacket);

            synchronized (thread) {
                try {
                    thread.wait();
                } catch (InterruptedException e) {
                    throw new PacketTransmissionException("Synchronization error: " + e.getMessage(), e);
                }
            }
        }
    }

    public void sendPacket(ServerPacket serverPacket) throws PacketTransmissionException {
        sendPacket(new PendingPacket(serverPacket, PendingPacketStatus.SENDING));
    }

    protected void sendPacket(PendingPacket pendingPacket) throws PacketTransmissionException {
        if(!(pendingPacket.packet instanceof ServerPacket)) {
            throw new PacketTransmissionException("Pending Packet sent by server is not a server packet");
        }

        try {
            ServerPacket clientPacket = (ServerPacket) pendingPacket.packet;

            if(!(clientPacket instanceof NoTransitUpdates)) {
                transitPackets.put(clientPacket.packetId, pendingPacket);
            }

            pendingPacket.fireEvent(PendingPacket.PendingPacketEvent.PRE_SENT);

            byte[] bytes;

            try {
                bytes = clientPacket.toByte();
            } catch (IOException exception) {
                throw new PacketTransmissionException("IOException when serializing client packet", exception);
            }

            try {
                sendBytes(bytes);
            } catch (IOException e) {
                throw new PacketTransmissionException("IOException when sending client packet: " + e.getMessage());
            } catch (InvalidAlgorithmParameterException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException exception) {
                throw new PacketTransmissionException(exception.getClass().getName() + " when encrypting client packet: " + exception.getMessage());
            }

            pendingPacket.fireEvent(PendingPacket.PendingPacketEvent.POST_SENT);
        } catch (Exception e) {
            throw new PacketTransmissionException(e.getMessage(), e);
        }
    }

    public void sendBytes(byte[] bytes) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        sendRawBytes(EncryptionManager.encrypt(bytes, this.password));
    }

    public void sendRawBytes(byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public ArrayList<Player> getPlayers(boolean refetch) throws PacketTransmissionException {
        if(refetch) {
               sendPacket(new PacketServerRequestPlayers(Packet.generateId()), Packet.PacketOperationTiming.SYNC_FINISH_AFTER_PROCESSED);
        }
        return players;
    }

    public void log(String message) {
        System.out.println("[TRAFFIC CONN " + this.hashCode() + "] " + message);
    }
}
