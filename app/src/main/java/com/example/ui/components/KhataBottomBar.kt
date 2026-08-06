package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme

enum class NavDestination(val route: String, val label: String, val filledIcon: ImageVector, val outlinedIcon: ImageVector) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    AI_HUB("ai_hub", "AI Hub", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    REMINDERS("reminders", "Reminders", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    REPORTS("reports", "Reports", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun KhataBottomBar(
    currentRoute: String,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSurface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavDestination.values().forEach { destination ->
                val isSelected = currentRoute == destination.route
                val icon = if (isSelected) destination.filledIcon else destination.outlinedIcon
                val textColor = if (isSelected) colors.textPrimary else colors.textSecondary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(destination) }
                        .testTag("nav_${destination.route}")
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = destination.label,
                        tint = textColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = destination.label,
                        style = CaptionStyle.copy(fontSize = 11.sp),
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    // Neutral primary-text underline indicator for active tab
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) colors.textPrimary else colors.bgSurface)
                    )
                }
            }
        }
    }
}
