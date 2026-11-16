# Moonfall Cinematic

A client-side, visual-only cinematic mod for **Minecraft Java 1.21.11** (Fabric).

## Features
- Full-screen cinematic overlay with letterbox bars and star field.
- Moon descent sequence, impact flash, expanding visual shockwave and fade-out.
- No entity damage, no block changes, no terrain destruction and no weapon mechanics.
- Trigger with **M** or the client command **/moonfall**.
- Runs entirely on the client; no server installation is required.

## Build
Requires Java 21 and Gradle 9.6.1:

```sh
gradle clean build
```

The remapped mod JAR is created in `build/libs/`.

## GitHub Actions
Every push to `main` runs the build workflow and uploads the JAR as an Actions artifact.
