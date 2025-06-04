/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

@file:Suppress("DEPRECATION")

package com.miapp.kairos24h.movilAPK


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.miapp.kairos24h.R
import com.miapp.kairos24h.enlaces_internos.BuildURLmovil
import com.miapp.kairos24h.MainActivity
import com.miapp.kairos24h.sesionesYSeguridad.AuthManager
import com.miapp.kairos24h.sesionesYSeguridad.ManejoDeSesion
import com.miapp.kairos24h.sesionesYSeguridad.SeguridadUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class Fichar : ComponentActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private val sessionTimeoutMillis = 2 * 60 * 60 * 1000L // 2 horas

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebView.setWebContentsDebuggingEnabled(true)
        Log.d("Fichar", "onCreate iniciado")

        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)

        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            navigateToLogin()
            return
        }
        val usuario = intent.getStringExtra("usuario") ?: storedUser
        val password = intent.getStringExtra("password") ?: storedPassword

        // Creamos el FrameLayout raíz
        val root = FrameLayout(this).apply { id = View.generateViewId() }

        // Creamos y configuramos el WebView
        webView = WebView(this).apply {
            val webSettings = settings
            webSettings.javaScriptEnabled = true
            webSettings.loadWithOverviewMode = true
            webSettings.useWideViewPort = true
            webSettings.domStorageEnabled = true
            webSettings.setSupportZoom(true)
            webSettings.builtInZoomControls = true
            webSettings.displayZoomControls = false
            webSettings.javaScriptCanOpenWindowsAutomatically = true
            webSettings.setSupportMultipleWindows(true)
            webSettings.databaseEnabled = true
            webSettings.allowFileAccess = true
            webSettings.allowContentAccess = true
            webSettings.userAgentString =
                "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36"

            // Habilitar scrollbars dentro del área visible
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    val newWebView = WebView(view!!.context)
                    newWebView.settings.javaScriptEnabled = true
                    newWebView.settings.javaScriptCanOpenWindowsAutomatically = true
                    newWebView.settings.setSupportMultipleWindows(true)
                    newWebView.settings.domStorageEnabled = true
                    newWebView.settings.databaseEnabled = true
                    newWebView.settings.allowFileAccess = true
                    newWebView.settings.allowContentAccess = true

                    val transport = resultMsg?.obj as WebView.WebViewTransport
                    transport.webView = newWebView
                    resultMsg.sendToTarget()

                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Decodifica la contraseña antes de insertarla en el formulario
                    val passwordCodificada = AuthManager.getUserCredentials(this@Fichar).password
                    val password = java.net.URLDecoder.decode(passwordCodificada, "UTF-8")
                    // Decodifica el usuario antes de insertarlo en el formulario
                    val usuarioCodificado = AuthManager.getUserCredentials(this@Fichar).usuario
                    val usuario = java.net.URLDecoder.decode(usuarioCodificado, "UTF-8")
                    view?.evaluateJavascript(
                        """
                        (function() {
                            isMobile = () => true;
                            document.getElementsByName('LoginForm[username]')[0].value = '$usuario';
                            document.getElementsByName('LoginForm[password]')[0].value = '$password';
                            document.querySelector('form').submit();
                            
                            setTimeout(function() {
                                var panels = document.querySelectorAll('.panel, .panel-body, .panel-heading');
                                panels.forEach(function(panel) {
                                    panel.style.display = 'block';
                                    panel.style.visibility = 'visible';
                                    panel.style.opacity = '1';
                                    panel.style.maxHeight = 'none';
                                });
                                document.body.style.overflow = 'auto';
                                document.documentElement.style.overflow = 'auto';
                            }, 3000);
                        })();
                        """.trimIndent(),
                        null
                    )
                }
            }

            loadUrl(BuildURLmovil.getIndex(this@Fichar))
        }

        // ComposeView superpuesto
        val composeView = ComposeView(this).apply {
            setContent {
                FicharScreen(
                    webView = webView,
                    onLogout = { navigateToLogin() }
                )
            }
        }

        // Ajuste de LayoutParams para WebView: altura delimitada por barra superior (30dp) y barra inferior (56dp)
        val displayMetrics = resources.displayMetrics
        val topMarginPx = (30 * displayMetrics.density).toInt()
        val bottomMarginPx = (56 * displayMetrics.density).toInt()

        val webViewLayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ).apply {
            topMargin = topMarginPx
            bottomMargin = bottomMarginPx
        }

        root.addView(
            webView,
            webViewLayoutParams
        )
        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        setContentView(root)

        ManejoDeSesion.startActivitySimulationTimer(handler, webView, sessionTimeoutMillis)
    }


    // Método del ciclo de vida que notifica pausa a la lógica de sesión
    override fun onPause() {
        super.onPause()
        ManejoDeSesion.onPause()
    }

    override fun onStop() {
        super.onStop()
        ManejoDeSesion.onStop(webView)
    }

    override fun onResume() {
        super.onResume()
        ManejoDeSesion.onResume(webView)
    }

    // Redirige a la pantalla de login eliminando cookies, datos de sesión y reinicia la actividad
    private fun navigateToLogin() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("usuario")
            remove("password")
            apply()
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    // Manejador principal usado para controlar los tiempos de la sesión activa
}


