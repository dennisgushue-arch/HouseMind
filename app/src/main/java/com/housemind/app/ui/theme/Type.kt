package com.housemind.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val HouseMindFont = FontFamily.SansSerif

val Typography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.6).sp
            ),
        headlineMedium =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.3).sp
            ),
        titleLarge =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            ),
        titleMedium =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 23.sp
            ),
        bodyLarge =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
        bodyMedium =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            ),
        labelMedium =
            TextStyle(
                fontFamily = HouseMindFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
    )
