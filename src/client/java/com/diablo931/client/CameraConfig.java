package com.diablo931.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class CameraConfig {

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("camera.json");

    private static String webhook = "";

    public static String getWebhook() { return webhook; }
    public static void setWebhook(String url) {
        webhook = url;
        save();
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                JsonObject o = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
                webhook = o.get("webhook").getAsString();
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("webhook", webhook);
            Files.writeString(FILE, o.toString());
        } catch (Exception ignored) {}
    }
}
