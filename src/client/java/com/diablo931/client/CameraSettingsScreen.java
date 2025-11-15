package com.diablo931.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import com.diablo931.network.SetCameraWebhookPayload;


import java.util.function.Consumer;

public class CameraSettingsScreen extends Screen {

    private TextFieldWidget input;
    private final Consumer<String> onDone;

    protected CameraSettingsScreen(Consumer<String> onDone) {
        super(Text.literal("Camera Settings"));
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        int w = 200;
        int h = 20;
        input = new TextFieldWidget(textRenderer,
                (width - w) / 2, height / 2 - 10, w, h, Text.of("Webhook"));

        input.setMaxLength(Integer.MAX_VALUE);
        addDrawableChild(input);

        addDrawableChild(ButtonWidget.builder(Text.of("Save"), b -> {
            onDone.accept(input.getText());
            String hook = input.getText();

            // send to server
            ClientPlayNetworking.send(new SetCameraWebhookPayload(hook));

            // optional callback
            onDone.accept(hook);
            close();
        }).dimensions(width / 2 - 40, height / 2 + 20, 80, 20).build());
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }
}
