package it.refertoelettronico

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.ocr.Camp3Ocr
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Camp3PhotoTest {
    @org.junit.Before fun resetProviderPathsBetweenRobolectricSandboxes(){
        org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String,Any>>(FileProvider::class.java,"sCache").clear()
    }
    @Test fun photoIsDecodedDownsampledAndCanBeRotatedWithoutAnInternetPermission() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val file=File(context.cacheDir,"camp3/test.jpg").apply{parentFile!!.mkdirs()}
        val original=Bitmap.createBitmap(3200,1600,Bitmap.Config.ARGB_8888)
        file.outputStream().use{original.compress(Bitmap.CompressFormat.JPEG,80,it)};original.recycle()
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        val normal=Camp3Ocr.decodePhoto(context,uri,0)
        assertEquals(1600,normal.width);assertEquals(800,normal.height);normal.recycle()
        val rotated=Camp3Ocr.decodePhoto(context,uri,1)
        assertEquals(800,rotated.width);assertEquals(1600,rotated.height);rotated.recycle()
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,context.checkSelfPermission("android.permission.INTERNET"))
        file.delete()
    }
}