// Composable principal de la pantalla de fichaje. Muestra WebView con login automático, cuadro para fichar, barra superior e inferior y lógica de navegación.
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FicharScreen(
    webView: WebView,
    onLogout: () -> Unit
) {
    // Controla si debe mostrarse la pantalla de carga
    var isLoading by remember { mutableStateOf(true) }
    // Controla la visibilidad del cuadro para fichar
    val showCuadroParaFicharState = remember { mutableStateOf(true) }
    // Lista de fichajes realizados (puede ser usada para mostrar historial o control)
    var fichajes by remember { mutableStateOf<List<String>>(emptyList()) }
    // Índice para alternar entre imágenes de usuario (cambia el avatar)
    var imageIndex by remember { mutableIntStateOf(0) }
    // Tipo de alerta de fichaje actual (usado para mostrar mensajes al usuario)
    var fichajeAlertTipo by remember { mutableStateOf<String?>(null) }
    // Ámbito de corrutina usado para manejar delays y tareas asincrónicas
    val scope = rememberCoroutineScope()
    // Controla la visibilidad del diálogo de confirmación para cerrar sesión
    val showLogoutDialog = remember { mutableStateOf(false) }

    // Lista de recursos de imagen para el avatar del usuario
    val imageList = listOf(
        R.drawable.cliente32,
    )

    // Ya no se necesita webViewState; usamos webView directamente
    // Contexto actual de la aplicación (necesario para acceder a preferencias y otros recursos)
    val context = LocalContext.current
    // Accede a las preferencias guardadas del usuario (credenciales y flags)
    val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    // Recupera el nombre de usuario desde las preferencias o usa valor por defecto
    val cUsuario = sharedPreferences.getString("usuario", "Usuario") ?: "Usuario"

    // Simula carga inicial de 1,5 segundos antes de mostrar contenido
    LaunchedEffect(Unit) {
        isLoading = true
        delay(1500)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Barra superior con avatar del usuario y botón para cerrar sesión
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(0xFFE2E4E5))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sección izquierda: botón avatar que alterna imagen + nombre del usuario
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { imageIndex = (imageIndex + 1) % imageList.size }
                ) {
                    Icon(
                        painter = painterResource(id = imageList[imageIndex]),
                        contentDescription = "Usuario",
                        modifier = Modifier.size(30.dp),
                        tint = Color.Unspecified
                    )
                }
                Text(
                    text = cUsuario,
                    color = Color(0xFF7599B6),
                    fontSize = 18.sp
                )
            }
            // Sección derecha: botón para cerrar sesión, abre un diálogo de confirmación
            IconButton(onClick = { showLogoutDialog.value = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cerrar32),
                    contentDescription = "Cerrar sesión",
                    modifier = Modifier.size(30.dp),
                    tint = Color.Unspecified
                )
            }
        }

        // Contenedor central que ocupa el espacio restante; contiene el WebView, cuadro de fichaje y mensajes
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // El WebView ya está en el FrameLayout de la Activity, no se necesita AndroidView aquí.

            // Pantalla de carga que se muestra mientras se realiza la autenticación automática
            LoadingScreen(isLoading = isLoading)

            // Cuadro emergente con botones de fichaje (Entrada/Salida) que solicita la ubicación GPS
            if (showCuadroParaFicharState.value) {
                // Lógica para mostrar u ocultar los botones de fichaje según lBotonesFichajeMovil
                val lBotonesFichajeMovil = sharedPreferences.getString("lBotonesFichajeMovil", "") ?: ""
                val mostrarBotones = lBotonesFichajeMovil != "N"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .background(Color.White)
                ) {
                    CuadroParaFichar(
                        isVisibleState = showCuadroParaFicharState,
                        fichajes = fichajes,
                        onFichaje = { tipo ->
                            obtenerCoord(
                                context,
                                onLocationObtained = { lat, lon ->
                                    if (lat == 0.0 || lon == 0.0) {
                                        Log.e("Fichar", "Ubicación inválida, no se enviará el fichaje")
                                        fichajeAlertTipo = "Ubicación inválida"
                                        return@obtenerCoord
                                    }
                                    fichajeAlertTipo = tipo
                                },
                                onShowAlert = { alertTipo ->
                                    fichajeAlertTipo = alertTipo
                                }
                            )
                        },
                        onShowAlert = { alertTipo ->
                            fichajeAlertTipo = alertTipo
                        },
                        webViewState = remember { mutableStateOf(webView) },
                        mostrarBotonesFichaje = mostrarBotones
                    )
                }
            }

            // Muestra un mensaje emergente si hay un error o advertencia en el proceso de fichaje
            fichajeAlertTipo?.let { tipo ->
                MensajeAlerta(
                    tipo = tipo,
                    onClose = { fichajeAlertTipo = null }
                )
            }
        }

        // Barra inferior con navegación entre secciones y botón para mostrar el cuadro de fichaje
        BottomNavigationBar(
            onNavigate = { url ->
                isLoading = true
                showCuadroParaFicharState.value = false
                webView.loadUrl(url)
                scope.launch {
                    delay(1500)
                    isLoading = false
                }
            },
            onToggleFichar = { showCuadroParaFicharState.value = true },
            hideCuadroParaFichar = { showCuadroParaFicharState.value = false },
            setIsLoading = { isLoading = it },
            scope = scope,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }

    // Diálogo modal que solicita confirmación para cerrar la sesión
    if (showLogoutDialog.value) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog.value = false },
            title = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF7599B6))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "¿Cerrar sesión?",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                Text(
                    "Si continuas cerrarás tu sesión, ¿Seguro que es lo que quieres hacer?",
                    color = Color.Black
                )
            },
            confirmButton = {},
            dismissButton = {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showLogoutDialog.value = false
                            webView.apply {
                                clearCache(true)
                                clearHistory()
                            }
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()
                            AuthManager.clearAllUserData(context)
                            onLogout()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7599B6),
                            contentColor = Color.White
                        ),
                        shape = RectangleShape
                    ) {
                        Text("Sí")
                    }

                    Spacer(modifier = Modifier.width(30.dp))

                    Button(
                        onClick = {
                            showLogoutDialog.value = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7599B6),
                            contentColor = Color.White
                        ),
                        shape = RectangleShape
                    ) {
                        Text("No")
                    }
                }
            },
            shape = RoundedCornerShape(30.dp)
        )
    }
}

