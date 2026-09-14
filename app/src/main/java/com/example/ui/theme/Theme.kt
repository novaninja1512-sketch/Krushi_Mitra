package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = FarmGreenPrimaryLight,
  onPrimary = FarmGreenOnPrimaryLight,
  primaryContainer = FarmGreenContainerLight,
  onPrimaryContainer = FarmGreenOnContainerLight,
  secondary = FarmSecondaryLight,
  onSecondary = FarmOnSecondaryLight,
  secondaryContainer = FarmSecondaryContainerLight,
  onSecondaryContainer = FarmOnSecondaryContainerLight,
  tertiary = FarmTertiaryLight,
  onTertiary = FarmOnTertiaryLight,
  tertiaryContainer = FarmTertiaryContainerLight,
  onTertiaryContainer = FarmOnTertiaryContainerLight,
  background = FarmBackgroundLight,
  onBackground = FarmOnBackgroundLight,
  surface = FarmSurfaceLight,
  onSurface = FarmOnSurfaceLight,
  surfaceVariant = FarmSurfaceVariantLight,
  onSurfaceVariant = FarmOnSurfaceVariantLight,
  outline = FarmOutlineLight
)

private val DarkColorScheme = darkColorScheme(
  primary = FarmGreenPrimaryDark,
  onPrimary = FarmGreenOnPrimaryDark,
  primaryContainer = FarmGreenContainerDark,
  onPrimaryContainer = FarmGreenOnContainerDark,
  secondary = FarmSecondaryDark,
  onSecondary = FarmOnSecondaryDark,
  secondaryContainer = FarmSecondaryContainerDark,
  onSecondaryContainer = FarmOnSecondaryContainerDark,
  tertiary = FarmTertiaryDark,
  onTertiary = FarmOnTertiaryDark,
  tertiaryContainer = FarmTertiaryContainerDark,
  onTertiaryContainer = FarmOnTertiaryContainerDark,
  background = FarmBackgroundDark,
  onBackground = FarmOnBackgroundDark,
  surface = FarmSurfaceDark,
  onSurface = FarmOnSurfaceDark,
  surfaceVariant = FarmSurfaceVariantDark,
  onSurfaceVariant = FarmOnSurfaceVariantDark,
  outline = FarmOutlineDark
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Set default dynamicColor to false to maintain the signature agricultural green identity
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
