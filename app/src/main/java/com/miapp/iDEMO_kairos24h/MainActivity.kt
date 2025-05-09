package com.miapp.iDEMO_kairos24h

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.miapp.Tragabuche.R
import com.miapp.iDEMO_kairos24h.enlaces_internos.AuthManager
import com.miapp.iDEMO_kairos24h.enlaces_internos.WebViewURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificamos si es el primer arranque
        val prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("first_run", true)
        if (isFirstRun) {
            // Limpiamos las credenciales para que se muestre siempre la pantalla de login en el primer arranque
            clearCredentials()
            prefs.edit { putBoolean("first_run", false) }
        }

        // Obtenemos las credenciales almacenadas (usuario, password y xEmpleado)
        val (storedUser, storedPassword, _) = AuthManager.getUserCredentials(this)

        if (storedUser.isNotEmpty() && storedPassword.isNotEmpty()) {
            // Si existen credenciales, redirigimos a Fichar directamente
            navigateToFichar(storedUser, storedPassword)
        } else {
            // Si no existen credenciales, mostramos la pantalla de inicio de sesión
            setContent {
                MaterialTheme {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            DisplayLogo(
                                onSubmit = { usuario: String, password: String ->
                                    if (!isInternetAvailable(this@MainActivity)) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Compruebe su conexión a Internet",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@DisplayLogo
                                    }

                                    if (usuario.isNotEmpty() && password.isNotEmpty()) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                val (success, xEmpleado) = AuthManager.authenticateUser(
                                                    usuario,
                                                    password
                                                )
                                                runOnUiThread {
                                                    if (success && xEmpleado != null) {
                                                        AuthManager.saveUserCredentials(
                                                            this@MainActivity,
                                                            xEmpleado.usuario,
                                                            xEmpleado.password,
                                                            xEmpleado.xEmpleado,
                                                            xEmpleado.lComGPS,
                                                            xEmpleado.lComIP,
                                                            xEmpleado.lBotonesFichajeMovil,
                                                            xEmpleado.xEntidad
                                                        )
                                                        navController.navigate("fichar/${xEmpleado.usuario}/${xEmpleado.password}")
                                                    } else {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Usuario o contraseña incorrectos",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                runOnUiThread {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Error de autenticación: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Por favor, completa ambos campos",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onForgotPassword = {
                                    val url = WebViewURL.FORGOT_PASSWORD
                                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                                    startActivity(intent)
                                }
                            )
                        }
                        composable("fichar/{usuario}/{password}") { backStackEntry ->
                            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""
                            val password = backStackEntry.arguments?.getString("password") ?: ""

                            FicharScreen(
                                usuario = usuario,
                                password = password,
                                onLogout = { navigateToLogin() }
                            ) // 🔥 Se pasa un valor vacío
                        }
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // 🔥 Redirige al usuario a la pantalla de fichaje pasando las credenciales como intent extras
    private fun navigateToFichar(usuario: String, password: String) {
        val intent = Intent(this, Fichar::class.java)
        intent.putExtra("usuario", usuario)
        intent.putExtra("password", password)
        startActivity(intent)
        finish() // Finaliza la actividad actual para evitar volver atrás
    }
    // 🔥 Borra las credenciales almacenadas en SharedPreferences
    private fun clearCredentials() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("usuario")
            remove("password")
            remove("xEmpleado")
            apply()
        }
    }
    // 📌 Función para verificar si hay conexión a Internet (WiFi o Datos Móviles)
    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}

    // 🔥 Pantalla de inicio de sesión con formulario de usuario y contraseña
    @Composable
    fun DisplayLogo(
        onSubmit: (String, String) -> Unit,
        onForgotPassword: () -> Unit
    ) {
        val usuario = remember { mutableStateOf("") }
        val password = remember { mutableStateOf("") }

        // 🔥 Estados para gestionar la UI del login
        var isChecked by remember { mutableStateOf(false) } // Estado del CheckBox para guardar datos localmente
        var passwordVisible by remember { mutableStateOf(false) } // Estado para mostrar/ocultar la contraseña
        val errorMessage by remember { mutableStateOf("") } // Mensaje de error
        val context = LocalContext.current // Contexto de la app
        var isLocationChecked by remember { mutableStateOf(false) }
        var locationPermissionDeniedCount by remember { mutableIntStateOf(0) }
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationChecked = isGranted
        if (!isGranted) {
            locationPermissionDeniedCount++
            if (locationPermissionDeniedCount >= 3) {
                Toast.makeText(context, "Debe habilitar los permisos de GPS manualmente en los ajustes.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Debe aceptar los permisos de GPS para continuar.", Toast.LENGTH_SHORT).show()
            }
        }
    }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .offset(y = (-40).dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 🔥 Logo de la app
                    Image(
                        painter = painterResource(id = R.drawable.tragabuche),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .width(500.dp)
                            .height(150.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔥 Campo de usuario
                    OutlinedTextField(
                        value = usuario.value,
                        onValueChange = { newValue ->
                            usuario.value = newValue.filter { it != ' ' } // Elimina espacios en blanco
                        },
                        label = { Text("Usuario") },
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔥 Campo de contraseña
                    OutlinedTextField(
                        value = password.value,
                        onValueChange = { newValue ->
                            password.value = newValue.filter { it != ' ' } // Elimina espacios en blanco
                        },
                        label = { Text("Contraseña") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )

                    // 🔥 Checkbox para mostrar contraseña
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = passwordVisible,
                            onCheckedChange = { passwordVisible = it }
                        )
                        Text(
                            text = "Mostrar contraseña",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // 🔥 Checkbox para guardar datos localmente
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it }
                        )
                        Text(
                            text = "Doy mi consentimiento para guardar mis datos localmente en mi dispositivo.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // 🔥 Muestra mensaje de error si los datos son incorrectos
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // 🔥 Checkbox obligatorio para aceptar permisos de ubicación
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = isLocationChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                } else {
                                    isLocationChecked = false
                                }
                            }
                        )
                        Text(
                            text = "Acepto que la app acceda a la ubicación donde ficho",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // 🔥 Botón de acceso
                    Button(
                        onClick = {
                            val trimmedUsuario = usuario.value.trim()
                            val trimmedPassword = password.value.trim()

                            // Si los campos están completos, codifica las credenciales y las envía
                            if (trimmedUsuario.isNotEmpty() && trimmedPassword.isNotEmpty()) {
                                val encodedUsuario = URLEncoder.encode(trimmedUsuario, StandardCharsets.UTF_8.toString())
                                val encodedPassword = URLEncoder.encode(trimmedPassword, StandardCharsets.UTF_8.toString())
                                onSubmit(encodedUsuario, encodedPassword)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7599B6)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = usuario.value.isNotEmpty() && password.value.isNotEmpty() && isChecked && isLocationChecked
                    ) {
                        Text("Acceso", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔥 Botón de recuperación de contraseña
                    Text(
                        text = "¿Olvidaste la contraseña?",
                        color = Color(0xFF7599B6),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onForgotPassword() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 🔥 Mensaje de trazabilidad
                    Text(
                        text = """
                        Para control de calidad y aumentar la seguridad de nuestro sistema, todos los accesos, acciones, consultas o cambios (Trazabilidad) que realice dentro de Kairos24h serán almacenados.
                        Les recordamos que la Empresa podrá auditar los medios técnicos que pone a disposición del Trabajador para el desempeño de sus funciones.
                    """.trimIndent(),
                        color = Color(0xFF447094),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 25.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    // 🔥 Vista previa para Android Studio
    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        MaterialTheme {
            DisplayLogo(onSubmit = { _: String, _: String -> }, onForgotPassword = {})
        }
    }

