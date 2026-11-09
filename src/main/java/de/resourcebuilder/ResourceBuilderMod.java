package de.resourcebuilder;

import de.resourcebuilder.gui.TextureStudioScreen;
import de.resourcebuilder.pack.LivePackManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceBuilderMod implements ClientModInitializer {

    public static final String MOD_ID = "resourcebuilder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    private static KeyMapping openGuiKey;
    private static int hotkeyCode = GLFW.GLFW_KEY_R;
    private static final Path HOTKEY_CONFIG = Paths.get("config", "resourcebuilder", "hotkey.txt");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Resource Builder (Texture Studio) initializing...");

        loadHotkeyCode();

        LivePackManager.getInstance().initPackFolder();
        LivePackManager.getInstance().scanAllPacks();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.resourcebuilder.open",
                InputConstants.Type.KEYSYM,
                hotkeyCode,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGuiKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new TextureStudioScreen());
                }
            }
        });

        // Register Chat Commands: /rb, /textures, /resourcebuilder, /studio
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("rb").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new TextureStudioScreen());
                });
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("textures").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new TextureStudioScreen());
                });
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("resourcebuilder").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new TextureStudioScreen());
                });
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("studio").executes(ctx -> {
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new TextureStudioScreen());
                });
                return 1;
            }));
        });

        LOGGER.info("Resource Builder initialized successfully.");
    }

    public static int getHotkeyCode() {
        return hotkeyCode;
    }

    public static void setHotkeyCode(int key) {
        hotkeyCode = key;
        if (openGuiKey != null) {
            openGuiKey.setKey(InputConstants.Type.KEYSYM.getOrCreate(key));
            KeyMapping.resetMapping();
        }
        try {
            Files.createDirectories(HOTKEY_CONFIG.getParent());
            Files.writeString(HOTKEY_CONFIG, String.valueOf(key));
        } catch (IOException ignored) {}
    }

    private static void loadHotkeyCode() {
        if (Files.isRegularFile(HOTKEY_CONFIG)) {
            try {
                String str = Files.readString(HOTKEY_CONFIG).trim();
                hotkeyCode = Integer.parseInt(str);
            } catch (Exception ignored) {}
        }
    }
}
