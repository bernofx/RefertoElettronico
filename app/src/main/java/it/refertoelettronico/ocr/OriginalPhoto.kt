package it.refertoelettronico.ocr

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.Closeable
import java.io.File
import kotlin.math.*

/** Owns a private bounded copy of the encoded original, including stream-only providers. */
class OriginalPhoto private constructor(private val file:File,val rawWidth:Int,val rawHeight:Int,val orientation:Int,private val regionReader:(File,Rect,BitmapFactory.Options)->Bitmap?):Closeable {
    private var references=1
    private var closed=false
    private fun retain()=synchronized(this){check(!closed){"La foto è stata chiusa."};references++}
    private fun release()=synchronized(this){references--;if(references==0)file.delete();Unit}
    override fun close()=synchronized(this){if(!closed){closed=true;release()};Unit}
    private fun <T> withFile(block:()->T):T {
        retain()
        try{return block()}catch(t:OutOfMemoryError){throw IllegalArgumentException("Memoria insufficiente per questa foto. Fotografa più da vicino la tabella.",t)}
        finally{release()}
    }
    fun decode(crop:PhotoCrop=PhotoCrop(),quarterTurns:Int=0,edgeBudget:Int=MAX_EDGE):DecodedPhoto=withFile {
        require(edgeBudget in 1..MAX_EDGE)
        val geometry=PhotoGeometry(rawWidth,rawHeight,orientation,quarterTurns)
        val upright=crop.pixels(geometry.width,geometry.height)
        val raw=geometry.rawRect(upright)
        val regional=try {regionReader(file,raw,options(raw.width(),raw.height(),edgeBudget))}catch(_:Exception){null}
        if(regional!=null) {
            try {
                val bitmap=orient(regional,orientation,quarterTurns,edgeBudget)
                DecodedPhoto(bitmap,upright,geometry.width,false)
            }catch(t:Throwable){if(!regional.isRecycled)regional.recycle();throw t}
        } else {
            // Unsupported regional codecs use a bounded full decode, never a huge full ARGB image.
            val full=BitmapFactory.decodeFile(file.absolutePath,options(rawWidth,rawHeight,edgeBudget)) ?: error("Foto non leggibile o formato non supportato.")
            var oriented:Bitmap?=null
            try {
                oriented=orient(full,orientation,quarterTurns,edgeBudget)
                val area=crop.pixels(oriented.width,oriented.height)
                val selected=Bitmap.createBitmap(oriented,area.left,area.top,area.width(),area.height())
                if(selected!==oriented)oriented.recycle()
                DecodedPhoto(selected,upright,geometry.width,true)
            }catch(t:Throwable){oriented?.takeUnless{it.isRecycled}?.recycle();if(!full.isRecycled)full.recycle();throw t}
        }
    }
    fun preview(quarterTurns:Int=0):Bitmap=decode(quarterTurns=quarterTurns,edgeBudget=1800).bitmap

    companion object {
        const val MAX_FILE_BYTES=32_000_000
        const val MAX_EDGE=3600
        fun open(context:Context,uri:Uri,checkActive:()->Unit={}):OriginalPhoto = openWithDecoder(context,uri,checkActive,::androidRegion)

        @Suppress("DEPRECATION")
        private fun androidRegion(file:File,rect:Rect,options:BitmapFactory.Options):Bitmap? = file.inputStream().use { input->
            val decoder=BitmapRegionDecoder.newInstance(input,false)
            if(decoder==null)null else try{decoder.decodeRegion(rect,options)}finally{decoder.recycle()}
        }

        internal fun openWithDecoder(context:Context,uri:Uri,checkActive:()->Unit={},reader:(File,Rect,BitmapFactory.Options)->Bitmap?):OriginalPhoto {
            require(uri.scheme=="content"){"Seleziona una foto tramite Android."}
            val directory=File(context.cacheDir,"camp3-sources").apply{check(mkdirs() || isDirectory)}
            val file=File.createTempFile("original-",".img",directory)
            try {
                context.contentResolver.openInputStream(uri)?.use { input->
                    file.outputStream().use { out->
                        val block=ByteArray(8192);var count=0
                        while(true){checkActive();val n=input.read(block);if(n<0)break;count+=n;require(count<=MAX_FILE_BYTES){"Foto troppo grande (massimo 32 MB)."};out.write(block,0,n)}
                    }
                } ?: error("Impossibile leggere la foto.")
                checkActive()
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
                BitmapFactory.decodeFile(file.absolutePath,bounds)
                require(bounds.outWidth>0 && bounds.outHeight>0){"Il file non è una foto leggibile."}
                val orientation=runCatching{ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION,1)}.getOrDefault(1).takeIf{it in 1..8} ?: 1
                return OriginalPhoto(file,bounds.outWidth,bounds.outHeight,orientation,reader)
            }catch(t:Throwable){file.delete();throw t}
        }
        private fun options(width:Int,height:Int,budget:Int):BitmapFactory.Options {
            var sample=1
            while(max(width,height)/(sample*2)>=budget || width.toLong()*height/(sample.toLong()*sample)>14_000_000)sample*=2
            return BitmapFactory.Options().apply{inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888}
        }
        private fun orient(input:Bitmap,exif:Int,turns:Int,budget:Int):Bitmap {
            val geometry=PhotoGeometry(input.width,input.height,exif,turns)
            val scale=min(1f,budget.toFloat()/max(geometry.width,geometry.height))
            val output=Bitmap.createBitmap(input,0,0,input.width,input.height,geometry.matrix(scale,scale),true)
            if(output!==input)input.recycle()
            return output
        }
    }
}

/** The bitmap belongs to this result; preview and original cache have separate lifetimes. */
class DecodedPhoto(val bitmap:Bitmap,val uprightRegion:Rect,val originalUprightWidth:Int,val limited:Boolean):Closeable {
    fun columnX(normalized:Float?):Float?=normalized?.let {
        val x=it*originalUprightWidth
        if(x<uprightRegion.left || x>uprightRegion.right)null else (x-uprightRegion.left)/uprightRegion.width()*bitmap.width
    }
    override fun close(){if(!bitmap.isRecycled)bitmap.recycle()}
}
