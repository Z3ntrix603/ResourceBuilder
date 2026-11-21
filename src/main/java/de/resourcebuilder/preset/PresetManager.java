package de.resourcebuilder.preset;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.resourcebuilder.ResourceBuilderMod;
import de.resourcebuilder.category.PvPItem;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class PresetManager {
    private static final Path FILE = Path.of("config", "resourcebuilder", "presets.json");
    private static final PresetManager INSTANCE = new PresetManager();
    private final List<Preset> presets = new ArrayList<>();

    private PresetManager() {
        if (!Files.isRegularFile(FILE)) return;
        boolean migrated = false;
        try (var reader = Files.newBufferedReader(FILE)) {
            List<Preset> saved =
                    new Gson().fromJson(reader, new TypeToken<List<Preset>>() {}.getType());
            if (saved != null) {
                for (var preset : saved) {
                    if (preset == null || preset.getName() == null) continue;
                    migrated |= preset.removeRetiredSelections();
                    presets.add(preset);
                }
            }
        } catch (Exception e) {
            ResourceBuilderMod.LOGGER.warn("Could not load presets", e);
            return;
        }
        if (migrated) save();
    }

    public static PresetManager getInstance() {
        return INSTANCE;
    }

    public List<Preset> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    public void savePreset(String name, Map<PvPItem, String> selections) {
        var next = new Preset(name);
        selections.forEach(next::setSelection);
        presets.removeIf(p -> p.getName().equalsIgnoreCase(name));
        presets.add(next);
        save();
    }

    public void deletePreset(String name) {
        presets.removeIf(p -> p.getName().equalsIgnoreCase(name));
        save();
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            var tmp = Files.createTempFile(FILE.getParent(), "presets", ".tmp");
            try {
                Files.writeString(tmp, new Gson().toJson(presets));
                Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(tmp);
            }
        } catch (IOException e) {
            ResourceBuilderMod.LOGGER.warn("Could not save presets", e);
        }
    }
}
