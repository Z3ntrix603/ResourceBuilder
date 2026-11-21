package de.resourcebuilder.preset;

import de.resourcebuilder.category.PvPItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Preset {

    private final String name;
    private final Map<String, String> selections;

    public Preset(String name) {
        this.name = name;
        this.selections = new HashMap<>();
    }

    public Preset(String name, Map<String, String> selections) {
        this.name = name;
        this.selections = new HashMap<>(selections);
        removeRetiredSelections();
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getSelections() {
        return selections == null ? Map.of() : Collections.unmodifiableMap(selections);
    }

    public void setSelection(PvPItem item, String packName) {
        if (packName == null || packName.equalsIgnoreCase("Default")) {
            selections.remove(item.name());
        } else {
            selections.put(item.name(), packName);
        }
    }

    public String getSelection(PvPItem item) {
        return selections.get(item.name());
    }

    boolean removeRetiredSelections() {
        return selections != null && selections.keySet().removeAll(PvPItem.RETIRED);
    }

    public int size() {
        return getSelections().size();
    }
}
