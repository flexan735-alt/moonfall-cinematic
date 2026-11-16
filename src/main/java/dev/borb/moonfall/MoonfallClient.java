package dev.borb.moonfall;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class MoonfallClient implements ClientModInitializer {
    private static final Identifier HUD_ID = Identifier.of("moonfall_cinematic", "cinematic");
    private static final int DURATION_TICKS = 20 * 18;
    private static int elapsed = -1;
    private static KeyBinding triggerKey;

    @Override
    public void onInitializeClient() {
        triggerKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moonfall_cinematic.play",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KeyBinding.Category.MISC
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("moonfall")
                        .executes(context -> {
                            startCinematic();
                            return 1;
                        })));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (triggerKey.wasPressed()) {
                startCinematic();
            }
            if (elapsed >= 0) {
                elapsed++;
                if (elapsed >= DURATION_TICKS) {
                    elapsed = -1;
                }
            }
        });

        HudElementRegistry.addLast(HUD_ID, (drawContext, tickCounter) -> render(drawContext));
    }

    private static void startCinematic() {
        elapsed = 0;
    }

    private static void render(DrawContext context) {
        if (elapsed < 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float progress = elapsed / (float) DURATION_TICKS;
        float seconds = elapsed / 20.0f;

        int fadeIn = Math.min(255, elapsed * 12);
        int fadeOut = Math.min(255, Math.max(0, (DURATION_TICKS - elapsed) * 10));
        int alpha = Math.min(fadeIn, fadeOut);

        // Cinematic letterbox and darkened sky.
        int shade = (alpha * 105 / 255) << 24;
        context.fill(0, 0, width, height, shade | 0x050914);
        int barHeight = Math.max(12, height / 9);
        context.fill(0, 0, width, barHeight, 0xFF000000);
        context.fill(0, height - barHeight, width, height, 0xFF000000);

        // Star field.
        for (int i = 0; i < 42; i++) {
            int x = (i * 197 + 53) % Math.max(1, width);
            int y = (i * 83 + 29) % Math.max(1, height - barHeight * 2) + barHeight;
            int twinkle = 120 + (int) (Math.sin(seconds * 2.0 + i) * 70 + 70);
            context.fill(x, y, x + 2, y + 2, (Math.min(255, twinkle) << 24) | 0x00DDEEFF);
        }

        int centerX = width / 2;
        int centerY = height / 2;
        int moonSize = Math.max(22, Math.min(width, height) / 7);
        float descent = clamp((progress - 0.12f) / 0.55f);
        int moonY = (int) (-moonSize + (height * 0.57f) * descent);
        int moonX = centerX + (int) (Math.sin(seconds * 0.35f) * width * 0.08f);

        // Moon body and soft halo, drawn as layered rectangles for broad compatibility.
        int halo = moonSize + moonSize / 2;
        context.fill(moonX - halo, moonY - halo, moonX + halo, moonY + halo, 0x223F8DFF);
        context.fill(moonX - moonSize, moonY - moonSize, moonX + moonSize, moonY + moonSize, 0xFFE2E5E8);
        context.fill(moonX - moonSize / 2, moonY - moonSize / 2, moonX + moonSize / 3, moonY + moonSize / 3, 0xFFB9C0C8);
        context.fill(moonX - moonSize / 3, moonY - moonSize / 2, moonX - moonSize / 6, moonY - moonSize / 3, 0xFF9EA7B1);

        // Bright impact flash followed by expanding concentric visual shockwave rings.
        float impact = clamp((progress - 0.69f) / 0.08f);
        if (impact > 0.0f) {
            int flashAlpha = (int) (Math.max(0, 1.0f - (progress - 0.69f) * 5.5f) * 170);
            context.fill(0, barHeight, width, height - barHeight, (flashAlpha << 24) | 0x00D9F4FF);
            float wave = clamp((progress - 0.72f) / 0.24f);
            int radius = (int) (wave * Math.max(width, height) * 0.85f);
            drawRing(context, centerX, centerY, radius, 3, 0xBBDDF7FF);
            drawRing(context, centerX, centerY, (int) (radius * 0.72f), 2, 0x66FFFFFF);
        }

        String title;
        if (progress < 0.18f) title = "SOMETHING IS APPROACHING";
        else if (progress < 0.48f) title = "THE MOON IS FALLING";
        else if (progress < 0.70f) title = "IMPACT IMMINENT";
        else if (progress < 0.91f) title = "MOONFALL";
        else title = "THE END";

        int textAlpha = Math.min(255, alpha);
        int color = (textAlpha << 24) | 0x00FFFFFF;
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(title),
                centerX, height - barHeight + barHeight / 2 - 4, color);

        if (progress > 0.92f) {
            int endAlpha = (int) (clamp((progress - 0.92f) / 0.08f) * 255);
            context.fill(0, 0, width, height, (endAlpha << 24) | 0x00000000);
        }
    }

    private static void drawRing(DrawContext context, int cx, int cy, int radius, int thickness, int color) {
        if (radius <= 0) return;
        context.fill(cx - radius, cy - radius, cx + radius, cy - radius + thickness, color);
        context.fill(cx - radius, cy + radius - thickness, cx + radius, cy + radius, color);
        context.fill(cx - radius, cy - radius, cx - radius + thickness, cy + radius, color);
        context.fill(cx + radius - thickness, cy - radius, cx + radius, cy + radius, color);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
