package com.miapp.beiman

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import com.miapp.beiman.enlaces_internos.AuthManager
import com.miapp.beiman.enlaces_internos.WebViewURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificamos si es el primer arranque
        val prefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("first_run", true)
        if (isFirstRun) {
            // Limpiamos las credenciales para que se muestre siempre la pantalla de login en el primer arranque
            clearCredentials()
            prefs.edit().putBoolean("first_run", false).apply()
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
                                    if (usuario.isNotEmpty() && password.isNotEmpty()) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                // Se llama a authenticateUser y se obtiene xEmpleado desde el JSON
                                                val (success, xEmpleado) = AuthManager.authenticateUser(
                                                    this@MainActivity,
                                                    usuario,
                                                    password,
                                                    ""
                                                )
                                                runOnUiThread {
                                                    if (success) {
                                                        // Guardamos las credenciales incluyendo xEmpleado
                                                        AuthManager.saveUserCredentials(
                                                            this@MainActivity,
                                                            usuario,
                                                            password,
                                                            xEmpleado
                                                        )
                                                        navController.navigate("fichar/$usuario/$password")
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
                                    val url = WebViewURL.forgotPassword  // Usa la constante definida en URL.kt
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    startActivity(intent)
                                }
                            )
                        }
                        composable("fichar/{usuario}/{password}") { backStackEntry ->
                            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""
                            val password = backStackEntry.arguments?.getString("password") ?: ""
                            FicharScreen(usuario = usuario, password = password)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToFichar(usuario: String, password: String) {
        val intent = Intent(this, Fichar::class.java)
        intent.putExtra("usuario", usuario)
        intent.putExtra("password", password)
        startActivity(intent)
        finish() // Cierra MainActivity para evitar volver al login
    }

    private fun clearCredentials() {
        val sharedPreferences = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("usuario")
            remove("password")
            remove("xEmpleado")
            apply()
        }
    }
}

// Función composable para la pantalla de login
@Composable
fun DisplayLogo(
    onSubmit: (String, String) -> Unit,
    onForgotPassword: () -> Unit
) {
    val usuario = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    // Estado del CheckBox para guardar datos localmente
    var isChecked by remember { mutableStateOf(false) }
    // Estado del CheckBox para mostrar/ocultar contraseña
    var passwordVisible by remember { mutableStateOf(false) }
    // Mensaje de error
    var errorMessage by remember { mutableStateOf("") }
    // Contexto
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .width(200.dp)
                        .height(60.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Campo de usuario: el usuario ve su texto "normal"
                OutlinedTextField(
                    value = usuario.value,
                    onValueChange = { newValue ->
                        // Se eliminan los espacios, pero se permite cualquier otro carácter (como #, @, etc.)
                        usuario.value = newValue.filter { it != ' ' }
                    },
                    label = { Text("Usuario") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Campo de contraseña
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { newValue ->
                        password.value = newValue.filter { it != ' ' }
                    },
                    label = { Text("Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                )
                // Checkbox para mostrar contraseña
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
                // CheckBox para dar consentimiento de guardar datos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it }
                    )
                    Text(
                        text = "Doy mi consentimiento para guardar mis datos localmente en mi dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Botón de acceso: al pulsarlo se recortan y codifican los valores
                Button(
                    onClick = {
                        val trimmedUsuario = usuario.value.trim()
                        val trimmedPassword = password.value.trim()
                        if (trimmedUsuario != usuario.value || trimmedPassword != password.value) {
                            errorMessage = "No se permiten espacios al principio o al final"
                            return@Button
                        }
                        if (trimmedUsuario.isNotEmpty() && trimmedPassword.isNotEmpty()) {
                            // Codificar las credenciales para evitar problemas en la URL
                            val encodedUsuario = URLEncoder.encode(trimmedUsuario, StandardCharsets.UTF_8.toString())
                            val encodedPassword = URLEncoder.encode(trimmedPassword, StandardCharsets.UTF_8.toString())
                            if (isChecked) {
                                // Guardamos las credenciales sin xEmpleado (se actualizará tras autenticarse)
                                AuthManager.saveUserCredentials(context, encodedUsuario, encodedPassword, null)
                            }
                            onSubmit(encodedUsuario, encodedPassword)
                        } else {
                            errorMessage = "Por favor, completa ambos campos"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7599B6)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = usuario.value.isNotEmpty() && password.value.isNotEmpty() && isChecked
                ) {
                    Text("Acceso", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "¿Olvidaste la contraseña?",
                    color = Color(0xFF7599B6),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onForgotPassword() }
                )
                Spacer(modifier = Modifier.height(8.dp))
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

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        DisplayLogo(onSubmit = { _: String, _: String -> }, onForgotPassword = {})
    }
}
