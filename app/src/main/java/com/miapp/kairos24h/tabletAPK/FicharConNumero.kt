package com.miapp.kairos24h.tabletAPK

import android.content.res.Configuration
import android.text.InputType
import android.app.ActivityManager
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.toColorInt
import com.example.relojfichajeskairos24h.FichajesSQLiteHelper
import com.example.relojfichajeskairos24h.iniciarReintentosAutomaticos
import com.google.gson.Gson
import com.miapp.kairos24h.enlaces_internos.BuildURLtablet
import com.miapp.kairos24h.enlaces_internos.ImagenesTablet
import com.miapp.kairos24h.enlaces_internos.ImagenesMovil
import com.bumptech.glide.Glide
import com.miapp.kairos24h.R
import com.miapp.kairos24h.deviceOwner.MyDeviceAdminReceiver
import com.miapp.kairos24h.sesionesYSeguridad.GPSUtils
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Actividad principal de la aplicación de fichaje Kairos24h
class MainActivity : AppCompatActivity() {

    // Handler para manejar temporizadores en el hilo principal
    private val handler = Handler(Looper.getMainLooper())

    // Campo de texto donde se introduce el código de fichaje
    private lateinit var campoTexto: EditText

    // Texto dinámico que muestra mensajes al usuario
    private lateinit var mensajeDinamico: TextView

    // Reproductor de audio para efectos de sonido al fichar
    private var mediaPlayer: MediaPlayer? = null

    // Acumulador para recoger los números introducidos por el usuario
    private val stringBuilder = StringBuilder()

    // Duración del mensaje en pantalla en milisegundos
    private val duracionMensajeMs = 6000L

    companion object {
        // Colores personalizados para los mensajes visuales
        val COLOR_INCORRECTO = "#DC143C".toColorInt()
        val COLOR_CORRECTO = "#4F8ABA".toColorInt()
    }

