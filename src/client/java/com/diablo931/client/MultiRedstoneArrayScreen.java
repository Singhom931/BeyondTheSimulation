package com.diablo931.client;

import com.diablo931.block.MultiRedstoneArrayBlockEntity;
import com.diablo931.network.C2SUpdateUrlPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

//import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import com.diablo931.block.MultiRedstoneArrayScreenHandler;
import net.minecraft.util.Identifier;
import com.diablo931.beyondthesimulation;
import net.minecraft.util.math.BlockPos;

public class MultiRedstoneArrayScreen extends HandledScreen<MultiRedstoneArrayScreenHandler> {

    private TextFieldWidget urlField;
    private ButtonWidget applyButton;
    BlockPos pos = LastClickedBlockTracker.getLastClickedPos();
    MultiRedstoneArrayBlockEntity be =(MultiRedstoneArrayBlockEntity) MinecraftClient.getInstance().world.getBlockEntity(pos);


    public MultiRedstoneArrayScreen(MultiRedstoneArrayScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176; // standard container width
        this.backgroundHeight = 166; // standard container height
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.urlField.keyPressed(keyCode, scanCode, modifiers) || this.urlField.isActive()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (this.urlField.charTyped(chr, keyCode)) {
            return true;
        }
        return super.charTyped(chr, keyCode);
    }

    @Override
    protected void init() {
        super.init();

        pos = LastClickedBlockTracker.getLastClickedPos();
        be =(MultiRedstoneArrayBlockEntity) MinecraftClient.getInstance().world.getBlockEntity(pos);

        String urlText = "";

        if (be != null) {
            urlText = be.getUrl();
        }

        if (client != null && client.player != null) {
            Text msg = Text.literal("[DEBUG] Opening MultiRedstoneArrayScreen, URL: " + urlText);
            client.player.sendMessage(msg, false); // false = not a system message
        }

        this.urlField = new TextFieldWidget(textRenderer, x + 10, y + 20, 150, 20, Text.literal("Enter URL"));
        this.urlField.setText(urlText);
        addSelectableChild(this.urlField);

        applyButton = ButtonWidget.builder(Text.literal("Apply"), button -> {
                    if (be != null) {
                        be.setUrl(this.urlField.getText());
                        C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlField.getText());
                        ClientPlayNetworking.send(payload);
                    }
                })
                .dimensions(x + 10, y + 45, 50, 20) // x, y, width, height
                .build();
        addDrawableChild(applyButton);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawBackground(drawContext, delta, mouseX, mouseY);
        this.urlField.render(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
    }

//    private static final Identifier TEXTURE = Identifier.of( beyondthesimulation.MOD_ID, "textures/gui/multi_redstone_array.png");

    @Override
    protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
        //drawContext.fill(x, y, x + 176, y + 166, 0xFFAAAAAA); // light gray background
        //drawContext.drawTexture(RenderLayer::getGuiTextured, TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight );

    }

    @Override
    public void close() {
        super.close();
        if (be != null) {
            be.setUrl(this.urlField.getText());
        }
    }
}
