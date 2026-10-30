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
        assertEquals(3200,normal.width);assertEquals(1600,normal.height);normal.recycle()
        val rotated=Camp3Ocr.decodePhoto(context,uri,1)
        assertEquals(1600,rotated.width);assertEquals(3200,rotated.height);rotated.recycle()
        assertEquals(android.content.pm.PackageManager.PERMISSION_DENIED,context.checkSelfPermission("android.permission.INTERNET"))
        file.delete()
    }
    @Test fun largePhotoUsesTheFullResolutionBudgetAndCropIncludesItsLastPixels() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val file=File(context.cacheDir,"camp3/large.jpg").apply{parentFile!!.mkdirs()}
        val original=Bitmap.createBitmap(4000,2000,Bitmap.Config.ARGB_8888)
        file.outputStream().use{original.compress(Bitmap.CompressFormat.JPEG,80,it)};original.recycle()
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        val photo=Camp3Ocr.decodePhoto(context,uri,0)
        assertEquals(3600,photo.width);assertEquals(1800,photo.height)
        val cropped=it.refertoelettronico.ocr.PhotoCrop(.1f,.25f,1f,1f).pixels(photo.width,photo.height)
        assertEquals(android.graphics.Rect(360,450,3600,1800),cropped)
        photo.recycle();file.delete()
    }

}
