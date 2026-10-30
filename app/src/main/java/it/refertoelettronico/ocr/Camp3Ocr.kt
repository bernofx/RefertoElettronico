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
        try{return readPhoto(bitmap)}finally{bitmap.recycle()}
    }

    fun readPhoto(photo:Bitmap,crop:PhotoCrop=PhotoCrop(),numberColumn:Float?=null,section:Camp3Section=Camp3Section.PLAYERS):Camp3Reading {
        val rect=crop.pixels(photo.width,photo.height)
        val bitmap=Bitmap.createBitmap(photo,rect.left,rect.top,rect.width(),rect.height())
        var recognizer:com.google.mlkit.vision.text.TextRecognizer?=null
        try {
            recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val result=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)))
            val lines=result.textBlocks.flatMap{it.lines}
            val slopes=lines.mapNotNull { line->
                val corners=line.cornerPoints
                if(corners==null || corners.size<2)null else {
                    val dx=corners[1].x-corners[0].x;val dy=corners[1].y-corners[0].y
                    if(dx<40)null else (dy.toFloat()/dx).takeIf{kotlin.math.abs(it)<.3f}
                }
            }.sorted()
            val slope=slopes.getOrNull(slopes.size/2) ?: 0f
            val words=lines.flatMap{it.elements}.mapNotNull { element->
                element.boundingBox?.let{r->OcrWord(element.text,r.left.toFloat(),r.top.toFloat(),r.right.toFloat(),r.bottom.toFloat())}
            }
            val column=numberColumn?.takeIf{it>=crop.left && it<=crop.right}?.let{it*photo.width-rect.left}
            val rows=if(section==Camp3Section.PLAYERS)Camp3Rows.suggest(words,column,slope) else emptyList()
            val staff=if(section==Camp3Section.STAFF)Camp3Staff.suggest(words,slope) else emptyList()
            val notices=buildList {
                if(words.isEmpty())add("Testo non leggibile: scegli una foto più nitida o avvicina la tabella.")
                else if(words.map{it.height}.sorted().let{it[it.size/2]}<16f)add("Il testo nella foto è piccolo: avvicina la fotocamera alla tabella per una lettura più precisa.")
                if(section==Camp3Section.STAFF && staff.isEmpty())add("Ruoli non riconosciuti. Includi le etichette dello staff oppure completa i nomi manualmente.")
                if(section==Camp3Section.PLAYERS && rows.size<6)add("Sono state riconosciute poche atlete. Verifica il ritaglio e prova a indicare la colonna delle maglie.")
                if(rows.any{it.number==null})add("Alcuni numeri non sono riconoscibili con certezza: controlla le righe evidenziate.")
            }
            return Camp3Reading(rows,result.text,notices,staff,Camp3Table.detect(words,bitmap.width,bitmap.height,section,slope))
        } finally {recognizer?.close();if(bitmap!==photo)bitmap.recycle()}
    }

    internal fun decodePhoto(context:Context,uri:Uri,quarterTurns:Int):Bitmap {
        val resolver=context.contentResolver
        if(android.os.Build.VERSION.SDK_INT>=28) {
            // A byte source also works with providers that only expose a stream, not a seekable FD.
            val encoded=resolver.openInputStream(uri)?.use { input->
                val bytes=java.io.ByteArrayOutputStream();val block=ByteArray(8192)
                while(true){val n=input.read(block);if(n<0)break;require(bytes.size()+n<=32_000_000){"Foto troppo grande (massimo 32 MB)."};bytes.write(block,0,n)}
                bytes.toByteArray()
            } ?: error("Immagine non leggibile.")
            val bitmap=android.graphics.ImageDecoder.decodeBitmap(android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(encoded))){decoder,info,_->
                decoder.allocator=android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                val scale=minOf(1f,3600f/maxOf(info.size.width,info.size.height))
                decoder.setTargetSize((info.size.width*scale).toInt().coerceAtLeast(1),(info.size.height*scale).toInt().coerceAtLeast(1))
            }
            if(quarterTurns%4==0)return bitmap
            try {
                val oriented=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,Matrix().apply{postRotate(quarterTurns*90f)},true)
                if(oriented!==bitmap)bitmap.recycle()
                return oriented
            }catch(t:Exception){bitmap.recycle();throw t}
        }
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds);true} ?: error("Immagine non leggibile.")
        require(bounds.outWidth>0 && bounds.outHeight>0){"Il file non è una foto leggibile."}
        var sample=1
        // Decode just enough pixels, then resize exactly: a 3200px photo must not become 1600px.
        while(maxOf(bounds.outWidth,bounds.outHeight)/(sample*2)>=3600 || bounds.outWidth.toLong()*bounds.outHeight/(sample.toLong()*sample)>14_000_000)sample*=2
        val options=BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}
        val bitmap=resolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,options)} ?: error("Impossibile aprire la foto.")
        try {
            val exif=resolver.openInputStream(uri)?.use{runCatching{ExifInterface(it)}.getOrNull()}
            val matrix=Matrix().apply {
                if(exif?.isFlipped==true)postScale(-1f,1f)
                postRotate(((exif?.rotationDegrees ?: 0)+quarterTurns*90).toFloat())
            }
            val longest=maxOf(bitmap.width,bitmap.height)
            val scale=minOf(1f,3600f/longest)
            matrix.postScale(scale,scale)
            val oriented=Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
            if(oriented!==bitmap)bitmap.recycle()
            return oriented
        } catch(t:Exception){bitmap.recycle();throw t}
    }
}
