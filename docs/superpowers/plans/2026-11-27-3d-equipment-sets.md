# 3D Equipment Sets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship three performant selectable 3D armor-and-Spear sets that render on real players and are managed like Resource Builder styles.

**Architecture:** Immutable validated equipment definitions drive a cached custom humanoid armor model. A small renderer layer follows vanilla player poses; a resource-pack item model supplies the 3D Spear. The Studio activates a whole set atomically through one new tab.

**Tech Stack:** Java, Fabric, Minecraft humanoid `ModelPart`/render layers, existing live-pack and Studio systems.

**Spec:** `docs/superpowers/specs/2026-11-27-3d-equipment-sets-design.md`

## Global Constraints

- Implement for Minecraft 26.2 and 1.21.11.
- No GeckoLib and no per-frame parsing, file access, texture generation, or model baking.
- Preserve ordinary armor and Spear fallback when the feature is Off or invalid.
- Do not copy the named third-party designs; ship original styles.

## Review Focus

- Off selection must submit no custom geometry.
- Missing armor pieces must not crash the layer.
- Model cache identity must remain stable across repeated frames.
- Remote players farther than 64 blocks must skip the custom layer.
- Selection persists and produces a valid Spear model resource.

---

### Task 1: Set definitions and selection

**Files:** Create `equipment/EquipmentSetDefinition.java`, `equipment/EquipmentSetManager.java`; modify `StudioSettings.java`; create `EquipmentSetSmoke.java`.

**Interfaces:** `EquipmentSetManager.builtins()`, `active()`, `select(String)`, and immutable cuboid/attachment definitions.

- [ ] Write failing smoke assertions for the three IDs, component presence, Off behavior, invalid-ID fallback, persistence field, and stable cached definitions.
- [ ] Implement immutable built-ins `void_warden`, `frost_seraph`, and `crimson_revenant` with bounded geometry.
- [ ] Run `compileSmokeJava` and commit.

### Task 2: Cached player render layer

**Files:** Create `render/EquipmentSetModel.java`, `render/EquipmentSetLayer.java`; create `mixin/AvatarRendererMixin.java`; modify mixin config and runtime smoke.

**Interfaces:** `EquipmentSetModel.forDefinition(EquipmentSetDefinition)` returns one cached baked model per definition; layer reads only `EquipmentSetManager.active()` during submit.

- [ ] Write failing cache/visibility assertions and an in-world smoke stage that equips armor and activates each set.
- [ ] Bake head/body/arm/leg/back cuboids once and add the layer to the avatar renderer.
- [ ] Run full `runSmoke`, inspect the captured player image, and commit.

### Task 3: 3D Spear and Studio tab

**Files:** Add Spear model JSON resources; create `gui/EquipmentSetsScreen.java`; modify `StudioTab.java`, `TextureStudioScreen.java`, and smoke navigation.

**Interfaces:** Selecting a card calls `EquipmentSetManager.select(id)` and the card preview uses the same definition/model metadata.

- [ ] Write failing assertions for Spear resources and the `3D Sets` navigation label.
- [ ] Add a substantial cuboid Spear in-hand model, three wide set cards, Off, and one-reload atomic activation.
- [ ] Capture/inspect the Studio screen and commit.

### Task 4: Port and release

**Files:** Port all production/tests/resources to `codex/fabric-1.21.11`; update README and version to 3.1.0.

- [ ] Adapt only version-specific render/input APIs.
- [ ] Run `build` and `runSmoke` for both versions.
- [ ] Push 26.2 to `main` and 1.21.11 to its compatibility branch.
