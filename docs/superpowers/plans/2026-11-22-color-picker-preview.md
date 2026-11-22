# Color Picker Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace blind palette cycling with an obvious live color picker and preview everywhere a Resource Builder color is edited.

**Architecture:** Keep `StudioColorButton` as the entry point, but open a focused `ColorPickerScreen`. Put RGB/HSV/hex conversion in a small pure helper so both Minecraft ports share behavior and smoke tests can verify it without UI automation.

**Tech Stack:** Java 25/21, Fabric client GUI, existing Studio widgets and smoke source set.

**Spec:** User feedback in the 2026-11-22 Codex chat; compact in-place workflow, saturation/value plane, hue slider, hex field, presets, large swatch, live updates, Apply/Cancel.

## Global Constraints

- Ship matching behavior for Minecraft 26.2 and 1.21.11.
- Preserve the clean rounded Studio visual language.
- Invalid hex input must not destroy the current color.

## Review Focus

- Black, white and grayscale HSV conversion.
- Hue wrap at 0/360 degrees.
- Invalid and partial hex typing.
- Cancel restores the original color after live preview.
- Small game windows keep controls reachable.

---

### Task 1: Color model

**Files:** Create `ColorPickerModel.java`; create `ColorPickerSmoke.java`; modify `StudioSmoke.java`.

- [ ] Write smoke assertions for HSV/RGB round trips and strict six-digit hex parsing, then observe RED.
- [ ] Implement the pure conversion/parser helper and observe GREEN.

### Task 2: Picker and preview

**Files:** Create `ColorPickerScreen.java`; modify `StudioColorButton.java`.

- [ ] Open a compact picker from every color row.
- [ ] Render saturation/value plane, hue strip, large selected-color swatch, hex field and presets.
- [ ] Update effects live; Apply keeps the value and Cancel restores the original.

### Task 3: Dual-version verification

- [ ] Port the GUI rendering/input differences to 1.21.11.
- [ ] Run `build` and `runSmoke` for both versions.
- [ ] Commit and push `main` plus `codex/fabric-1.21.11`.
