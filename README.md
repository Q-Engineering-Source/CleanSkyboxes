# CleanSkyboxes

Client-side custom skybox support for Cleanroom 1.12.2, based on the existing FabricSkyBoxes/Nuit and OptiFine/MCPatcher compatibility work in this repository.

## Port status

The build now targets Cleanroom 1.12.2. The active source tree supports the registered FabricSkyBoxes/Nuit types: monocolor, end, overworld, six-face, single-sprite, animated, single-sprite animated, and multi-texture skyboxes. It evaluates fade and common world conditions, draws skyboxes over vanilla skies, and supports custom sun, moon, stars, independent decoration rotation, custom blend factors/equations/channel alpha, fog color/density, and the Actinium Iris phase bridge. It also converts resource-pack OptiFine/MCPatcher `skyN.properties` layers, including relative textures, fades, arbitrary rotation axes, blend modes, weather-weighted alpha, biome, height, and day-loop conditions. The Forge entry point, toggle key, and resource discovery are ported too. The previous NeoForge 1.21.1 implementation is kept under `src/modern-reference` and is not compiled. The OptiFine importer targets this sky-layer properties format; it does not implement unrelated OptiFine features outside that format.

Resource discovery scans active directory and ZIP packs for `assets/fabricskyboxes/sky/*.json`. The 1.12.2 resource manager does not expose a list operation, so the port reads the active pack list through mixin accessors and asks the resource manager for each discovered ID.

## Build

Use JDK 25 and run:

```powershell
.\gradlew.bat build
```

The development classpath includes Actinium from JitPack, pinned to commit `c318ed54` so its Cleanroom and Iris APIs match the local Actinium checkout. Its JitPack POM omits runtime dependencies, so the dev run declares Actinium's ANTLR, GLSL transformation, JCPP, JOML, and Gson libraries explicitly. Actinium remains optional in ordinary modpacks.

## GitHub Actions

Pushes and manual runs build the dev and distributable jars. Pushing a tag creates a GitHub release containing the distributable jar. The manual CurseForge/Modrinth workflow publishes only to services with a project ID configured in the repository variables `CURSEFORGE_PROJECT_ID` or `MODRINTH_PROJECT_ID`; their matching API tokens must be stored as `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN` secrets.

## License

This project is licensed under the [MIT License](LICENSE).
