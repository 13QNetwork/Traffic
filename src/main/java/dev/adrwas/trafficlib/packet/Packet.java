package dev.adrwas.trafficlib.packet;

import java.io.*;
import java.util.Random;

public interface Packet {

    Random packetIdGenerator = new Random();

    enum PacketOperationTiming {
        ASYNC, // continue process immediately
        SYNC_FINISH_WHEN_SENT, // continue process when local operation is finished
        SYNC_FINISH_WHEN_RECIEVED, // continue process when the packet is received by the remote server
        SYNC_FINISH_AFTER_RECIEVED, // continue process when the packet is received by the remote server and local processing is done
        SYNC_FINISH_WHEN_PROCESSED, // continue process when the packet is processed by the remote server
        SYNC_FINISH_AFTER_PROCESSED // continue process when the packet is processed by the remote server and local processing is done
    }

    static long generateId() {
        return packetIdGenerator.nextLong();
    }

    default byte[] toByte() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;

        out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();

        byte[] bytes = bos.toByteArray();
        out.close();

        return bytes;
    }

    static Packet fromByte(byte[] bytes) throws ClassNotFoundException {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            ObjectInput in = new ObjectInputStream(bis);

            Object o = in.readObject();
            Packet packet = (Packet) o;

            in.close();
            return packet;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    static Object fromByteToObject(byte[] bytes) throws ClassNotFoundException {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

            ObjectInput in = new ObjectInputStream(bis);

            Object o = in.readObject();

            in.close();
            return o;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }
}
