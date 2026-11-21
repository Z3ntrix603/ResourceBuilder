package de.resourcebuilder.pack;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.resourcebuilder.ResourceBuilderMod;
import de.resourcebuilder.category.PvPItem;
import de.resourcebuilder.preset.Preset;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;

public final class LivePackManager {
    private static final String PACK = "ResourceBuilder Studio.zip";
    private static final String ALTERNATE = "ResourceBuilder Studio B.zip";
    private static final int MAX_FILE = 8 * 1024 * 1024;
    private static final Pattern RETIRED_TEXTURE =
            Pattern.compile(
                    Pattern.quote(StyleAssets.ROOT)
                            + "(?:item/(?:enchanted_book|item_frame|(?:iron|golden)_(?:helmet|chestplate|leggings|boots))"
                            + "|block/item_frame|entity/chest/normal(?:_left|_right)?"
                            + "|entity/equipment/(?:humanoid|humanoid_leggings)/(?:iron|gold))\\.png(?:\\.mcmeta)?");
    private static final LivePackManager INSTANCE = new LivePackManager();
    private final Map<PvPItem, List<TextureVariant>> library = new EnumMap<>(PvPItem.class);
    private final Map<PvPItem, TextureVariant> active = new EnumMap<>(PvPItem.class);
    private final Map<String, PvPItem> index = new HashMap<>();
    private final Path config = Path.of("config", "resourcebuilder", "active_mix.json");
    private boolean scanned;

    public static LivePackManager getInstance() {
        return INSTANCE;
    }

    /**
     * Remove retired overrides from an already equipped Studio ZIP on the first upgraded launch.
     */
    public void migrateRetiredSelections() {
        if (!Files.isRegularFile(config)) return;
        try {
            var saved = readSelections();
            if (saved == null || !saved.keySet().removeAll(PvPItem.RETIRED)) return;
            var mc = Minecraft.getInstance();
            var ids = mc.getResourcePackRepository().getSelectedIds();
            String equipped =
                    ids.contains("file/" + PACK)
                            ? PACK
                            : ids.contains("file/" + ALTERNATE) ? ALTERNATE : null;
            if (equipped != null) {
                if (saved.isEmpty()) {
                    enablePack(mc, null);
                } else {
                    String next = equipped.equals(PACK) ? ALTERNATE : PACK;
                    copyRetainedAssets(mc.getResourcePackDirectory(), equipped, next);
                    enablePack(mc, next);
                }
            }
            saveSelections(saved);
        } catch (IOException | com.google.gson.JsonParseException e) {
            ResourceBuilderMod.LOGGER.warn("Cannot migrate retired Studio selections", e);
        }
    }

