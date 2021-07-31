package dev.adrwas.trafficlib.server;

import com.sun.jdi.InvalidTypeException;
import dev.adrwas.trafficlib.packet.*;
import dev.adrwas.trafficlib.packet.PendingPacket.PendingPacketStatus;
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
import java.util.HashMap;

public class SocketServerRequestHandler extends Thread {

    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    public final SocketServer server;

    private String password;

    public HashMap<Long, PendingPacket> transitPackets = new HashMap<Long, PendingPacket>();

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

            System.out.println("starting new thread...");
            new Thread(() -> {
                System.out.println("new thread started");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    sendPacket(new PacketServerRequestPlayers(Packet.generateId()), Packet.PacketOperationTiming.SYNC_FINISH_AFTER_PROCESSED);
                } catch (PacketTransmissionException e) {
                    e.printStackTrace();
                }

                System.out.println("got players successfully (v2)");
            }).start();

            try {
                while((length = in.readInt()) > -1) {
                    bytes = new byte[length];
                    in.readFully(bytes, 0, length);

                    bytes = EncryptionManager.decrypt(bytes, this.password);
                    try {
                        ClientPacket packet = (ClientPacket) Packet.fromByte(bytes);
                        if(!(packet instanceof NoTransitUpdates)) {
                            sendPacket(new PacketServerPacketStatus(packet.packetId, PendingPacketStatus.PROCESSING));
                        }

                        final SocketServerRequestHandler me = this;

                        new Thread(() -> {
                            packet.onRecievedByThisServer(me);

                            try {
                                sendPacket(new PacketServerPacketStatus(packet.packetId, PendingPacketStatus.DONE));
                            } catch (PacketTransmissionException e) {
                                e.printStackTrace();
                            }
                        }).start();
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

    public void log(String message) {
        System.out.println("[TRAFFIC CONN " + this.hashCode() + "] " + message);
    }
}
