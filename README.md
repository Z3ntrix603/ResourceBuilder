# Resource Builder Studio

A client-side resource pack builder for Minecraft Java 1.21.11, Fabric Loader 0.20.1 or newer, Fabric API for 1.21.11, and Java 21. Built on ResourceBuilder 1.0.14 with original styles inspired by the reference video.

## Installation and controls

Place the mod JAR and Fabric API in your instance's `mods` folder. Replace any older Resource Builder JAR. Open the studio in a world with **R** or **/rb**. Use Minecraft's key bindings to change the studio and zoom keys.

- Choose **Items** or **Armor** to see placed crystals and equipped armor. Use **Settings → Controls** to toggle rotating previews.
- **Hotbar**, **Sky**, **Font**, and **Crosshair** each have a gallery. **Vitals** lets you pick different styles for hearts, hunger, and the experience bar.
- **Visuals** controls global saturation/contrast/brightness/gamma/hue, particle colors and density, fire, glint, sky colors, and clean menu backgrounds. Sky and menu pages accept PNG files by drag-and-drop.
- **Effects** contains texture styles for particles and fire. **Kill Effects** provides a vanilla lightning bolt, expanding ring, ascending souls, or a spark burst at the defeated entity. Finishers support a custom color and local sound.
- **Mace FX** adds Shockwave, Fracture, and Impact effects to your mace hits. Each style has its own local sound and can be disabled independently.
- Singleplayer uses Minecraft's kill credit. On multiplayer servers, the client looks for damage attributed to you immediately before the death event. A server that omits one of these updates can cause the effect to be skipped.
- Left-click a texture to apply it. Right-click to add or remove a favorite. Scroll over the categories or gallery to browse.
- The studio also reads textures and sounds from ZIPs and folders in `resourcepacks`. Restart the game after adding a pack.
- Open **Profiles** to save/load combinations, export a ZIP, or copy/import an `RB3` share code for the visual setup.
- Adjust the studio's theme, size, and opacity in **Settings → Window**. The blurred background shows the live game. Drop a PNG or JPG on the Window page to use your own background.

## Building and testing

Run `gradlew.bat build` on Windows or `./gradlew build` with JDK 21. The mod JAR is written to `build/libs`.

Run `gradlew.bat runSmoke` to launch an isolated client with a disposable test world. The smoke checks cover HUD selections, loaded resources, export, gallery navigation, and kill effects. The client writes screenshots and a `PASSED` or `FAILED` marker in `build/studio-runtime`.
