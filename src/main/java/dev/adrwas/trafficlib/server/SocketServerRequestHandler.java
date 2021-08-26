package dev.adrwas.trafficlib.server;

import dev.adrwas.trafficlib.packet.*;
import dev.adrwas.trafficlib.packet.PendingPacket.PendingPacketStatus;
import dev.adrwas.trafficlib.packet.player.Player;
import dev.adrwas.trafficlib.packet.Packet;
import dev.adrwas.trafficlib.packet.v_0_1_R1.PacketClientHandshake;
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
import java.util.function.Function;

/**
 * A <b>SocketServerRequestHandler</b> facilitates a
 * socket connection between the master server and a
 * connecting socket client. It also handles the
 * server-side processing of the connection. The
 * SocketServerRequestHandler class can be used to send a
 * {@link dev.adrwas.trafficlib.packet.ServerPacket}
 * to the connecting socket client. The request handler
 * processes any incoming {@link dev.adrwas.trafficlib.packet.ClientPacket}.
 * <br>
 * <br>
 * The request handler runs on a separate {@link java.lang.Thread}
 * to allow other connections to run alongside it.
 * When a {@link dev.adrwas.trafficlib.packet.ClientPacket}
 * is received by the handler, it creates a new temporary
 * Thread to process it, which prevents packet
 * backlogging.
 *
 * @since Traffic 0.1
 **/
public class SocketServerRequestHandler extends Thread {

    public Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String password; // Encryption password

    /**
     * A {@link dev.adrwas.trafficlib.server.SocketServer}
     * containing active Traffic connections and server
     * configuration. It handles the core socket server
     * and its connections.
     *
     * @since Traffic 0.1
     **/
    public final SocketServer server;


    /**
     * A {@link java.util.HashMap} of packet IDs and
     * {@link dev.adrwas.trafficlib.packet.PendingPacket}s,
     * storing data about packets in transit. Packets without
     * the {@link dev.adrwas.trafficlib.packet.NoTransitUpdates}
     * marker are added to the {@link java.util.HashMap} when
     * sent and removed when fully processed by the master server.
     *
     * @since Traffic 0.1
     **/
    public HashMap<Long, PendingPacket> transitPackets = new HashMap<Long, PendingPacket>();

    /**
     * A {@link java.util.HashMap} of
     * {@link dev.adrwas.trafficlib.packet.GlobalPacketListener}s.
     * Listeners in the {@link java.util.HashMap} contain a
     * {@link java.util.function.Function} with a <b>boolean</b>
     * return value and a {@link Packet} parameter.
     *
     * The {@link Function} is run every time a
     * {@link dev.adrwas.trafficlib.packet.Packet} is received by the
     * client, and the listener will be removed from the {@link java.util.HashMap}
     * if the {@link Function}
     * returns <b>true</b>.
     *
     * @since Traffic 0.1
     **/
    public List<GlobalPacketListener> globalPacketListeners = new ArrayList<GlobalPacketListener>();

    /**
     * A {@link java.util.List} of all
     * {@link dev.adrwas.trafficlib.packet.player.Player}s
     * connected to the client Minecraft server.
     * **/
    public ArrayList<Player> players = new ArrayList<Player>();

    public String serverId;
    public String serverType;
    public String trafficLibVersion;

    /**
     * A {@link java.util.List} of all Traffic plugins, used to
     * ensure plugin compatibility with connecting clients.
     * **/
    public String[] plugins;

    /**
     * Prevents non-handshake incoming packets from being
     * received without a valid handshake packet being recieved.
     * **/
    public boolean recievedHandshake = false;

    /**
     * Creates a {@link SocketServerRequestHandler} with a
     * {@link dev.adrwas.trafficlib.server.SocketServer}, {@link java.net.Socket},
     * and password ({@link String})
     * **/
    public SocketServerRequestHandler(SocketServer server, Socket socket, String password) {
        this.socket = socket;
        this.server = server;
        this.password = password;
    }

