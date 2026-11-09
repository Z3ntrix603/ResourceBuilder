# ResourceBuilder 🎨

> **ResourceBuilder** is a high-performance Minecraft Fabric mod for dynamic, on-the-fly texture pack mixing and custom sound selection without restarting the client.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2%20%7C%201.21.11-brightgreen.svg)](https://fabricmc.net/)
[![Fabric](https://img.shields.io/badge/ModLoader-Fabric-blue.svg)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## ✨ Features

- **Live Dynamic Mixing:** Mix textures (Swords, Crystals, Totems, Armor, Fire, Cobwebs, Particles, etc.) from any of your installed resource packs into a single active live pack.
- **Obsidian Dark Studio UI:** Modern dark slate UI with smooth pagination, category sidebar, and real-time live preview cards.
- **Sound Selector & Preview:** Customize hit sounds, crystal explosions, mace smashes, totem pops, and eat sounds with in-game audio previews.
- **Auto Animation Fixing:** Automatically fixes static and animated low-fire textures, particle layers, and armor textures to prevent missing textures.
- **Instant Hot-Reload:** Changes apply immediately in-game without restarting Minecraft.
- **Preset System:** Save, export, and load your favorite PvP texture combinations with 1 click.
- **Full Reset:** Instantly revert all textures back to clean vanilla defaults.

---

## ⌨️ Commands & Keybinds

| Action | Command / Key | Description |
|---|---|---|
| Open Studio | `/resource` or `/rb` | Opens the ResourceBuilder Studio UI |
| Keybind | Configurable | Can be bound in Options → Controls → Key Binds |

---

## 🛠️ Building from Source

### Prerequisites
- **JDK 21+**
- Git

### Build Instructions
```bash
git clone https://github.com/<username>/ResourceBuilder.git
cd ResourceBuilder
./gradlew build
```
The compiled `.jar` file will be located in `build/libs/`.

---

## 📜 License
This project is licensed under the [MIT License](LICENSE).
