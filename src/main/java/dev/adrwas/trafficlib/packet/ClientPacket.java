package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.server.SocketServer;

import java.io.*;
import java.util.Random;

public abstract class ClientPacket implements Serializable {

    public abstract void onRecievedByServer(SocketServer server);

    public final long packetId;

    public ClientPacket(long packetId) {
        this.packetId = packetId;
    }

    public ClientPacket() {
        this.packetId = new Random().nextLong();
    }

    public byte[] toByte() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;

        out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();

        byte[] bytes = bos.toByteArray();
        out.close();

        return bytes;
    }

    public static ClientPacket fromByte(byte[] bytes) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            ObjectInput in = new ObjectInputStream(bis);

            Object o = in.readObject();
            ClientPacket packet = (ClientPacket) o;

            in.close();
            return packet;
        } catch (IOException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }

        return null;
    }
}
