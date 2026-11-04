package dev.buni.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.BuniSettings
import dev.buni.theme.Black
import dev.buni.theme.Emerald500
import dev.buni.theme.White
import dev.buni.theme.Zinc400
import dev.buni.theme.Zinc500
import dev.buni.theme.Zinc600
import dev.buni.theme.Zinc700
import dev.buni.theme.Zinc800
import dev.buni.theme.Zinc900
import dev.buni.theme.Zinc950

@Composable
fun NodeScreen() {
    val context = LocalContext.current
    val settings = remember { BuniSettings(context) }
    val scrollState = rememberScrollState()

    var endpoint by remember { mutableStateOf(settings.getWireGuardEndpoint()) }
    var publicKey by remember { mutableStateOf(settings.getWireGuardPublicKey()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "remote node",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "wireguard full encrypted tunnel uplink",
            color = Zinc500,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Zinc950)
                .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PROTOCOL",
                    color = Zinc500,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "WireGuard / ChaCha20",
                    color = White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (endpoint.isNotEmpty()) Emerald500 else Zinc600)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (endpoint.isNotEmpty()) "READY" else "UNCONFIGURED",
                    color = if (endpoint.isNotEmpty()) White else Zinc500,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "ENDPOINT ADDRESS",
            color = Zinc500,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = endpoint,
            onValueChange = {
                endpoint = it
                settings.setWireGuardEndpoint(it)
            },
            placeholder = { Text("e.g. 198.51.100.1:51820", color = Zinc600, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Zinc950,
                unfocusedContainerColor = Zinc950,
                focusedBorderColor = Zinc600,
                unfocusedBorderColor = Zinc800,
                focusedTextColor = White,
                unfocusedTextColor = White
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "PEER PUBLIC KEY",
            color = Zinc500,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = publicKey,
            onValueChange = {
                publicKey = it
                settings.setWireGuardPublicKey(it)
            },
            placeholder = { Text("Base64 public key from server", color = Zinc600, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Zinc950,
                unfocusedContainerColor = Zinc950,
                focusedBorderColor = Zinc600,
                unfocusedBorderColor = Zinc800,
                focusedTextColor = White,
                unfocusedTextColor = White
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Zinc950)
                .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "ABOUT TUNNEL MODE",
                    color = Zinc500,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When activated on the main dashboard, Buni encapsulates 100% of device network traffic (0.0.0.0/0) through this remote node. Your public IP is completely masked.",
                    color = Zinc400,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}
