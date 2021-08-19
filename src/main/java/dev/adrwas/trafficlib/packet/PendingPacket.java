package dev.adrwas.trafficlib.packet;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class PendingPacket<T extends Packet> {

    public enum PendingPacketStatus {
        SENDING,
        PROCESSING,
        DONE
    }

    public enum PendingPacketEvent {
        PRE_SENT,
        POST_SENT,
        PRE_RECEIVED,
        POST_RECEIVED,
        PRE_PROCESSED,
        POST_PROCESSED
    }

    public T packet;
    public PendingPacketStatus status;

    public PendingPacket(T packet) {
        this.packet = packet;
        this.status = PendingPacketStatus.SENDING;
    }

    public PendingPacket(T packet, PendingPacketStatus status) {
        this.packet = packet;
        this.status = status;
    }

    private final HashMap<PendingPacketEvent, List<Callable>> eventListeners = new HashMap<PendingPacketEvent, List<Callable>>();

    public void addEventListener(PendingPacketEvent event, Callable func) {
        if(eventListeners.containsKey(event)) {
            eventListeners.get(event).add(func);
        } else {
            eventListeners.put(event, Lists.newArrayList(func));
        }
    }

    public void fireEvent(PendingPacketEvent event) {
        if(eventListeners.containsKey(event)) {
            eventListeners.get(event).forEach((func) -> {
                try {
                    func.call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            eventListeners.put(event, new ArrayList<Callable>());
        }
    }
}
