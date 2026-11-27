# 3D Equipment Sets Design

## Intent

Resource Builder gains complete pack-driven 3D equipment sets inspired by the dramatic silhouettes in the supplied reference video. A set changes the equipped armor geometry, matching back attachment, and the Spear as one coherent style. These are original Resource Builder designs, not copies of Refciamon's named packs.

The result must behave like a resource-pack selection: choose a set in the Studio, reload once, and see it on the real player in normal gameplay and in the inventory player preview.

## Goals

- Add a dedicated **3D Sets** Studio tab with a large rotating player preview.
- Ship three original built-in sets:
  - **Void Warden** — black/white plated armor, swept horns, broad monochrome Spear.
  - **Frost Seraph** — silver/ice armor, wing-like helmet and shoulders, cyan crystal Spear.
  - **Crimson Revenant** — black/red armor, hooked horns, back crest, barbed red Spear.
- Apply helmet, chest/shoulders, legs, boots, back attachment, and Spear together.
- Render the selected set in third person, inventory previews, and other normal player render contexts.
- Support external packs using the same Resource Builder model manifest format.
- Keep feature behavior matched across Minecraft 26.2 and 1.21.11.

## Non-goals

- No exact recreation or redistribution of the creators' models shown in the video.
- No GeckoLib dependency or skeletal animation system in the first release.
- No per-frame model generation, texture processing, filesystem access, or JSON parsing.
- No change to hitboxes, damage, reach, or Spear gameplay.

## Chosen approach

Use a mod-enhanced resource-pack format.

Pure vanilla item-model JSON is sufficient for the 3D Spear but cannot reproduce large horn, shoulder, and back geometry on worn armor. Resource Builder therefore adds a lightweight equipment render layer whose inputs still come entirely from the active pack.

The rejected alternatives are:

- **Vanilla-only pack:** portable, but cannot achieve the reference armor silhouette.
- **GeckoLib models:** powerful animation support, but adds a dependency and unnecessary runtime complexity for rigid armor pieces.

## Pack format

Each set lives under `assets/resourcebuilder/equipment_sets/<id>/`:

- `set.json` — display name, preview colors, component/model references, Spear model, attachment transforms.
- `armor.json` — named cuboids grouped under head, body, left/right arm, left/right leg, and back anchors.
- `armor.png` — nearest-neighbor texture atlas.
- `spear.json` — standard item-model-compatible cuboids and display transforms.
- `spear.png` — Spear texture atlas.

Manifests are bounded: maximum file sizes, cube counts, texture dimensions, and coordinate ranges are validated before use. Invalid external sets are ignored with one log warning and never replace the last valid active set.

## Runtime architecture

- `EquipmentSetManager` scans built-ins and installed resource packs during resource reload.
- JSON is parsed into immutable definitions, validated, and baked into cached `ModelPart` trees once per reload.
- Textures use Minecraft's resource manager and texture atlas path; no runtime image conversion occurs.
- `EquipmentSetRenderLayer` follows the vanilla humanoid pose by copying head/body/limb transforms to the matching custom roots before submission.
- The Spear uses Minecraft's normal item renderer and pack model, preserving first-person and third-person handling.
- Only the active set renders. A distance limit skips custom geometry on remote players beyond 64 blocks.
- Selecting **Off** restores ordinary armor and Spear resources.

## Studio experience

- The **3D Sets** tab uses wide cards and a rotating full-player preview holding the Spear.
- Each card shows the complete silhouette rather than separate armor texture thumbnails.
- Clicking a card activates the entire set and performs one resource reload.
- The bottom status line explicitly lists `Armor · Back · Spear` so users know the selection is bundled.
- External compatible sets appear beside the built-ins with their pack name.

## Compatibility and fallback

- 26.2 and 1.21.11 share manifest parsing, validation, set selection, and generated built-in assets.
- Thin version-specific render adapters handle Minecraft rendering API differences.
- The live resource pack continues to contain normal armor/Spear texture fallbacks. If custom geometry cannot load, the player remains visible with the fallback rather than missing or checkerboard equipment.

## Verification

- Pure smoke tests validate manifests, limits, transforms, set discovery, and atomic selection.
- Render smoke opens the 3D Sets tab, activates every built-in set, captures a preview, enters the world, and verifies the active cached model and Spear resources.
- Existing full smoke suites must remain green for both Minecraft versions.
- A performance assertion confirms model parsing/baking counts do not increase during repeated render frames.
