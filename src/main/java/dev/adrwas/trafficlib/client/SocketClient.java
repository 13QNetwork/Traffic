package dev.adrwas.trafficlib.client;

import dev.adrwas.trafficlib.packet.ClientPacket;
import dev.adrwas.trafficlib.packet.ExamplePacket;
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

public class SocketClient {

    private Socket socket;

    private DataInputStream input;
    private DataOutputStream output;

    private String address;
    private int port;

    private boolean closed = false;

    private String encryptionPassword;

    private Thread thread;

    protected SocketClient(String address, int port, String encryptionPassword) {
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

            try {
                sendPacket(new ExamplePacket());
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
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

            try {
                while(!closed && (length = input.readInt()) > -1) {
                    bytes = new byte[length];

                    input.readFully(bytes);
                }
            } catch (SSLException e) {
                System.out.println("Socket server closed!");
            }


            input.close();
            output.close();
            socket.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendPacket(ClientPacket clientPacket) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        sendBytes(clientPacket.toByte());
    }

    public void sendBytes(byte[] bytes) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeySpecException, InvalidKeyException {
        sendRawBytes(EncryptionManager.encrypt(bytes, this.encryptionPassword));
    }

    public void sendRawBytes(byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
    }
}
