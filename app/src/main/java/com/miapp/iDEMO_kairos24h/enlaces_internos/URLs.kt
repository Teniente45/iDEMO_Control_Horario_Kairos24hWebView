/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.miapp.kairos24h.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Este objeto centraliza el acceso a los recursos gráficos usados en la aplicación
object ImagenesApp {
    // Imagen del logo principal que se muestra en la pantalla de login
    @DrawableRes
    val logoCliente = R.drawable.kairos24h
    val logoCliente_x_programa = R.drawable.tragabuche
    val lodoDesarrolladora = R.drawable.logo_i3data



    // Centraliza las características del logo de empresa cliente
    val logoBoxModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 5.dp)
    // Estilo aplicado al logo (tamaño y proporción)
    val logoModifier = Modifier
        .width(356.dp)
        .height(100.dp)


    // Centraliza las características del contenedor del logo de desarrolladora
    val logoBoxModifierDev = Modifier
        .fillMaxWidth()
    // Estilo aplicado al logo de desarrolladora (tamaño y proporción)
    val logoModifierDev = Modifier
        .width(200.dp)
        .height(75.dp)

}



// Estás son las URL que se nos mostrarán en el WebView, se usa sólo para logearse desde la APK
object WebViewURL {
    const val HOST = "https://rincontragabuche.kairos24h.es"
    const val ENTRY_POINT = "/index.php"
    const val URL_USADA = "$HOST$ENTRY_POINT"

    const val ACTION_LOGIN = "r=wsExterno/loginExterno"

    const val LOGINAPK = "$URL_USADA?$ACTION_LOGIN"
}

// Esta será la URL que construiremos cuando desde el login de nuestra APK introduzcamos el Usuario y la Contraseña
object BuildURL {
    const val HOST = "https://rincontragabuche.kairos24h.es"
    const val ENTRY_POINT = "/index.php"
    const val URL_USADA = "$HOST$ENTRY_POINT?"

    const val ACTION_FORGOTPASS = "r=site/solicitudRestablecerClave"

    const val ACTION_LOGIN = "r=site/index"
    const val ACTION_FICHAJE = "r=explotacion/creaFichaje"
    const val ACTION_CONSULTAR = "r=explotacion/consultarExplotacion"

    const val ACTION_CONSULTHORARIO = "r=wsExterno/consultarHorarioExterno"
    const val ACTION_CONSULTFIC_DIA = "r=wsExterno/consultarFichajesExterno"
    const val ACTION_CONSULT_ALERTAS = "r=wsExterno/consultarAlertasExterno"

    const val INDEX = "$URL_USADA$ACTION_LOGIN"
    const val FORGOT_PASSWORD = "$URL_USADA?$ACTION_FORGOTPASS"


    const val FICHAJE = "$URL_USADA$ACTION_CONSULTAR" +
            "&cTipExp=FICHAJE"
    const val INCIDENCIA = "$URL_USADA$ACTION_CONSULTAR" +
            "&cTipExp=INCIDENCIA" + "&cOpcionVisual=INCBAN"
    const val HORARIOS = "$URL_USADA$ACTION_CONSULTAR" +
            "&cTipExp=HORARIO" + "&cModoVisual=HORMEN"
    const val SOLICITUDES = "$URL_USADA$ACTION_CONSULTAR" +
            "&cTipExp=SOLICITUD"



    const val X_GRUPO = ""
    const val C_KIOSKO = ""
    const val C_FIC_ORI = "APP"


    fun getStaticParams(context: Context): String {
        val creds = AuthManager.getUserCredentials(context)
        val xEntidad = creds.xEntidad ?: ""
        val xEmpleado = creds.xEmpleado ?: ""
        return "&xGrupo=$X_GRUPO" +
                "&xEntidad=$xEntidad" +
                "&xEmpleado=$xEmpleado" +
                "&cKiosko=$C_KIOSKO" +
                "&cFicOri=$C_FIC_ORI"
    }
    /*==================================================================*/

    fun getCrearFichaje(context: Context): String = URL_USADA + ACTION_FICHAJE + getStaticParams(context)
    fun getMostrarHorarios(context: Context): String = URL_USADA + ACTION_CONSULTHORARIO + getStaticParams(context)
    fun getMostrarFichajes(context: Context): String = URL_USADA + ACTION_CONSULTFIC_DIA + getStaticParams(context)
    fun getMostrarAlertas(context: Context): String = URL_USADA + ACTION_CONSULT_ALERTAS + getStaticParams(context)
}
