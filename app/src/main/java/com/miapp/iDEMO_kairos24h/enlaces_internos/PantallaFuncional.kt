package com.miapp.iDEMO_kairos24h.enlaces_internos

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.util.Log
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.miapp.Tragabuche.R
import com.miapp.iDEMO_kairos24h.enlaces_internos.SeguridadUtils.ResultadoUbicacion
import com.miapp.iDEMO_kairos24h.fichar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


//============================== CUADRO PARA FICHAR ======================================
@Composable
fun CuadroParaFichar(
    isVisibleState: MutableState<Boolean>,
    fichajes: List<String>,
    onFichaje: (String) -> Unit,
    onShowAlert: (String) -> Unit,
    webViewState: MutableState<WebView?>,
    mostrarBotonesFichaje: Boolean // ← NUEVO PARÁMETRO
) {
    // Mover refreshTrigger fuera del if para que se ejecute siempre
    val refreshTrigger = remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Añadir observer de ON_RESUME para refrescar el trigger
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshTrigger.value = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (isVisibleState.value) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .zIndex(2f)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    // Habilita el scroll vertical:
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Mostrar lista de fichajes si existe
                if (fichajes.isNotEmpty()) {
                    Text(text = "Fichajes del Día", color = Color.Blue)
                    fichajes.forEach { fichaje ->
                        Text(text = fichaje, color = Color.DarkGray)
                    }
                }
                Logo_empresa_cliente()
                MiHorario()
                BotonesFichajeConPermisos(
                    onFichaje = onFichaje,
                    onShowAlert = onShowAlert,
                    webView = webViewState.value ?: return@CuadroParaFichar,
                    refreshTrigger = refreshTrigger // 7. Pasar refreshTrigger
                )
                // rememberDatosHorario()  // Eliminado porque ya no se necesita
                RecuadroFichajesDia(refreshTrigger) // 4. Pasar refreshTrigger a RecuadroFichajesDia
                AlertasDiarias(
                    onAbrirWebView = { url -> webViewState.value?.loadUrl(url) },
                    hideCuadroParaFichar = { isVisibleState.value = false },
                    refreshTrigger = refreshTrigger
                )
                Logo_empresa_desarrolladora()
            }
        }
    }
}

@Composable
fun Logo_empresa_cliente() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-10).dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.tragabuche_x_kairos24h),
            contentDescription = "Logo del cliente",
            contentScale = ContentScale.Fit, // Ajusta la imagen para que se vea completa
            modifier = Modifier
                .width(500.dp)
                .height(150.dp)
        )
    }
}

@Composable
fun Logo_empresa_desarrolladora() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-10).dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_i3data),
            contentDescription = "Logo i3data",
            contentScale = ContentScale.Fit, // Ajusta la imagen para que se vea completa
            modifier = Modifier
                .width(140.dp)
                .height(100.dp)
        )
    }
}

data class DatosHorario(
    val fechaFormateada: String,
    val fechaSeleccionada: String,
    val xEmpleado: String,
    val urlHorario: String
)


@Composable
fun rememberDatosHorario(): DatosHorario {
    val context = LocalContext.current

    val dateFormatterTexto = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    }
    val dateFormatterURL = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    var fechaFormateada by remember { mutableStateOf("Cargando...") }
    var fechaSeleccionada by remember { mutableStateOf("") }

    val (_, _, xEmpleadoRaw) = AuthManager.getUserCredentials(context)
    val xEmpleado = xEmpleadoRaw ?: "SIN_EMPLEADO"

    LaunchedEffect(Unit) {
        val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
        if (fechaServidor != null) {
            fechaFormateada = dateFormatterTexto.format(fechaServidor)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
            fechaSeleccionada = dateFormatterURL.format(fechaServidor)
        } else {
            fechaFormateada = "Error al obtener fecha"
            fechaSeleccionada = "0000-00-00"
        }
    }
    val urlHorario = BuildURL.getMostrarHorarios(context) + "&fecha=$fechaSeleccionada"



    return DatosHorario(
        fechaFormateada = fechaFormateada,
        fechaSeleccionada = fechaSeleccionada,
        xEmpleado = xEmpleado,
        urlHorario = urlHorario
    )
}


