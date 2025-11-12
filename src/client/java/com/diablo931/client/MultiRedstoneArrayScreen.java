package com.diablo931.client;

import com.diablo931.block.MultiRedstoneArray.MultiRedstoneArrayBlockEntity;
import com.diablo931.block.MultiRedstoneArray.MultiRedstoneArrayScreenHandler;
import com.diablo931.network.C2SUpdateUrlPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class MultiRedstoneArrayScreen extends HandledScreen<MultiRedstoneArrayScreenHandler> {

    private BlockPos pos;
    private MultiRedstoneArrayBlockEntity be;

    private TextFieldWidget urlField;
    private ButtonWidget applyButton;
    private ButtonWidget modeButton;

    public MultiRedstoneArrayScreen(MultiRedstoneArrayScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 80;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Do nothing: hide title and inventory
    }

    @Override
    protected void init() {
        super.init();

        // Hide title and inventory
        this.titleX = -1000;
        this.playerInventoryTitleY = -1000;

        // Get the last clicked block entity
        pos = LastClickedBlockTracker.getLastClickedPos();
        BlockEntity entity = MinecraftClient.getInstance().world.getBlockEntity(pos);
        if (entity instanceof MultiRedstoneArrayBlockEntity mbe) {
            this.be = mbe;
        }

        // Initialize TextFieldWidget
        urlField = new TextFieldWidget(textRenderer, x + 10, y + 20, 156, 20,
                Text.literal("Enter URL"));
        urlField.setText(be != null ? be.getUrl() : "");
        urlField.setEditable(true);
        urlField.setFocused(false); // focused when clicked
        addSelectableChild(urlField);

        // Apply button
        applyButton = ButtonWidget.builder(Text.literal("Apply"), button -> {
            if (be != null) {
                be.setUrl(urlField.getText());
                C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlField.getText(), be.getMode());
                ClientPlayNetworking.send(payload);
            }
        }).dimensions(x + 10, y + 50, 50, 20).build();
        addDrawableChild(applyButton);

        // Mode toggle button
        modeButton = ButtonWidget.builder(Text.literal(be != null ? be.getMode().name() : "HTTP"), button -> {
            if (be != null) {
                be.setMode(be.getMode() == MultiRedstoneArrayBlockEntity.Mode.HTTP
                        ? MultiRedstoneArrayBlockEntity.Mode.WEB_STOCK
                        : MultiRedstoneArrayBlockEntity.Mode.HTTP);
                button.setMessage(Text.literal(be.getMode().name()));

                // Send update to server
                C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlField.getText(), be.getMode());
                ClientPlayNetworking.send(payload);
            }
        }).dimensions(x + 70, y + 50, 80, 20).build();
        addDrawableChild(modeButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        drawBackground(context, delta, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // GUI background
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xAA000000);
        context.fill(x + 2, y + 2, x + backgroundWidth - 2, y + backgroundHeight - 2, 0xFF444444);

        // URL box background
        context.fill(x + 10, y + 20, x + 166, y + 40, 0xFF222222);

        // Render the TextFieldWidget (handles blinking cursor, selection, typing)
        urlField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean clicked = urlField.mouseClicked(mouseX, mouseY, button);
        urlField.setFocused(clicked); // Focus when clicked
        return clicked || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (urlField.charTyped(chr, keyCode)) return true;
        return super.charTyped(chr, keyCode);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Let TextFieldWidget handle movement, deletion, typing, etc.
        if (urlField.keyPressed(keyCode, scanCode, modifiers)) return true;

        // Optional: Ctrl+V (paste)
        if ((modifiers & 2) != 0 && keyCode == 86) {
            String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clipboard != null) urlField.setText(urlField.getText() + clipboard);
            return true;
        }

        // Optional: Ctrl+C (copy)
        if ((modifiers & 2) != 0 && keyCode == 67) {
            MinecraftClient.getInstance().keyboard.setClipboard(urlField.getText());
            return true;
        }

        // Enter key to finish editing
        if (keyCode == 257 || keyCode == 335) {
            if (be != null) be.setUrl(urlField.getText());
            urlField.setFocused(false);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        super.close();
        if (be != null) {
            C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlField.getText(), be.getMode());
            ClientPlayNetworking.send(payload);
        }
    }
}
