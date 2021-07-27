package dev.adrwas.trafficlib.server;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread {

    private ServerSocket serverSocket;
    private int port;
    private boolean running = false;
    private String password;
    public SocketServer(int port, String password) {
        this.port = port; this.password = password;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            this.start();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            try {
                System.out.println("Listening for a connection");

                Socket socket = serverSocket.accept();

                SocketServerRequestHandler requestHandler = new SocketServerRequestHandler(this, socket, password);
                requestHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
