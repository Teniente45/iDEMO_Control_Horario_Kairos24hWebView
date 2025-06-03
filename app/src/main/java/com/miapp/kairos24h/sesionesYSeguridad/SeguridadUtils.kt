/*
 * Copyright (c) 2025 Juan López
 * Todos los derechos reservados.
 *
 * Este archivo forma parte de la aplicación Kairos24h.
 * Proyecto académico de desarrollo Android.
 */

@file:Suppress("DEPRECATION")

package com.miapp.kairos24h.sesionesYSeguridad

import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationAvailability
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.location.Location
import kotlinx.coroutines.withTimeoutOrNull

object SeguridadUtils {

    enum class ResultadoUbicacion {
        OK,
        GPS_DESACTIVADO,
        UBICACION_SIMULADA
    }

    // Verifica si el dispositivo está usando una conexión VPN activa
    fun isUsingVPN(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("Seguridad", "Error al verificar VPN: ${e.message}")
            false
        }
    }

    // Comprueba si hay conexión a Internet disponible (cualquier tipo)
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    // Devuelve false por defecto. La verificación real se hace desde detectarUbicacionReal()
    fun isMockLocationEnabled(): Boolean {
        return false
    }

    // Verifica si la ubicación recibida es real o simulada usando FusedLocationProviderClient
    suspend fun detectarUbicacionReal(context: Context): ResultadoUbicacion {
        try {
            val permissionGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                Log.e("Seguridad", "Permiso ACCESS_FINE_LOCATION no concedido")
                return ResultadoUbicacion.GPS_DESACTIVADO
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.e("Seguridad", "GPS desactivado por el usuario")
                return ResultadoUbicacion.GPS_DESACTIVADO
            }

            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 0
                fastestInterval = 0
                numUpdates = 1
            }

            val location = withTimeoutOrNull(1000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val callback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            Log.d("Seguridad", "onLocationResult recibido")
                            cont.resume(result.lastLocation)
                            fusedClient.removeLocationUpdates(this)
                        }

                        override fun onLocationAvailability(availability: LocationAvailability) {
                            Log.d("Seguridad", "onLocationAvailability: ${availability.isLocationAvailable}")
                            if (!availability.isLocationAvailable) {
                                cont.resume(null)
                                fusedClient.removeLocationUpdates(this)
                            }
                        }
                    }

                    fusedClient.requestLocationUpdates(locationRequest, callback, null)
                }
            } ?: suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }

            if (location != null) {
                return if (location.isFromMockProvider) {
                    Log.w("Seguridad", "Ubicación simulada detectada")
                    ResultadoUbicacion.UBICACION_SIMULADA
                } else {
                    Log.i("Seguridad", "Ubicación válida detectada (posiblemente de respaldo)")
                    ResultadoUbicacion.OK
                }
            }

            Log.e("Seguridad", "No se pudo obtener ubicación ni siquiera con respaldo")
            return ResultadoUbicacion.GPS_DESACTIVADO

        } catch (e: Exception) {
            Log.e("Seguridad", "Error al obtener ubicación: ${e.message}")
            return ResultadoUbicacion.GPS_DESACTIVADO
        }
    }

    // Comprueba si se ha concedido el permiso de ubicación ACCESS_FINE_LOCATION
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Verifica las condiciones de seguridad para permitir fichaje (GPS, VPN, botones activados)
    suspend fun checkSecurity(
        context: Context,
        lComGPS: String,
        lComIP: String,
        lBotonesFichajeMovil: String,
        onShowAlert: (String) -> Unit
    ): Boolean {
        val validarGPS = lComGPS == "S"
        val validarIP = lComIP == "S"
        val mostrarBotones = lBotonesFichajeMovil != "N"

        Log.d("Seguridad", "Validaciones: GPS=$validarGPS, IP=$validarIP, Botones=$mostrarBotones")

        if (validarGPS) {
            if (!hasLocationPermission(context)) {
                Log.e("Seguridad", "GPS obligatorio, pero sin permiso")
                onShowAlert("PROBLEMA GPS")
                return false
            }

            when (detectarUbicacionReal(context)) {
                ResultadoUbicacion.GPS_DESACTIVADO -> {
                    Log.e("Seguridad", "GPS desactivado")
                    onShowAlert("PROBLEMA GPS")
                    return false
                }
                ResultadoUbicacion.UBICACION_SIMULADA,
                ResultadoUbicacion.OK -> { /* continuar */ }
            }
        }

        if (validarIP) {
            if (isUsingVPN(context)) {
                Log.e("Seguridad", "VPN detectada y uso de IP obligatorio")
                onShowAlert("VPN DETECTADA")
                return false
            }
        }

        return true
    }
}