// ================================== Preview de la pantalla ==================================
@Composable
@Preview(showBackground = true)
fun PreviewFicharScreen() {
    // No se puede previsualizar el WebView real en preview, así que pasamos un dummy
    FicharScreen(
        webView = WebView(LocalContext.current),
        onLogout = {}
    )
}
// ================================== Preview de la pantalla ==================================



internal fun fichar(context: Context, tipo: String, webView: WebView) {
    // Verifica si se tiene permiso de ubicación fina antes de continuar
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Si no hay permisos de GPS, muestra un mensaje y no continúa con el fichaje
    if (!hasPermission) {
        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
        Toast.makeText(context, "Debe aceptar los permisos de GPS para poder fichar.", Toast.LENGTH_SHORT).show()
        return
    }

    // Intenta obtener las coordenadas del dispositivo y realizar el fichaje con ellas
    try {
        obtenerCoord(
            context,
            onLocationObtained = { lat, lon ->
                if (lat == 0.0 || lon == 0.0) {
                    Log.e("Fichar", "Ubicación inválida, no se enviará el fichaje")
                    return@obtenerCoord
                }

                // Construye la URL de fichaje con el tipo y las coordenadas
                val urlFichaje = BuildURLmovil.getCrearFichaje(context) +
                        "&cTipFic=$tipo" +
                        "&tGpsLat=$lat" +
                        "&tGpsLon=$lon"

                Log.d("Fichar", "URL que se va a enviar desde WebView: $urlFichaje")
                // Ejecuta la URL de fichaje en el WebView
                webView.evaluateJavascript("window.location.href = '$urlFichaje';", null)
            },
            onShowAlert = { alertTipo ->
                Log.e("Fichar", "Alerta: $alertTipo")
            }
        )
    } catch (e: SecurityException) {
        Log.e("Fichar", "Error de seguridad al acceder a la ubicación: ${e.message}")
    }
}

