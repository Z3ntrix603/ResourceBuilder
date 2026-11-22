# Resource Builder Studio

A client-side resource pack builder for Minecraft Java 26.2, Fabric Loader 0.20.1 or newer, Fabric API for 26.2, and Java 25. Built on ResourceBuilder 1.0.14 with original styles inspired by the reference video.

## Installation and controls

Place the mod JAR and Fabric API in your instance's `mods` folder. Replace any older Resource Builder JAR. Open the studio in a world with **R** or **/rb**. Use Minecraft's key bindings to change the studio and zoom keys.

- Choose **Items** or **Armor** to see placed crystals and equipped armor. Use **Settings → Controls** to toggle rotating previews.
- **Hotbar**, **Sky**, **Font**, and **Crosshair** each have a gallery. **Vitals** lets you pick different styles for hearts, hunger, and the experience bar.
- **Effects** contains texture styles for particles and fire. **Kill Effects** lets you turn on a cosmetic lightning bolt, an expanding ring, ascending souls, or a golden spark burst at the defeated entity. The effect is visible to you and starts turned off. Each finisher has a local sound, controlled by the Finisher sound toggle.
- **Mace FX** adds Shockwave, Fracture, and Impact effects to your mace hits. Each style has its own local sound and can be disabled independently.
- Singleplayer uses Minecraft's kill credit. On multiplayer servers, the client looks for damage attributed to you immediately before the death event. A server that omits one of these updates can cause the effect to be skipped.
- Left-click a texture to apply it. Right-click to add or remove a favorite. Scroll over the categories or gallery to browse.
- The studio also reads textures and sounds from ZIPs and folders in `resourcepacks`. Restart the game after adding a pack.
- Open **Profiles** to save or load a combination, reset the active mix, or export the textures and sounds to a ZIP under `resourcebuilder-exports`. Kill effects and other visual settings are stored locally.
- Adjust the studio's theme, size, and opacity in **Settings → Window**. The blurred background shows the live game. Drop a PNG or JPG on the Window page to use your own background.

## Building and testing

Run `gradlew.bat build` on Windows or `./gradlew build` with JDK 25. The mod JAR is written to `build/libs`.

Run `gradlew.bat runSmoke` to launch an isolated client with a disposable test world. The smoke checks cover HUD selections, loaded resources, export, gallery navigation, and kill effects. The client writes screenshots and a `PASSED` or `FAILED` marker in `build/studio-runtime`.

Every feature release is built for both Minecraft 26.2 and Minecraft 1.21.11. The 26.2 source is maintained on `main`; the 1.21.11 port is maintained on `codex/fabric-1.21.11`.
