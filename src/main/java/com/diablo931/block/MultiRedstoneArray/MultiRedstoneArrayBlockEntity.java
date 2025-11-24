package com.diablo931.block.MultiRedstoneArray;

import com.diablo931.block.ModBlockEntities;
import com.diablo931.network.WSClient;
import com.diablo931.util.TickableBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// --- Added imports for MQTT and concurrent maps ---
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.concurrent.ConcurrentHashMap;


public class MultiRedstoneArrayBlockEntity extends BlockEntity implements TickableBlockEntity, NamedScreenHandlerFactory {

    //    private final Map<Direction, Integer> signals = new EnumMap<>(Direction.class);
    private static final Map<String, Direction> API_TO_MC = Map.of(
            "north", Direction.SOUTH,
            "south", Direction.NORTH,
            "east", Direction.WEST,
            "west", Direction.EAST
    );

    private static final Map<Direction, String> MC_TO_API = Map.of(
            Direction.NORTH, "north",
            Direction.SOUTH, "south",
            Direction.EAST, "east",
            Direction.WEST, "west"
    );

    // ---------------------------------------------------------------------
    // MQTT Support Additions
    // ---------------------------------------------------------------------

    public enum MqttType {
        PUBLISH,
        SUBSCRIBE
    }

    public enum Mode {
        HTTP,
        WEB_STOCK,
        MQTT
    }

    private Mode mode = Mode.MQTT;
    private MqttType mqttType = MqttType.SUBSCRIBE;

    public static final String MQTT_BROKER = "tcp://broker.hivemq.com:1883";


    private final Map<Direction, Integer> inputSignals = new EnumMap<>(Direction.class);
    private final Map<Direction, Integer> outputSignals = new EnumMap<>(Direction.class);

    private final Map<Direction, Integer> lastSentInputs = new EnumMap<>(Direction.class);
    private String url = "";
    private int failedAttempts = 0;
    private UUID uniqueId = UUID.randomUUID();
    private int tickCounter = 0;
    private WSClient wsClient;

    private int stage = 0;
    private volatile boolean api_is_working = true;
//    private String uniqueId = java.util.UUID.randomUUID().toString().replace("-", "");

    // --- MQTT runtime fields (added) ---
    private IMqttClient mqttClient = null;
    private volatile boolean mqttConnected = false;

    /**
     * mqttContributions:
     * topic -> publisherId -> EnumMap<Direction,Integer>
     * We keep per-publisher contributions so we can recompute merged = max(...) when publishers update.
     */
    private final Map<String, Map<String, EnumMap<Direction, Integer>>> mqttContributions = new ConcurrentHashMap<>();


