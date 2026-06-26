# DroneScope — Fase 1 (verificación del drone)

App Android (Kotlin + **DJI Mobile SDK v5.18.0**) que se conecta a un drone DJI
(Mini 3 / Mini 3 Pro u otros compatibles) a través del control **RC-N1** y muestra
**todos los datos que el SDK expone**, para confirmar acceso y compatibilidad antes
de seguir con el control.

## Qué muestra
- Estado de **registro** del SDK (y el error exacto si falla)
- **Conexión** del drone + **modelo**, **firmware**, **serial**
- Telemetría en vivo: **batería** (%, voltaje, temperatura), **GPS**, **satélites**,
  **altitud**, **velocidad**, **actitud** (pitch/roll/yaw), **rumbo**, **¿volando?**
- Botón **"Probar Virtual Stick"** → te dice si este drone permite **control por código**
  (✅/❌). Este es el test que decide si la Fase 2/3 son viables.

---

## ⚙️ Pasos para que funcione

### 1. Conseguir el App Key (gratis)
1. Entrá a https://developer.dji.com → creá cuenta.
2. **Developer Center → Apps → Create App** → tipo **Mobile SDK**.
3. **Package Name:** poné EXACTAMENTE `com.matias.dronetest`
   (tiene que coincidir con el `applicationId` de la app).
4. Copiá el **App Key** que te da.

### 2. Pegar el App Key
Abrí `gradle.properties` y reemplazá:
```
AIRCRAFT_API_KEY=PEGA_TU_APP_KEY_AQUI
```
por tu key real.

### 3. Abrir en Android Studio
- **File → Open** → elegí la carpeta `DroneScope`.
- Esperá el **Gradle Sync** (la primera vez descarga el SDK de DJI, tarda unos minutos).
- Si pide generar el **Gradle wrapper**, aceptá.

### 4. Generar la APK
- **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
- La APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

### 5. Probar con el drone (lo hace quien tenga el drone)
1. Instalar la APK en un teléfono Android.
2. Conectar el teléfono al control **RC-N1** por USB.
3. Encender el control y el drone.
4. Abrir **DroneScope** y mandar una **captura** de lo que aparece.

> El teléfono **debe tener internet** la primera vez (el registro del SDK valida online).

---

## Notas técnicas
- Solo compila para `arm64-v8a` (todos los teléfonos modernos).
- Si el registro falla, la pantalla muestra el código y la descripción del error de DJI.
- ⚠️ El test de Virtual Stick: hacerlo con el **drone en el piso y SIN hélices**.

## Si trabajás dentro de OneDrive
Pausá la sincronización de OneDrive mientras compilás (los archivos de `build/`
chocan con la sync y dan errores raros). El `.gitignore` ya excluye `build/`.
