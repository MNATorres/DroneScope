package com.dronescope.app

import android.app.Application
import android.content.Context

/**
 * Clase Application de la app.
 *
 * Dos cosas OBLIGATORIAS para el MSDK v5:
 *  1) Helper.install() en attachBaseContext  -> descomprime/instala las librerías nativas del SDK.
 *     Si esto falta, el registro falla siempre.
 *  2) Iniciar el SDK en onCreate.
 */
class DroneScopeApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        // Instalador nativo del MSDK v5 (viene dentro del paquete dji-sdk-v5-aircraft)
        com.cySdkyc.clx.Helper.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        MsdkManager.init(this)
    }
}
