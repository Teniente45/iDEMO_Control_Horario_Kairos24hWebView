package com.miapp.beiman.enlaces_internos

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object AuthManager {

    // Obtener las credenciales del usuario desde SharedPreferences
    fun getUserCredentials(context: Context): Triple<String, String, String?> {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val usuario = sharedPreferences.getString("usuario", "") ?: ""
        val password = sharedPreferences.getString("password", "") ?: ""
        val xEmpleado = sharedPreferences.getString("xEmpleado", null)
        return Triple(usuario, password, xEmpleado)
    }
    // Función adicional para dispositivos con SDK 23 (o donde solo se necesiten usuario y password)
    fun getUserCredentialsPair(context: Context): Pair<String, String> {
        val (usuario, password, _) = getUserCredentials(context)
        return Pair(usuario, password)
    }


    // Guardar las credenciales del usuario en SharedPreferences, junto con xEmpleado
    fun saveUserCredentials(context: Context, usuario: String, password: String, xEmpleado: String?) {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("usuario", usuario)
            putString("password", password)
            if (xEmpleado != null) {
                putString("xEmpleado", xEmpleado)
            }
            apply()
        }
    }

    // Método para realizar el login y obtener el xEmpleado
    fun authenticateUser(context: Context, usuario: String, password: String, s: String): Pair<Boolean, String?> {
        val client = OkHttpClient()
        // Se usa cUsuario y tPassword en la URL
        val url = BuildURL.LOGIN_URL + "loginExterno&cUsuario=$usuario&tPassword=$password"
        Log.d("AuthManager", "URL: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("AuthManager", "Response Body: $responseBody")
                val jsonResponse = JSONObject(responseBody)
                val code = jsonResponse.optInt("code", -1)
                val xEmpleado = jsonResponse.optString("xEmpleado", null) // Extraer xEmpleado del JSON
                if (code == 1) {
                    // Respuesta exitosa: retornamos true y el xEmpleado
                    Pair(true, xEmpleado)
                } else {
                    Pair(false, null)
                }
            } else {
                Log.d("AuthManager", "Request failed with status: ${response.code}")
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error de autenticación: ", e)
            Pair(false, null)
        }
    }
}