fun obtenerCoord(
    context: Context,
    onLocationObtained: (lat: Double, lon: Double) -> Unit,
    onShowAlert: (String) -> Unit
) {
    // Obtiene los valores de control de seguridad desde AuthManager
    val (_, _, _, lComGPS, lComIP, lBotonesFichajeMovil) = AuthManager.getUserCredentials(context)
    // Log para verificar los valores de seguridad
    Log.d("Seguridad", "lComGPS=$lComGPS, lComIP=$lComIP, lBotonesFichajeMovil=$lBotonesFichajeMovil")
    // Registra advertencias si alguna de las condiciones de seguridad deshabilita el fichaje
    if (lComGPS != "S") Log.w("Seguridad", "El fichaje está deshabilitado por GPS: lComGPS=$lComGPS")
    if (lComIP != "S") Log.w("Seguridad", "El fichaje está deshabilitado por IP: lComIP=$lComIP")
    // Define si se debe validar el GPS e IP para el fichaje
    val validarGPS = lComGPS == "S"
    val validarIP = lComIP == "S"

    val scope = CoroutineScope(Dispatchers.Main)
    scope.launch {
        // Verifica que se cumplan las condiciones de seguridad configuradas antes de obtener ubicación
        val permitido = SeguridadUtils.checkSecurity(
            context,
            if (validarGPS) "S" else "N",
            if (validarIP) "S" else "N",
            "S"
        ) { mensaje ->
            onShowAlert(mensaje)
        }
        if (!permitido) return@launch

        // Cliente de ubicación para obtener la última localización disponible
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        // Verifica que los permisos de GPS estén concedidos
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Fichar", "No se cuenta con los permisos de ubicación.")
            onShowAlert("PROBLEMA GPS")
            return@launch
        }

        // Verifica que el GPS esté activado en el dispositivo
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.e("Fichar", "GPS desactivado.")
            onShowAlert("PROBLEMA GPS")
            return@launch
        }

        // Intenta obtener la última ubicación del dispositivo y valida si es real o falsa
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) {
                Log.e("Fichar", "No se pudo obtener la ubicación.")
                onShowAlert("PROBLEMA GPS")
                return@addOnSuccessListener
            }

            // Verifica si la ubicación está siendo falsificada (mock location)
            if (SeguridadUtils.isMockLocationEnabled()) {
                Log.e("Fichar", "Ubicación falsa detectada.")
                onShowAlert("POSIBLE UBI FALSA")
                return@addOnSuccessListener
            }

            onLocationObtained(location.latitude, location.longitude)
        }.addOnFailureListener { e ->
            Log.e("Fichar", "Error obteniendo ubicación: ${e.message}")
            onShowAlert("PROBLEMA GPS")
        }
    }
}
//============================================== FICHAJE DE LA APP =====================================