    @SuppressLint("ClickableViewAccessibility", "DiscouragedApi", "UseKtx")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.portada)

        val rootView = findViewById<View>(android.R.id.content)
        rootView.setOnTouchListener { _, _ ->
            if (::mensajeDinamico.isInitialized && mensajeDinamico.visibility == View.VISIBLE) {
                mensajeDinamico.visibility = View.GONE
                handler.removeCallbacksAndMessages(null)
            }
            false // permite que el resto de toques funcionen normalmente
        }

        // Registrar este paquete como autorizado para Lock Task (modo quiosco)
        val devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = android.content.ComponentName(this, MyDeviceAdminReceiver::class.java)

        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            devicePolicyManager.setLockTaskPackages(componentName, arrayOf(packageName))
        }

        // Iniciar Lock Task si está permitido por DevicePolicyManager
        if (devicePolicyManager.isLockTaskPermitted(packageName)) {
            startLockTask()
            Log.d("MainActivity", "Modo quiosco activado correctamente.")
        }

        // Ocultar barra de navegación y de estado (modo inmersivo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }


        // Revisar si debe iniciar en modo kiosco tras reinicio
        val preferencias = getSharedPreferences("prefs_kiosco", MODE_PRIVATE)
        val modoKioscoActivo = preferencias.getBoolean("activar_kiosco", false)
        if (modoKioscoActivo) {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                @Suppress("DEPRECATION")
                startLockTask()
                Log.d("MainActivity", "Modo kiosco iniciado tras reinicio.")
            }
        }

        // Cargar logos dinámicamente
        val logo1 = findViewById<ImageView>(R.id.logo1)
        ImagenesTablet.cargarLogoClienteEnImageView(this, logo1)
        Log.d("LogoCliente", "URL del logo cargado: ${ImagenesTablet.getLogoCliente(this)}")

        val logo2 = findViewById<ImageView>(R.id.logo2)
        val logo2ResId = resources.getIdentifier(ImagenesTablet.LOGO_DESARROLLADORA, "drawable", packageName)
        logo2.setImageResource(logo2ResId)


        // Permitir cambiar entre propiedades verticales y horizontales de los logos
        val usarVertical = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        fun String.toLayoutSize(): Int = when (this) {
            "wrap_content" -> ViewGroup.LayoutParams.WRAP_CONTENT
            "match_parent" -> ViewGroup.LayoutParams.MATCH_PARENT
            else -> this.replace("dp", "").toIntOrNull()?.let {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, it.toFloat(), resources.displayMetrics).toInt()
            } ?: ViewGroup.LayoutParams.WRAP_CONTENT
        }
        // toPixelSize moved to class scope below

        if (usarVertical) {
            logo1.layoutParams = logo1.layoutParams.apply {
                width = ImagenesTablet.Vertical.LOGO_CLIENTE.width.toLayoutSize()
                height = ImagenesTablet.Vertical.LOGO_CLIENTE.height.toLayoutSize()
            }
            (logo1.layoutParams as? LinearLayout.LayoutParams)?.apply {
                gravity = when (ImagenesTablet.Vertical.LOGO_CLIENTE.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
                val marginTopPx = ImagenesTablet.Vertical.LOGO_CLIENTE.marginTop.toPixelSize()
                val marginBottomPx = ImagenesTablet.Vertical.LOGO_CLIENTE.marginBottom.toPixelSize()
                setMargins(0, marginTopPx, 0, marginBottomPx)
            }

            logo2.layoutParams = logo2.layoutParams.apply {
                width = ImagenesTablet.Vertical.LOGO_DESARROLLADORA.width.toLayoutSize()
                height = ImagenesTablet.Vertical.LOGO_DESARROLLADORA.height.toLayoutSize()
            }
            (logo2.layoutParams as? LinearLayout.LayoutParams)?.gravity =
                when (ImagenesTablet.Vertical.LOGO_DESARROLLADORA.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
        } else {
            logo1.layoutParams = logo1.layoutParams.apply {
                width = ImagenesTablet.Horizontal.LOGO_CLIENTE.width.toLayoutSize()
                height = ImagenesTablet.Horizontal.LOGO_CLIENTE.height.toLayoutSize()
            }
            (logo1.layoutParams as? LinearLayout.LayoutParams)?.apply {
                gravity = when (ImagenesTablet.Horizontal.LOGO_CLIENTE.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
                val marginTopPx = ImagenesTablet.Horizontal.LOGO_CLIENTE.marginTop.toPixelSize()
                val marginBottomPx = ImagenesTablet.Horizontal.LOGO_CLIENTE.marginBottom.toPixelSize()
                setMargins(0, marginTopPx, 0, marginBottomPx)
            }

            logo2.layoutParams = logo2.layoutParams.apply {
                width = ImagenesTablet.Horizontal.LOGO_DESARROLLADORA.width.toLayoutSize()
                height = ImagenesTablet.Horizontal.LOGO_DESARROLLADORA.height.toLayoutSize()
            }
            (logo2.layoutParams as? LinearLayout.LayoutParams)?.gravity =
                when (ImagenesTablet.Horizontal.LOGO_DESARROLLADORA.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
        }

        // Inicialización de vistas y botones
        campoTexto = findViewById(R.id.campoTexto)
        mensajeDinamico = findViewById(R.id.mensajeDinamico)
        val btnEntrada = findViewById<Button>(R.id.btn_entrada)
        val btnSalida = findViewById<Button>(R.id.btn_salida)
        val btnBorrarTeclado = findViewById<Button>(R.id.btnBorrarTeclado)

        // Asignar funcionalidad a los botones numéricos
        listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        ).forEach { id ->
            findViewById<Button>(id).setOnClickListener { view ->
                val button = view as? Button ?: return@setOnClickListener
                if (stringBuilder.length < 4) {
                    stringBuilder.append(button.tag ?: button.text.toString())
                    campoTexto.setText(stringBuilder.toString())
                    animarBoton(button)
                    resetearInactividad()
                }
            }
        }

        // Borrar el campo de texto
        btnBorrarTeclado.setOnClickListener {
            borrarCampoTexto()
            resetearInactividad()
        }

        // Fichaje de entrada
        btnEntrada.setOnClickListener {
            manejarCodigoEntradaSalida(stringBuilder.toString(), "ENTRADA")
            borrarCampoTexto()
            resetearInactividad()
        }

        // Fichaje de salida
        btnSalida.setOnClickListener {
            manejarCodigoEntradaSalida(stringBuilder.toString(), "SALIDA")
            borrarCampoTexto()
            resetearInactividad()
        }

        // Detectar pulsación larga para salir de la app desde el logo
        logo1.setOnLongClickListener {
            mostrarDialogoConfirmacionSalida()
            true
        }

        // Activar temporizador de limpieza de inactividad
        resetearInactividad()
        iniciarReintentosAutomaticos(this) // Activa la lógica de reintento cada 10 segundos
        Log.d("MainActivity", "Lógica de reintento automático iniciada correctamente.")

        // Mostrar contenido de la base de datos 'informado' en Logcat
        mostrarContenidoDeBaseDeDatos(this)
    }

    // Mostrar diálogo de confirmación para salir
    private fun mostrarDialogoConfirmacionSalida() {
        AlertDialog.Builder(this)
            .setTitle("¿Seguro que quieres salir?")
            .setPositiveButton("Salir") { _, _ -> solicitarPinParaSalir() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Solicitar PIN para confirmar salida de la app
    private fun solicitarPinParaSalir() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        AlertDialog.Builder(this)
            .setTitle("Introduce el PIN")
            .setView(input)
            .setPositiveButton("Aceptar") { dialog, _ ->
                if (input.text.toString() == "1005") {
                    dialog.dismiss()
                    salirAlLauncher()
                } else {
                    dialog.dismiss()
                    AlertDialog.Builder(this)
                        .setMessage("PIN incorrecto")
                        .setPositiveButton("Aceptar", null)
                        .show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Enviar al launcher principal del dispositivo y salir del modo kiosco si está activo
    private fun salirAlLauncher() {
        stopLockTask()

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    // Mostrar mensaje animado en la interfaz con texto, color y audio
    @SuppressLint("ClickableViewAccessibility")
    private fun mostrarMensajeDinamico(texto: String, color: Int, nombreAudio: String? = null) {
        mensajeDinamico.text = texto
        mensajeDinamico.setTextColor(color)
        mensajeDinamico.textSize = 25f
        mensajeDinamico.visibility = View.VISIBLE
        mensajeDinamico.setOnTouchListener { v, _ ->
            v.visibility = View.GONE
            handler.removeCallbacksAndMessages(null)
            true
        }

        nombreAudio?.let { reproducirAudio(it) }

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            mensajeDinamico.visibility = View.GONE
        }, duracionMensajeMs)
    }

    // Lógica para manejar un código numérico y tipo de fichaje
    private fun manejarCodigoEntradaSalida(codigo: String, tipo: String) {
        codigo.toIntOrNull()?.let {
            if (hayConexionInternet()) {
                // Verificar si el GPS está activado antes de intentar fichar
                // IMPORTANTE GPS
                val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    mostrarMensajeDinamico("Fichaje bloqueado. Encienda el GPS.", COLOR_INCORRECTO)
                    return
                }
                // Obtener coordenadas GPS para el fichaje
                val latitud = GPSUtils.obtenerLatitud(this)
                val longitud = GPSUtils.obtenerLongitud(this)
                // Añadir coordenadas GPS a la URL del fichaje
                val url = BuildURLtablet.getSetFichaje(this)
                    .replace("cEmpCppExt=", "cEmpCppExt=${URLEncoder.encode(it.toString(), "UTF-8")}")
                    .replace("cTipFic=", "cTipFic=${URLEncoder.encode(tipo, "UTF-8")}")
                    .plus("&tGpsLat=${URLEncoder.encode(latitud.toString(), "UTF-8")}")
                    .plus("&tGpsLon=${URLEncoder.encode(longitud.toString(), "UTF-8")}")
                Log.d("FichajeApp", "URL generada para fichaje: $url")
                enviarFichajeAServidor(url)
            } else {
                mostrarMensajeDinamico("No estás conectado a Internet", COLOR_INCORRECTO, "no_internet")
                Log.d("FichajeApp", "No hay conexión. Fichaje guardado localmente.")
            }
        } ?: mostrarMensajeDinamico("Código incorrecto", COLOR_INCORRECTO)
    }

    // Comprobar si hay conexión a internet activa
    @Suppress("DEPRECATION")
    private fun hayConexionInternet(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    // Animar visualmente el botón pulsado
    private fun animarBoton(button: Button) {
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.5f, 1f)
        val alpha = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.5f, 1f)
        scaleX.duration = 400
        scaleY.duration = 400
        alpha.duration = 400
        val interpolator = LinearInterpolator()
        scaleX.interpolator = interpolator
        scaleY.interpolator = interpolator
        alpha.interpolator = interpolator
        scaleX.start()
        scaleY.start()
        alpha.start()
    }

    // Reiniciar el temporizador de limpieza de inactividad
    private fun resetearInactividad() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            borrarCampoTexto()
        }, 20000L) // 20 segundos
    }

    // Limpiar el campo de texto y reiniciar acumulador
    private fun borrarCampoTexto() {
        campoTexto.setText("")
        stringBuilder.clear()
    }

    // Enviar la URL generada al servidor usando HttpURLConnection
    private fun enviarFichajeAServidor(url: String) {
        Thread {
            Log.d("FichajeApp", "Invocando URL al servidor: $url")
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                }

                connection.inputStream.use { stream ->
                    val responseText = stream.bufferedReader().use { it.readText() }
                    Log.d("FichajeApp", "Respuesta del servidor: $responseText")

                    // Guardar respuesta en base de datos si corresponde
                    val dbHelper = FichajesSQLiteHelper(this@MainActivity)
                    val jsonResponse = JSONObject(responseText)
                    val codigoEmpleado = url.substringAfter("cEmpCppExt=").substringBefore("&").toString()
                    dbHelper.insertarFichajeDesdeJson(jsonResponse, codigoEmpleado)
                    Log.d("SQLite", "Registro insertado: xFichaje=${jsonResponse.optString("xFichaje")}, cTipFic=${jsonResponse.optString("cTipFic")}")

                    val respuesta = Gson().fromJson(responseText, RespuestaFichaje::class.java)

                    runOnUiThread {
                        // Procesar la respuesta y mostrar mensaje visual
                        val codigoEnviado = url.substringAfter("cEmpCppExt=").substringBefore("&")
                        val esFichajeCorrecto = respuesta.message.isNullOrBlank()
                        val tipo = respuesta.cTipFic?.uppercase()

                        // Extraer el nombre del empleado para incluirlo en el mensaje de confirmación
                        val sEmpleado = jsonResponse.optString("sEmpleado", "Empleado")
                        val mensajeVisual = if (esFichajeCorrecto)
                            "($codigoEnviado) $sEmpleado $tipo a las ${respuesta.hFichaje}h"
                        else
                            "($codigoEnviado) Fichaje Incorrecto"

                        val nombreAudio = when {
                            !esFichajeCorrecto -> "codigo_incorrecto"
                            tipo == "ENTRADA" -> "fichaje_de_entrada"
                            tipo == "SALIDA" -> "fichaje_de_salida_correcto"
                            else -> null
                        }

                        val color = if (esFichajeCorrecto) COLOR_CORRECTO else COLOR_INCORRECTO
                        mostrarMensajeDinamico(mensajeVisual, color, nombreAudio)
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    val codigoEnviado = url.substringAfter("cEmpCppExt=").substringBefore("&")
                    val errorMsgVisual = "($codigoEnviado) Error de conexión al fichar"
                    mostrarMensajeDinamico(errorMsgVisual, COLOR_INCORRECTO, "no_internet")
                }
            }
        }.start()
    }

    // Reproduce un archivo de audio si existe
    @Suppress("DiscouragedApi")
    private fun reproducirAudio(nombreArchivo: String) {
        val resId = resources.getIdentifier(nombreArchivo, "raw", packageName)
        if (resId != 0) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, resId)
            mediaPlayer?.start()
        } else {
            Log.e("Audio", "No se encontró el archivo de audio: $nombreArchivo")
        }
    }

    // Libera recursos del reproductor al cerrar la actividad
    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    // Mantener el modo inmersivo al cambiar el foco de la ventana
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
            }
        }
    }

    // Función de extensión movida al nivel de clase para acceso en onCreate y onConfigurationChanged
    fun String.toPixelSize(): Int {
        return if (this.endsWith("dp")) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.removeSuffix("dp").toFloat(), resources.displayMetrics).toInt()
        } else if (this.endsWith("sp")) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, this.removeSuffix("sp").toFloat(), resources.displayMetrics).toInt()
        } else {
            0
        }
    }
    // Manejar el cambio de configuración para aplicar las propiedades visuales correctas a los logos
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val usarVertical = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT

        val logo1 = findViewById<ImageView>(R.id.logo1)
        val logo2 = findViewById<ImageView>(R.id.logo2)

        fun String.toLayoutSize(): Int = when (this) {
            "wrap_content" -> ViewGroup.LayoutParams.WRAP_CONTENT
            "match_parent" -> ViewGroup.LayoutParams.MATCH_PARENT
            else -> this.replace("dp", "").toIntOrNull()?.let {
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, it.toFloat(), resources.displayMetrics).toInt()
            } ?: ViewGroup.LayoutParams.WRAP_CONTENT
        }

        if (usarVertical) {
            logo1.layoutParams = logo1.layoutParams.apply {
                width = ImagenesTablet.Vertical.LOGO_CLIENTE.width.toLayoutSize()
                height = ImagenesTablet.Vertical.LOGO_CLIENTE.height.toLayoutSize()
            }
            (logo1.layoutParams as? LinearLayout.LayoutParams)?.apply {
                gravity = when (ImagenesTablet.Vertical.LOGO_CLIENTE.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
                val marginTopPx = ImagenesTablet.Vertical.LOGO_CLIENTE.marginTop.toPixelSize()
                val marginBottomPx = ImagenesTablet.Vertical.LOGO_CLIENTE.marginBottom.toPixelSize()
                setMargins(0, marginTopPx, 0, marginBottomPx)
            }

            logo2.layoutParams = logo2.layoutParams.apply {
                width = ImagenesTablet.Vertical.LOGO_DESARROLLADORA.width.toLayoutSize()
                height = ImagenesTablet.Vertical.LOGO_DESARROLLADORA.height.toLayoutSize()
            }
            (logo2.layoutParams as? LinearLayout.LayoutParams)?.gravity =
                when (ImagenesTablet.Vertical.LOGO_DESARROLLADORA.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
        } else {
            logo1.layoutParams = logo1.layoutParams.apply {
                width = ImagenesTablet.Horizontal.LOGO_CLIENTE.width.toLayoutSize()
                height = ImagenesTablet.Horizontal.LOGO_CLIENTE.height.toLayoutSize()
            }
            (logo1.layoutParams as? LinearLayout.LayoutParams)?.apply {
                gravity = when (ImagenesTablet.Horizontal.LOGO_CLIENTE.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
                val marginTopPx = ImagenesTablet.Horizontal.LOGO_CLIENTE.marginTop.toPixelSize()
                val marginBottomPx = ImagenesTablet.Horizontal.LOGO_CLIENTE.marginBottom.toPixelSize()
                setMargins(0, marginTopPx, 0, marginBottomPx)
            }

            logo2.layoutParams = logo2.layoutParams.apply {
                width = ImagenesTablet.Horizontal.LOGO_DESARROLLADORA.width.toLayoutSize()
                height = ImagenesTablet.Horizontal.LOGO_DESARROLLADORA.height.toLayoutSize()
            }
            (logo2.layoutParams as? LinearLayout.LayoutParams)?.gravity =
                when (ImagenesTablet.Horizontal.LOGO_DESARROLLADORA.gravity) {
                    "center_horizontal" -> Gravity.CENTER_HORIZONTAL
                    "center" -> Gravity.CENTER
                    "start" -> Gravity.START
                    "end" -> Gravity.END
                    else -> Gravity.NO_GRAVITY
                }
        }
    }
}

// Muestra todos los registros actuales de la tabla 'l_informados' en Logcat
fun mostrarContenidoDeBaseDeDatos(context: Context) {
    val db = FichajesSQLiteHelper(context).readableDatabase
    val cursor = db.rawQuery("SELECT * FROM l_informados", null)

    Log.d("DB_DUMP", "---- Comprobando registros en l_informados ----")
    if (cursor.count == 0) {
        Log.d("DB_DUMP", "No hay registros en la tabla 'l_informados'")
    } else {
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val cEmpCppExt = cursor.getString(cursor.getColumnIndexOrThrow("cEmpCppExt"))
            val xFichaje = cursor.getString(cursor.getColumnIndexOrThrow("xFichaje"))
            val cTipFic = cursor.getString(cursor.getColumnIndexOrThrow("cTipFic"))
            val fFichaje = cursor.getString(cursor.getColumnIndexOrThrow("fFichaje"))
            val hFichaje = cursor.getString(cursor.getColumnIndexOrThrow("hFichaje"))
            val lInformado = cursor.getString(cursor.getColumnIndexOrThrow("L_INFORMADO"))

            // Este log muestra todos los campos de la tabla 'l_informados', útil para depuración completa del estado actual de fichajes almacenados
            Log.d("DB_DUMP", "id=$id | cEmpCppExt=$cEmpCppExt | xFichaje=$xFichaje | cTipFic=$cTipFic | fFichaje=$fFichaje | hFichaje=$hFichaje | L_INFORMADO=$lInformado")
        }
    }

    cursor.close()
}



// Modelo de datos para interpretar la respuesta del servidor al fichar
data class RespuestaFichaje(
    val code: Int,
    val message: String?,
    val xFichaje: String?,
    val cTipFic: String?,
    val fFichaje: String?,
    val hFichaje: String?
)

