package dev.adrwas.trafficlib;

public class TrafficLib {

    private static TrafficLib instance;

    public static TrafficLib getInstance() {
        return instance;
    }

    public static void setInstance(TrafficLib newInstance) {
        instance = newInstance;
    }

    public final String environment;
    public final String trafficLibVersion;

    public TrafficLib(String environment, String trafficLibVersion) {
        this.environment = environment;
        this.trafficLibVersion = trafficLibVersion;
    }
}