    @Override
    public void run() {
        try {
            // print connection recieved TODO improve with emojis, colors more
            System.out.println("Recieved a connection from " + socket.getInetAddress().toString() + "...");

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            byte[] bytes;
            int length;

            try {

                while((length = in.readInt()) > -1) {

                    bytes = new byte[length];
                    in.readFully(bytes, 0, length); // Read bytes
                    bytes = EncryptionManager.decrypt(bytes, this.password);

                    try {

                        ClientPacket packet;

                        // Try/catch block prevents ClassNotFoundException
                        try {
                            // Gets packet as an object to prevent casting exception
                            Object packetAsObject = Packet.fromByteToObject(bytes);

                            // Packet will be null if deserialization throws error
                            if(packetAsObject == null) {
                                log("Client sent null object, closing connection!");

                                socket.close();
                                return;
                            }

                            // If object recieved is not a ClientPacket
                            if(!(packetAsObject instanceof ClientPacket)) {
                                log("Client sent object without ClientPacket class, closing connection!");
                                socket.close();
                                return;
                            }

                            packet = (ClientPacket) packetAsObject;
                        } catch (ClassNotFoundException exception) {
                                log("Client sent object of unknown class, closing connection!");
                                socket.close();
                                return;
                        }

                        // Only lets in handshake packets, unless already received one
                        if(recievedHandshake || packet instanceof PacketClientHandshake) {

                            // Sends a transit update, unless marked with NoTransitUpdates
                            if (!(packet instanceof NoTransitUpdates))
                                sendPacket(new PacketServerPacketStatus(packet.packetId, PendingPacketStatus.PROCESSING));

                            // Assigns this to a final variable, so it can be used inside the threads
                            final SocketServerRequestHandler me = this;

                            // Runs code asynchronously to prevent poorly-written user code from causing a bottleneck.
                            new Thread(() -> {

                                // Iterator to allow removal of listeners
                                Iterator<GlobalPacketListener> iterator = globalPacketListeners.iterator();
                                while (iterator.hasNext()) {
                                    GlobalPacketListener listener = iterator.next();

                                    // This block is run before processing, so all non-runBeforeProcessing listeners should be ignored
                                    if (!listener.runBeforeProcessing)
                                        break;

                                    try {
                                        if(listener.fn.apply(packet))
                                            iterator.remove();
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }

                                }

                                // Runs packets onReceived code
                                // This is the most important part of the thread block
                                packet.onRecievedByThisServer(me);

                                // Repeats the runBeforeProcessing code, running all non-runBeforeProcessing listeners
                                iterator = globalPacketListeners.iterator();

                                while (iterator.hasNext()) {
                                    GlobalPacketListener listener = iterator.next();

                                    // This block is run after processing, so all runBeforeProcessing listeners should be ignored
                                    if (listener.runBeforeProcessing)
                                        break;

                                    try {
                                        if (listener.fn.apply(packet)) iterator.remove();
                                    } catch (Exception exception) {
                                        exception.printStackTrace();
                                    }
                                }

                                // Send done status
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

    /**
     * Sends a basic {@link dev.adrwas.trafficlib.packet.ServerPacket} to
     * the connecting {@link dev.adrwas.trafficlib.client.SocketClient} and
     * pauses the current {@link java.lang.Thread} until a specific desired
     * packet is received. The {@link java.util.function.Function} within the
     * {@link dev.adrwas.trafficlib.packet.GlobalPacketListener} parameter is run
     * every time a {@link dev.adrwas.trafficlib.packet.ClientPacket} is received.
     * If the function returns true for any incoming packet, the original Thread
     * will resume and the function will no longer run when packets are received.
     * This function can be run either before or after the packet is processed by
     * specifying **GlobalPacketListener.runBeforeProcessing**.
     *
     * Your {@link dev.adrwas.trafficlib.packet.GlobalPacketListener} packet is
     * run on a packet processing Thread, so avoid long, synchronous operations such as
     * database queries ir large calculations as they'll bottleneck other listeners, as
     * well as the packet itself if your listener is run before processing.
     *
     * @param serverPacket packet you want to send
     * @param listener listener, run every time a packet is received until its function returns true
     * @return the {@link dev.adrwas.trafficlib.packet.ClientPacket} received when the {@link dev.adrwas.trafficlib.packet.GlobalPacketListener} returns **true**
     * @throws PacketTransmissionException
     */
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
