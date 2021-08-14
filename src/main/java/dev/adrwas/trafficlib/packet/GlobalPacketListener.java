package dev.adrwas.trafficlib.packet;

import java.util.function.Function;

public class GlobalPacketListener {

    public final boolean runBeforeProcessing;
    public final Function<Packet, Boolean> fn;

    public GlobalPacketListener(boolean runBeforeProcessing, Function<Packet, Boolean> fn) {
        this.runBeforeProcessing = runBeforeProcessing;
        this.fn = fn;
    }
}
