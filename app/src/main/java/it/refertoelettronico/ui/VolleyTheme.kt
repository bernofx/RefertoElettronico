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
    val styled=appearance.layout!=InterfaceStyle.CLASSIC
    CompositionLocalProvider(LocalAppearance provides appearance) { MaterialTheme(
        colorScheme=appearance.scheme(androidx.compose.foundation.isSystemInDarkTheme()),
        shapes=Shapes(extraSmall=RoundedCornerShape(8.dp),small=RoundedCornerShape(12.dp),
            medium=RoundedCornerShape(if(appearance.layout==InterfaceStyle.DASHBOARD)12.dp else 16.dp),large=RoundedCornerShape(if(appearance.layout==InterfaceStyle.DASHBOARD)16.dp else 24.dp),extraLarge=RoundedCornerShape(28.dp)),
        typography=Typography(
            headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=32.sp,lineHeight=38.sp,letterSpacing=(-.7).sp),
            headlineSmall=TextStyle(fontWeight=FontWeight.Bold,fontSize=24.sp,lineHeight=30.sp,letterSpacing=(-.3).sp),
            titleLarge=TextStyle(fontWeight=if(styled)FontWeight.Bold else FontWeight.SemiBold,fontSize=if(styled)22.sp else 21.sp,lineHeight=if(styled)28.sp else 27.sp),
            titleMedium=TextStyle(fontWeight=if(styled)FontWeight.Bold else FontWeight.SemiBold,fontSize=if(styled)18.sp else 16.sp,lineHeight=if(styled)24.sp else 22.sp),
            titleSmall=if(styled)TextStyle(fontWeight=FontWeight.Bold,fontSize=16.sp,lineHeight=22.sp)else Typography().titleSmall,
            bodyLarge=TextStyle(fontSize=16.sp,lineHeight=24.sp),bodyMedium=TextStyle(fontSize=if(styled)15.sp else 14.sp,lineHeight=if(styled)22.sp else 21.sp),
            bodySmall=TextStyle(fontSize=if(styled)14.sp else 12.sp,lineHeight=if(styled)20.sp else 17.sp),
            labelLarge=TextStyle(fontWeight=FontWeight.SemiBold,fontSize=if(styled)16.sp else 14.sp,lineHeight=if(styled)22.sp else 20.sp),
            labelMedium=if(styled)TextStyle(fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=20.sp)else Typography().labelMedium,
            labelSmall=if(styled)TextStyle(fontWeight=FontWeight.Medium,fontSize=12.sp,lineHeight=16.sp)else Typography().labelSmall),
        content=content) }
}