    public MultiRedstoneArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTI_REDSTONE_ARRAY_ENTITY, pos, state);
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; markDirty(); }


    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        markDirty();
    }

    public void setMqttType(MqttType t) { mqttType = t; markDirty(); }
    public MqttType getMqttType() { return mqttType; }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        url = nbt.getString("url");
        if (nbt.contains("UniqueId")) uniqueId = nbt.getUuid("UniqueId");
        if (nbt.contains("mode")) mode = Mode.valueOf(nbt.getString("mode")); // <-- load mode
        if (nbt.contains("mqttType")) mqttType = MqttType.valueOf(nbt.getString("mqttType")); // <-- load mqtt type
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("url", url == null ? "" : url);
        nbt.putUuid("UniqueId", uniqueId);
        nbt.putString("mode", mode.name()); // <-- save mode
        nbt.putString("mqttType", mqttType.name()); // <-- save mqtt type
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

    public void tick() {
//        System.out.println("[DEBUG] Tick called at " + pos + " URL: " + be.getUrl());
        if (world == null || world.isClient) return;
        if (api_is_working) {
            if (world.getTime() % 10 == 0) {

                stage = (stage + 1) % 2;
                BlockState state = getCachedState();
                if (state.contains(MultiRedstoneArrayBlock.STAGE)) {
                    world.setBlockState(pos, state.with(MultiRedstoneArrayBlock.STAGE, this.stage), Block.NOTIFY_ALL);
                }
            }
        }
        tickCounter++;
        if (tickCounter % 2 == 0) { // approx every 2 redstone ticks
            updateSignalsFromWorld();
//            System.out.println("Ticking MultiRedstoneArray at " + pos + " with URL: " + url);
            fetchServerOutputs();
        }
        if (inputSignalsChanged()) {
            System.out.println("Ticking MultiRedstoneArray at " + pos + " with URL: " + url);
            sendSignalsToServer();
            System.out.println("Redstone Updated");
        }
    }

    private void initWebSocketIfNeeded() {
        if (url != null && url.startsWith("ws://") && (wsClient == null || wsClient.isClosed())) {
            wsClient = new WSClient(response -> {
                // Handle incoming WebSocket data (same as HTTP response)
                processServerJson(response);
            });
            try {
                wsClient.connect(url);
                System.out.println("[WS] Connected to " + url);
            } catch (Exception e) {
                System.out.println("[WS] Failed to connect: " + e);
            }
        }
    }

    public void syncToClient() {
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    private boolean isUrlCompatibleWithMode() {
        return switch (mode) {
            case HTTP -> url.startsWith("http://") || url.startsWith("https://");
            case WEB_STOCK -> url.startsWith("ws://");
            case MQTT -> !url.isBlank();
        };
    }


    private void fetchServerOutputs() {
        if (url.isEmpty() || world == null || world.isClient) return;

        // ✅ Skip if mode and URL don't match
        if (!isUrlCompatibleWithMode()) {
            System.out.println("[WARN] URL and mode mismatch: " + url + " (mode=" + mode + ")");
            return;
        }

        // ✅ Handle HTTP mode
        if (mode == Mode.HTTP) {
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
                            processServerJson(response);
                        }
                    } else {
                        System.out.println("[ERROR] GET failed: " + responseCode);
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

        // ✅ Handle WebSocket mode
        else if (mode == Mode.WEB_STOCK) {
            initWebSocketIfNeeded();

            if (wsClient != null) {
                String payload = buildJsonPayload();
                wsClient.send(payload);
                System.out.println("[WS] Sent payload: " + payload);
            }
        }

        // ✅ Handle MQTT mode (ensure connection and subscription if needed)
        else if (mode == Mode.MQTT) {
            ensureMqttConnectionIfNeeded();
            // Note: we do not publish here; publishing happens only on change via sendSignalsToServer()
        }
    }

    // Shared method to process JSON for both HTTP and WebSocket
    private void processServerJson(String response) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(response).getAsJsonObject();

            if (!json.has(uniqueId.toString())) {
                System.out.println("[WARN] No data for UUID " + uniqueId);
                return;
            }

            com.google.gson.JsonObject outputSignals_json = json.getAsJsonObject(uniqueId.toString());

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
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to parse JSON: " + e);
        }
    }

    // Handle incoming MQTT payloads (topic + payload) ---
    private void handleMqttMessage(String topic, String payload) {
        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();

            // Expecting the same payload shape produced by buildJsonPayload():
            // { "id": "<uuid>", "pos": {...}, "signals": { "north": 1, "south": 0, ... } }
            if (!json.has("id") || !json.has("signals")) {
                return;
            }

            String publisherId = json.get("id").getAsString();
            com.google.gson.JsonObject signals = json.getAsJsonObject("signals");

            // Update contribution map for this topic and publisher
            mqttContributions.computeIfAbsent(topic, t -> new ConcurrentHashMap<>());
            Map<String, EnumMap<Direction, Integer>> topicMap = mqttContributions.get(topic);

            EnumMap<Direction, Integer> pubMap = topicMap.computeIfAbsent(publisherId, k -> new EnumMap<>(Direction.class));

            // Fill publisher's contribution
            for (Map.Entry<String, com.google.gson.JsonElement> e : signals.entrySet()) {
                String apiDir = e.getKey().toLowerCase();
                Direction dir = API_TO_MC.get(apiDir);
                if (dir != null) {
                    int power = e.getValue().getAsInt();
                    pubMap.put(dir, power);
                }
            }

            // Recompute merged maximums for topic
            EnumMap<Direction, Integer> merged = new EnumMap<>(Direction.class);
            // initialize merged with zeros
            merged.put(Direction.NORTH, 0);
            merged.put(Direction.EAST, 0);
            merged.put(Direction.SOUTH, 0);
            merged.put(Direction.WEST, 0);

            for (Map.Entry<String, EnumMap<Direction, Integer>> pubEntry : topicMap.entrySet()) {
                EnumMap<Direction, Integer> contrib = pubEntry.getValue();
                for (Direction d : contrib.keySet()) {
                    int v = contrib.getOrDefault(d, 0);
                    int old = merged.getOrDefault(d, 0);
                    if (v > old) merged.put(d, v);
                }
            }

            // Apply merged values to this block's outputs if this block is subscribed to this topic
            if (world != null && !world.isClient) {
                world.getServer().execute(() -> {
                    // Only update this block if its topic matches and it's in SUBSCRIBE mode
                    if (mode == Mode.MQTT && mqttType == MqttType.SUBSCRIBE && topic.equals(this.url)) {
                        for (Map.Entry<Direction, Integer> me : merged.entrySet()) {
                            Direction dir = me.getKey();
                            int newPower = me.getValue();
                            int oldPower = outputSignals.getOrDefault(dir, 0);
                            if (oldPower != newPower) {
                                setPowerForDirection(dir, newPower);
                                updateNeighborRedstone(dir, newPower);
                                System.out.println("[MQTT UPDATE] " + dir + " power: " + oldPower + " -> " + newPower + " from topic " + topic);
                            }
                        }
                    }
                });
            }

        } catch (Exception ex) {
            System.out.println("[MQTT] Failed to handle message: " + ex);
        }
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

        // If MQTT & PUBLISH, publish via MQTT instead of HTTP POST
        if (mode == Mode.MQTT && mqttType == MqttType.PUBLISH) {
            String payload = buildJsonPayload();
            mqttPublish(payload);
            return;
        }

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

    private String buildJsonPayload() {
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

    // --- MQTT helpers (added) ---
    private void ensureMqttConnectionIfNeeded() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttConnected = true;
                // Ensure subscription state matches
                if (mqttType == MqttType.SUBSCRIBE && url != null && !url.isBlank()) {
                    // subscribe to topic (idempotent)
                    mqttClient.subscribe(url, (topic, msg) -> {
                        String payload = new String(msg.getPayload());
                        handleMqttMessage(topic, payload);
                    });
                }
                return;
            }

            // Create & connect
            String clientId = "mc-" + uniqueId.toString();
            mqttClient = new MqttClient(MQTT_BROKER, clientId);
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setAutomaticReconnect(true);
            opts.setCleanSession(true);
            mqttClient.connect(opts);
            mqttConnected = true;
            System.out.println("[MQTT] Connected to " + MQTT_BROKER);

            // Subscribe if necessary
            if (mqttType == MqttType.SUBSCRIBE && url != null && !url.isBlank()) {
                mqttClient.subscribe(url, (topic, msg) -> {
                    String payload = new String(msg.getPayload());
                    handleMqttMessage(topic, payload);
                });
                System.out.println("[MQTT] Subscribed to topic: " + url);
            }
        } catch (Exception e) {
            mqttConnected = false;
            System.out.println("[MQTT] Connection/subscribe failed: " + e);
        }
    }

    private void mqttPublish(String json) {
        try {
            if (!mqttConnected || mqttClient == null) {
                // Try to connect quickly and then publish
                ensureMqttConnectionIfNeeded();
            }
            if (mqttClient != null && mqttClient.isConnected() && url != null && !url.isBlank()) {
                MqttMessage m = new MqttMessage(json.getBytes());
                m.setQos(0);
                m.setRetained(false);
                mqttClient.publish(url, m);
                System.out.println("[MQTT] Published to " + url + ": " + json);
            } else {
                System.out.println("[MQTT] Not connected or invalid topic, cannot publish");
            }
        } catch (Exception e) {
            System.out.println("[MQTT] Publish failed: " + e);
        }
    }

    private void closeMqtt() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            if (mqttClient != null) {
                mqttClient.close();
            }
        } catch (Exception e) {
            System.out.println("[MQTT] Cleanup failed: " + e);
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        closeMqtt();
    }

    @Override
    public void cancelRemoval() {
        super.cancelRemoval();
    }


}