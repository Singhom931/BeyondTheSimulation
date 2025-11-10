package com.diablo931.network;

import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class WSClient implements WebSocket.Listener {
    private WebSocket ws;
    private boolean open = false;
    private Consumer<String> onMessage;

    public WSClient(Consumer<String> onMessage) {
        this.onMessage = onMessage;
    }

    public void connect(String url) {
        ws = java.net.http.HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create(url), this)
                .join();
        open = true;
        System.out.println("[WS] Connected to " + url);
    }

    public boolean isClosed() {
        return !open;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        open = true;
        System.out.println("[WS] onOpen");
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String msg = data.toString();
        System.out.println("[WS RECV] " + msg);
        if (onMessage != null) onMessage.accept(msg);
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        open = false;
        System.out.println("[WS] Closed: " + statusCode + " -> " + reason);
        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        open = false;
        System.out.println("[WS] Error: " + error);
    }

    public void send(String message) {
        if (ws != null && open) {
            ws.sendText(message, true);
        } else {
            System.out.println("[WS] Tried to send but not open");
        }
    }
}