// Barra de navegación inferior que permite acceder a distintas secciones (Fichajes, Incidencias, etc.) y abrir el cuadro para fichar
@Composable
fun BottomNavigationBar(
    onNavigate: (String) -> Unit,
    onToggleFichar: () -> Unit,
    modifier: Modifier = Modifier,
    hideCuadroParaFichar: () -> Unit,
    setIsLoading: (Boolean) -> Unit,
    scope: CoroutineScope
) {
    // Controla si se ha pulsado el botón de fichar (para alternar su estado visual o funcional)
    var isChecked by remember { mutableStateOf(false) }

    // Contenedor horizontal que agrupa todos los botones de navegación
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFE2E4E5))
            .padding(2.dp)
            .zIndex(3f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón de fichar: lanza el cuadro para fichar y activa animación de carga
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = {
                    isChecked = !isChecked
                    setIsLoading(true)
                    scope.launch {
                        delay(1500)
                        setIsLoading(false)
                    }
                    onToggleFichar()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home32_2),
                    contentDescription = "Fichar",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
            }
            Text(text = "Fichar", textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        }
        val context = LocalContext.current

        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Fichajes", R.drawable.ic_fichajes32) {
            hideCuadroParaFichar()
            val dominio = BuildURLmovil.getHost(context)
            val cookieManager = CookieManager.getInstance()
            val cookie = cookieManager.getCookie(dominio)
            if (!cookie.isNullOrEmpty()) {
                cookieManager.setCookie(dominio, cookie)
                cookieManager.flush()
            }
            onNavigate(BuildURLmovil.getFichaje(context))
        }
        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Incidencias", R.drawable.ic_incidencia32) {
            hideCuadroParaFichar()
            val dominio = BuildURLmovil.getHost(context)
            val cookieManager = CookieManager.getInstance()
            val cookie = cookieManager.getCookie(dominio)
            if (!cookie.isNullOrEmpty()) {
                cookieManager.setCookie(dominio, cookie)
                cookieManager.flush()
            }
            onNavigate(BuildURLmovil.getIncidencia(context))
        }
        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Horarios", R.drawable.ic_horario32) {
            hideCuadroParaFichar()
            val dominio = BuildURLmovil.getHost(context)
            val cookieManager = CookieManager.getInstance()
            val cookie = cookieManager.getCookie(dominio)
            if (!cookie.isNullOrEmpty()) {
                cookieManager.setCookie(dominio, cookie)
                cookieManager.flush()
            }
            onNavigate(BuildURLmovil.getHorarios(context))
        }
        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Solicitudes", R.drawable.solicitudes32) {
            hideCuadroParaFichar()
            val dominio = BuildURLmovil.getHost(context)
            val cookieManager = CookieManager.getInstance()
            val cookie = cookieManager.getCookie(dominio)
            if (!cookie.isNullOrEmpty()) {
                cookieManager.setCookie(dominio, cookie)
                cookieManager.flush()
            }
            onNavigate(BuildURLmovil.getSolicitudes(context))
        }
    }
}

// Botón reutilizable de navegación inferior, con icono e identificador de sección
@Composable
fun NavigationButton(text: String, iconResId: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = text,
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )
        }
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
//============================== CUADRO PARA FICHAR ======================================

// Pantalla de carga que muestra un GIF mientras se carga la vista principal (WebView o datos)
@Composable
fun LoadingScreen(isLoading: Boolean) {
    if (isLoading) {
        val context = LocalContext.current
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.drawable.version_2)
                    .crossfade(true)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "Loading GIF",
                modifier = Modifier.size(200.dp)
            )
        }
    }
}
