package dev.adrwas.trafficlib.server;

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

            try {
                while((length = in.readInt()) > -1) {
                    bytes = new byte[length];
                    in.readFully(bytes, 0, length);

                    System.out.println("Read bytes as " + new String(bytes));
                    System.out.println("Decrypting...");

                    bytes = EncryptionManager.decrypt(bytes, this.password);
                    try {
                        ClientPacket packet = (ClientPacket) Packet.fromByte(bytes);
                        System.out.println("[server] got packet " + packet.toString() + " with id " + packet.packetId);

                        if(!(packet instanceof NoTransitUpdates)) {
                            sendPacket(new PacketServerPacketStatus(packet.packetId, PendingPacketStatus.PROCESSING));
                        }

                        final SocketServerRequestHandler me = this;

                        new Thread() {
                            public void run() {
                                packet.onRecievedByThisServer(me);

                                try {
                                    sendPacket(new PacketServerPacketStatus(packet.packetId, PendingPacketStatus.DONE));
                                } catch (IOException | InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeySpecException | InvalidKeyException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
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

    public void sendPacket(ServerPacket serverPacket) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
//        if(!(serverPacket instanceof NoTransitUpdates)) {
//            transitPackets.put(serverPacket.packetId, new PendingPacket(serverPacket, PendingPacketStatus.SENDING));
//        }

        System.out.println("Sending " + serverPacket.toString());

        sendBytes(serverPacket.toByte());
    }

    public void sendBytes(byte[] bytes) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        sendRawBytes(EncryptionManager.encrypt(bytes, this.password));
    }

    public void sendRawBytes(byte[] bytes) throws IOException {
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
