package com.example.srchicken.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary           = Indigo500,
    onPrimary         = Neutral900,
    primaryContainer  = Indigo900,
    onPrimaryContainer= Indigo200,

    secondary         = Indigo700,
    onSecondary       = Neutral50,

    background        = Surface1,
    onBackground      = OnSurface,

    surface           = Surface2,
    onSurface         = OnSurface,
    surfaceVariant    = Surface3,
    onSurfaceVariant  = OnSurface60,

    outline           = Surface4,
    outlineVariant    = DividerColor,

    error             = Danger,
    onError           = Neutral50,

    inverseSurface    = Neutral100,
    inverseOnSurface  = Neutral900,
)

@Composable
fun BillingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
