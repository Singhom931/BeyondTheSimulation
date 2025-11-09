package com.diablo931.block;

import com.diablo931.network.RedstoneWebSocketClient;
import com.diablo931.util.TickableBlockEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class MultiRedstoneArrayBlockEntity extends BlockEntity implements TickableBlockEntity, NamedScreenHandlerFactory {

//    private final Map<Direction, Integer> signals = new EnumMap<>(Direction.class);
private static final Map<String, Direction> API_TO_MC = Map.of(
        "north", Direction.SOUTH,
        "south", Direction.NORTH,
        "east", Direction.WEST,
        "west", Direction.EAST
);

    public void handleServerResponse(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            JsonObject signals = json.getAsJsonObject("signals");
            int north = signals.get("north").getAsInt();
            int east = signals.get("east").getAsInt();
            int south = signals.get("south").getAsInt();
            int west = signals.get("west").getAsInt();

            // Apply signals (example)
            System.out.println("[WS] Updating signals: N=" + north + " E=" + east + " S=" + south + " W=" + west);
            // You can update your redstone output here.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


private static final Map<Direction, String> MC_TO_API = Map.of(
        Direction.NORTH, "north",
        Direction.SOUTH, "south",
        Direction.EAST, "east",
        Direction.WEST, "west"
);

    private final Map<Direction, Integer> inputSignals = new EnumMap<>(Direction.class);
    private final Map<Direction, Integer> outputSignals = new EnumMap<>(Direction.class);

    private final Map<Direction, Integer> lastSentInputs = new EnumMap<>(Direction.class);
    private String url = "";
    private int failedAttempts = 0;
    private UUID uniqueId = UUID.randomUUID();
    private int tickCounter = 0;

    private ConnectionMode connectionMode = ConnectionMode.HTTP;
    private RedstoneWebSocketClient wsClient;

//    private String uniqueId = java.util.UUID.randomUUID().toString().replace("-", "");

    public enum ConnectionMode {
        HTTP,
        WEBSOCKET
    }

    public MultiRedstoneArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTI_REDSTONE_ARRAY_ENTITY, pos, state);
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; markDirty(); }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(ConnectionMode mode) {
        this.connectionMode = mode;
        markDirty();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        url = nbt.getString("url");
        if (nbt.contains("UniqueId")) {
            uniqueId = nbt.getUuid("UniqueId");
        }
        if (nbt.contains("ConnectionMode")) {
            connectionMode = ConnectionMode.valueOf(nbt.getString("ConnectionMode"));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("url", url == null ? "" : url);
        nbt.putUuid("UniqueId", uniqueId);
        nbt.putString("ConnectionMode", connectionMode.name());
    }



    @Override
    public Text getDisplayName() {
        return Text.literal("Multi Redstone Array");
    }

    private void markFailed() {
        failedAttempts++;
        if (failedAttempts >= 40) {
            // Stop trying temporarily or log a message
            failedAttempts = 0;
            System.out.println("Block at " + pos + " failed to reach URL: " + url);
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception ignored) {}
        }
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new MultiRedstoneArrayScreenHandler(syncId, inv, this);
    }

    public int getPowerForDirection(Direction dir) {
        // return 0 if no signal stored for this direction
        return outputSignals.getOrDefault(dir, 0);
    }

    public void setPowerForDirection(Direction dir, int power) {
        outputSignals.put(dir, power);
        markDirty();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) return;

        tickCounter++;

        // --- Handle WebSocket mode ---
        if (connectionMode == ConnectionMode.WEBSOCKET) {
            try {
                // Create connection if not open
                if (wsClient == null || !wsClient.isOpen()) {
                    if (url == null || url.isEmpty()) return;
                    System.out.println("[WS] Connecting to " + url);
                    wsClient = new RedstoneWebSocketClient(new java.net.URI(url), this);
                    wsClient.connect();
                    return; // Wait for connection to open
                }

                // Periodically refresh signals from world
                if (tickCounter % 2 == 0) {
                    updateSignalsFromWorld();
                }

                // If redstone input changed, send an update
                if (inputSignalsChanged()) {
                    String payload = buildJsonPayload();
                    System.out.println("[WS] Sending: " + payload);
                    wsClient.send(payload);
                }

            } catch (Exception e) {
                System.out.println("[WS] Error: " + e.getMessage());
            }

            return; // Do not run HTTP code if WS mode is active
        }

        // --- Handle HTTP mode ---
        if (tickCounter % 2 == 0) { // Poll every 2 ticks
            updateSignalsFromWorld();
            fetchServerOutputs();
        }

        if (inputSignalsChanged()) {
            System.out.println("[HTTP] Sending to " + url);
            sendSignalsToServer();
        }
    }

    private void fetchServerOutputs() {
        if (url.isEmpty() || world == null || world.isClient) return;

        CompletableFuture.runAsync(() -> {
            java.net.HttpURLConnection con = null;
            try {
                String fullUrl = url + (url.contains("?") ? "&" : "?") + "uuid=" + uniqueId;
                java.net.URL u = new java.net.URL(fullUrl);
                con = (java.net.HttpURLConnection) u.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);

                int responseCode = con.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    try (InputStream is = con.getInputStream()) {
                        String response = new String(is.readAllBytes());
                        System.out.println("[GET] Response for " + pos + ": " + response);

                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response).getAsJsonObject();

                        if (json.has(uniqueId.toString())) {
                            com.google.gson.JsonObject outputSignals_json = json.getAsJsonObject(uniqueId.toString());

                            // Run updates on server thread to safely modify blocks
                            if (world != null && !world.isClient) {
                                world.getServer().execute(() -> {
                                    for (Map.Entry<String, com.google.gson.JsonElement> entry : outputSignals_json.entrySet()) {
                                        String apiDir = entry.getKey().toLowerCase();
                                        Direction dir = API_TO_MC.get(apiDir);
                                        if (dir != null) {
                                            int newPower = entry.getValue().getAsInt();
                                            int oldPower = outputSignals.getOrDefault(dir, 0);
                                            if (oldPower != newPower) {
                                                System.out.println("[UPDATE] " + dir + " power: " + oldPower + " → " + newPower);
                                                setPowerForDirection(dir, newPower);
                                                updateNeighborRedstone(dir, newPower);
                                            }
                                        }
                                    }
                                });
                            }
                        } else {
                            System.out.println("[WARN] No data for UUID " + uniqueId + " in response.");
                        }
                    }
                } else {
                    System.out.println("[ERROR] GET failed with response code: " + responseCode);
                    markFailed();
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Exception during GET: " + e);
                markFailed();
            } finally {
                if (con != null) con.disconnect();
            }
        });
    }



    private boolean inputSignalsChanged() {
        // Compare current input directions with last sent values
        boolean changed = false;
        Direction[] horizontal = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction dir : horizontal) {
            if (isInputDirection(dir)) {
                int current = inputSignals.getOrDefault(dir, 0);
                int lastSent = lastSentInputs.getOrDefault(dir, 0);
                if (current != lastSent) {
                    lastSentInputs.put(dir, current);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean isInputDirection(Direction dir) {
        // Decide which directions are inputs
        // Could be configurable per block type or hardcoded
        return true; // for example, all directions are inputs
    }


    private void sendSignalsToServer() {
        if (world == null || world.isClient || url.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            java.net.HttpURLConnection con = null;
            try {
                java.net.URL u = new java.net.URL(url);
                con = (java.net.HttpURLConnection) u.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setConnectTimeout(3000);
                con.setReadTimeout(3000);
                con.setRequestProperty("Content-Type", "application/json");

                // Build JSON payload
                String payload = buildJsonPayload();
//                con.getOutputStream().write(payload.getBytes());
                System.out.println("[POST] Sending to " + url + ": " + payload);

                try (java.io.OutputStream os = con.getOutputStream()) {
                    os.write(payload.getBytes());
                    os.flush();
                }
                int code = con.getResponseCode();
                System.out.println("[POST] Sent → " + url + " | Response: " + code);

                if (code < 200 || code >= 300) {
                    markFailed();
                }

            } catch (Exception e) {
                markFailed();
            } finally {
                if (con != null) con.disconnect();
            }
        });
    }

    public String buildJsonPayload() {
        StringBuilder signalsJson = new StringBuilder();
        signalsJson.append("{");

        for (Map.Entry<Direction, Integer> entry : inputSignals.entrySet()) {
            Direction dir = entry.getKey();
            int power = entry.getValue();
            String apiDir = MC_TO_API.get(dir);
            if (apiDir != null) {
                signalsJson.append("\"").append(apiDir).append("\":").append(power).append(",");
            }
        }

        if (signalsJson.charAt(signalsJson.length() - 1) == ',') {
            signalsJson.deleteCharAt(signalsJson.length() - 1); // remove trailing comma
        }
        signalsJson.append("}");

        return String.format(
                "{\"id\":\"%s\",\"pos\":{\"x\":%d,\"y\":%d,\"z\":%d},\"signals\":%s}",
                uniqueId.toString(),
                pos.getX(), pos.getY(), pos.getZ(),
                signalsJson.toString()
        );
    }

    public void updateSignalsFromWorld() {
        if (world == null) return;

        Direction[] horizontal = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (Direction dir : horizontal) {
            BlockPos neighborPos = pos.offset(dir);
            int power = world.getEmittedRedstonePower(neighborPos, dir); // reads redstone power
            inputSignals.put(dir, power);
        }
    }


    private void updateNeighborRedstone(Direction dir, int power) {
        BlockPos neighborPos = pos.offset(dir);
        BlockState neighbor = world.getBlockState(neighborPos);

        // Only update if there’s a redstone dust or compatible block
        if (neighbor.getBlock() instanceof RedstoneWireBlock) {
            BlockState newState = neighbor.with(RedstoneWireBlock.POWER, power);
            world.setBlockState(neighborPos, newState, 3);
        }
            world.updateNeighbors(pos, this.getCachedState().getBlock());
            world.updateNeighborsAlways(neighborPos, neighbor.getBlock());
    }


}
