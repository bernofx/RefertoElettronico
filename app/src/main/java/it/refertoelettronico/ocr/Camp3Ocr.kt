package it.refertoelettronico.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Bundled Latin model. Photo and recognized text never leave the device. Run on Dispatchers.IO. */
object Camp3Ocr {
    fun read(context:Context,uri:Uri,quarterTurns:Int=0):Camp3Reading {
        val bitmap=decodePhoto(context,uri,quarterTurns)
        val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)))
            val words=result.textBlocks.flatMap{it.lines}.flatMap{it.elements}.mapNotNull { element ->
                element.boundingBox?.let{r->OcrWord(element.text,r.left.toFloat(),r.top.toFloat(),r.right.toFloat(),r.bottom.toFloat())}
            }
            return Camp3Reading(Camp3Rows.suggest(words),result.text)
        } finally { recognizer.close();bitmap.recycle() }
    }

    internal fun decodePhoto(context:Context,uri:Uri,quarterTurns:Int):Bitmap {
        val resolver=context.contentResolver
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds);true} ?: error("Immagine non leggibile.")
        require(bounds.outWidth>0 && bounds.outHeight>0){"Il file non è una foto leggibile."}
        var sample=1
        while(maxOf(bounds.outWidth,bounds.outHeight)/sample>2600)sample*=2
        val options=BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}
        val bitmap=resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,options)} ?: error("Impossibile aprire la foto.")
        try {
            val exif=resolver.openInputStream(uri)?.use{runCatching{ExifInterface(it)}.getOrNull()}
            val matrix=Matrix().apply {
                if(exif?.isFlipped==true)postScale(-1f,1f)
                postRotate(((exif?.rotationDegrees ?: 0)+quarterTurns*90).toFloat())
            }
            val oriented=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
            if(oriented!==bitmap)bitmap.recycle()
            return oriented
        } catch(t:Exception){bitmap.recycle();throw t}
    }
}
