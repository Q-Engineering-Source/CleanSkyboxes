# CleanSkyboxes

Client-side custom skybox support for Cleanroom 1.12.2, based on the existing FabricSkyBoxes/Nuit and OptiFine/MCPatcher compatibility work in this repository.

## Port status

The build now targets Cleanroom 1.12.2. The active source tree supports the registered FabricSkyBoxes/Nuit types: monocolor, end, overworld, six-face, single-sprite, animated, single-sprite animated, and multi-texture skyboxes. It evaluates fade and common world conditions, draws skyboxes over vanilla skies, and supports custom sun, moon, stars, fog color/density, and the Actinium Iris phase bridge. It also converts common OptiFine/MCPatcher `skyN.properties` layers, including relative textures, fades, rotation, blend, weather, biome, height, and day-loop conditions. The Forge entry point, toggle key, and resource discovery are ported too. The previous NeoForge 1.21.1 implementation is kept under `src/modern-reference` and is not compiled. Remaining gaps are decoration-specific rotation, custom blender factors, and some OptiFine edge cases.

Resource discovery scans active directory and ZIP packs for `assets/fabricskyboxes/sky/*.json`. The 1.12.2 resource manager does not expose a list operation, so the port reads the active pack list through mixin accessors and asks the resource manager for each discovered ID.

## Build

Use JDK 25 and run:

```powershell
.\gradlew.bat build
```

The development classpath includes Actinium from JitPack, pinned to commit `c318ed54` so its Cleanroom and Iris APIs match the local Actinium checkout. Actinium remains optional at runtime.

## License

This project is licensed under the [MIT License](LICENSE).
