package dev.adrwas.trafficlib;

import dev.adrwas.trafficlib.TrafficLib;
import dev.adrwas.trafficlib.server.SocketServer;

public class TrafficLibExecutable {
    public static void main(String[] args) {
        System.out.println("TrafficLib executed as a standalone JAR");
        TrafficLib.setInstance(new TrafficLib("SERVER_EXEC", "0.1"));

        SocketServer server = new SocketServer(6000, "password");
        server.startServer();
    }
}
