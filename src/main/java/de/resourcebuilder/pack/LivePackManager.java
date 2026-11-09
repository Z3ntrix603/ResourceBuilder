package de.resourcebuilder.pack;

import de.resourcebuilder.ResourceBuilderMod;
import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.preset.Preset;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LivePackManager {

    public static final String PACK_FOLDER_NAME = "ResourceBuilder_Active";
    public static final String PACK_ID = "file/" + PACK_FOLDER_NAME;

    private static LivePackManager instance;

    private final Map<PvPItem, List<TextureVariant>> availableVariants = new EnumMap<>(PvPItem.class);
    private final Map<PvPItem, TextureVariant> activeSelections = new EnumMap<>(PvPItem.class);

    private final Path configFile = Paths.get("config", "resourcebuilder", "active_mix.json");

    private LivePackManager() {
        for (PvPItem item : PvPItem.values()) {
            List<TextureVariant> list = new ArrayList<>();
            list.add(TextureVariant.createDefault());
            availableVariants.put(item, list);
        }
    }

    public static synchronized LivePackManager getInstance() {
        if (instance == null) {
            instance = new LivePackManager();
        }
        return instance;
    }

    public Path getPackDir() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return Paths.get("resourcepacks", PACK_FOLDER_NAME);
        Path rpDir = client.getResourcePackDirectory();
        if (rpDir == null) rpDir = Paths.get("resourcepacks");
        return rpDir.resolve(PACK_FOLDER_NAME);
    }

    public void initPackFolder() {
        try {
            Path dir = getPackDir();
            Files.createDirectories(dir);
            Path mcmeta = dir.resolve("pack.mcmeta");
            if (!Files.exists(mcmeta)) {
                String metaContent = "{\n  \"pack\": {\n    \"pack_format\": 34,\n    \"description\": \"Resource Builder Active Mix (Live)\"\n  }\n}";
                Files.writeString(mcmeta, metaContent, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            ResourceBuilderMod.LOGGER.error("Failed to initialize active pack folder: {}", e.getMessage());
        }
    }

    public void scanAllPacks() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        Path rpDir = client.getResourcePackDirectory();
        if (rpDir == null || !Files.isDirectory(rpDir)) return;

        ResourceBuilderMod.LOGGER.info("Scanning all resource packs for PvP textures...");

        for (PvPItem item : PvPItem.values()) {
            List<TextureVariant> list = availableVariants.get(item);
            list.clear();
            list.add(TextureVariant.createDefault());
        }

        try (var stream = Files.list(rpDir)) {
            stream.forEach(path -> {
                String fileName = path.getFileName().toString();
                if (fileName.equals(PACK_FOLDER_NAME)) return;

                String packName = cleanPackName(fileName);

                if (Files.isRegularFile(path) && fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                    scanZip(path, packName);
                } else if (Files.isDirectory(path)) {
                    scanDirectory(path, packName);
                }
            });
        } catch (IOException e) {
            ResourceBuilderMod.LOGGER.error("Failed to scan resource packs: {}", e.getMessage());
        }

        loadActiveMix();
        ResourceBuilderMod.LOGGER.info("Resource packs scanned successfully.");
    }

    private String cleanPackName(String fileName) {
        String name = fileName;
        if (name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        name = name.replaceAll("^[!#\\s]+", "").trim();
        name = name.replaceAll("§[0-9a-fk-or]", "").trim();
        if (name.isEmpty()) name = fileName;
        return name;
    }

    private void scanZip(Path zipPath, String packName) {
        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            scanZipEntries(zip, packName);
        } catch (Exception e1) {
            try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.ISO_8859_1)) {
                scanZipEntries(zip, packName);
            } catch (Exception ignored) {}
        }
    }

    private void scanZipEntries(ZipFile zip, String packName) {
        Map<PvPItem, Map<String, byte[]>> collectedFiles = new EnumMap<>(PvPItem.class);
        byte[] packIcon = null;

        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            String path = entry.getName().toLowerCase(Locale.ROOT).replace('\\', '/');

            if (path.equals("pack.png") || path.endsWith("/pack.png")) {
                try {
                    packIcon = zip.getInputStream(entry).readAllBytes();
                } catch (IOException ignored) {}
            }

            for (PvPItem item : PvPItem.values()) {
                if (matchesPath(item, path)) {
                    try {
                        byte[] bytes = zip.getInputStream(entry).readAllBytes();
                        if (bytes.length > 0) {
                            String relKey = normalizeAssetPath(path);
                            collectedFiles.computeIfAbsent(item, k -> new HashMap<>()).put(relKey, bytes);
                        }
                    } catch (IOException ignored) {}
                }
            }
        }

        assembleVariants(packName, collectedFiles, packIcon);
    }

    private void scanDirectory(Path root, String packName) {
        Map<PvPItem, Map<String, byte[]>> collectedFiles = new EnumMap<>(PvPItem.class);
        byte[] packIcon = null;

        Path packIconPath = root.resolve("pack.png");
        if (Files.isRegularFile(packIconPath)) {
            try { packIcon = Files.readAllBytes(packIconPath); } catch (IOException ignored) {}
        }

        try (var walker = Files.walk(root)) {
            walker.filter(Files::isRegularFile).forEach(file -> {
                String path = root.relativize(file).toString().toLowerCase(Locale.ROOT).replace('\\', '/');

                for (PvPItem item : PvPItem.values()) {
                    if (matchesPath(item, path)) {
                        try {
                            byte[] bytes = Files.readAllBytes(file);
                            if (bytes.length > 0) {
                                String relKey = normalizeAssetPath(path);
                                collectedFiles.computeIfAbsent(item, k -> new HashMap<>()).put(relKey, bytes);
                            }
                        } catch (IOException ignored) {}
                    }
                }
            });
        } catch (IOException ignored) {}

        assembleVariants(packName, collectedFiles, packIcon);
    }

    private String normalizeAssetPath(String fullPath) {
        int idx = fullPath.indexOf("assets/");
        if (idx >= 0) {
            return fullPath.substring(idx);
        }
        int mcIdx = fullPath.indexOf("minecraft/");
        if (mcIdx >= 0) {
            return "assets/" + fullPath.substring(mcIdx);
        }
        if (fullPath.startsWith("textures/") || fullPath.startsWith("particles/") || fullPath.startsWith("models/") || fullPath.startsWith("sounds/")) {
            return "assets/minecraft/" + fullPath;
        }
        return "assets/minecraft/" + fullPath;
    }

    private void assembleVariants(String packName, Map<PvPItem, Map<String, byte[]>> collectedFiles, byte[] packIcon) {
        for (Map.Entry<PvPItem, Map<String, byte[]>> entry : collectedFiles.entrySet()) {
            PvPItem item = entry.getKey();
            Map<String, byte[]> files = entry.getValue();

            if (files.isEmpty()) continue;

            if (item.isSound()) {
                addVariantIfAbsent(item, packName, packIcon, null, files);
                continue;
            }

            byte[] previewPng = null;
            byte[] previewMcmeta = null;

            if (item == PvPItem.CRYSTALS) {
                // Must prefer 2D item icon for slot preview
                previewPng = findFile(files, "item/end_crystal.png");
                if (previewPng == null) previewPng = findAnyVisiblePng(files);
            } else if (item == PvPItem.RESPAWN_ANCHOR) {
                previewPng = findFile(files, "respawn_anchor_top_off.png");
                if (previewPng == null) previewPng = findFile(files, "respawn_anchor_top.png");
                if (previewPng == null) previewPng = findAnyVisiblePng(files);
            } else if (item == PvPItem.TNT) {
                previewPng = findFile(files, "tnt_side.png");
                if (previewPng == null) previewPng = findAnyVisiblePng(files);
            } else if (item == PvPItem.FIRE) {
                previewPng = findVisibleFile(files, "fire_0.png", "fire_1.png", "fire_layer_0.png", "fire_layer_1.png", "soul_fire_0.png", "soul_fire_1.png", "fire.png");
                if (previewPng == null) previewPng = findAnyVisiblePng(files);
            } else if (item == PvPItem.DIAMOND_ARMOR) {
                previewPng = findVisibleFile(files, "item/diamond_chestplate.png", "item/diamond_helmet.png");
            } else if (item == PvPItem.NETHERITE_ARMOR) {
                previewPng = findVisibleFile(files, "item/netherite_chestplate.png", "item/netherite_helmet.png");
            } else if (item == PvPItem.CRIT_PARTICLES) {
                previewPng = findVisibleFile(files, "critical_hit.png", "crit.png", "enchanted_hit.png", "damage.png");
            } else if (item == PvPItem.CRYSTAL_EXPLOSION) {
                previewPng = findVisibleFile(files, "explosion_5.png", "explosion_4.png", "explosion_6.png", "explosion_3.png", "flash.png", "explosion.png", "explosion_2.png", "explosion_1.png");
            } else if (item == PvPItem.TOTEM_POPS) {
                previewPng = findVisibleFile(files, "glitter_5.png", "glitter_4.png", "glitter_6.png", "glitter_7.png", "glitter_3.png", "glitter_2.png", "glitter_1.png");
            } else {
                previewPng = findAnyVisiblePng(files);
            }

            if (previewPng == null || !TextureVariant.isImageVisible(previewPng)) {
                previewPng = findAnyVisiblePng(files);
            }

            // Strictly filter out non-visible/empty slots
            if (previewPng != null && TextureVariant.isImageVisible(previewPng)) {
                addVariantIfAbsent(item, packName, previewPng, previewMcmeta, files);
            }
        }
    }

    private byte[] findVisibleFile(Map<String, byte[]> files, String... suffixes) {
        for (String suffix : suffixes) {
            String lowerSuffix = suffix.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                if (e.getKey().toLowerCase(Locale.ROOT).endsWith(lowerSuffix)) {
                    byte[] data = e.getValue();
                    if (TextureVariant.isImageVisible(data)) {
                        return data;
                    }
                }
            }
        }
        return null;
    }

    private byte[] findAnyVisiblePng(Map<String, byte[]> files) {
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            if (e.getKey().toLowerCase(Locale.ROOT).endsWith(".png")) {
                byte[] data = e.getValue();
                if (TextureVariant.isImageVisible(data)) {
                    return data;
                }
            }
        }
        return null;
    }

    private byte[] findFile(Map<String, byte[]> files, String fileNameSuffix) {
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            if (e.getKey().toLowerCase(Locale.ROOT).endsWith(fileNameSuffix.toLowerCase(Locale.ROOT))) {
                return e.getValue();
            }
        }
        return null;
    }

    private boolean matchesPath(PvPItem item, String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);

        boolean isItem = path.contains("/item/") || path.contains("/items/");
        boolean isBlock = path.contains("/block/") || path.contains("/blocks/");
        boolean isParticle = path.contains("/particle/") || path.contains("/particles/");
        boolean isArmorModel = path.contains("/models/armor/") || path.contains("/entity/equipment/");

        return switch (item) {
            case CRYSTALS -> (isItem && (fileName.equals("end_crystal.png") || fileName.equals("endercrystal.png")))
                    || ((path.contains("end_crystal") || path.contains("endercrystal")) && (fileName.equals("end_crystal.png") || fileName.equals("endercrystal.png") || fileName.equals("end_crystal_beam.png") || fileName.equals("endercrystal_beam.png")));
            case RESPAWN_ANCHOR -> isBlock && (fileName.startsWith("respawn_anchor") || fileName.contains("respawn_anchor"));
            case GLOWSTONE -> isBlock && fileName.equals("glowstone.png");
            case OBSIDIAN -> isBlock && fileName.equals("obsidian.png") && !fileName.contains("crying");
            case CRYING_OBSIDIAN -> isBlock && fileName.equals("crying_obsidian.png");
            case TOTEM -> isItem && (fileName.equals("totem_of_undying.png") || fileName.equals("totem.png"));
            case GOLDEN_APPLE -> isItem && fileName.equals("golden_apple.png") && !path.contains("enchanted");
            case ENCHANTED_GOLDEN_APPLE -> isItem && fileName.equals("enchanted_golden_apple.png");
            case ENDER_PEARL -> isItem && fileName.equals("ender_pearl.png");
            case COBWEB -> isBlock && fileName.equals("cobweb.png");
            case TNT -> isBlock && (fileName.startsWith("tnt") || fileName.contains("tnt"));
            case FIRE -> (isBlock || path.contains("/blocks/") || !path.contains("/")) && (
                    fileName.startsWith("fire_0") ||
                    fileName.startsWith("fire_1") ||
                    fileName.startsWith("fire_layer_0") ||
                    fileName.startsWith("fire_layer_1") ||
                    fileName.startsWith("soul_fire_0") ||
                    fileName.startsWith("soul_fire_1") ||
                    fileName.startsWith("soul_fire_layer_0") ||
                    fileName.startsWith("soul_fire_layer_1") ||
                    fileName.equals("fire.png") ||
                    fileName.equals("fire.png.mcmeta")
            );
            case WIND_CHARGE -> (isItem && fileName.equals("wind_charge.png")) || (path.contains("projectiles") && fileName.equals("wind_charge.png"));
            case FIREWORK_ROCKET -> isItem && fileName.equals("firework_rocket.png");
            case SHIELD -> (isItem && (fileName.equals("shield.png") || fileName.equals("shield_item.png")))
                    || (path.contains("/entity/") && fileName.contains("shield"));
            case DIAMOND_SWORD -> isItem && fileName.equals("diamond_sword.png");
            case NETHERITE_SWORD -> isItem && fileName.equals("netherite_sword.png");
            case MACE -> isItem && fileName.equals("mace.png");
            case DIAMOND_AXE -> isItem && fileName.equals("diamond_axe.png");
            case NETHERITE_AXE -> isItem && fileName.equals("netherite_axe.png");
            case DIAMOND_PICKAXE -> isItem && fileName.equals("diamond_pickaxe.png");
            case NETHERITE_PICKAXE -> isItem && fileName.equals("netherite_pickaxe.png");
            case BOW -> isItem && (fileName.equals("bow.png") || fileName.equals("bow_standby.png"));
            case CROSSBOW -> isItem && (fileName.equals("crossbow_standby.png") || fileName.equals("crossbow.png"));
            case POTION -> isItem && fileName.equals("potion.png");
            case SPLASH_POTION -> isItem && fileName.equals("splash_potion.png");
            case DIAMOND_ARMOR -> (isItem && (fileName.equals("diamond_helmet.png") || fileName.equals("diamond_chestplate.png") || fileName.equals("diamond_leggings.png") || fileName.equals("diamond_boots.png")))
                    || (isArmorModel && fileName.contains("diamond"));
            case NETHERITE_ARMOR -> (isItem && (fileName.equals("netherite_helmet.png") || fileName.equals("netherite_chestplate.png") || fileName.equals("netherite_leggings.png") || fileName.equals("netherite_boots.png")))
                    || (isArmorModel && fileName.contains("netherite"));
            case ELYTRA -> isItem && fileName.equals("elytra.png");
            case CRIT_PARTICLES -> isParticle && (fileName.equals("critical_hit.png") || fileName.equals("crit.png") || fileName.equals("enchanted_hit.png") || fileName.equals("crit.json") || fileName.equals("enchanted_hit.json") || fileName.equals("damage.png"));
            case CRYSTAL_EXPLOSION -> isParticle && (fileName.startsWith("explosion_") || fileName.equals("flash.png") || fileName.equals("explosion.png") || fileName.equals("explosion.json") || fileName.equals("explosion_emitter.json") || fileName.equals("flash.json"));
            case TOTEM_POPS -> isParticle && (fileName.startsWith("glitter_") || fileName.equals("totem_of_undying.json"));
            case SOUND_MACE -> path.endsWith(".ogg") && (path.contains("mace") || path.contains("smash_ground") || path.contains("smash_air"));
            case SOUND_CRYSTAL -> path.endsWith(".ogg") && (path.contains("explode") || path.contains("explosion")) && (path.contains("sound") || path.contains("random/") || path.contains("generic/"));
            case SOUND_SWORD -> path.endsWith(".ogg") && (path.contains("/sounds/entity/player/attack/") || path.contains("/sounds/damage/") || path.contains("successful_hit") || (path.contains("crit") && path.contains("sound") && !path.contains("mace")));
            case SOUND_EAT -> path.endsWith(".ogg") && (path.contains("eat") || path.contains("burp")) && (path.contains("sound") || path.contains("random/") || path.contains("generic/"));
            case SOUND_TOTEM -> path.endsWith(".ogg") && path.contains("totem") && (path.contains("sound") || path.contains("custom"));
        };
    }

    private void addVariantIfAbsent(PvPItem item, String packName, byte[] pngBytes, byte[] mcmetaBytes, Map<String, byte[]> additionalFiles) {
        List<TextureVariant> list = availableVariants.get(item);
        for (TextureVariant v : list) {
            if (!v.isDefault() && v.getPackName().equals(packName)) {
                return;
            }
        }
        list.add(new TextureVariant(packName, pngBytes, mcmetaBytes, additionalFiles, false));
    }

    public List<TextureVariant> getVariants(PvPItem item) {
        List<TextureVariant> list = availableVariants.getOrDefault(item, Collections.emptyList());
        if (list.isEmpty()) return list;
        return list.stream().filter(TextureVariant::isValid).toList();
    }

    public TextureVariant getActiveSelection(PvPItem item) {
        return activeSelections.getOrDefault(item, availableVariants.get(item).get(0));
    }

    public void selectVariant(PvPItem item, TextureVariant variant, boolean triggerReload) {
        if (variant == null || variant.isDefault()) {
            activeSelections.remove(item);
            removeTextureFromLivePack(item);
        } else {
            removeTextureFromLivePack(item);
            activeSelections.put(item, variant);
            writeTextureToLivePack(item, variant);
        }
        saveActiveMix();

        if (triggerReload) {
            ensurePackEnabledAndReload();
        }
    }

    public void applyPreset(Preset preset) {
        activeSelections.clear();
        clearLivePackTextures();

        for (PvPItem item : PvPItem.values()) {
            String selectedPack = preset.getSelection(item);
            if (selectedPack != null) {
                List<TextureVariant> variants = getVariants(item);
                for (TextureVariant v : variants) {
                    if (v.getPackName().equalsIgnoreCase(selectedPack)) {
                        activeSelections.put(item, v);
                        writeTextureToLivePack(item, v);
                        break;
                    }
                }
            }
        }
        saveActiveMix();
        ensurePackEnabledAndReload();
    }

    public void resetAll() {
        activeSelections.clear();
        clearLivePackTextures();
        saveActiveMix();
        ensurePackEnabledAndReload();
    }

    private void writeTextureToLivePack(PvPItem item, TextureVariant variant) {
        if (variant == null) return;
        try {
            Path root = getPackDir();

            // Write all collected files belonging to this variant
            Map<String, byte[]> add = variant.getAdditionalFiles();
            for (Map.Entry<String, byte[]> e : add.entrySet()) {
                String relPath = e.getKey();
                byte[] data = e.getValue();
                if (data == null || data.length == 0) continue;

                Path target = root.resolve(relPath);
                Files.createDirectories(target.getParent());
                Files.write(target, data);

                // Auto-generate animation mcmeta if animated strip without mcmeta
                if (relPath.endsWith(".png") && TextureVariant.isPngAnimated(data)) {
                    Path mcmetaTarget = root.resolve(relPath + ".mcmeta");
                    if (!Files.exists(mcmetaTarget)) {
                        Files.writeString(mcmetaTarget, "{\"animation\":{\"frametime\":2,\"interpolate\":true}}", StandardCharsets.UTF_8);
                    }
                }

                // If writing particle texture, ensure both critical_hit and crit are available
                if (relPath.endsWith("critical_hit.png")) {
                    Path alias = target.resolveSibling("crit.png");
                    if (!Files.exists(alias)) Files.write(alias, data);
                } else if (relPath.endsWith("crit.png") && !relPath.contains("critical_hit")) {
                    Path alias = target.resolveSibling("critical_hit.png");
                    if (!Files.exists(alias)) Files.write(alias, data);
                }

                // If writing armor layer 1 or 2, ensure both legacy and modern paths are populated
                if (relPath.contains("diamond_layer_1.png")) {
                    Path modern = root.resolve("assets/minecraft/textures/entity/equipment/humanoid/diamond.png");
                    Files.createDirectories(modern.getParent());
                    Files.write(modern, data);
                } else if (relPath.contains("diamond_layer_2.png")) {
                    Path modern = root.resolve("assets/minecraft/textures/entity/equipment/humanoid_leggings/diamond.png");
                    Files.createDirectories(modern.getParent());
                    Files.write(modern, data);
                } else if (relPath.contains("netherite_layer_1.png")) {
                    Path modern = root.resolve("assets/minecraft/textures/entity/equipment/humanoid/netherite.png");
                    Files.createDirectories(modern.getParent());
                    Files.write(modern, data);
                } else if (relPath.contains("netherite_layer_2.png")) {
                    Path modern = root.resolve("assets/minecraft/textures/entity/equipment/humanoid_leggings/netherite.png");
                    Files.createDirectories(modern.getParent());
                    Files.write(modern, data);
                }

                // If writing respawn anchor side, ensure all side levels 0-4 are populated if only one is present
                if (relPath.contains("respawn_anchor_side")) {
                    Path blockDir = target.getParent();
                    for (int i = 0; i <= 4; i++) {
                        Path sidePath = blockDir.resolve("respawn_anchor_side" + i + ".png");
                        if (!Files.exists(sidePath)) {
                            Files.write(sidePath, data);
                        }
                    }
                }

                // If writing respawn anchor top, ensure both top and top_off are present
                if (relPath.contains("respawn_anchor_top_off.png")) {
                    Path onPath = target.resolveSibling("respawn_anchor_top.png");
                    if (!Files.exists(onPath)) Files.write(onPath, data);
                } else if (relPath.contains("respawn_anchor_top.png")) {
                    Path offPath = target.resolveSibling("respawn_anchor_top_off.png");
                    if (!Files.exists(offPath)) Files.write(offPath, data);
                }

                // If writing TNT side, ensure top and bottom exist
                if (relPath.contains("tnt_side.png")) {
                    Path top = target.resolveSibling("tnt_top.png");
                    Path bot = target.resolveSibling("tnt_bottom.png");
                    if (!Files.exists(top)) Files.write(top, data);
                    if (!Files.exists(bot)) Files.write(bot, data);
                }

                // Fire textures: strictly distinguish .png from .mcmeta to never write JSON into PNG!
                String fileName = relPath.substring(relPath.lastIndexOf('/') + 1);

                if (relPath.endsWith(".png") && TextureVariant.isPngBytes(data)) {
                    if (fileName.equals("fire_0.png") || fileName.equals("fire_layer_0.png") || fileName.equals("fire.png")) {
                        Path fire0 = root.resolve("assets/minecraft/textures/block/fire_0.png");
                        Files.createDirectories(fire0.getParent());
                        Files.write(fire0, data);
                    }
                    if (fileName.equals("fire_1.png") || fileName.equals("fire_layer_1.png") || fileName.equals("fire.png")) {
                        Path fire1 = root.resolve("assets/minecraft/textures/block/fire_1.png");
                        Files.createDirectories(fire1.getParent());
                        Files.write(fire1, data);
                    }
                    if (fileName.equals("soul_fire_0.png") || fileName.equals("soul_fire_layer_0.png")) {
                        Path sfire0 = root.resolve("assets/minecraft/textures/block/soul_fire_0.png");
                        Files.createDirectories(sfire0.getParent());
                        Files.write(sfire0, data);
                    }
                    if (fileName.equals("soul_fire_1.png") || fileName.equals("soul_fire_layer_1.png")) {
                        Path sfire1 = root.resolve("assets/minecraft/textures/block/soul_fire_1.png");
                        Files.createDirectories(sfire1.getParent());
                        Files.write(sfire1, data);
                    }
                } else if (relPath.endsWith(".mcmeta")) {
                    if (fileName.equals("fire_0.png.mcmeta") || fileName.equals("fire_layer_0.png.mcmeta") || fileName.equals("fire.png.mcmeta")) {
                        Path m0 = root.resolve("assets/minecraft/textures/block/fire_0.png.mcmeta");
                        Files.createDirectories(m0.getParent());
                        Files.write(m0, data);
                    }
                    if (fileName.equals("fire_1.png.mcmeta") || fileName.equals("fire_layer_1.png.mcmeta") || fileName.equals("fire.png.mcmeta")) {
                        Path m1 = root.resolve("assets/minecraft/textures/block/fire_1.png.mcmeta");
                        Files.createDirectories(m1.getParent());
                        Files.write(m1, data);
                    }
                    if (fileName.equals("soul_fire_0.png.mcmeta") || fileName.equals("soul_fire_layer_0.png.mcmeta")) {
                        Path sm0 = root.resolve("assets/minecraft/textures/block/soul_fire_0.png.mcmeta");
                        Files.createDirectories(sm0.getParent());
                        Files.write(sm0, data);
                    }
                    if (fileName.equals("soul_fire_1.png.mcmeta") || fileName.equals("soul_fire_layer_1.png.mcmeta")) {
                        Path sm1 = root.resolve("assets/minecraft/textures/block/soul_fire_1.png.mcmeta");
                        Files.createDirectories(sm1.getParent());
                        Files.write(sm1, data);
                    }
                }
            }

            // Write primary texture if not already in additional files (only if valid PNG!)
            if (variant.getPngData() != null && TextureVariant.isPngBytes(variant.getPngData())) {
                Path targetPng = root.resolve("assets/minecraft/textures/" + item.getPrimaryTexturePath() + ".png");
                if (!Files.exists(targetPng)) {
                    Files.createDirectories(targetPng.getParent());
                    Files.write(targetPng, variant.getPngData());
                }
            }

            if (item == PvPItem.FIRE) {
                Path fire0 = root.resolve("assets/minecraft/textures/block/fire_0.png");
                Path fire1 = root.resolve("assets/minecraft/textures/block/fire_1.png");
                Path m0 = root.resolve("assets/minecraft/textures/block/fire_0.png.mcmeta");
                Path m1 = root.resolve("assets/minecraft/textures/block/fire_1.png.mcmeta");

                // Ensure both fire_0 and fire_1 exist
                if (Files.exists(fire0) && !Files.exists(fire1)) {
                    Files.copy(fire0, fire1);
                } else if (Files.exists(fire1) && !Files.exists(fire0)) {
                    Files.copy(fire1, fire0);
                }

                // Ensure both m0 and m1 exist if one was provided
                if (Files.exists(m0) && !Files.exists(m1)) {
                    Files.copy(m0, m1);
                } else if (Files.exists(m1) && !Files.exists(m0)) {
                    Files.copy(m1, m0);
                }

                // If animated PNG without mcmeta, auto-generate mcmeta.
                // If static 1-frame PNG (height == width), MUST provide explicit 1-frame animation so vanilla 32-frame animation is not inherited!
                if (Files.exists(fire0)) {
                    byte[] f0 = Files.readAllBytes(fire0);
                    if (!TextureVariant.isPngAnimated(f0)) {
                        Files.writeString(m0, "{\"animation\":{\"frametime\":100000,\"frames\":[0]}}", StandardCharsets.UTF_8);
                    } else if (!Files.exists(m0)) {
                        Files.writeString(m0, "{\"animation\":{\"frametime\":2,\"interpolate\":true}}", StandardCharsets.UTF_8);
                    }
                }
                if (Files.exists(fire1)) {
                    byte[] f1 = Files.readAllBytes(fire1);
                    if (!TextureVariant.isPngAnimated(f1)) {
                        Files.writeString(m1, "{\"animation\":{\"frametime\":100000,\"frames\":[0]}}", StandardCharsets.UTF_8);
                    } else if (!Files.exists(m1)) {
                        Files.writeString(m1, "{\"animation\":{\"frametime\":2,\"interpolate\":true}}", StandardCharsets.UTF_8);
                    }
                }

                // Soul fire pairing
                Path sfire0 = root.resolve("assets/minecraft/textures/block/soul_fire_0.png");
                Path sfire1 = root.resolve("assets/minecraft/textures/block/soul_fire_1.png");
                Path sm0 = root.resolve("assets/minecraft/textures/block/soul_fire_0.png.mcmeta");
                Path sm1 = root.resolve("assets/minecraft/textures/block/soul_fire_1.png.mcmeta");

                if (Files.exists(sfire0) && !Files.exists(sfire1)) {
                    Files.copy(sfire0, sfire1);
                } else if (Files.exists(sfire1) && !Files.exists(sfire0)) {
                    Files.copy(sfire1, sfire0);
                }
                if (Files.exists(sm0) && !Files.exists(sm1)) {
                    Files.copy(sm0, sm1);
                } else if (Files.exists(sm1) && !Files.exists(sm0)) {
                    Files.copy(sm1, sm0);
                }
                if (Files.exists(sfire0)) {
                    byte[] sf0 = Files.readAllBytes(sfire0);
                    if (!TextureVariant.isPngAnimated(sf0)) {
                        Files.writeString(sm0, "{\"animation\":{\"frametime\":100000,\"frames\":[0]}}", StandardCharsets.UTF_8);
                    } else if (!Files.exists(sm0)) {
                        Files.writeString(sm0, "{\"animation\":{\"frametime\":2,\"interpolate\":true}}", StandardCharsets.UTF_8);
                    }
                }
                if (Files.exists(sfire1)) {
                    byte[] sf1 = Files.readAllBytes(sfire1);
                    if (!TextureVariant.isPngAnimated(sf1)) {
                        Files.writeString(sm1, "{\"animation\":{\"frametime\":100000,\"frames\":[0]}}", StandardCharsets.UTF_8);
                    } else if (!Files.exists(sm1)) {
                        Files.writeString(sm1, "{\"animation\":{\"frametime\":2,\"interpolate\":true}}", StandardCharsets.UTF_8);
                    }
                }
            }

            if (item == PvPItem.CRYSTALS) {
                Path modernPng = root.resolve("assets/minecraft/textures/entity/end_crystal/end_crystal.png");
                Path legacyPng = root.resolve("assets/minecraft/textures/entity/endercrystal/endercrystal.png");
                if (Files.exists(modernPng) && !Files.exists(legacyPng)) {
                    Files.createDirectories(legacyPng.getParent());
                    Files.copy(modernPng, legacyPng);
                } else if (Files.exists(legacyPng) && !Files.exists(modernPng)) {
                    Files.createDirectories(modernPng.getParent());
                    Files.copy(legacyPng, modernPng);
                }
            }

            if (item.isSound()) {
                byte[] firstOgg = null;
                for (Map.Entry<String, byte[]> e : add.entrySet()) {
                    if (e.getKey().endsWith(".ogg") && e.getValue() != null && e.getValue().length > 0) {
                        firstOgg = e.getValue();
                        break;
                    }
                }

                if (firstOgg != null) {
                    if (item == PvPItem.SOUND_CRYSTAL) {
                        for (int i = 1; i <= 4; i++) {
                            Path p1 = root.resolve("assets/minecraft/sounds/random/explode" + i + ".ogg");
                            Path p2 = root.resolve("assets/minecraft/sounds/entity/generic/explode" + i + ".ogg");
                            Files.createDirectories(p1.getParent());
                            Files.createDirectories(p2.getParent());
                            if (!Files.exists(p1)) Files.write(p1, firstOgg);
                            if (!Files.exists(p2)) Files.write(p2, firstOgg);
                        }
                    } else if (item == PvPItem.SOUND_TOTEM) {
                        Path p1 = root.resolve("assets/minecraft/sounds/item/totem/use_totem.ogg");
                        Path p2 = root.resolve("assets/minecraft/sounds/item/totem/totem_use.ogg");
                        Path p3 = root.resolve("assets/minecraft/sounds/custom/totem_pop.ogg");
                        Files.createDirectories(p1.getParent());
                        Files.createDirectories(p3.getParent());
                        if (!Files.exists(p1)) Files.write(p1, firstOgg);
                        if (!Files.exists(p2)) Files.write(p2, firstOgg);
                        if (!Files.exists(p3)) Files.write(p3, firstOgg);
                    } else if (item == PvPItem.SOUND_EAT) {
                        for (int i = 1; i <= 3; i++) {
                            Path p1 = root.resolve("assets/minecraft/sounds/random/eat" + i + ".ogg");
                            Path p2 = root.resolve("assets/minecraft/sounds/entity/generic/eat" + i + ".ogg");
                            Files.createDirectories(p1.getParent());
                            Files.createDirectories(p2.getParent());
                            if (!Files.exists(p1)) Files.write(p1, firstOgg);
                            if (!Files.exists(p2)) Files.write(p2, firstOgg);
                        }
                    } else if (item == PvPItem.SOUND_MACE) {
                        for (int i = 1; i <= 4; i++) {
                            Path p = root.resolve("assets/minecraft/sounds/item/mace/smash_ground" + i + ".ogg");
                            Files.createDirectories(p.getParent());
                            if (!Files.exists(p)) Files.write(p, firstOgg);
                        }
                        Path h = root.resolve("assets/minecraft/sounds/item/mace/smash_ground_heavy.ogg");
                        if (!Files.exists(h)) Files.write(h, firstOgg);
                    } else if (item == PvPItem.SOUND_SWORD) {
                        for (int i = 1; i <= 3; i++) {
                            Path p = root.resolve("assets/minecraft/sounds/entity/player/attack/crit" + i + ".ogg");
                            Path c = root.resolve("assets/minecraft/sounds/custom/crit" + i + ".ogg");
                            Files.createDirectories(p.getParent());
                            Files.createDirectories(c.getParent());
                            if (!Files.exists(p)) Files.write(p, firstOgg);
                            if (!Files.exists(c)) Files.write(c, firstOgg);
                        }
                    }
                }
                updateLivePackSoundsJson(root);
            }

        } catch (IOException e) {
            ResourceBuilderMod.LOGGER.error("Failed to write texture to live pack: {}", e.getMessage());
        }
    }

    private void removeTextureFromLivePack(PvPItem item) {
        try {
            Path root = getPackDir();

            if (item.isSound()) {
                if (item == PvPItem.SOUND_MACE) {
                    deleteDirectory(root.resolve("assets/minecraft/sounds/item/mace"));
                } else if (item == PvPItem.SOUND_CRYSTAL) {
                    for (int i = 1; i <= 4; i++) {
                        Files.deleteIfExists(root.resolve("assets/minecraft/sounds/random/explode" + i + ".ogg"));
                        Files.deleteIfExists(root.resolve("assets/minecraft/sounds/entity/generic/explode" + i + ".ogg"));
                    }
                } else if (item == PvPItem.SOUND_SWORD) {
                    deleteDirectory(root.resolve("assets/minecraft/sounds/entity/player/attack"));
                    deleteDirectory(root.resolve("assets/minecraft/sounds/damage"));
                    for (int i = 1; i <= 3; i++) {
                        Files.deleteIfExists(root.resolve("assets/minecraft/sounds/custom/crit" + i + ".ogg"));
                    }
                } else if (item == PvPItem.SOUND_EAT) {
                    for (int i = 1; i <= 3; i++) {
                        Files.deleteIfExists(root.resolve("assets/minecraft/sounds/random/eat" + i + ".ogg"));
                        Files.deleteIfExists(root.resolve("assets/minecraft/sounds/entity/generic/eat" + i + ".ogg"));
                    }
                    Files.deleteIfExists(root.resolve("assets/minecraft/sounds/random/burp.ogg"));
                } else if (item == PvPItem.SOUND_TOTEM) {
                    deleteDirectory(root.resolve("assets/minecraft/sounds/item/totem"));
                    Files.deleteIfExists(root.resolve("assets/minecraft/sounds/custom/totem_pop.ogg"));
                }
                updateLivePackSoundsJson(root);
            } else if (item == PvPItem.CRYSTALS) {
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/item/end_crystal.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/item/end_crystal.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/end_crystal/end_crystal.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/end_crystal/end_crystal.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/end_crystal/end_crystal_beam.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/endercrystal/endercrystal.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/endercrystal/endercrystal.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/endercrystal/endercrystal_beam.png"));
            } else if (item == PvPItem.RESPAWN_ANCHOR) {
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/respawn_anchor_top_off.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/respawn_anchor_top_off.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/respawn_anchor_top.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/respawn_anchor_top.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/respawn_anchor_bottom.png"));
                for (int i = 0; i <= 4; i++) {
                    Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/respawn_anchor_side" + i + ".png"));
                    Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/respawn_anchor_side" + i + ".png.mcmeta"));
                }
            } else if (item == PvPItem.TNT) {
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/tnt_side.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/tnt_top.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/tnt_bottom.png"));
            } else if (item == PvPItem.FIRE) {
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/fire_0.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/fire_0.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/fire_1.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/fire_1.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/soul_fire_0.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/soul_fire_0.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/soul_fire_1.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/block/soul_fire_1.png.mcmeta"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/blocks/fire_0.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/blocks/fire_1.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/blocks/fire_layer_0.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/blocks/fire_layer_1.png"));
                deleteDirectory(root.resolve("assets/minecraft/textures/block/smaller_fire"));
            } else if (item == PvPItem.CRYSTAL_EXPLOSION) {
                for (int i = 0; i <= 15; i++) {
                    Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/explosion_" + i + ".png"));
                }
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/flash.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/explosion.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/particles/explosion.json"));
                Files.deleteIfExists(root.resolve("assets/minecraft/particles/explosion_emitter.json"));
                Files.deleteIfExists(root.resolve("assets/minecraft/particles/flash.json"));
            } else if (item == PvPItem.TOTEM_POPS) {
                for (int i = 0; i <= 7; i++) {
                    Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/glitter_" + i + ".png"));
                }
                Files.deleteIfExists(root.resolve("assets/minecraft/particles/totem_of_undying.json"));
            } else if (item == PvPItem.CRIT_PARTICLES) {
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/critical_hit.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/crit.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/enchanted_hit.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/particle/damage.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/particles/crit.json"));
                Files.deleteIfExists(root.resolve("assets/minecraft/particles/enchanted_hit.json"));
            } else if (item == PvPItem.DIAMOND_ARMOR || item == PvPItem.NETHERITE_ARMOR) {
                String prefix = (item == PvPItem.DIAMOND_ARMOR) ? "diamond" : "netherite";
                String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
                for (String p : pieces) {
                    Files.deleteIfExists(root.resolve("assets/minecraft/textures/item/" + prefix + "_" + p + ".png"));
                    Files.deleteIfExists(root.resolve("assets/minecraft/textures/item/" + prefix + "_" + p + ".png.mcmeta"));
                }
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/models/armor/" + prefix + "_layer_1.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/models/armor/" + prefix + "_layer_2.png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/equipment/humanoid/" + prefix + ".png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/entity/equipment/humanoid_leggings/" + prefix + ".png"));
            } else {
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/" + item.getPrimaryTexturePath() + ".png"));
                Files.deleteIfExists(root.resolve("assets/minecraft/textures/" + item.getPrimaryTexturePath() + ".png.mcmeta"));
            }
        } catch (IOException ignored) {}
    }

    private void updateLivePackSoundsJson(Path root) {
        try {
            Path soundsJson = root.resolve("assets/minecraft/sounds.json");
            StringBuilder sb = new StringBuilder("{\n");
            List<String> entries = new ArrayList<>();

            if (activeSelections.containsKey(PvPItem.SOUND_CRYSTAL)) {
                entries.add("  \"entity.generic.explode\": {\n    \"replace\": true,\n    \"sounds\": [\"random/explode1\", \"random/explode2\", \"random/explode3\", \"random/explode4\"]\n  }");
            }
            if (activeSelections.containsKey(PvPItem.SOUND_TOTEM)) {
                entries.add("  \"item.totem.use\": {\n    \"replace\": true,\n    \"sounds\": [\"item/totem/use_totem\"]\n  }");
            }
            if (activeSelections.containsKey(PvPItem.SOUND_MACE)) {
                entries.add("  \"item.mace.smash_ground\": {\n    \"replace\": true,\n    \"sounds\": [\"item/mace/smash_ground1\", \"item/mace/smash_ground2\", \"item/mace/smash_ground3\", \"item/mace/smash_ground4\"]\n  }");
                entries.add("  \"item.mace.smash_ground_heavy\": {\n    \"replace\": true,\n    \"sounds\": [\"item/mace/smash_ground_heavy\"]\n  }");
            }
            if (activeSelections.containsKey(PvPItem.SOUND_SWORD)) {
                entries.add("  \"entity.player.attack.crit\": {\n    \"replace\": true,\n    \"sounds\": [\"entity/player/attack/crit1\", \"entity/player/attack/crit2\", \"entity/player/attack/crit3\"]\n  }");
                entries.add("  \"entity.player.attack.strong\": {\n    \"replace\": true,\n    \"sounds\": [\"entity/player/attack/strong1\", \"entity/player/attack/strong2\", \"entity/player/attack/strong3\"]\n  }");
            }
            if (activeSelections.containsKey(PvPItem.SOUND_EAT)) {
                entries.add("  \"entity.generic.eat\": {\n    \"replace\": true,\n    \"sounds\": [\"random/eat1\", \"random/eat2\", \"random/eat3\"]\n  }");
                entries.add("  \"entity.player.burp\": {\n    \"replace\": true,\n    \"sounds\": [\"random/burp\"]\n  }");
            }

            if (entries.isEmpty()) {
                Files.deleteIfExists(soundsJson);
            } else {
                Files.createDirectories(soundsJson.getParent());
                sb.append(String.join(",\n", entries));
                sb.append("\n}\n");
                Files.writeString(soundsJson, sb.toString(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {}
    }

    private void deleteDirectory(Path dir) {
        if (dir == null || !Files.exists(dir)) return;
        try {
            List<Path> paths;
            try (var stream = Files.walk(dir)) {
                paths = stream.sorted(Comparator.reverseOrder()).toList();
            }
            for (Path p : paths) {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception e) {
                    try {
                        File f = p.toFile();
                        f.setWritable(true);
                        f.delete();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    private void clearLivePackTextures() {
        try {
            Path root = getPackDir();
            Path assetsDir = root.resolve("assets");
            deleteDirectory(assetsDir);
            initPackFolder();
        } catch (Exception ignored) {}
    }

    public void ensurePackEnabledAndReload() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;

        PackRepository repo = client.getResourcePackRepository();
        if (repo != null) {
            repo.reload();
            List<String> selected = new ArrayList<>(repo.getSelectedIds());
            if (activeSelections.isEmpty()) {
                selected.remove(PACK_ID);
            } else {
                selected.remove(PACK_ID);
                selected.add(PACK_ID); // Ensure ResourceBuilder_Active is top priority
            }
            repo.setSelected(selected);
        }

        client.execute(() -> {
            client.reloadResourcePacks();
        });
    }

    private void saveActiveMix() {
        try {
            Files.createDirectories(configFile.getParent());
            StringBuilder sb = new StringBuilder("{\n");
            int count = 0;
            int total = activeSelections.size();
            for (Map.Entry<PvPItem, TextureVariant> entry : activeSelections.entrySet()) {
                sb.append("  \"").append(entry.getKey().name()).append("\": \"")
                        .append(entry.getValue().getPackName().replace("\"", "\\\"")).append("\"");
                count++;
                if (count < total) sb.append(",");
                sb.append("\n");
            }
            sb.append("}\n");
            Files.writeString(configFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    private void loadActiveMix() {
        activeSelections.clear();
        if (!Files.isRegularFile(configFile)) return;
        try {
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            String[] lines = content.replace("{", "").replace("}", "").split(",");
            for (String line : lines) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    String k = line.substring(0, colon).trim().replace("\"", "");
                    String v = line.substring(colon + 1).trim().replace("\"", "");
                    try {
                        PvPItem item = PvPItem.valueOf(k);
                        List<TextureVariant> variants = getVariants(item);
                        for (TextureVariant var : variants) {
                            if (var.getPackName().equalsIgnoreCase(v)) {
                                activeSelections.put(item, var);
                                writeTextureToLivePack(item, var);
                                break;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException ignored) {}
    }
}
