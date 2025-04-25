package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.content.Context

// Estás son las URL que se nos mostrarán en el WebView
object WebViewURL {
    /**
     * Beiman = https://beimancpp.tucitamedica.es
     * Kairos24h = https://controlhorario.kairos24h.es
     * Máquina = http://192.168.25.67:8008/kairos24h
     */


    const val HOST = "https://sermanten.kairos24h.es"
    //const val HOST = "http://192.168.25.67:8008/kairos24h"
    const val ENTRY_POINT = "/index.php"
    const val URL_USADA = "$HOST$ENTRY_POINT"

    const val ACTION_LOGIN = "r=site/index"
    const val ACTION_FORGOTPASS = "r=site/solicitudRestablecerClave"
    const val ACTION_CONSULTAR = "r=explotacion/consultarExplotacion"

    const val LOGIN = "$URL_USADA?$ACTION_LOGIN"
    const val forgotPassword = "$URL_USADA?$ACTION_FORGOTPASS"

    const val Fichaje = "$URL_USADA?$ACTION_CONSULTAR" +
            "&cTipExp=FICHAJE"
    const val Incidencia = "$URL_USADA?$ACTION_CONSULTAR" +
            "&cTipExp=INCIDENCIA" + "&cOpcionVisual=INCBAN"
    const val Horarios = "$URL_USADA?$ACTION_CONSULTAR" +
            "&cTipExp=HORARIO" + "&cModoVisual=HORMEN"
    const val Solicitudes = "$URL_USADA?$ACTION_CONSULTAR" +
            "&cTipExp=SOLICITUD"
    const val BandejaDeSolicitudes = "$URL_USADA?$ACTION_CONSULTAR" +
            "&cTipExp=SOLICITUD" + "&xEmpleadoBandeja="+ "&cOpcionVisual=SOLBAN"
}

// Esta será la URL que construiremos cuando desde el login de nuestra APK introduzcamos el Usuario y la Contraseña
object BuildURL {
    /**
     * Beiman = https://beimancpp.tucitamedica.es
     * Kairos24h = https://controlhorario.kairos24h.es
     * Máquina = http://localhost:8008/kairos24h
     */

    /*==================================================================*/
    const val HOST = "https://sermanten.kairos24h.es"
    //const val HOST = "http://192.168.25.67:8008/kairos24h"

    const val ENTRY_POINT = "/index.php"
    const val ACTION_FICHAJE = "r=explotacion/creaFichaje"
    const val ACTION_LOGIN = "r=wsExterno/loginExterno"
    const val ACTION_CONSULTHORARIO = "r=wsExterno/consultarHorarioExterno"
    const val ACTION_CONSULTFIC_DIA = "r=wsExterno/consultarFichajesExterno"
    const val ACTION_CONSULT_ALERTAS = "r=wsExterno/consultarAlertasExterno"

    const val X_GRUPO = ""
    const val C_KIOSKO = ""
    const val C_FIC_ORI = "APP"
    const val X_ENTIDAD = "1005"


    fun getStaticParams(context: Context): String {
        val creds = AuthManager.getUserCredentials(context)
        val xEntidad = creds.xEntidad ?: ""
        val xEmpleado = creds.xEmpleado ?: ""
        return "&xGrupo=$X_GRUPO" +
                //"&xEntidad=$xEntidad" +
                "&xEntidad=$X_ENTIDAD" + // Está Hardcodeado hasta que obtengamos el xEntidad del JSON
                "&xEmpleado=$xEmpleado" +
                "&cKiosko=$C_KIOSKO" +
                "&cDomFicOri=$C_FIC_ORI"
    }

    const val URL_USADA = "$HOST$ENTRY_POINT?"
    /*==================================================================*/

    const val LOGIN = URL_USADA + ACTION_LOGIN

    fun getCrearFichaje(context: Context): String = URL_USADA + ACTION_FICHAJE + getStaticParams(context)
    fun getMostrarHorarios(context: Context): String = URL_USADA + ACTION_CONSULTHORARIO + getStaticParams(context)
    fun getMostrarFichajes(context: Context): String = URL_USADA + ACTION_CONSULTFIC_DIA + getStaticParams(context)
    fun getMostrarAlertas(context: Context): String = URL_USADA + ACTION_CONSULT_ALERTAS + getStaticParams(context)
}
