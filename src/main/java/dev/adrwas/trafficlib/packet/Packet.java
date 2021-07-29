package dev.adrwas.trafficlib.packet;

import java.io.*;
import java.util.Random;

public interface Packet {

    final Random packetIdGenerator = new Random();

    public static long generateId() {
        return packetIdGenerator.nextLong();
    }

    public default byte[] toByte() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;

        out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();

        byte[] bytes = bos.toByteArray();
        out.close();

        return bytes;
    }

    public static Packet fromByte(byte[] bytes) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            ObjectInput in = new ObjectInputStream(bis);

            Object o = in.readObject();
            Packet packet = (Packet) o;

            in.close();
            return packet;
        } catch (IOException | ClassNotFoundException exception) {
            exception.printStackTrace();
        }

        return null;
    }

}
