package com.diablo931.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

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

    public static void captureAndSend(String webhook) {
        captureScreenshot(file -> {
            System.out.println("Uploading screenshot to: " + webhook);
            sendToWebhook(file, webhook);
        });
    }

    private static void sendToWebhook(File file, String webhookUrl) {
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
            writer.append("📸 Minecraft Camera Screenshot").append("\r\n");

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
