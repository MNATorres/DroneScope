# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Qué es esto

DroneScope es una app Android (Kotlin + **DJI Mobile SDK v5.18.0**) que se conecta a un drone DJI (Mini 3 / Mini 3 Pro) vía el control **RC-N1** por USB. Es la **Fase 1** de un proyecto mayor: una APK de verificación que lee toda la telemetría que el SDK expone y testea si el drone permite control por código (Virtual Stick).

El proyecto completo tiene 3 fases: (1) esta APK de verificación, (2) app puente Android que expone WebSocket (`/telemetry` `/video` `/control` `/waypoints`), (3) app Python (PyQt6) en PC con mapa, video, gamepad y control por voz. El Android es un **puente fino**; la UI rica vive en la app Python. Waypoints reales NO existen en este drone → habrá que emularlos vía Virtual Stick en tiempo real con el RC conectado.

> El usuario es **principiante en Kotlin/Android** y trabaja en español. Preferí explicaciones claras y respuestas en español.

## Comandos

No hay `gradlew` chequeado para correr desde acá; el flujo normal es vía **Android Studio** (File → Open la carpeta, Gradle Sync, Build → Build APK(s)). Si querés línea de comandos en Windows:

```powershell
.\gradlew.bat assembleDebug      # genera app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat installDebug       # compila e instala en un dispositivo conectado por adb
.\gradlew.bat lint               # corre Android Lint (abortOnError=false, no frena el build)
.\gradlew.bat clean
```

No hay tests todavía (no existe `src/test` ni `src/androidTest`).

**Claude no puede compilar ni probar de verdad**: el registro del SDK valida online y la telemetría/Virtual Stick requieren el drone real + RC-N1. La validación final siempre la hace el usuario con el hardware.

## Arquitectura

Una sola pantalla, tres archivos en `app/src/main/java/com/dronescope/app/`:

- **`DroneScopeApplication.kt`** — clase `Application`. Dos pasos OBLIGATORIOS del MSDK v5, sin ellos el registro falla siempre:
  1. `com.cySdkyc.clx.Helper.install(this)` en `attachBaseContext` (instala las librerías nativas).
  2. `MsdkManager.init(this)` en `onCreate`.
- **`MsdkManager.kt`** — singleton (`object`) que maneja el ciclo de vida del SDK: `init → onInitProcess(INITIALIZE_COMPLETE) → registerApp() → onRegisterSuccess`. Expone estado vía **LiveData** (`registerState`, `productConnected`, `initProgress`). El registro recién se dispara cuando el init termina.
- **`MainActivity.kt`** — observa el LiveData de `MsdkManager`. Cuando el registro da `Success`, llama `startListeningTelemetry()`, que se suscribe a las keys del SDK con el patrón `SomeKey.create().listen(this) { ... }`. Cada listener actualiza un campo `String` y llama `render()`, que pinta todo en los `TextView`. Los listeners se cancelan en `onDestroy` con `KeyManager.getInstance().cancelListen(this)`.

Flujo de datos: **SDK callbacks → LiveData (MsdkManager) → observers (MainActivity) → render()**. La telemetría usa el sistema de **keys** del MSDK (`BatteryKey`, `FlightControllerKey`, `ProductKey`), no callbacks directos.

El test clave de la Fase 1 es `testVirtualStick()`: hace `enableVirtualStick` y reporta éxito/error exacto, después lo desactiva. **Siempre con el drone en el piso y SIN hélices.**

## Reglas críticas del build (no romper)

- **`applicationId = "com.matias.dronetest"`** debe coincidir EXACTAMENTE con el Package Name registrado en el DJI Developer Portal, o el registro falla. El `namespace`/paquete del código (`com.dronescope.app`) es independiente y puede diferir.
- El **App Key** se inyecta desde `gradle.properties` (`AIRCRAFT_API_KEY`) → `manifestPlaceholders["API_KEY"]` → `<meta-data com.dji.sdk.API_KEY>` en el manifest.
- Solo se compila para **`arm64-v8a`** (el MSDK v5 solo trae nativos para arm64).
- **No tocar** el bloque `packagingOptions` (lista de `doNotStrip` + `pickFirst`): viene del sample oficial de DJI; sin él las librerías nativas se corrompen y la app crashea.
- **No activar `minifyEnabled`/ProGuard agresivo**: el SDK de DJI se rompe si se ofusca/strippea.
- Las 3 dependencias del SDK tienen roles distintos: `compileOnly` (headers `-provided`), `implementation` (`-aircraft`, SDK principal), `runtimeOnly` (`-networkImp`, necesario para el registro). No las consolides.

## Toolchain

AGP 8.10.1 · Kotlin 1.9.24 · Gradle wrapper 8.11.1 · compileSdk/targetSdk 34 · minSdk 24 · JVM target 1.8 · viewBinding activado.

## Entorno

Carpeta movida fuera de OneDrive (a `C:\Users\matia\apps\`) para evitar que la sync choque con `build/`. Si el proyecto vuelve a estar dentro de OneDrive, pausar la sincronización al compilar.
