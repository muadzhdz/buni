package dev.buni.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val name: String,
    val packageName: String,
    val isBypassed: Boolean
)

@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val settings = remember { BuniSettings(context) }

    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val whitelisted = settings.getWhitelistedPackages()

            val list = resolveInfos.mapNotNull { ri ->
                val appInfo = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
                val name = pm.getApplicationLabel(appInfo).toString()
                val pkg = appInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                InstalledAppInfo(
                    name = name,
                    packageName = pkg,
                    isBypassed = whitelisted.contains(pkg)
                )
            }.distinctBy { it.packageName }.sortedBy { it.name.lowercase() }

            appsList = list
            isLoading = false
        }
    }

    val filteredApps = remember(searchQuery, appsList) {
        if (searchQuery.isBlank()) {
            appsList
        } else {
            val q = searchQuery.trim().lowercase()
            appsList.filter { it.name.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Text(
            text = "split-tunneling",
            color = White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Text(
            text = "bypass apps from vpn interface directly to cellular/wi-fi",
            color = Zinc500,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("search apps...", color = Zinc600, fontSize = 13.sp) },
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

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Zinc950)
                .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BYPASSED APPS",
                color = Zinc500,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "${appsList.count { it.isBypassed }} / ${appsList.size}",
                color = White,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "> reading installed packages...",
                    color = Zinc600,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppRowItem(
                        app = app,
                        onToggle = {
                            val newStatus = settings.toggleAppWhitelist(app.packageName)
                            appsList = appsList.map {
                                if (it.packageName == app.packageName) it.copy(isBypassed = newStatus) else it
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun AppRowItem(
    app: InstalledAppInfo,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Zinc950)
            .border(1.dp, if (app.isBypassed) Zinc700 else Zinc900, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.name,
                color = White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = app.packageName,
                color = Zinc600,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (app.isBypassed) Zinc800 else Zinc900)
                .border(1.dp, if (app.isBypassed) Emerald500 else Zinc800, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (app.isBypassed) "BYPASS" else "SHIELD",
                color = if (app.isBypassed) Emerald500 else Zinc500,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
