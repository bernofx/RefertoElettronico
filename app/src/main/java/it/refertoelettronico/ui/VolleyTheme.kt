package it.refertoelettronico.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stable court-side palette: team colours remain independent of the app theme. */
@Composable
fun VolleyTheme(content:@Composable ()->Unit) {
    MaterialTheme(
        colorScheme=lightColorScheme(
            primary=Color(0xFF126657),onPrimary=Color.White,
            primaryContainer=Color(0xFFD7F2E7),onPrimaryContainer=Color(0xFF103F35),
            secondary=Color(0xFF53665E),secondaryContainer=Color(0xFFE3EDE7),onSecondaryContainer=Color(0xFF20372D),
            tertiary=Color(0xFF795517),tertiaryContainer=Color(0xFFFFE6B3),
            background=Color(0xFFF4F7F5),onBackground=Color(0xFF192C25),
            surface=Color(0xFFFCFEFC),onSurface=Color(0xFF192C25),
            surfaceVariant=Color(0xFFEAF0EB),onSurfaceVariant=Color(0xFF52645B),
            surfaceContainer=Color(0xFFEEF3EF),surfaceContainerLow=Color(0xFFF6F9F6),
            surfaceContainerHigh=Color(0xFFE6EEE8),surfaceContainerHighest=Color(0xFFE0E9E2),
            outline=Color(0xFF75857B),outlineVariant=Color(0xFFD4DFD6)),
        shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),
            medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(28.dp)),
        typography=Typography(
            headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=38.sp,letterSpacing=(-.7).sp),
            headlineSmall=TextStyle(fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=30.sp,letterSpacing=(-.3).sp),
            titleLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=21.sp,lineHeight=27.sp),
            titleMedium=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=22.sp),
            bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),
            bodySmall=TextStyle(fontSize=12.sp,lineHeight=17.sp),labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp)),
        content=content)
}
