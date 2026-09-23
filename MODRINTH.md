# Modrinth release notes

This file is a publishing checklist, not part of the in-game documentation.

## Project fields

- **Title:** BareBonesBrews
- **Slug:** `barebonesbrews`
- **Summary:** Brew weaker versions of vanilla and modded potions in a heated water cauldron.
- **Project type:** Mod
- **Loader:** NeoForge
- **Minecraft version:** 1.21.1
- **Environment:** Client and server
- **Featured categories:** Magic, Game Mechanics, Food
- **License:** All Rights Reserved / No License
- **Source:** https://github.com/RainLasea/BareBonesBrews
- **Issues:** https://github.com/RainLasea/BareBonesBrews/issues
- **Long description:** Use the contents of `README.md`.

## Version 1.0.0

- **Version name:** BareBonesBrews 1.0.0 for NeoForge 1.21.1
- **Version number:** `1.0.0`
- **Primary file:** `build/libs/barebonesbrews-1.0.0.jar`
- **Changelog:** Use the `1.0.0` section of `CHANGELOG.md`.
- **Required platform:** NeoForge
- **Optional dependencies:** Jade, Just Enough Items (JEI), Roughly Enough Items (REI), EMI, Hexalia

Use the **Release** version type only after testing a clean client and a dedicated server. Otherwise,
publish the first file as **Beta** and promote a later tested build.

## Before submitting

- Build with `gradlew.bat clean build` on Windows.
- Confirm the jar contains `META-INF/neoforge.mods.toml` and `icon.png`.
- Launch once without optional mods, then repeat with the integrations you intend to list.
- Test joining a dedicated server with the same jar installed on both sides.
- Upload only the normal release jar as the primary file. Do not upload a dev jar as an additional
  game-version build.
- Set optional dependencies on the uploaded version, not only in the project description.
- Use real in-game screenshots for the gallery.
- Enable Modrinth's **Contains AI-generated content: Text** disclosure if this prepared summary or
  description is used on the project page.

The icon in this repository is 512 x 512 pixels. Only upload it if its original artwork and editing
process comply with Modrinth's image rules.
