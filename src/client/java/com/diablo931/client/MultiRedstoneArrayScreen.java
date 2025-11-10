package com.diablo931.client;

import com.diablo931.block.MultiRedstoneArrayBlockEntity;
import com.diablo931.network.C2SUpdateUrlPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
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

    private BlockPos pos;
    private MultiRedstoneArrayBlockEntity be;
    private TextFieldWidget urlField;
    private ButtonWidget applyButton;
    private ButtonWidget modeButton;


    public MultiRedstoneArrayScreen(MultiRedstoneArrayScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176; // standard container width
        this.backgroundHeight = 166; // standard container height
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (urlField.keyPressed(keyCode, scanCode, modifiers) || urlField.isActive()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (urlField.charTyped(chr, keyCode)) return true;
        return super.charTyped(chr, keyCode);
    }

    @Override
    protected void init() {
        // Get the last clicked block
        super.init();

        pos = LastClickedBlockTracker.getLastClickedPos();
        if (MinecraftClient.getInstance().world != null) {
            BlockEntity entity = MinecraftClient.getInstance().world.getBlockEntity(pos);
            if (entity instanceof MultiRedstoneArrayBlockEntity mbe) {
                this.be = mbe;
            }
        }

        String urlText = be != null ? be.getUrl() : "";
        this.urlField = new TextFieldWidget(textRenderer, x + 10, y + 20, 150, 20, Text.literal("Enter URL"));
        this.urlField.setText(urlText);
        addSelectableChild(this.urlField);

        // Apply button
        applyButton = ButtonWidget.builder(Text.literal("Apply"), button -> {
            if (be != null) {
                be.setUrl(urlField.getText());

                // Send to server
                C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlField.getText(), be.getMode());
                ClientPlayNetworking.send(payload);
            }
        }).dimensions(x + 10, y + 45, 50, 20).build();
        addDrawableChild(applyButton);

        // Mode toggle button
        modeButton = ButtonWidget.builder(Text.literal(be != null ? be.getMode().name() : "HTTP"), button -> {
            if (be != null) {
                if (be.getMode() == MultiRedstoneArrayBlockEntity.Mode.HTTP) {
                    be.setMode(MultiRedstoneArrayBlockEntity.Mode.WEB_STOCK);
                } else {
                    be.setMode(MultiRedstoneArrayBlockEntity.Mode.HTTP);
                }

                // Update button label
                button.setMessage(Text.literal(be.getMode().name()));

                // Send mode change to server
                C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlField.getText(), be.getMode());
                ClientPlayNetworking.send(payload);
            }
        }).dimensions(x + 70, y + 45, 80, 20).build();
        addDrawableChild(modeButton);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawBackground(drawContext, delta, mouseX, mouseY);
        urlField.render(drawContext, mouseX, mouseY, delta);
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
            // Send final URL and mode to server
            C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlField.getText(), be.getMode());
            ClientPlayNetworking.send(payload);
        }
    }
}
