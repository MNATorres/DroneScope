package com.dronescope.app

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

/**
 * Maneja el ciclo de vida del SDK de DJI: init -> registro -> conexión del drone.
 *
 * Expone el estado vía LiveData para que la pantalla (MainActivity) lo observe.
 * Es un `object` (singleton) porque el SDK solo se inicia una vez por proceso.
 */
object MsdkManager {

    /** Estado del registro de la app contra los servidores de DJI. */
    sealed class RegisterState {
        data object Idle : RegisterState()
        data object Registering : RegisterState()
        data object Success : RegisterState()
        data class Failed(val error: IDJIError) : RegisterState()
    }

    val registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val productConnected = MutableLiveData(false)
    val initProgress = MutableLiveData("—")

    private var isInit = false

    fun init(context: Context) {
        registerState.postValue(RegisterState.Registering)

        SDKManager.getInstance().init(context, object : SDKManagerCallback {

            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                initProgress.postValue(event.name)
                // Cuando el SDK terminó de inicializar, recién ahí se puede registrar la app.
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    isInit = true
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onRegisterSuccess() {
                registerState.postValue(RegisterState.Success)
            }

            override fun onRegisterFailure(error: IDJIError) {
                registerState.postValue(RegisterState.Failed(error))
            }

            override fun onProductConnect(productId: Int) {
                productConnected.postValue(true)
            }

            override fun onProductDisconnect(productId: Int) {
                productConnected.postValue(false)
            }

            override fun onProductChanged(productId: Int) {
                // El producto cambió (p. ej. se reconectó). No hace falta hacer nada acá.
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                // Descarga de la base de datos de zonas de vuelo (FlySafe). No la usamos en Fase 1.
            }
        })
    }
}
