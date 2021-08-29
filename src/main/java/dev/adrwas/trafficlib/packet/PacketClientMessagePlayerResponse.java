package dev.adrwas.trafficlib.packet;

import dev.adrwas.trafficlib.client.SocketClient;
import dev.adrwas.trafficlib.server.SocketServerRequestHandler;

import java.io.Serializable;

public class PacketClientMessagePlayerResponse extends ClientPacket {

    public static class MessagePlayerResult implements Serializable {

        public enum MessagePlayerResultEnum {
            SUCCESS,
            PLAYER_OFFLINE,
            INCOMPATIBLE_TYPE,
            SERVER_NOT_FOUND
        }

        public MessagePlayerResultEnum resultEnum;
        public Object data;

        public MessagePlayerResult(MessagePlayerResultEnum resultEnum, Object data) {
            this.resultEnum = resultEnum;
            this.data = data;
        }
    }

    public final MessagePlayerResult result;

    public PacketClientMessagePlayerResponse(long packetId, MessagePlayerResult result) {
        super(packetId);
        this.result = result;
    }

    @Override
    public void onRecievedByThisServer(SocketServerRequestHandler server) {

    }

    @Override
    public void onRecievedByRemoteServer(SocketClient client) {

    }

    @Override
    public void onProcessedByRemoteServer(SocketClient client) {

    }
}
