package dev.adrwas.trafficlib.server;

import dev.adrwas.trafficlib.packet.ClientPacket;
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

public class SocketServerRequestHandler extends Thread {

    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    public final SocketServer server;

    private String password;

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
                        ClientPacket packet = ClientPacket.fromByte(bytes);
                        System.out.println("got packet " + packet);
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
}
