# Resource Builder v3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the complete Resource Builder v3 editor and runtime visual system for Minecraft 26.2 and port the same release to 1.21.11.

**Architecture:** Add a versioned `VisualProfile` model with validation and share-code serialization, then expose it through focused studio pages using reusable controls. Runtime mixins consume the profile; pack-backed editors feed imported images and generated textures through `LivePackManager`.

**Tech Stack:** Java 25/21, Fabric Loader/API, Sponge Mixin, Gson, Minecraft client rendering APIs, Gradle smoke source set.

**Spec:** `docs/superpowers/specs/2026-11-22-resource-builder-v3-design.md`

## Global Constraints

- Implement every feature on both Minecraft 26.2 and 1.21.11.
- Preserve v2.4.0 settings and selections.
- Keep imported data local and bound all decoded/imported inputs.
- Write a failing smoke/unit check before each production subsystem.
- Do not touch the unrelated `hugpsmp-invsee` directory.

## Review Focus

- Malformed or oversized share codes leave current settings unchanged.
- Missing/corrupt PNG imports report an error without deleting the previous asset.
- Particle amount and lifetime bounds cannot freeze the client.
- Color correction can be disabled to restore vanilla rendering exactly.
- Old `studio.json` files load with safe v3 defaults.

---

### Task 1: Versioned profile and share codes

**Files:**
- Create: `src/main/java/de/resourcebuilder/profile/VisualProfile.java`
- Create: `src/main/java/de/resourcebuilder/profile/ShareCode.java`
- Modify: `src/main/java/de/resourcebuilder/StudioSettings.java`
- Test: `src/smoke/java/verify/ProfileSmoke.java`

**Interfaces:**
- Produces: `VisualProfile normalized()`, `ShareCode.encode(StudioSettings)`, and atomic `ShareCode.apply(String, StudioSettings)`.

- [ ] Write profile/share-code checks for defaults, round-trip, invalid input, oversized input, and old JSON defaults.
- [ ] Run `compileSmokeJava` and verify failure from missing classes.
- [ ] Implement the model, validation, versioned gzip/base64 code, and atomic apply.
- [ ] Run `compileSmokeJava` and the profile smoke harness; commit.

### Task 2: Reusable editor controls and clean-tab navigation

**Files:**
- Create: `src/main/java/de/resourcebuilder/gui/VisualsScreen.java`
- Create: `src/main/java/de/resourcebuilder/gui/StudioSlider.java`
- Create: `src/main/java/de/resourcebuilder/gui/StudioColorButton.java`
- Modify: `src/main/java/de/resourcebuilder/gui/TextureStudioScreen.java`
- Modify: `src/main/java/de/resourcebuilder/gui/StudioTab.java`
- Test: `src/smoke/java/verify/StudioSmoke.java`

**Interfaces:**
- Consumes: Task 1 profile fields.
- Produces: focused Global Color, Particles, Fire, and Glint subpages.

- [ ] Add failing navigation and setting-mutation smoke checks.
- [ ] Implement shared controls and the selected clean-tabs layout.
- [ ] Verify screenshots at normal and 320x240 sizes; commit.

### Task 3: Runtime color, particle, fire, and glint rendering

**Files:**
- Create: `src/main/java/de/resourcebuilder/render/VisualRuntime.java`
- Modify: `src/main/java/de/resourcebuilder/mixin/StudioRenderMixins.java`
- Modify: `src/main/resources/resourcebuilder.mixins.json`
- Test: `src/smoke/java/verify/VisualRuntimeSmoke.java`

**Interfaces:**
- Consumes: normalized profile from Task 1.
- Produces: deterministic transforms and bounded particle multipliers used by mixins.

- [ ] Write failing transform/bounds/color tests.
- [ ] Implement pure transforms, then hook particles, fire, glint, and post-processing color correction.
- [ ] Verify disabling every effect is vanilla-equivalent and run smoke client; commit.

### Task 4: Inventory and Natural Blocks editors

**Files:**
- Modify: `src/main/java/de/resourcebuilder/category/PvPItem.java`
- Modify: `src/main/java/de/resourcebuilder/gui/StudioTab.java`
- Create: `src/main/java/de/resourcebuilder/mixin/InventoryHighlightMixin.java`
- Modify: `src/main/resources/assets/resourcebuilder/catalog.json`
- Test: `src/smoke/java/verify/StudioSmoke.java`

**Interfaces:**
- Produces: inventory gallery, important-block gallery, and configurable item highlights.

- [ ] Add failing catalog, navigation, and highlight checks.
- [ ] Implement the constrained galleries and safe screen-space highlights.
- [ ] Verify inventory and terrain exports; commit.

### Task 5: Sky, menu, panorama, and PNG importing

**Files:**
- Create: `src/main/java/de/resourcebuilder/importer/ImageImportService.java`
- Create: `src/main/java/de/resourcebuilder/gui/SkyEditorScreen.java`
- Create: `src/main/java/de/resourcebuilder/gui/MenuEditorScreen.java`
- Modify: `src/main/java/de/resourcebuilder/mixin/StudioRenderMixins.java`
- Test: `src/smoke/java/verify/ImageImportSmoke.java`

**Interfaces:**
- Produces: validated/copy-on-success imports and runtime texture identifiers for sky/menu layers.

- [ ] Write failing validation tests for type, size, dimensions, copying, and rollback.
- [ ] Implement import storage and editors.
- [ ] Hook sun, moon, stars, clouds, fog, background image, and panorama rendering.
- [ ] Verify imported PNGs survive restart and malformed files preserve the previous asset; commit.

### Task 6: Vanilla lightning and configurable pop colors

**Files:**
- Modify: `src/main/java/de/resourcebuilder/KillEffects.java`
- Modify: `src/main/java/de/resourcebuilder/gui/KillEffectCard.java`
- Test: `src/smoke/java/verify/KillEffectSmoke.java`

**Interfaces:**
- Consumes: Task 1 effect colors.
- Produces: client-only vanilla lightning and colored finisher particles.

- [ ] Add failing entity/type/color checks.
- [ ] Replace synthetic Lightning particles with a visual-only `LightningBolt` and apply configured RGB to pop effects.
- [ ] Verify no damage, fire, or server entity is produced; commit.

### Task 7: Full integration, dual-version port, and release artifacts

**Files:**
- Modify: `README.md`
- Modify: `gradle.properties`
- Modify: 1.21.11 equivalents on `codex/fabric-1.21.11`

**Interfaces:**
- Consumes: Tasks 1-6.
- Produces: v3.0.0 JARs for both supported Minecraft versions.

- [ ] Run formatting, `git diff --check`, `build`, and `runSmoke` on 26.2.
- [ ] Port only the v3 commits to 1.21.11 and resolve API differences.
- [ ] Run formatting, `git diff --check`, `build`, and `runSmoke` on 1.21.11.
- [ ] Inspect both screenshots and JAR metadata, then commit each branch.
