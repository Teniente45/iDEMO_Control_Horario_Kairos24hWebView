package com.miapp.iDEMO_kairos24h

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.miapp.iDEMO_kairos24h_prueba.R

class PruebaCarton : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val usuario = intent.getStringExtra("usuario") ?: "desconocido"
        setContent {
            MaterialTheme {
                PantallaBienvenida(usuario) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

@Composable
fun PantallaBienvenida(usuario: String, onVolver: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 60.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.beiman1), // cambia "bienvenida" por el nombre real de tu archivo sin extensión
            contentDescription = "Imagen de bienvenida",
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentScale = ContentScale.Crop
        )

        var expanded by remember { mutableStateOf(false) }
        val offsetX by animateDpAsState(if (expanded) 0.dp else (-240).dp, label = "panelOffset")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.opciones_mas),
                contentDescription = "Abrir menú",
                modifier = Modifier
                    .size(50.dp)
                    .clickable { expanded = !expanded }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                buildAnnotatedString {
                    append("Bienvenido,  ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)) {
                        append(usuario)
                    }
                },
                fontSize = 20.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(vertical = 40.dp)
                    .padding(horizontal = 50.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.slideInHorizontally(initialOffsetX = { -240 }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -240 }) + androidx.compose.animation.fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF024897))
                    .clickable(
                        onClick = { expanded = false },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .offset(x = offsetX)
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val botones = listOf(
                        "Mis datos" to "https://www.clinicabeiman.es/",
                        "Mis partes" to "https://www.clinicabeiman.es/",
                        "Mis citas" to "https://www.clinicabeiman.es/",
                        "Mis consultas" to "https://www.clinicabeiman.es/"
                    )

                    botones.forEach { (titulo, url) ->
                        val context = LocalContext.current
                        Button(
                            onClick = {
                                if (url.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(url)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(titulo, color = Color(0xFF003366))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onVolver,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier
                    .border(1.dp, Color(0xFF024897), shape = RoundedCornerShape(30.dp))
                    .clip(RoundedCornerShape(30.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cerrar32),
                        contentDescription = "Icono volver",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Volver a inicio", color = Color(0xFF024897))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
fun PreviewPantallaBienvenida() {
    MaterialTheme {
        PantallaBienvenida(usuario = "ejemplo@correo.com") {}
    }
}