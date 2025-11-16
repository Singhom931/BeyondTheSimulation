package com.diablo931.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ScreenshotUtil {

    public static void captureScreenshot(java.util.function.Consumer<File> callback) {
        System.out.println("capturing screenshot");
        MinecraftClient mc = MinecraftClient.getInstance();

        ScreenshotRecorder.saveScreenshot(
                mc.runDirectory,
                "camera",
                mc.getFramebuffer(),
                (text) -> {

                    File folder = new File(mc.runDirectory, "screenshots");
                    File[] files = folder.listFiles();
                    if (files == null || files.length == 0) return;

                    // newest file
                    File newest = files[0];
                    for (File f : files) {
                        if (f.lastModified() > newest.lastModified())
                            newest = f;
                    }

                    callback.accept(newest);
                }
        );
    }

    // Helper method to get targeted block position using client's hit result
    private static String getTargetedBlockPos(MinecraftClient mc) {
        // Fallback check to existing target is removed as it's reach-limited.
        // We now force a long-distance manual raycast.

        if (mc.player != null && mc.world != null) {
            // Use a very long distance (e.g., 1024 blocks) for "any" distance check
            float reachDistance = 1024.0f;

            Vec3d cameraPos = mc.player.getCameraPosVec(1.0f);
            Vec3d rotationVec = mc.player.getRotationVec(1.0f);
            Vec3d endVec = cameraPos.add(rotationVec.x * reachDistance, rotationVec.y * reachDistance, rotationVec.z * reachDistance);

            BlockHitResult newHit = mc.world.raycast(new RaycastContext(
                    cameraPos,
                    endVec,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    mc.player
            ));

            if (newHit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = newHit.getBlockPos();
                return String.format("%d, %d, %d", pos.getX(), pos.getY(), pos.getZ());
            }

            if (newHit.getType() == HitResult.Type.MISS) {
                return "None (looking at air within 1024 blocks)";
            }
        }

        return "None (no target data available)";
    }

    public static void captureAndSend(String webhook) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PlayerEntity player = mc.player;
        String playerName = (player != null) ? player.getName().getString() : "Unknown Player";
        String playerPos = (player != null) ? String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ()) : "N/A";
        String targetedBlockPos = getTargetedBlockPos(mc);

        captureScreenshot(file -> {
            System.out.println("Uploading screenshot to: " + webhook);
            sendToWebhook(file, webhook, playerName, playerPos, targetedBlockPos);
        });
    }

    private static void sendToWebhook(File file, String webhookUrl, String playerName, String playerPos, String targetedBlockPos) {
        String boundary = "----MinecraftScreenshotBoundary";

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(webhookUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream out = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true);

            // TEXT MESSAGE PART
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"content\"\r\n\r\n");
            writer.append("> **Player:** ").append(playerName).append("\n");
            writer.append("> **Photo Clicked at:** ").append(playerPos).append("\n");
            writer.append("> **Looking at :** ").append(targetedBlockPos).append("\r\n");

            // FILE PART
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"capture.png\"\r\n");
            writer.append("Content-Type: image/png\r\n\r\n");
            writer.flush();

            // Write file bytes
            FileInputStream fis = new FileInputStream(file);
            fis.transferTo(out);
            out.flush();
            fis.close();

            writer.append("\r\n");
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();

            int code = conn.getResponseCode();
            System.out.println("Webhook response code = " + code);

            // Print error response if any
            if (code >= 400) {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    System.out.println("Error Response:");
                    reader.lines().forEach(System.out::println);
                }
            }

        } catch (Exception e) {
            System.out.println("UPLOAD ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
