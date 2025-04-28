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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.BuildURL
import com.miapp.iDEMO_kairos24h.enlaces_internos.CuadroParaFichar
import com.miapp.iDEMO_kairos24h.enlaces_internos.ManejoDeSesion
import com.miapp.iDEMO_kairos24h.enlaces_internos.MensajeAlerta
import com.miapp.iDEMO_kairos24h.enlaces_internos.SeguridadUtils
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import com.miapp.iDEMO_kairos24hXtragabuche.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Fichar : ComponentActivity() {

    // Variable para almacenar el WebView creado en Compose
    private var webView: WebView? = null

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
        ManejoDeSesion.startActivitySimulationTimer(handler, webView, sessionTimeoutMillis)
    }


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
        ManejoDeSesion.onResume(this, webView)
    }

    // Redirige al usuario a la pantalla de login y limpia la actividad actual
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
    // Handler y duración de sesión para ManejoDeSesion
    private val handler = Handler(Looper.getMainLooper())
    private val sessionTimeoutMillis = 2 * 60 * 60 * 1000L // 2 horas
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FicharScreen(
    usuario: String,
    password: String,
    onLogout: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var showCuadroParaFichar by remember { mutableStateOf(true) }
    var fichajes by remember { mutableStateOf<List<String>>(emptyList()) }
    var imageIndex by remember { mutableIntStateOf(0) }
    var fichajeAlertTipo by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val showLogoutDialog = remember { mutableStateOf(false) }

    val imageList = listOf(
        R.drawable.cliente32,
    )

    val webViewState = remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
    val cUsuario = sharedPreferences.getString("usuario", "Usuario") ?: "Usuario"
    // Obtener y convertir el valor lBotonesFichajeMovil
    val mostrarBotonesFichaje = sharedPreferences.getString("lBotonesFichajeMovil", "S")?.equals("S", ignoreCase = true) == true

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
        // 1. TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(0xFFE2E4E5))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // icono usuario + nombre
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
            // logout
            IconButton(onClick = { showLogoutDialog.value = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cerrar32),
                    contentDescription = "Cerrar sesión",
                    modifier = Modifier.size(30.dp),
                    tint = Color.Unspecified
                )
            }
        }

        // 2. Contenedor de contenido scrollable entre top y bottom
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // WebView de fondo
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
                        isVerticalScrollBarEnabled = true
                        isHorizontalScrollBarEnabled = true

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        document.getElementsByName('LoginForm[username]')[0].value = '$usuario';
                                        document.getElementsByName('LoginForm[password]')[0].value = '$password';
                                        document.querySelector('form').submit();

                                        setTimeout(function() {
                                            var dialogs = document.querySelectorAll('.ui-dialog');
                                            dialogs.forEach(function(d) {
                                                d.style.position = 'fixed';
                                                d.style.top = '5vh';
                                                d.style.maxHeight = '90vh';
                                                d.style.overflow = 'auto';
                                                d.style.zIndex = '9999';
                                                d.style.display = 'block';
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
                        loadUrl(WebViewURL.LOGIN)
                        webViewState.value = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f)
            )

            // Pantalla de carga
            LoadingScreen(isLoading = isLoading)

            // Cuadro para fichar con altura adecuada, sin scroll
            if (showCuadroParaFichar && mostrarBotonesFichaje) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .background(Color.White)
                ) {
                    CuadroParaFichar(
                        isVisible = showCuadroParaFichar,
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
                        webViewState = webViewState
                    )
                }
            }

            // Mensaje de alerta
            fichajeAlertTipo?.let { tipo ->
                MensajeAlerta(
                    tipo = tipo,
                    onClose = { fichajeAlertTipo = null }
                )
            }
        }

        // 3. BottomNavigationBar
        BottomNavigationBar(
            onNavigate = { url ->
                isLoading = true
                showCuadroParaFichar = false
                webViewState.value?.loadUrl(url)
                scope.launch {
                    delay(1500)
                    isLoading = false
                }
            },
            onToggleFichar = { showCuadroParaFichar = !showCuadroParaFichar },
            hideCuadroParaFichar = { showCuadroParaFichar = false },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
    }
    // Diálogo de confirmación para cerrar sesión
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
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showLogoutDialog.value = false
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7599B6),
                            contentColor = Color.White
                        ),
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text("Sí")
                    }

                    Spacer(modifier = Modifier.width(1.dp))

                    Button(
                        onClick = {
                            showLogoutDialog.value = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7599B6),
                            contentColor = Color.White
                        ),
                        shape = RectangleShape,
                        modifier = Modifier
                            .weight(1f)
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
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) {
        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
        Toast.makeText(context, "Debe aceptar los permisos de GPS para poder fichar.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        obtenerCoord(
            context,
            onLocationObtained = { lat, lon ->
                if (lat == 0.0 || lon == 0.0) {
                    Log.e("Fichar", "Ubicación inválida, no se enviará el fichaje")
                    return@obtenerCoord
                }

                val urlFichaje = BuildURL.getCrearFichaje(context) +
                        "&cDomTipFic=$tipo" +
                        "&tGpsLat=$lat" +
                        "&tGpsLon=$lon"

                Log.d("Fichar", "URL que se va a enviar desde WebView: $urlFichaje")
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
    // Extraer lComGPS y lComIP desde AuthManager.getUserCredentials
    val (_, _, _, lComGPS, lComIP, _) = AuthManager.getUserCredentials(context)
    val validarGPS = lComGPS.equals("S", ignoreCase = true)
    val validarIP = lComIP.equals("S", ignoreCase = true)

    val permitido = SeguridadUtils.checkSecurity(context, validarGPS, validarIP) { mensaje ->
        onShowAlert(mensaje)
    }
    if (!permitido) return

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // 🔍 Verificar si los permisos de ubicación están concedidos
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        Log.e("Fichar", "No se cuenta con los permisos de ubicación.")
        onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
        return
    }

    // 🔍 Verificar si el GPS está activado
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        Log.e("Fichar", "GPS desactivado.")
        onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
        return
    }

    // 🔍 Intentar obtener la última ubicación conocida
    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        if (location == null) {
            Log.e("Fichar", "No se pudo obtener la ubicación.")
            onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
            return@addOnSuccessListener
        }

        if (SeguridadUtils.isMockLocationEnabled(context)) {
            Log.e("Fichar", "Ubicación falsa detectada.")
            onShowAlert("POSIBLE UBI FALSA") // Muestra mensaje de alerta
            return@addOnSuccessListener
        }

        // ✅ Ubicación válida
        onLocationObtained(location.latitude, location.longitude)
    }.addOnFailureListener { e ->
        Log.e("Fichar", "Error obteniendo ubicación: ${e.message}")
        onShowAlert("PROBLEMA GPS") // Muestra mensaje de alerta
    }
}
//============================================== FICHAJE DE LA APP =====================================

@Composable
fun BottomNavigationBar(
    onNavigate: (String) -> Unit,
    onToggleFichar: () -> Unit,
    modifier: Modifier = Modifier,
    hideCuadroParaFichar: () -> Unit // 🔥 Nueva función para ocultar el cuadro
) {
    var isChecked by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFE2E4E5))
            .padding(2.dp)
            .zIndex(3f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = {
                    isChecked = !isChecked
                    onToggleFichar() // ✅ Alterna la visibilidad del cuadro de fichar
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
        // 🔥 Modificamos las funciones de navegación para ocultar el cuadro
        NavigationButton("Fichajes", R.drawable.ic_fichajes32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.FICHAJE)
        }
        NavigationButton("Incidencias", R.drawable.ic_incidencia32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.INCIDENCIA)
        }
        NavigationButton("Horarios", R.drawable.ic_horario32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.HORARIOS)
        }
        NavigationButton("Solicitudes", R.drawable.solicitudes32) {
            hideCuadroParaFichar()
            onNavigate(WebViewURL.SOLICITUDES)
        }
    }
}


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
