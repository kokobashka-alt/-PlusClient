# zov 1.3 — recovered sources

This project was reconstructed from compiled Fabric mod classes. It targets
Minecraft 1.21.11, Fabric Loader 0.19.3, Java 21, and Yarn
`1.21.11+build.6`.

The distributable JAR bundles Sodium, Lithium, and In-Game Account Switcher.

The Java files are decompiler output, not the original source code. Original
comments, formatting, and some local variable names cannot be recovered.
Decompiler artifacts may still require manual fixes.

Build on Windows:

```powershell
.\gradlew.bat build
```

Run a development client:

```powershell
.\gradlew.bat runClient
```

The recovered mod sources are under `src/main/java/ru/zov`. Third-party
classes that were embedded in the original mod are retained as binary
resources rather than treated as authored source code.
