@file:Suppress("DEPRECATION")

package com.miapp.iDEMO_kairos24h


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.miapp.Compilance.R
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL
import com.miapp.iDEMO_kairos24h.enlaces_internos.CuadroParaFichar
import com.miapp.iDEMO_kairos24h.enlaces_internos.ManejoDeSesion
import com.miapp.iDEMO_kairos24h.enlaces_internos.MensajeAlerta
import com.miapp.iDEMO_kairos24h.enlaces_internos.SeguridadUtils
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class Fichar : ComponentActivity() {

    // Referencia al WebView principal de la actividad, utilizado para cargar y manipular contenido web
    private var webView: WebView? = null

    // Método principal que se ejecuta al crear la actividad; valida credenciales y lanza la interfaz FicharScreen
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Fichar", "onCreate iniciado")

        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)

        if (storedUser.isEmpty() || storedPassword.isEmpty()) {
            navigateToLogin()
            return
        }
        // Usamos las credenciales del Intent, o las almacenadas
        val usuario = intent.getStringExtra("usuario") ?: storedUser
        val password = intent.getStringExtra("password") ?: storedPassword
        setContent {
            FicharScreen(
                usuario = usuario,
                password = password,
                onLogout = { navigateToLogin() }
            )
        }
        // Inicia el temporizador de simulación de actividad para gestionar la caducidad de la sesión del usuario
        ManejoDeSesion.startActivitySimulationTimer(handler, webView, sessionTimeoutMillis)
    }


    // Método del ciclo de vida que notifica pausa a la lógica de sesión
    override fun onPause() {
        super.onPause()
        ManejoDeSesion.onPause()
    }

    // Método del ciclo de vida que detiene lógica de sesión y la vincula al WebView actual
    override fun onStop() {
        super.onStop()
        ManejoDeSesion.onStop(webView)
    }

    // Método del ciclo de vida que reanuda la lógica de sesión y la vincula al WebView actual
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
    private val handler = Handler(Looper.getMainLooper())
    // Tiempo máximo de inactividad permitido antes de cerrar sesión (2 horas)
    private val sessionTimeoutMillis = 2 * 60 * 60 * 1000L // 2 horas
}


