# Resource Builder v3 Design

## Intent

Turn Resource Builder into a comprehensive in-game resource-pack studio while keeping the clean blurred interface chosen in the first mockup. Texture-backed changes remain exportable; runtime-only visual controls are stored in the mod profile. Every feature ships for Minecraft 26.2 and 1.21.11.

## Scope

- Global live color correction: saturation, contrast, brightness, gamma, and hue.
- Particle Studio: independent colors for critical hits, totem pops, explosions, and enchantment particles plus size, amount, lifetime, and fullbright controls.
- Inventory editor under Hotbar, including inventory texture selection and configurable highlights behind important items such as totems.
- Fire editor: height, opacity, tint, and animation speed.
- Menu background builder: blur, tint, image import, and opacity.
- Sky editor: sun, moon, stars, clouds, and fog controls plus importing custom PNG sky layers.
- Custom main-menu panoramas imported from PNGs.
- Glint editor: color, strength, speed, and pattern.
- Natural Blocks gallery restricted to important terrain blocks.
- Share codes that round-trip all v3 runtime settings and selected resource styles.
- Replace the synthetic Lightning kill finisher with a visual-only vanilla lightning entity.
- Configurable colors for the existing pop/finisher particle effects.

## Product behavior

The existing top navigation stays compact. `Visuals` contains Global Color, Particles, Fire, and Glint. Inventory is grouped with Hotbar. Sky and Menus retain dedicated top-level areas. Imported files are copied into the Resource Builder config directory rather than referenced in place. Invalid images or share codes show an in-studio error and do not alter the active profile.

Runtime controls apply immediately and persist in `studio.json`. Exportable assets are composed by `LivePackManager`; settings that vanilla packs cannot represent remain explicitly marked `Mod effect`. The editor provides reset actions and live previews. No gameplay utility, combat automation, or server-side behavior is added.

## Approaches

- One monolithic screen: rejected because the controls would be unmanageable.
- Separate editor screens: rejected because it breaks the fast gallery workflow.
- Chosen: the first clean-tabs mockup with focused subpages and shared sliders, color controls, import controls, and preview panels.

## Constraints

- Fabric client mod only.
- Preserve current v2 selections and tolerate missing new JSON fields.
- No network dependency for editing, importing, sharing, or exporting.
- Imported PNGs have bounded dimensions and file sizes.
- Share codes are versioned and reject malformed or oversized payloads.
- Release artifacts must be tested separately on 26.2 and 1.21.11.
