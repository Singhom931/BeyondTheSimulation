package com.diablo931.network;

import com.diablo931.block.MultiRedstoneArrayBlockEntity;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

import static net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking.send;

public class RedstoneWebSocketClient extends WebSocketClient {
    private final MultiRedstoneArrayBlockEntity block;

    public RedstoneWebSocketClient(URI serverUri, MultiRedstoneArrayBlockEntity block) {
        super(serverUri);
        this.block = block;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("[WS] Connected → " + uri);
        send(block.buildJsonPayload());
    }

    @Override
    public void onMessage(String message) {
        System.out.println("[WS] Received: " + message);
        // parse and update redstone signals on main thread
        if (block.getWorld() != null && !block.getWorld().isClient()) {
            block.getWorld().getServer().execute(() -> block.handleServerResponse(message));
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("[WS] Closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