// Composable principal de la pantalla de fichaje. Muestra WebView con login automático, cuadro para fichar, barra superior e inferior y lógica de navegación.
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FicharScreen(
    usuario: String,
    password: String,
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

    // Referencia reactiva al WebView utilizado para interactuar desde el Compose
    val webViewState = remember { mutableStateOf<WebView?>(null) }
    // Contexto actual de la aplicación (necesario para acceder a preferencias y otros recursos)
    val context = LocalContext.current
    // Accede a las preferencias guardadas del usuario (credenciales y flags)
    val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    // Recupera el nombre de usuario desde las preferencias o usa valor por defecto
    val cUsuario = sharedPreferences.getString("usuario", "Usuario") ?: "Usuario"
    // Determina si deben mostrarse los botones de fichaje en la interfaz
    val mostrarBotonesFichaje = sharedPreferences.getString("lBotonesFichajeMovil", "S")?.equals("S", ignoreCase = true) == true

    // Simula carga inicial de 1,5 segundos antes de mostrar contenido
    LaunchedEffect(Unit) {
        isLoading = true
        delay(1500)
        isLoading = false
    }

    // Estructura principal vertical de la pantalla, contiene barra superior, contenido central (WebView + fichar), y barra inferior
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
            // WebView que carga la URL de login y realiza login automático con JavaScript
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.databaseEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = true

                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
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
                                // Inyecta jQuery, jQuery UI y selectpicker y fuerza el renderizado correcto y visibilidad de paneles
                                view?.evaluateJavascript("""
    (function() {
        // Carga jQuery si no está presente
        if (typeof jQuery == 'undefined') {
            var jq = document.createElement('script');
            jq.src = 'https://code.jquery.com/jquery-3.6.0.min.js';
            document.head.appendChild(jq);
        }

        // Carga jQuery UI si no está presente
        if (typeof jQuery.ui == 'undefined') {
            var scriptUI = document.createElement('script');
            scriptUI.src = 'https://code.jquery.com/ui/1.12.1/jquery-ui.min.js';
            document.head.appendChild(scriptUI);

            var linkUI = document.createElement('link');
            linkUI.rel = 'stylesheet';
            linkUI.href = 'https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css';
            document.head.appendChild(linkUI);
        }

        // Carga Bootstrap selectpicker si no está presente
        if (typeof $.fn.selectpicker == 'undefined') {
            var scriptSP = document.createElement('script');
            scriptSP.src = 'https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.18/js/bootstrap-select.min.js';
            document.head.appendChild(scriptSP);

            var linkSP = document.createElement('link');
            linkSP.rel = 'stylesheet';
            linkSP.href = 'https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.18/css/bootstrap-select.min.css';
            document.head.appendChild(linkSP);
        }

        // Autocompleta el login y envía el formulario
        document.getElementsByName('LoginForm[username]')[0].value = '$usuario';
        document.getElementsByName('LoginForm[password]')[0].value = '$password';
        document.querySelector('form').submit();

        setTimeout(function() {
            const visibles = document.querySelectorAll(
                '.panel, .panel-primary, .panel-body, .panel-heading, .modal-header, .form, .row, .form-group, input, select, textarea'
            );
            visibles.forEach(el => {
                el.style.display = 'block';
                el.style.visibility = 'visible';
                el.style.opacity = '1';
                el.style.maxHeight = 'initial';
                el.style.height = 'auto';
            });

            // Expande panel colapsado si existe
            const colapsable = document.querySelector('#cuerpoFormExplotacion');
            if (colapsable) {
                colapsable.classList.add('in');
                colapsable.style.display = 'block';
                colapsable.style.maxHeight = 'none';
                colapsable.style.opacity = '1';
                colapsable.style.visibility = 'visible';
            }

            // Reactiva scroll por si se había bloqueado
            document.body.style.overflow = 'auto';
            document.documentElement.style.overflow = 'auto';

            // Inicializa selectpicker si está presente
            if (typeof $ !== 'undefined' && $('.selectpicker').length > 0) {
                $('.selectpicker').selectpicker('refresh');
            }
        }, 3000);
    })();
""".trimIndent(), null)
                                // Fuerza visibilidad de los formularios y elimina .noDisplay de inmediato (versión mejorada)
                                view?.evaluateJavascript("""
    (function() {
        function forzarVisibilidadFormularios() {
            const forms = document.querySelectorAll("#incidencias-form, #solicitudes-form");
            forms.forEach(function(form) {
                form.style.display = "block";
                form.style.visibility = "visible";
                form.style.opacity = "1";
                form.style.maxHeight = "none";
                form.style.height = "auto";

                const panel = form.closest('.panel');
                if (panel) {
                    panel.style.display = "block";
                    panel.style.visibility = "visible";
                    panel.style.opacity = "1";
                    panel.style.maxHeight = "none";
                    panel.style.height = "auto";
                    panel.style.zIndex = "9999";
                }
            });
        }

        // Aplica visibilidad inmediata si ya existen
        forzarVisibilidadFormularios();

        // Observa si se cargan dinámicamente más tarde
        const observer = new MutationObserver(function(mutations) {
            forzarVisibilidadFormularios();
        });
        observer.observe(document.body, { childList: true, subtree: true });

        // Elimina clase .noDisplay y aplica visibilidad
        const noDisplayElements = document.querySelectorAll('.noDisplay');
        noDisplayElements.forEach(el => {
            el.classList.remove('noDisplay');
            el.style.display = 'block';
            el.style.visibility = 'visible';
            el.style.opacity = '1';
        });
    })();
""".trimIndent(), null)
                                // Observa la aparición dinámica de formularios y fuerza visibilidad SOLO si están dentro del contenedor correcto
                                view?.evaluateJavascript("""
    (function() {
        const observer = new MutationObserver(() => {
            // Detecta si se ha insertado el contenedor con el ID esperado y contiene un formulario
            const contenedor = document.querySelector("#dialogMttoExplotacion .form");
            const formulario = contenedor?.querySelector("form");

            if (formulario && contenedor) {
                // Fuerza visibilidad solo dentro del contenedor correcto
                contenedor.style.display = "block";
                contenedor.style.visibility = "visible";
                contenedor.style.opacity = "1";
                contenedor.style.maxHeight = "none";
                contenedor.style.height = "auto";
                contenedor.style.zIndex = "9999";

                formulario.style.display = "block";
                formulario.style.visibility = "visible";
                formulario.style.opacity = "1";
                formulario.style.maxHeight = "none";
                formulario.style.height = "auto";

                // Aplica visibilidad a todos los elementos internos del formulario
                const elementos = formulario.querySelectorAll("input, select, textarea, .form-group, .panel, .panel-body, .modal-header, .row");
                elementos.forEach(el => {
                    el.style.display = "block";
                    el.style.visibility = "visible";
                    el.style.opacity = "1";
                    el.style.maxHeight = "none";
                    el.style.height = "auto";
                });

                // Expande también el contenedor principal .ui-dialog-content y .ui-dialog
                const dialogContent = contenedor.closest('.ui-dialog-content');
                if (dialogContent) {
                    dialogContent.style.display = "block";
                    dialogContent.style.visibility = "visible";
                    dialogContent.style.opacity = "1";
                    dialogContent.style.maxHeight = "none";
                    dialogContent.style.height = "auto";
                    dialogContent.style.overflow = "visible";
                }

                const dialogWrapper = dialogContent?.closest('.ui-dialog');
                if (dialogWrapper) {
                    dialogWrapper.style.display = "block";
                    dialogWrapper.style.visibility = "visible";
                    dialogWrapper.style.opacity = "1";
                    dialogWrapper.style.maxHeight = "none";
                    dialogWrapper.style.height = "auto";
                    dialogWrapper.style.overflow = "visible";
                    dialogWrapper.style.overflowY = "auto";
                    dialogWrapper.style.top = "10px";
                    dialogWrapper.style.position = "absolute";
                    dialogWrapper.style.paddingTop = "20px";
                    dialogWrapper.style.paddingBottom = "20px";
                }

                observer.disconnect(); // Detiene el observador
            }
        });
        observer.observe(document.body, { childList: true, subtree: true });
    })();
""".trimIndent(), null)
                                // Fuerza la expansión del panel #cuerpoFormExplotacion aunque el evento data-toggle no funcione
                                view?.evaluateJavascript("""
    (function() {
        const target = document.getElementById("cuerpoFormExplotacion");
        if (target) {
            target.style.display = "block";
            target.style.visibility = "visible";
            target.style.opacity = "1";
            target.style.maxHeight = "none";
            target.setAttribute("aria-expanded", "true");
            console.log("🟢 cuerpoFormExplotacion forzado a visible");
        } else {
            console.warn("⚠️ No se encontró cuerpoFormExplotacion");
        }
    })();
""".trimIndent(), null)
                                // Fuerza la visibilidad del panel de incidencias y el bloque #cuerpoFormExplotacion
                                view?.evaluateJavascript("""
    (function() {
        // Fuerza la visibilidad del contenedor de incidencias
        const panelIncidencias = Array.from(document.querySelectorAll('.panel.panel-primary'))
            .find(panel => panel.textContent.includes('INCIDENCIAS'));

        if (panelIncidencias) {
            panelIncidencias.style.display = "block";
            panelIncidencias.style.visibility = "visible";
            panelIncidencias.style.opacity = "1";
            panelIncidencias.style.maxHeight = "none";
            panelIncidencias.style.height = "auto";
        }

        const cuerpo = document.getElementById("cuerpoFormExplotacion");
        if (cuerpo) {
            cuerpo.style.display = "block";
            cuerpo.style.visibility = "visible";
            cuerpo.style.opacity = "1";
            cuerpo.style.maxHeight = "none";
            cuerpo.style.height = "auto";
            cuerpo.setAttribute("aria-expanded", "true");
        }

        // Elimina clases que podrían estar ocultando el panel
        const ocultos = document.querySelectorAll('.noDisplay, .collapse, .hidden');
        ocultos.forEach(el => {
            el.classList.remove('noDisplay', 'collapse', 'hidden');
            el.style.display = "block";
            el.style.visibility = "visible";
            el.style.opacity = "1";
        });
    })();
""".trimIndent(), null)
                            }
                        }

                        loadUrl(WebViewURL.LOGIN)
                        webViewState.value = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )

            // Pantalla de carga que se muestra mientras se realiza la autenticación automática
            LoadingScreen(isLoading = isLoading)

            // Cuadro emergente con botones de fichaje (Entrada/Salida) que solicita la ubicación GPS
            if (showCuadroParaFicharState.value) {
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
                        webViewState = webViewState,
                        mostrarBotonesFichaje = mostrarBotonesFichaje
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
                webViewState.value?.loadUrl(url)
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

                            webViewState.value?.apply {
                                clearCache(true)
                                clearHistory()
                            }
                            CookieManager.getInstance().removeAllCookies(null)
                            CookieManager.getInstance().flush()

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

                    Spacer(modifier = Modifier.width(1.dp))

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

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun PreviewFicharScreen() {
    FicharScreen(
        usuario = "demoUsuario",
        password = "demoPassword",
        onLogout = {}
    )
}


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
                val urlFichaje = BuildURL.getCrearFichaje(context) +
                        "&cDomTipFic=$tipo" +
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
    if (lBotonesFichajeMovil != "S") Log.w("Seguridad", "Los botones de fichaje están deshabilitados: lBotonesFichajeMovil=$lBotonesFichajeMovil")
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
            lBotonesFichajeMovil
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
        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Fichajes", R.drawable.ic_fichajes32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.FICHAJE)
        }
        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Incidencias", R.drawable.ic_incidencia32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.INCIDENCIA)
        }
        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Horarios", R.drawable.ic_horario32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.HORARIOS)
        }
        // Botón de navegación que cambia de sección y oculta el cuadro para fichar
        NavigationButton("Solicitudes", R.drawable.solicitudes32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.SOLICITUDES)
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
