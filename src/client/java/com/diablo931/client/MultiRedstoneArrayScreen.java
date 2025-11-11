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
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class MultiRedstoneArrayScreen extends HandledScreen<MultiRedstoneArrayScreenHandler> {

    private BlockPos pos;
    private MultiRedstoneArrayBlockEntity be;

    private String urlText = "";
    private boolean editing = false;
    private long lastBlinkTime = 0;
    private boolean showCursor = true;

    private ButtonWidget applyButton;
    private ButtonWidget modeButton;

    public MultiRedstoneArrayScreen(MultiRedstoneArrayScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 80;
    }

    // Prevent player inventory and title from showing
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {}

    @Override
    protected void init() {
        super.init();

        // Hide title/inventory text
        this.titleX = -1000;
        this.playerInventoryTitleY = -1000;

        // Get block entity from world
        pos = LastClickedBlockTracker.getLastClickedPos();
        BlockEntity entity = MinecraftClient.getInstance().world.getBlockEntity(pos);
        if (entity instanceof MultiRedstoneArrayBlockEntity mbe) {
            this.be = mbe;
            urlText = mbe.getUrl();
        }

        // Apply button
        applyButton = ButtonWidget.builder(Text.literal("Apply"), button -> {
            if (be != null) {
                be.setUrl(urlText);
                C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlText, be.getMode());
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
                C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlText, be.getMode());
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
        // Main GUI box
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xAA000000);
        context.fill(x + 2, y + 2, x + backgroundWidth - 2, y + backgroundHeight - 2, 0xFF444444);

        // URL box
        int boxX = x + 10;
        int boxY = y + 20;
        int boxWidth = 156;
        int boxHeight = 20;

        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF222222);

        // Blinking cursor
        long time = System.currentTimeMillis();
        if (time - lastBlinkTime > 500) {
            showCursor = !showCursor;
            lastBlinkTime = time;
        }

        String displayText = urlText.isEmpty() && !editing ? "Click to edit URL" : urlText;
        int color = urlText.isEmpty() && !editing ? 0x777777 : 0xFFFFFF;

        context.drawTextWithShadow(textRenderer, displayText, boxX + 3, boxY + 6, color);

        if (editing && showCursor) {
            int textWidth = textRenderer.getWidth(displayText);
            context.fill(boxX + 3 + textWidth + 1, boxY + 5, boxX + 3 + textWidth + 2, boxY + 17, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int boxX = x + 10;
        int boxY = y + 20;
        int boxWidth = 156;
        int boxHeight = 20;

        if (mouseX >= boxX && mouseX <= boxX + boxWidth &&
                mouseY >= boxY && mouseY <= boxY + boxHeight) {
            editing = true;
            return true;
        } else {
            editing = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int keyCode) {
        if (editing && chr >= 32) {
            urlText += chr;
            return true;
        }
        return super.charTyped(chr, keyCode);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            // ✅ Handle Ctrl + V (paste)
            if ((modifiers & 2) != 0 && keyCode == 86) { // 2 = Ctrl, 86 = V
                String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                if (clipboard != null && !clipboard.isEmpty()) {
                    urlText += clipboard;
                }
                return true;
            }

            // ✅ Optional: Ctrl + C (copy current text)
            if ((modifiers & 2) != 0 && keyCode == 67) { // 67 = C
                MinecraftClient.getInstance().keyboard.setClipboard(urlText);
                return true;
            }

            switch (keyCode) {
                case 259 -> { // Backspace
                    if (!urlText.isEmpty()) urlText = urlText.substring(0, urlText.length() - 1);
                    return true;
                }
                case 257, 335 -> { // Enter
                    if (be != null) be.setUrl(urlText);
                    editing = false;
                    return true;
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        super.close();
        if (be != null) {
            C2SUpdateUrlPayload payload = new C2SUpdateUrlPayload(pos, urlText, be.getMode());
            ClientPlayNetworking.send(payload);
        }
    }
}
