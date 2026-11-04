package it.refertoelettronico.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stable court-side palette: team colours remain independent of the app theme. */
@Composable
fun VolleyTheme(appearance:Appearance=Appearance(),content:@Composable ()->Unit) {
    CompositionLocalProvider(LocalAppearance provides appearance) { MaterialTheme(
        colorScheme=appearance.scheme(androidx.compose.foundation.isSystemInDarkTheme()),
        shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),
            medium=RoundedCornerShape(if(appearance.layout==InterfaceStyle.DASHBOARD)12.dp else 16.dp),large=RoundedCornerShape(if(appearance.layout==InterfaceStyle.DASHBOARD)16.dp else 24.dp),extraLarge=RoundedCornerShape(28.dp)),
        typography=Typography(
            headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=38.sp,letterSpacing=(-.7).sp),
            headlineSmall=TextStyle(fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=30.sp,letterSpacing=(-.3).sp),
            titleLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=21.sp,lineHeight=27.sp),
            titleMedium=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=16.sp,lineHeight=22.sp),
            bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=21.sp),
            bodySmall=TextStyle(fontSize=12.sp,lineHeight=17.sp),labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp)),
        content=content) }
}
