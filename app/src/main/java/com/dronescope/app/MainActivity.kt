package com.dronescope.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dronescope.app.databinding.ActivityMainBinding
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.common.Velocity3D
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.et.create
import dji.v5.et.listen
import dji.v5.manager.KeyManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import kotlin.math.hypot

/**
 * Pantalla única de DroneScope (Fase 1).
 *
 * Muestra:
 *  - Estado del registro del SDK
 *  - Conexión del drone + modelo / firmware / serial
 *  - Telemetría en vivo (batería, GPS, altitud, velocidad, actitud, rumbo, etc.)
 *  - Test de Virtual Stick: ¿se puede controlar el drone por código? (sí/no)
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // --- Estado de telemetría (se rellena a medida que llegan los datos del SDK) ---
    private var model = "—"
    private var firmware = "—"
    private var serial = "—"
    private var battery = "—"
    private var voltage = "—"
    private var temperature = "—"
    private var gps = "—"
    private var satellites = "—"
    private var altitude = "—"
    private var velocity = "—"
    private var attitude = "—"
    private var heading = "—"
    private var flying = "—"

    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestRuntimePermissions()

        binding.btnTestVirtualStick.setOnClickListener { testVirtualStick() }

        observeSdk()
        render()
    }

    private fun observeSdk() {
        MsdkManager.registerState.observe(this) { state ->
            binding.tvRegister.text = when (state) {
                is MsdkManager.RegisterState.Idle -> "Registro del SDK: inactivo"
                is MsdkManager.RegisterState.Registering -> "Registro del SDK: registrando…"
                is MsdkManager.RegisterState.Success -> "Registro del SDK: ✅ OK"
                is MsdkManager.RegisterState.Failed ->
                    "Registro del SDK: ❌ FALLÓ\n${state.error}"
            }
            if (state is MsdkManager.RegisterState.Success) startListeningTelemetry()
        }

        MsdkManager.productConnected.observe(this) { connected ->
            binding.tvConnection.text =
                if (connected == true) "Drone: 🟢 CONECTADO" else "Drone: 🔴 desconectado"
        }

        MsdkManager.initProgress.observe(this) { p ->
            binding.tvInit.text = "Inicialización SDK: $p"
        }
    }

    /**
     * Se suscribe a todas las keys de telemetría. Cada key, con getOnce=true (por defecto),
     * dispara el valor actual apenas hay datos y después cada vez que cambia.
     */
    private fun startListeningTelemetry() {
        if (listening) return
        listening = true

        ProductKey.KeyProductType.create().listen(this) { v ->
            model = v?.name ?: "—"; render()
        }
        ProductKey.KeyFirmwareVersion.create().listen(this) { v ->
            firmware = v ?: "—"; render()
        }
        FlightControllerKey.KeySerialNumber.create().listen(this) { v ->
            serial = v ?: "—"; render()
        }

        BatteryKey.KeyChargeRemainingInPercent.create().listen(this) { v ->
            battery = v?.let { "$it %" } ?: "—"; render()
        }
        BatteryKey.KeyVoltage.create().listen(this) { v ->
            voltage = v?.let { "%.2f V".format(it / 1000.0) } ?: "—"; render()
        }
        BatteryKey.KeyBatteryTemperature.create().listen(this) { v ->
            temperature = v?.let { "%.1f °C".format(it.toDouble()) } ?: "—"; render()
        }

        FlightControllerKey.KeyAircraftLocation3D.create().listen(this) { v: LocationCoordinate3D? ->
            v?.let {
                gps = "%.6f, %.6f".format(it.latitude, it.longitude)
                altitude = "%.1f m".format(it.altitude)
            }
            render()
        }
        FlightControllerKey.KeyGPSSatelliteCount.create().listen(this) { v ->
            satellites = v?.toString() ?: "—"; render()
        }
        FlightControllerKey.KeyAircraftVelocity.create().listen(this) { v: Velocity3D? ->
            v?.let {
                val horizontal = hypot(it.x, it.y)
                velocity = "H %.1f m/s · V %.1f m/s".format(horizontal, -it.z)
            }
            render()
        }
        FlightControllerKey.KeyAircraftAttitude.create().listen(this) { v: Attitude? ->
            v?.let {
                attitude = "Pitch %.0f°  Roll %.0f°  Yaw %.0f°".format(it.pitch, it.roll, it.yaw)
            }
            render()
        }
        FlightControllerKey.KeyCompassHeading.create().listen(this) { v ->
            heading = v?.let { "%.0f°".format(it.toDouble()) } ?: "—"; render()
        }
        FlightControllerKey.KeyIsFlying.create().listen(this) { v ->
            flying = when (v) { true -> "sí"; false -> "no"; else -> "—" }; render()
        }
    }

    private fun render() {
        runOnUiThread {
            binding.tvProduct.text =
                "Modelo: $model\nFirmware: $firmware\nSerial: $serial"

            binding.tvTelemetry.text = buildString {
                appendLine("🔋 Batería: $battery   ($voltage · $temperature)")
                appendLine("📍 GPS: $gps")
                appendLine("🛰️ Satélites: $satellites")
                appendLine("⛰️ Altitud: $altitude")
                appendLine("💨 Velocidad: $velocity")
                appendLine("🧭 Rumbo: $heading")
                appendLine("🎚️ Actitud: $attitude")
                append("🚁 Volando: $flying")
            }
        }
    }

    /**
     * Test clave de la Fase 1: ¿este drone permite control por código (Virtual Stick)?
     * Intenta habilitarlo y reporta éxito o el error exacto del SDK. Lo desactiva enseguida.
     *
     * ⚠️ Hacelo con el drone en el piso y SIN hélices, nunca volando.
     */
    private fun testVirtualStick() {
        binding.tvVirtualStick.text = "Virtual Stick: probando…"

        VirtualStickManager.getInstance().enableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                runOnUiThread {
                    binding.tvVirtualStick.text =
                        "Virtual Stick: ✅ DISPONIBLE\nSe puede controlar el drone por código."
                }
                // Era solo una prueba: lo desactivamos de inmediato.
                VirtualStickManager.getInstance().disableVirtualStick(object : CommonCallbacks.CompletionCallback {
                    override fun onSuccess() { /* ok */ }
                    override fun onFailure(error: IDJIError) { /* ignorar */ }
                })
            }

            override fun onFailure(error: IDJIError) {
                runOnUiThread {
                    binding.tvVirtualStick.text =
                        "Virtual Stick: ❌ NO disponible ahora\n$error"
                }
            }
        })
    }

    private fun requestRuntimePermissions() {
        val needed = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancela todos los listeners registrados con `this` como holder.
        KeyManager.getInstance().cancelListen(this)
    }
}
