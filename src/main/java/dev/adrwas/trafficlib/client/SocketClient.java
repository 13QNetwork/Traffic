package dev.adrwas.trafficlib.client;

import dev.adrwas.trafficlib.packet.*;
import dev.adrwas.trafficlib.packet.PendingPacket.PendingPacketStatus;
import dev.adrwas.trafficlib.packet.Packet;
import dev.adrwas.trafficlib.packet.v_0_1_R1.PacketClientHandshake;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;
import dev.adrwas.trafficlib.util.EncryptionManager;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A <i>socket client</i> facilitates a socket connection between a client and a master server,
 * and handles the client-side processing of the connection. The SocketClient class can be
 * used to send packets to the master server.
 * @since Traffic 0.1
 **/
public class SocketClient {

    public Socket socket;
    public DataInputStream input;
    public DataOutputStream output;

    public String address;
    public int port;

    private String encryptionPassword;

    public Thread thread;

    /**
     * A hashmap of packet IDs and pending packets, storing data about packets
     * in transit. Packets without the NoTransitUpdates marker are added to the hashmap
     * when sent and removed when fully processed by the master server.
     * @since Traffic 0.1
     **/
    public HashMap<Long, PendingPacket> transitPackets = new HashMap<Long, PendingPacket>();

    /**
     * A {@link java.util.HashMap} of global packet listeners. Listeners in the hashmap contain a {@link java.util.function.Function}
     * with a <b>boolean</b> return value and a {@link Packet} parameter.
     *
     * The function is run every time a packet is received by the client, and the listener
     * will be removed from the {@link java.util.HashMap} if the {@link java.util.function.Function} returns <b>true</b>.
     * @since Traffic 0.1
     **/
    public List<GlobalPacketListener> globalPacketListeners = new ArrayList<GlobalPacketListener>();
    public boolean sentSuccessfulHandshake = false;

    public SocketClient(String address, int port, String encryptionPassword) {
        this.address = address;
        this.port = port;
        this.encryptionPassword = encryptionPassword;

        this.thread = new Thread() {
            public void run() {
                startClient();
            }
        };

        this.thread.start();
    }

    public void startClient() {
        try {
            this.socket = new Socket(address, port);

            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            byte[] bytes;
            int length;

            sendPacket(new PacketClientHandshake(
                Packet.generateId(),
                "server",
                "bungee",
                "0.1",
                new String[]{}
            ), Packet.PacketOperationTiming.ASYNC);

            try {
                while(!socket.isClosed() && (length = input.readInt()) > -1) {
                    bytes = new byte[length];

                    input.readFully(bytes);

                    bytes = EncryptionManager.decrypt(bytes, this.encryptionPassword);

                    ServerPacket packet = (ServerPacket) Packet.fromByte(bytes);

                    if (!(packet instanceof NoTransitUpdates)) {
                        sendPacket(new PacketClientPacketStatus(packet.packetId, PendingPacketStatus.PROCESSING));
                    }

                    final SocketClient me = this;

                    new Thread() {
                        public void run() {
                            Iterator<GlobalPacketListener> iterator = globalPacketListeners.iterator();
                            while(iterator.hasNext()) {
                                GlobalPacketListener listener = iterator.next();
                                if (!listener.runBeforeProcessing) break;
                                try {
                                    if (listener.fn.apply(packet)) iterator.remove();
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                            }

                            packet.onRecievedByThisClient(me);

                            iterator = globalPacketListeners.iterator();
                            while(iterator.hasNext()) {
                                GlobalPacketListener listener = iterator.next();
                                if(listener.runBeforeProcessing) break;
                                try {
                                    if(listener.fn.apply(packet)) iterator.remove();
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                            }

                            if (!(packet instanceof NoTransitUpdates)) {
                                try {
                                    sendPacket(new PacketClientPacketStatus(packet.packetId, PendingPacketStatus.DONE));
                                } catch (PacketTransmissionException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }.start();
                }
            } catch (SSLException e) {
                System.out.println("Socket server closed!");
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


            input.close();
            output.close();
            socket.close();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (PacketTransmissionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public Packet sendPacketWaitOtherPacket(ClientPacket clientPacket, GlobalPacketListener listener) throws PacketTransmissionException {
        PendingPacket pendingPacket = new PendingPacket(clientPacket, PendingPacketStatus.SENDING);

        final Thread thread = Thread.currentThread();

        AtomicReference<Packet> packetAtomicReference = new AtomicReference<Packet>();

        globalPacketListeners.add(new GlobalPacketListener(listener.runBeforeProcessing, (packet) -> {
            if(listener.fn.apply(packet)) {
                synchronized (thread) {
                    packetAtomicReference.set(packet);
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

    public void sendPacket(ClientPacket clientPacket, Packet.PacketOperationTiming packetOperationTiming) throws PacketTransmissionException {
        PendingPacket pendingPacket = new PendingPacket(clientPacket, PendingPacketStatus.SENDING);

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

    public void sendPacket(ClientPacket clientPacket) throws PacketTransmissionException {
        sendPacket(new PendingPacket(clientPacket, PendingPacketStatus.SENDING));
    }

    protected void sendPacket(PendingPacket pendingPacket) throws PacketTransmissionException {
        if(!(pendingPacket.packet instanceof ClientPacket)) {
            throw new PacketTransmissionException("Pending Packet sent by client is not a client packet");
        }

        try {
            ClientPacket clientPacket = (ClientPacket) pendingPacket.packet;

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
        sendRawBytes(EncryptionManager.encrypt(bytes, this.encryptionPassword));
    }

    public void sendRawBytes(byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    public void log(String message) {
        System.out.println("[TRAFFIC CLIENT] " + message);
    }
}