@Composable
fun MiHorario() {
    val datos = rememberDatosHorario()

    val urlHorario = datos.urlHorario

    // Estado para mostrar el horario
    val horarioTexto by produceState(initialValue = "Cargando horario...") {
        value = try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val request = Request.Builder().url(urlHorario).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("MiHorario", "Respuesta completa del servidor:\n$responseBody")

                val cleanedBody = responseBody?.replace("\uFEFF", "")

                if (!response.isSuccessful || cleanedBody.isNullOrEmpty()) {
                    Log.e("MiHorario", "Error: ${response.code}")
                    "Error al obtener horario"
                } else {
                    try {
                        val json = JSONObject(cleanedBody)
                        val dataArray = json.getJSONArray("dataHorario")
                        if (dataArray.length() > 0) {
                            val item = dataArray.getJSONObject(0)
                            val horaIni = item.optInt("N_HORINI", 0)
                            val horaFin = item.optInt("N_HORFIN", 0)
                            Log.d("MiHorario", "Valor N_HORINI: $horaIni")
                            Log.d("MiHorario", "Valor N_HORFIN: $horaFin")

                            if (horaIni == 0 && horaFin == 0) {
                                "No Horario"
                            } else {
                                @SuppressLint("DefaultLocale")
                                fun minutosAHora(minutos: Int): String {
                                    val horas = minutos / 60
                                    val mins = minutos % 60
                                    return String.format("%02d:%02d", horas, mins)
                                }
                                minutosAHora(horaIni) + " - " + minutosAHora(horaFin)
                            }
                        } else {
                            "No hay horario"
                        }
                    } catch (e: Exception) {
                        Log.e("MiHorario", "Error al parsear JSON: ${e.message}\nResponse body: $responseBody")
                        "Error al procesar horario"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MiHorario", "Excepción al obtener horario: ${e.message}")
            "Error de conexión"
        }
    }

    // Interfaz
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-40).dp)
            .border(width = 1.dp, color = Color(0xFFC0C0C0))
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = datos.fechaFormateada,
            color = Color(0xFF7599B6),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 22.sp
        )
        Text(
            text = horarioTexto,
            color = if (horarioTexto.contains("Error") || horarioTexto.contains("No hay")) Color.Red else Color(0xFF7599B6),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BotonesFichajeConPermisos(
    onFichaje: (tipo: String) -> Unit,
    onShowAlert: (String) -> Unit,
    webView: WebView?,
    refreshTrigger: MutableState<Long> // 5. Añadir parámetro refreshTrigger
) {
    // Añadir al principio de BotonesFichajeConPermisos
    var ultimoFichajeTimestamp by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    var pendingFichaje by remember { mutableStateOf<String?>(null) }

    // Launcher para solicitar el permiso de ubicación
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingFichaje?.let { tipo ->
                Log.d("Fichaje", "Permiso concedido. Procesando fichaje de: $tipo")
                if (webView != null) {
                    fichar(context, tipo, webView)
                } else {
                    Log.e("Fichaje", "webView es null. No se puede fichar.")
                }
                onFichaje(tipo)
            }
        } else {
            Log.d("Fichaje", "Permiso denegado para ACCESS_FINE_LOCATION")
        }
        pendingFichaje = null
    }

    // BOTÓN ENTRADA
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(55.dp)
            .offset(y = (-20).dp)
            .clickable {
                when {
                    SeguridadUtils.isUsingVPN(context) -> {
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }
                    !SeguridadUtils.isInternetAvailable(context) -> {
                        Log.e("Fichar", "No hay conexión a Internet")
                        onShowAlert("PROBLEMA INTERNET")
                        return@clickable
                    }
                    !SeguridadUtils.hasLocationPermission(context) -> {
                        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
                        onShowAlert("PROBLEMA GPS")
                        return@clickable
                    }
                }
                // Lanzar la comprobación real de ubicación simulada
                CoroutineScope(Dispatchers.Main).launch {
                    when (SeguridadUtils.detectarUbicacionReal(context)) {
                        ResultadoUbicacion.GPS_DESACTIVADO -> {
                            Log.e("Seguridad", "GPS desactivado")
                            onShowAlert("PROBLEMA GPS")
                            return@launch
                        }
                        ResultadoUbicacion.UBICACION_SIMULADA -> {
                            Log.e("Seguridad", "Ubicación simulada detectada")
                            onShowAlert("POSIBLE UBI FALSA")
                            return@launch
                        }
                        ResultadoUbicacion.OK -> {
                            // continuar con fichaje
                        }
                    }
                    // --- Prevención de fichaje duplicado ---
                    val ahora = System.currentTimeMillis()
                    if (ahora - ultimoFichajeTimestamp < 5000) {
                        Log.w("Fichaje", "Fichaje repetido ignorado")
                        return@launch
                    }
                    ultimoFichajeTimestamp = ahora
                    // --- Fin prevención ---
                    Log.d(
                        "Fichaje",
                        "Fichaje Entrada: Permiso concedido. Procesando fichaje de ENTRADA"
                    )
                    webView?.let { fichar(context, "ENTRADA", it) }
                    onFichaje("ENTRADA")
                    refreshTrigger.value = System.currentTimeMillis() // 6. Actualizar refreshTrigger tras fichaje
                }
            },
        color = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color(0xFF7599B6))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.fichajeetrada32),
                contentDescription = "Imagen Fichaje Entrada",
                modifier = Modifier
                    .padding(start = 15.dp)
                    .height(40.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buildAnnotatedString {
                    append("Fichaje ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Entrada")
                    }
                },
                color = Color(0xFF7599B6),
                fontSize = 25.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // BOTÓN SALIDA
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offset(y = (-40).dp)
            .height(55.dp)
            .clickable {
                when {
                    SeguridadUtils.isUsingVPN(context) -> {
                        Log.e("Seguridad", "Intento de fichaje con VPN activa")
                        onShowAlert("VPN DETECTADA")
                        return@clickable
                    }
                    !SeguridadUtils.isInternetAvailable(context) -> {
                        Log.e("Fichar", "No hay conexión a Internet")
                        onShowAlert("PROBLEMA INTERNET")
                        return@clickable
                    }
                    !SeguridadUtils.hasLocationPermission(context) -> {
                        Log.e("Fichar", "No se cuenta con el permiso ACCESS_FINE_LOCATION")
                        onShowAlert("PROBLEMA GPS")
                        return@clickable
                    }
                }
                // Lanzar la comprobación real de ubicación simulada
                CoroutineScope(Dispatchers.Main).launch {
                    when (SeguridadUtils.detectarUbicacionReal(context)) {
                        ResultadoUbicacion.GPS_DESACTIVADO -> {
                            Log.e("Seguridad", "GPS desactivado")
                            onShowAlert("PROBLEMA GPS")
                            return@launch
                        }
                        ResultadoUbicacion.UBICACION_SIMULADA -> {
                            Log.e("Seguridad", "Ubicación simulada detectada")
                            onShowAlert("POSIBLE UBI FALSA")
                            return@launch
                        }
                        ResultadoUbicacion.OK -> {
                            // continuar con fichaje
                        }
                    }
                    // --- Prevención de fichaje duplicado ---
                    val ahora = System.currentTimeMillis()
                    if (ahora - ultimoFichajeTimestamp < 5000) {
                        Log.w("Fichaje", "Fichaje repetido ignorado")
                        return@launch
                    }
                    ultimoFichajeTimestamp = ahora
                    // --- Fin prevención ---
                    Log.d("Fichaje", "Fichaje Salida: Permiso concedido. Procesando fichaje de SALIDA")
                    webView?.let { fichar(context, "SALIDA", it) }
                    onFichaje("SALIDA")
                    refreshTrigger.value = System.currentTimeMillis() // 6. Actualizar refreshTrigger tras fichaje
                }
            },
        color = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(2.dp, Color(0xFF7599B6))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.fichajesalida32),
                contentDescription = "Imagen Fichaje Salida",
                modifier = Modifier
                    .padding(start = 15.dp)
                    .height(40.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buildAnnotatedString {
                    append("Fichaje ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Salida")
                    }
                },
                color = Color(0xFF7599B6),
                fontSize = 25.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun RecuadroFichajesDia(refreshTrigger: androidx.compose.runtime.State<Long>) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Necesario para la URL, mantener la lógica de fechaSeleccionada
    val fechaSeleccionada = remember { mutableStateOf("") }
    LaunchedEffect(refreshTrigger.value) {
        if (fechaSeleccionada.value.isEmpty()) {
            val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
            if (fechaServidor != null) {
                val formateador = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                fechaSeleccionada.value = formateador.format(fechaServidor)
            } else {
                fechaSeleccionada.value = "0000-00-00"
            }
        }
    }

    Log.d("RecuadroFichajesDia", "Fecha usada para la petición: ${fechaSeleccionada.value}")

    val (_, _, xEmpleadoRaw) = AuthManager.getUserCredentials(context)
    val xEmpleado = xEmpleadoRaw ?: "SIN_EMPLEADO"

    val fichajesTexto by produceState(
        initialValue = emptyList<String>(),
        key1 = Triple(fechaSeleccionada.value, xEmpleado, refreshTrigger.value)
    ) {
        value = try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val urlFichajes = BuildURL.getMostrarFichajes(context) + "&fecha=${fechaSeleccionada.value}"
                val request = Request.Builder().url(urlFichajes).build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()?.replace("\uFEFF", "")

                if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                    Log.e("RecuadroFichajesDia", "Error: ${response.code}")
                    emptyList()
                } else {
                    try {
                        val json = JSONObject(responseBody)
                        val dataFichajes = json.getJSONObject("dataFichajes")
                        val fichajesArray = dataFichajes.getJSONArray("fichajes")

                        buildList {
                            for (i in 0 until fichajesArray.length()) {
                                val item = fichajesArray.getJSONObject(i)
                                val nMinEntStr = item.optString("nMinEnt", "").trim()
                                val nMinSalStr = item.optString("nMinSal", "").trim()
                                val nMinEnt = nMinEntStr.toIntOrNull()
                                val nMinSal = nMinSalStr.toIntOrNull()

                                @SuppressLint("DefaultLocale")
                                fun minutosAHora(minutos: Int?): String {
                                    return if (minutos != null) {
                                        val horas = minutos / 60
                                        val mins = minutos % 60
                                        String.format("%02d:%02d", horas, mins)
                                    } else {
                                        "??"
                                    }
                                }
                                val horaEntrada = minutosAHora(nMinEnt)
                                val horaSalida = minutosAHora(nMinSal)
                                add("$horaEntrada h - $horaSalida h")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RecuadroFichajesDia", "Error al parsear JSON: ${e.message}")
                        emptyList()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RecuadroFichajesDia", "Excepción al obtener fichajes: ${e.message}")
            emptyList()
        }
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val nuevaFecha = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            fechaSeleccionada.value = dateFormatter.format(nuevaFecha.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Fichajes Día",
            color = Color(0xFF7599B6),
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(y = (-20).dp)
        )

        val sdfEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val iconColor = Color(0xFF7599B6)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-15).dp)
                .padding(horizontal = 16.dp)
        ) {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_calendario),
                    contentDescription = "Seleccionar fecha",
                    modifier = Modifier.size(26.dp),
                    tint = iconColor
                )
            }
            IconButton(onClick = {
                val actual = sdfEntrada.parse(fechaSeleccionada.value)
                val anterior = Calendar.getInstance().apply {
                    time = actual ?: Date()
                    add(Calendar.DAY_OF_MONTH, -1)
                }
                fechaSeleccionada.value = sdfEntrada.format(anterior.time)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.hacia_atras),
                    contentDescription = "Día anterior",
                    modifier = Modifier.size(26.dp),
                    tint = iconColor
                )
            }
            Text(
                text = try {
                    val date = sdfEntrada.parse(fechaSeleccionada.value)
                    sdfSalida.format(date ?: Date())
                } catch (_: Exception) {
                    fechaSeleccionada.value
                },
                color = Color.Gray,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val actual = sdfEntrada.parse(fechaSeleccionada.value)
                val siguiente = Calendar.getInstance().apply {
                    time = actual ?: Date()
                    add(Calendar.DAY_OF_MONTH, 1)
                }
                fechaSeleccionada.value = sdfEntrada.format(siguiente.time)
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.hacia_delante),
                    contentDescription = "Día siguiente",
                    modifier = Modifier.size(26.dp),
                    tint = iconColor
                )
            }
            IconButton(onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    val fechaServidor = ManejoDeSesion.obtenerFechaHoraInternet()
                    fechaServidor?.let {
                        val nuevaFecha = dateFormatter.format(it)
                        withContext(Dispatchers.Main) {
                            fechaSeleccionada.value = nuevaFecha
                        }
                    }
                }
            }) {
                Image(
                    painter = painterResource(id = R.drawable.reload),
                    contentDescription = "Fecha actual",
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-15).dp)
                .background(Color.White)
                .padding(10.dp)
                .align(Alignment.CenterHorizontally),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (fichajesTexto.isNotEmpty()) {
                fichajesTexto.forEach { fichaje ->
                    val partes = fichaje.split(" - ")
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = partes.getOrNull(0)?.plus(" - ") ?: "?? - ",
                            fontSize = 23.sp,
                            color = Color(0xFF7599B6)
                        )
                        Text(
                            text = partes.getOrNull(1) ?: "??",
                            fontSize = 23.sp,
                            color = Color(0xFF7599B6)
                        )
                    }
                }
            } else {
                Text(
                    text = "No hay fichajes hoy",
                    fontSize = 23.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


// --- Nueva clase de datos para avisos ---
data class AvisoItem(val titulo: String, val detalle: String, val url: String?)

@Composable
fun AlertasDiarias(
    onAbrirWebView: (String) -> Unit,
    hideCuadroParaFichar: () -> Unit,
    refreshTrigger: MutableState<Long>
) {
    val context = LocalContext.current
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }
    // Forzar fetch inicial
    LaunchedEffect(Unit) {
        refreshTrigger.value = System.currentTimeMillis()
    }

    // produceState para avisos, similar a RecuadroFichajesDia
    val avisos by produceState(
        initialValue = emptyList<AvisoItem>(),
        key1 = refreshTrigger.value
    ) {
        value = try {
            withContext(Dispatchers.IO) {
                val urlAlertas = BuildURL.getMostrarAlertas(context)
                Log.d("AlertasDiarias", "URL de alertas: $urlAlertas")
                val client = OkHttpClient()
                val cookie = android.webkit.CookieManager.getInstance()
                    .getCookie("https://democontrolhorario.kairos24h.es") ?: ""
                val request = Request.Builder()
                    .url(urlAlertas)
                    .addHeader("Cookie", cookie)
                    .build()
                val response = client.newCall(request).execute()
                val jsonBody = response.body?.string()
                val json = JSONObject(jsonBody ?: "")
                val dataArray = json.optJSONArray("dataAvisos")
                if (dataArray != null && dataArray.length() > 0) {
                    val nuevaLista = mutableListOf<AvisoItem>()
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val dAviso = item.optString("D_AVISO", "Sin aviso")
                        val tAviso = item.optString("T_AVISO", "")
                        val tUrl = item.optString("T_URL", "").takeIf { it.isNotBlank() && it != "null" }
                        Log.d("JSONAlertas", "[$i] D_AVISO: $dAviso")
                        Log.d("JSONAlertas", "[$i] T_AVISO: $tAviso")
                        Log.d("JSONAlertas", "[$i] T_URL: $tUrl")
                        nuevaLista.add(AvisoItem(dAviso, tAviso, tUrl))
                    }
                    nuevaLista
                } else {
                    Log.d("JSONAlertas", "Array 'dataAvisos' vacío o nulo")
                    listOf(AvisoItem("No hay alertas disponibles", "", null))
                }
            }
        } catch (e: Exception) {
            Log.e("AlertasDiarias", "Error obteniendo alertas: ${e.message}")
            listOf(AvisoItem("Error al cargar alertas", "", null))
        }
    }

    // Refresco automático cada 10 minutos
    LaunchedEffect(true) {
        while (true) {
            kotlinx.coroutines.delay(10 * 60 * 1000)
            refreshTrigger.value = System.currentTimeMillis()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF7599B6))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Avisos / Alertas",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 23.sp,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            Column(modifier = Modifier.padding(top = 8.dp)) {
                if (avisos.isEmpty()) {
                    Text(
                        text = "Cargando alertas...",
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                avisos.forEachIndexed { index, aviso ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.LightGray)
                                .clickable {
                                    expandedStates[index] = expandedStates[index] != true
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (expandedStates[index] == true) Icons.Default.Remove else Icons.Default.Add,
                                contentDescription = "Expandir",
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF7599B6)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = aviso.titulo,
                                fontSize = 18.sp,
                                color = Color(0xFF7599B6),
                                modifier = Modifier.weight(1f)
                            )
                            if (!aviso.url.isNullOrEmpty()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Redireccionar",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                onAbrirWebView(BuildURL.HOST.trimEnd('/') + "/" + aviso.url.trimStart('/'))
                                                kotlinx.coroutines.delay(1000)
                                                hideCuadroParaFichar()
                                            }
                                        },
                                    tint = Color(0xFF7599B6)
                                )
                            }
                        }
                        AnimatedVisibility(visible = expandedStates[index] == true) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                OutlinedTextField(
                                    value = aviso.detalle,
                                    onValueChange = {},
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 80.dp),
                                    readOnly = true,
                                    label = { Text("Detalle del aviso") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// Mensaje de alerta cuando se le da a uno de los botones de fichar
@Composable
fun MensajeAlerta(
    tipo: String = "ENTRADA",
    onClose: () -> Unit
) {
    val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm'h'", Locale.getDefault()).format(Date())

    val mensaje = when (tipo.uppercase()) {
        "ENTRADA" -> "Fichaje de Entrada realizado correctamente"
        "SALIDA" -> "Fichaje de Salida realizado correctamente"
        "PROBLEMA GPS" -> "No se detecta la geolocalización gps. Por favor, active la geolocalización gps para poder fichar y vuelvalo a intentar en unos segundos."
        "PROBLEMA INTERNET" -> "El dispositivo no está conectado a la red. Revise su conexión a Internet."
        "POSIBLE UBI FALSA" -> "Se detectó una posible ubicación falsa. Reinicie su geolocalización gps y vuelva a intentarlo en unos minutos"
        "VPN DETECTADA" -> "VPN detectada. Desactive la VPN para continuar y vuelva a intentarlo en unos minutos."
        else -> "Fichaje de $tipo realizado correctamente"
    }

    // 🎨 Color por tipo
    val colorFondo = when (tipo.uppercase()) {
        "ENTRADA" -> Color(0xFF124672) // Azul oscuro
        "SALIDA" -> Color(0xFFd7ebfa)  // Azul claro
        else -> Color(0xFFFF0101)      // Rojo para errores
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.LightGray),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // 🎨 Encabezado con color y texto según tipo
                val textoEncabezado = when (tipo.uppercase()) {
                    "ENTRADA" -> "ENTRADA"
                    "SALIDA" -> "SALIDA"
                    else -> "ERROR DE FICHAJE"
                }

                val colorTextoEncabezado = when (tipo.uppercase()) {
                    "SALIDA" -> Color(0xFF124672)
                    else -> Color.White
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorFondo)
                        .padding(8.dp)
                ) {
                    Text(
                        text = textoEncabezado,
                        color = colorTextoEncabezado,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 📝 Mensaje del fichaje
                Text(
                    text = mensaje,
                    color = Color.Black,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🕒 Fecha + hora con hora en negrita y más grande
                val partes = currentDateTime.split(" ")
                val fechaSolo = partes.getOrNull(0) ?: ""
                val horaSolo = partes.getOrNull(1) ?: ""

                Text(
                    text = buildAnnotatedString {
                        append("$fechaSolo ")
                        withStyle(
                            style = androidx.compose.ui.text.SpanStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        ) {
                            append(horaSolo)
                        }
                    },
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 🔘 Botón Cerrar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorFondo)
                    ) {
                        val colorTextoBoton = when (tipo.uppercase()) {
                            "SALIDA" -> Color(0xFF124672)
                            else -> Color.White
                        }

                        Text(
                            text = "Cerrar",
                            fontSize = 18.sp,
                            color = colorTextoBoton
                        )
                    }
                }
            }
        }
    }
}