    // Older mixes may still contain imports whose source pack has since been removed.
    private static void copyRetainedAssets(Path dir, String equipped, String next)
            throws IOException {
        Path temp = Files.createTempFile(dir, "rb-migrate-", ".tmp");
        try {
            try (var old = new ZipFile(dir.resolve(equipped).toFile());
                    var zip = new ZipOutputStream(Files.newOutputStream(temp))) {
                for (var entry : Collections.list(old.entries())) {
                    if (entry.isDirectory() || RETIRED_TEXTURE.matcher(entry.getName()).matches())
                        continue;
                    zip.putNextEntry(new ZipEntry(entry.getName()));
                    try (var in = old.getInputStream(entry)) {
                        in.transferTo(zip);
                    }
                    zip.closeEntry();
                }
            }
            Files.move(temp, dir.resolve(next), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private Map<String, String> readSelections() throws IOException {
        try (var reader = Files.newBufferedReader(config)) {
            return new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
        }
    }

    private void saveSelections(Map<String, String> selections) throws IOException {
        Files.createDirectories(config.getParent());
        Path temp = Files.createTempFile(config.getParent(), "active-mix-", ".tmp");
        try {
            Files.writeString(temp, new Gson().toJson(selections));
            Files.move(temp, config, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void enablePack(Minecraft mc, String next) {
        var repo = mc.getResourcePackRepository();
        repo.reload();
        var ids = new ArrayList<>(repo.getSelectedIds());
        ids.removeAll(List.of("file/" + PACK, "file/" + ALTERNATE, "file/ResourceBuilder_Active"));
        if (next != null) ids.add("file/" + next);
        repo.setSelected(ids);
        mc.options.updateResourcePacks(repo);
        mc.options.save();
    }

    public void scanAllPacks() {
        if (scanned) return;
        migrateRetiredSelections();
        for (PvPItem item : PvPItem.values()) {
            List<TextureVariant> list = new ArrayList<>();
            library.put(item, list);
            list.add(TextureVariant.standard(item));
            if (item.isSound()) {
                String[][] palette = {
                    {"Chime", "block.amethyst_block.chime"},
                    {"Bell", "block.note_block.bell"},
                    {"Click", "ui.button.click"},
                    {"Pling", "block.note_block.pling"},
                    {"Pop", "entity.item.pickup"},
                    {"Echo", "block.amethyst_block.resonate"}
                };
                for (var pair : palette) list.add(TextureVariant.sound(item, pair[0], pair[1]));
            } else {
                for (int i = 0; i < StyleAssets.count(item); i++)
                    list.add(TextureVariant.builtin(item, i));
                for (String sub : StyleAssets.CATALOG.get(item.name()))
                    index.put(StyleAssets.ROOT + sub + ".png", item);
            }
        }
        Path dir = Minecraft.getInstance().getResourcePackDirectory();
        try (var files = Files.list(dir)) {
            files.sorted()
                    .filter(
                            p ->
                                    !Set.of(PACK, ALTERNATE, "ResourceBuilder_Active")
                                            .contains(p.getFileName().toString()))
                    .forEach(this::scan);
        } catch (IOException e) {
            ResourceBuilderMod.LOGGER.warn("Cannot list packs", e);
        }
        if (Files.isRegularFile(config))
            try {
                Map<String, String> saved = readSelections();
                if (saved != null) restore(saved);
            } catch (Exception e) {
                ResourceBuilderMod.LOGGER.warn("Cannot read active mix", e);
            }
        scanned = true;
    }

    private void scan(Path pack) {
        Map<PvPItem, Map<String, byte[]>> parts = new EnumMap<>(PvPItem.class);
        try {
            if (Files.isDirectory(pack)) {
                try (var paths = Files.walk(pack)) {
                    for (Path p :
                            paths.filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS))
                                    .toList())
                        collect(
                                parts,
                                pack.relativize(p).toString(),
                                Files.size(p),
                                () -> Files.newInputStream(p));
                }
            } else if (pack.toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(pack.toFile())) {
                    for (var e : Collections.list(zip.entries()))
                        if (!e.isDirectory())
                            collect(parts, e.getName(), e.getSize(), () -> zip.getInputStream(e));
                }
            }
            String name =
                    pack.getFileName()
                            .toString()
                            .replaceFirst("(?i)\\.zip$", "")
                            .replaceAll("§[0-9a-fk-or]", "")
                            .replaceFirst("^[!#\\s]+", "")
                            .trim();
            for (var e : parts.entrySet()) {
                var list = library.get(e.getKey());
                String label =
                        list.stream().anyMatch(v -> v.getPackName().equalsIgnoreCase(name))
                                ? name + " (Pack)"
                                : name;
                if (e.getValue().keySet().stream()
                        .anyMatch(k -> k.endsWith(".png") || k.endsWith(".ogg")))
                    list.add(TextureVariant.imported(e.getKey(), label, e.getValue()));
            }
        } catch (Exception e) {
            ResourceBuilderMod.LOGGER.warn("Skipping pack {}", pack, e);
        }
    }

    private interface Source {
        InputStream open() throws IOException;
    }

    private void collect(Map<PvPItem, Map<String, byte[]>> parts, String raw, long size, Source src)
            throws IOException {
        if (size <= 0 || size > MAX_FILE) return;
        String path = raw.replace('\\', '/').toLowerCase(Locale.ROOT);
        int root = path.indexOf("assets/minecraft/");
        if (root < 0 || path.contains("..") || path.contains(":")) return;
        path =
                path.substring(root)
                        .replace("/textures/items/", "/textures/item/")
                        .replace("/textures/blocks/", "/textures/block/")
                        .replace(
                                "/entity/endercrystal/endercrystal",
                                "/entity/end_crystal/end_crystal")
                        .replace("/item/endercrystal.png", "/item/end_crystal.png")
                        .replace("/item/totem.png", "/item/totem_of_undying.png")
                        .replaceAll(
                                "/models/armor/(diamond|netherite)_layer_1",
                                "/entity/equipment/humanoid/$1")
                        .replaceAll(
                                "/models/armor/(diamond|netherite)_layer_2",
                                "/entity/equipment/humanoid_leggings/$1")
                        .replace("/block/fire_layer_", "/block/fire_")
                        .replace("/entity/elytra.png", "/entity/equipment/wings/elytra.png");
        PvPItem item =
                index.get(path.endsWith(".mcmeta") ? path.substring(0, path.length() - 7) : path);
        if (item == null && path.endsWith(".ogg") && path.contains("/sounds/"))
            item = soundGroup(path);
        if (item == null) return;
        try (var in = src.open()) {
            byte[] data = in.readNBytes((int) size + 1);
            if (data.length == size)
                parts.computeIfAbsent(item, unused -> new LinkedHashMap<>()).put(path, data);
        }
    }

    private PvPItem soundGroup(String file) {
        if (file.contains("mace") || file.contains("smash_")) return PvPItem.SOUND_MACE;
        if (file.contains("explode") || file.contains("explosion")) return PvPItem.SOUND_CRYSTAL;
        if (file.contains("attack/") || file.contains("crit")) return PvPItem.SOUND_SWORD;
        if (file.contains("/eat") || file.contains("burp")) return PvPItem.SOUND_EAT;
        if (file.contains("totem")) return PvPItem.SOUND_TOTEM;
        if (file.contains("enderpearl") || file.contains("ender_pearl")) return PvPItem.SOUND_PEARL;
        if (file.contains("experience") || file.contains("orb")) return PvPItem.SOUND_XP;
        if (file.contains("bow") || file.contains("arrow")) return PvPItem.SOUND_BOW;
        if (file.contains("shield")) return PvPItem.SOUND_SHIELD;
        if (file.contains("respawn_anchor")) return PvPItem.SOUND_ANCHOR;
        if (file.contains("armor/equip")) return PvPItem.SOUND_ARMOR;
        if (file.contains("hurt")) return PvPItem.SOUND_HURT;
        return null;
    }

    public List<TextureVariant> getVariants(PvPItem item) {
        return library.get(item);
    }

    public TextureVariant getActiveSelection(PvPItem item) {
        return active.getOrDefault(item, library.get(item).getFirst());
    }

    private void restore(Map<String, String> saved) {
        active.clear();
        for (PvPItem item : PvPItem.values())
            library.get(item).stream()
                    .filter(
                            v ->
                                    !v.isDefault()
                                            && v.getPackName()
                                                    .equalsIgnoreCase(saved.get(item.name())))
                    .findFirst()
                    .ifPresent(v -> active.put(item, v));
    }

    public void selectVariant(PvPItem item, TextureVariant choice, boolean reload) {
        var old = new EnumMap<>(active);
        if (choice.isDefault()) active.remove(item);
        else active.put(item, choice);
        if (reload) commit(old);
    }

    public void applyPreset(Preset preset) {
        var old = new EnumMap<>(active);
        restore(preset.getSelections());
        commit(old);
    }

    private Integer savedSkyTint;

    public int skyTint() {
        if (!scanned) {
            if (savedSkyTint == null) {
                savedSkyTint = 0;
                if (Files.isRegularFile(config))
                    try (var reader = Files.newBufferedReader(config)) {
                        var object =
                                com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                        if (object.has("SKY"))
                            for (int i = 0; i < StyleAssets.count(PvPItem.SKY); i++)
                                if (StyleAssets.name(PvPItem.SKY, i)
                                        .equals(object.get("SKY").getAsString()))
                                    savedSkyTint = StyleAssets.color(i);
                    } catch (Exception ex) {
                        ResourceBuilderMod.LOGGER.warn("Cannot restore sky style", ex);
                    }
            }
            return savedSkyTint;
        }
        var sky = active.get(PvPItem.SKY);
        return sky == null ? 0 : sky.paletteColor();
    }

    public Path exportMix() throws IOException {
        var client = Minecraft.getInstance();
        var ids = client.getResourcePackRepository().getSelectedIds();
        String name =
                ids.contains("file/" + PACK)
                        ? PACK
                        : ids.contains("file/" + ALTERNATE) ? ALTERNATE : null;
        if (name == null) throw new IOException("No active mix");
        Path folder = client.gameDirectory.toPath().resolve("resourcebuilder-exports");
        Files.createDirectories(folder);
        Path dest = Files.createTempFile(folder, "Studio-Mix-", ".zip");
        Files.copy(
                client.getResourcePackDirectory().resolve(name),
                dest,
                StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    public void resetAll() {
        var old = new EnumMap<>(active);
        active.clear();
        commit(old);
    }

    private void commit(Map<PvPItem, TextureVariant> old) {
        Minecraft mc = Minecraft.getInstance();
        Path dir = mc.getResourcePackDirectory(), temp = null;
        try {
            Files.createDirectories(dir);
            temp = Files.createTempFile(dir, "rb-build-", ".tmp");
            Map<String, byte[]> output = new TreeMap<>();
            Map<String, Object> sounds = new TreeMap<>();
            for (var e : active.entrySet()) {
                var assets = e.getValue().files();
                output.putAll(assets);
                if (e.getKey() == PvPItem.ENCHANTED_GOLDEN_APPLE) {
                    output.put(
                            "assets/minecraft/items/enchanted_golden_apple.json",
                            utf8(
                                    "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"minecraft:item/enchanted_golden_apple\"}}"));
                    output.put(
                            "assets/minecraft/models/item/enchanted_golden_apple.json",
                            utf8(
                                    "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"minecraft:item/enchanted_golden_apple\"}}"));
                }
                if (e.getKey().isSound()) {
                    var samples =
                            assets.keySet().stream()
                                    .filter(k -> k.endsWith(".ogg"))
                                    .map(
                                            k ->
                                                    k.substring(
                                                            "assets/minecraft/sounds/".length(),
                                                            k.length() - 4))
                                    .toList();
                    Object choices =
                            e.getValue().soundAlias() == null
                                    ? samples
                                    : List.of(
                                            Map.of(
                                                    "type",
                                                    "event",
                                                    "name",
                                                    e.getValue().soundAlias()));
                    for (String event : soundEvents(e.getKey()))
                        sounds.put(event, Map.of("replace", true, "sounds", choices));
                }
            }
            if (!sounds.isEmpty())
                output.put("assets/minecraft/sounds.json", utf8(new Gson().toJson(sounds)));
            output.put(
                    "pack.mcmeta",
                    utf8(
                            "{\"pack\":{\"min_format\":88,\"max_format\":88,\"description\":\"Resource"
                                + " Builder Studio\"}}"));
            try (var zip = new ZipOutputStream(Files.newOutputStream(temp))) {
                for (var e : output.entrySet()) {
                    zip.putNextEntry(new ZipEntry(e.getKey()));
                    zip.write(e.getValue());
                    zip.closeEntry();
                }
            }
            // Minecraft may keep the active ZIP open on Windows. Use two alternating slots.
            var repo = mc.getResourcePackRepository();
            String next = repo.getSelectedIds().contains("file/" + PACK) ? ALTERNATE : PACK;
            Files.move(temp, dir.resolve(next), StandardCopyOption.REPLACE_EXISTING);
            enablePack(mc, active.isEmpty() ? null : next);
            Map<String, String> names = new TreeMap<>();
            active.forEach((item, style) -> names.put(item.name(), style.getPackName()));
            saveSelections(names);

        } catch (IOException e) {
            active.clear();
            active.putAll(old);
            throw new UncheckedIOException("Could not save the resource mix", e);
        } finally {
            if (temp != null)
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
        }
    }

    static byte[] utf8(String text) {
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public static List<String> soundEvents(PvPItem item) {
        return switch (item) {
            case SOUND_MACE ->
                    List.of(
                            "item.mace.smash_ground",
                            "item.mace.smash_ground_heavy",
                            "item.mace.smash_air");
            case SOUND_SWORD -> List.of("entity.player.attack.crit", "entity.player.attack.strong");
            case SOUND_CRYSTAL -> List.of("entity.generic.explode");
            case SOUND_TOTEM -> List.of("item.totem.use");
            case SOUND_EAT -> List.of("entity.generic.eat", "entity.player.burp");
            default -> item.isSound() ? List.of(item.getPrimaryTexturePath()) : List.of();
        };
    }
}
