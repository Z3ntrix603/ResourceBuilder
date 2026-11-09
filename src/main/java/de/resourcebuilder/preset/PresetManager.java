package de.resourcebuilder.preset;

import de.resourcebuilder.ResourceBuilderMod;
import de.resourcebuilder.category.PvPItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PresetManager {

    private static PresetManager instance;
    private final List<Preset> presets = new ArrayList<>();
    private final Path configFile = Paths.get("config", "resourcebuilder", "presets.json");

    private PresetManager() {
        load();
    }

    public static synchronized PresetManager getInstance() {
        if (instance == null) {
            instance = new PresetManager();
        }
        return instance;
    }

    public List<Preset> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    public Preset getPreset(String name) {
        for (Preset p : presets) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    public void savePreset(String name, Map<PvPItem, String> selections) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<PvPItem, String> entry : selections.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().equalsIgnoreCase("Default")) {
                map.put(entry.getKey().name(), entry.getValue());
            }
        }

        presets.removeIf(p -> p.getName().equalsIgnoreCase(name));
        presets.add(new Preset(name, map));
        save();
    }

    public void deletePreset(String name) {
        presets.removeIf(p -> p.getName().equalsIgnoreCase(name));
        save();
    }

    public void save() {
        try {
            Files.createDirectories(configFile.getParent());
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < presets.size(); i++) {
                Preset p = presets.get(i);
                sb.append("  {\n");
                sb.append("    \"name\": ").append(quote(p.getName())).append(",\n");
                sb.append("    \"selections\": {\n");
                List<Map.Entry<String, String>> entries = new ArrayList<>(p.getSelections().entrySet());
                for (int j = 0; j < entries.size(); j++) {
                    Map.Entry<String, String> e = entries.get(j);
                    sb.append("      ").append(quote(e.getKey())).append(": ").append(quote(e.getValue()));
                    if (j < entries.size() - 1) sb.append(",");
                    sb.append("\n");
                }
                sb.append("    }\n");
                sb.append("  }");
                if (i < presets.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]\n");
            Files.writeString(configFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            ResourceBuilderMod.LOGGER.error("Failed to save presets: {}", e.getMessage());
        }
    }

    public void load() {
        presets.clear();
        if (!Files.isRegularFile(configFile)) {
            return;
        }

        try {
            String content = Files.readString(configFile, StandardCharsets.UTF_8);
            parsePresets(content);
        } catch (Exception e) {
            ResourceBuilderMod.LOGGER.error("Failed to load presets: {}", e.getMessage());
        }
    }

    private void parsePresets(String json) {
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return;

        int idx = 0;
        while (idx < trimmed.length()) {
            int objStart = trimmed.indexOf('{', idx);
            if (objStart < 0) break;
            int objEnd = findMatchingBrace(trimmed, objStart);
            if (objEnd < 0) break;

            String obj = trimmed.substring(objStart, objEnd + 1);
            String name = extractString(obj, "name");
            if (name != null) {
                Map<String, String> selections = new HashMap<>();
                int selStart = obj.indexOf("\"selections\"");
                if (selStart >= 0) {
                    int subStart = obj.indexOf('{', selStart);
                    if (subStart >= 0) {
                        int subEnd = findMatchingBrace(obj, subStart);
                        if (subEnd >= 0) {
                            String selObj = obj.substring(subStart + 1, subEnd);
                            parseKeyValues(selObj, selections);
                        }
                    }
                }
                presets.add(new Preset(name, selections));
            }
            idx = objEnd + 1;
        }
    }

    private void parseKeyValues(String block, Map<String, String> out) {
        String[] lines = block.split(",");
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon >= 0) {
                String k = cleanQuotes(line.substring(0, colon).trim());
                String v = cleanQuotes(line.substring(colon + 1).trim());
                if (!k.isEmpty() && !v.isEmpty()) {
                    out.put(k, v);
                }
            }
        }
    }

    private String cleanQuotes(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String extractString(String block, String key) {
        String pat = "\"" + key + "\"";
        int pos = block.indexOf(pat);
        if (pos < 0) return null;
        int colon = block.indexOf(':', pos + pat.length());
        if (colon < 0) return null;
        int q1 = block.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = block.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return block.substring(q1 + 1, q2);
    }

    private int findMatchingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
