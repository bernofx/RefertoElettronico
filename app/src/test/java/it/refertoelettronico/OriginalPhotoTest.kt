package it.refertoelettronico

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.ocr.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.InputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OriginalPhotoTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    @Before fun resetProvider(){org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String,Any>>(FileProvider::class.java,"sCache").clear()}
    // Robolectric's BitmapRegionDecoder shadow returns blank bitmaps and ignores sampling.
    // Use an independent ImageIO regional codec for pixel/geometry assertions; the production
    // adapter remains Android BitmapRegionDecoder and needs a physical-device smoke test.
    private fun open(context:Context,uri:Uri)=OriginalPhoto.openWithDecoder(context,uri){file,rect,options->
        // Reflection keeps desktop-only test APIs off the Android compilation classpath.
        val io=Class.forName("javax.imageio.ImageIO")
        val input=io.getMethod("createImageInputStream",Any::class.java).invoke(null,file) as java.io.Closeable
        val iterator=io.getMethod("getImageReaders",Any::class.java).invoke(null,input) as Iterator<*>
        val reader=iterator.next()!!
        val api=Class.forName("javax.imageio.ImageReader")
        try {
            api.getMethod("setInput",Any::class.java).invoke(reader,input)
            val params=api.getMethod("getDefaultReadParam").invoke(reader)
            val type=Class.forName("javax.imageio.ImageReadParam")
            val rectangle=Class.forName("java.awt.Rectangle")
            val region=rectangle.getConstructor(Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType).newInstance(rect.left,rect.top,rect.width(),rect.height())
            type.getMethod("setSourceRegion",rectangle).invoke(params,region)
            type.getMethod("setSourceSubsampling",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType).invoke(params,options.inSampleSize,options.inSampleSize,0,0)
            val image=api.getMethod("read",Int::class.javaPrimitiveType,type).invoke(reader,0,params)
            val buffered=Class.forName("java.awt.image.BufferedImage")
            val w=buffered.getMethod("getWidth").invoke(image) as Int
            val h=buffered.getMethod("getHeight").invoke(image) as Int
            val pixels=buffered.getMethod("getRGB",Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType,IntArray::class.java,Int::class.javaPrimitiveType,Int::class.javaPrimitiveType).invoke(image,0,0,w,h,null,0,w) as IntArray
            Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888)
        }finally{api.getMethod("dispose").invoke(reader);input.close()}
    }
    private fun uri(file:File)=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
    private fun jpeg(width:Int,height:Int,name:String="source.jpg"):File {
        val file=File(context.cacheDir,"camp3/$name").apply{parentFile!!.mkdirs()}
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap);val paint=Paint()
        listOf(Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW).forEachIndexed{i,c->
            paint.color=c;val x=(i%2)*width/2f;val y=(i/2)*height/2f
            canvas.drawRect(x,y,x+width/2f,y+height/2f,paint)
        }
        file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.JPEG,100,it)};bitmap.recycle()
        return file
    }
    private fun near(expected:Int,actual:Int) {
        for(shift in listOf(0,8,16))assertTrue("Colore atteso $expected, ottenuto $actual",kotlin.math.abs((expected shr shift and 255)-(actual shr shift and 255))<45)
    }
    @Test fun aSmallCropKeepsOriginalPixelsInsteadOfTheReducedPreviewPixels() {
        val file=jpeg(6000,3000)
        open(context,uri(file)).use{source->
            val preview=source.preview()
            assertTrue(preview.width<=1800 && preview.height<=1800)
            source.decode(PhotoCrop(.25f,.25f,.5f,.5f)).use{crop->
                assertFalse("JPEG deve usare la decodifica regionale",crop.limited)
                assertEquals(1500,crop.bitmap.width);assertEquals(750,crop.bitmap.height)
                assertTrue(crop.bitmap.width>preview.width/4)
                assertEquals(750f,crop.columnX(.375f)!!,.01f)
                assertNull(crop.columnX(.1f))
            }
            preview.recycle()
        }
        assertTrue(file.exists())
        assertTrue(File(context.cacheDir,"camp3-sources").listFiles().orEmpty().isEmpty())
    }
    @Test fun largeRegionsAreBoundedButSmallRegionsAreNeverUpscaled() {
        open(context,uri(jpeg(7200,1200))).use{source->
            source.decode().use{assertEquals(3600,it.bitmap.width);assertEquals(600,it.bitmap.height)}
            source.decode(PhotoCrop(.25f,0f,.5f,1f)).use{assertEquals(1800,it.bitmap.width);assertEquals(1200,it.bitmap.height)}
        }
    }
    @Test fun allExifMirrorsAndManualRotationsKeepTheCorrectRegionAndLastEdges() {
        val expected=listOf(
            listOf(Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW),listOf(Color.GREEN,Color.RED,Color.YELLOW,Color.BLUE),
            listOf(Color.YELLOW,Color.BLUE,Color.GREEN,Color.RED),listOf(Color.BLUE,Color.YELLOW,Color.RED,Color.GREEN),
            listOf(Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW),listOf(Color.BLUE,Color.RED,Color.YELLOW,Color.GREEN),
            listOf(Color.YELLOW,Color.GREEN,Color.BLUE,Color.RED),listOf(Color.GREEN,Color.YELLOW,Color.RED,Color.BLUE))
        for(exif in 1..8) {
            val file=jpeg(192,128,"orientation-$exif.jpg")
            ExifInterface(file).apply{setAttribute(ExifInterface.TAG_ORIENTATION,exif.toString());saveAttributes()}
            open(context,uri(file)).use{source->
                var colors=expected[exif-1]
                for(turn in 0..3) {
                    val geometry=PhotoGeometry(192,128,exif,turn)
                    source.decode(quarterTurns=turn).use{whole->
                        assertFalse(whole.limited)
                        assertEquals(geometry.width,whole.bitmap.width);assertEquals(geometry.height,whole.bitmap.height)
                        for(i in 0..3)near(colors[i],whole.bitmap.getPixel(whole.bitmap.width*(if(i%2==0)1 else 3)/4,whole.bitmap.height*(if(i/2==0)1 else 3)/4))
                    }
                    source.decode(PhotoCrop(0f,0f,.5f,.5f),turn).use{crop->
                        assertEquals(geometry.width/2,crop.bitmap.width);assertEquals(geometry.height/2,crop.bitmap.height)
                        near(colors[0],crop.bitmap.getPixel(crop.bitmap.width/2,crop.bitmap.height/2))
                        assertEquals(crop.bitmap.width/2f,crop.columnX(.25f)!!,.01f)
                    }
                    val raw=geometry.rawRect(PhotoCrop(.5f,.5f,1f,1f).pixels(geometry.width,geometry.height))
                    val corners=listOf(geometry.forward(raw.left.toDouble(),raw.top.toDouble()),geometry.forward(raw.right.toDouble(),raw.bottom.toDouble()))
                    assertEquals(geometry.width.toDouble(),corners.maxOf{it.x},.001)
                    assertEquals(geometry.height.toDouble(),corners.maxOf{it.y},.001)
                    val p=geometry.forward(31.0,47.0);val back=geometry.inverse(p.x,p.y)
                    assertEquals(31.0,back.x,.001);assertEquals(47.0,back.y,.001)
                    colors=listOf(colors[2],colors[0],colors[3],colors[1])
                }
            }
        }
    }
    @Test fun automaticAndManualOcrBoundaryGetsTheOriginalRegionAndReleasesItsOwnBitmap() {
        val source=open(context,uri(jpeg(6000,3000)))
        val preview=source.preview()
        var consumed:Bitmap?=null
        try {
            for(section in Camp3Section.entries) {
                val result=Camp3Ocr.readOriginal(source,PhotoCrop(.25f,.25f,.5f,.5f),.375f,section,recognize={bitmap,column,requested,limited->
                    consumed=bitmap
                    assertFalse(limited);assertEquals(section,requested)
                    assertEquals(1500,bitmap.width);assertEquals(750f,column!!,.01f)
                    assertFalse(preview.isRecycled)
                    Camp3Reading(emptyList(),"Test")
                })
                assertEquals("Test",result.text);assertTrue(consumed!!.isRecycled)
            }
            assertThrows(java.util.concurrent.CancellationException::class.java) {
                Camp3Ocr.readOriginal(source,recognize={bitmap,_,_,_->consumed=bitmap;throw java.util.concurrent.CancellationException("closed")})
            }
            assertTrue(consumed!!.isRecycled);assertFalse(preview.isRecycled)
            assertThrows(IllegalArgumentException::class.java) {
                Camp3Ocr.readOriginal(source,recognize={bitmap,_,_,_->consumed=bitmap;throw IllegalArgumentException("recognition failed")})
            }
            assertTrue(consumed!!.isRecycled)
        }finally{preview.recycle();source.close()}
        assertTrue(File(context.cacheDir,"camp3-sources").listFiles().orEmpty().isEmpty())
    }
    @Test fun streamOnlyProviderIsReadOnceAndCachedSourceIsDeletedWhenClosed() {
        val data=jpeg(192,128).readBytes()
        val uri=Uri.parse("content://stream-only/photo")
        org.robolectric.Shadows.shadowOf(context.contentResolver).registerInputStream(uri,data.inputStream())
        val source=open(context,uri)
        repeat(2){source.decode(PhotoCrop(.5f,.5f,1f,1f)).use{near(Color.YELLOW,it.bitmap.getPixel(10,10))}}
        source.close();source.close()
        assertTrue(File(context.cacheDir,"camp3-sources").listFiles().orEmpty().isEmpty())
        assertThrows(IllegalStateException::class.java){source.decode()}
    }
    @Test fun invalidAndOversizedStreamsDoNotLeavePrivateCopies() {
        val resolver=org.robolectric.Shadows.shadowOf(context.contentResolver)
        val invalid=Uri.parse("content://stream-only/invalid")
        resolver.registerInputStream(invalid,"not an image".byteInputStream())
        assertThrows(IllegalArgumentException::class.java){open(context,invalid)}
        var closed=false
        val large=Uri.parse("content://stream-only/large")
        resolver.registerInputStream(large,object:InputStream(){
            override fun read()=0
            override fun read(buffer:ByteArray,offset:Int,length:Int):Int {buffer.fill(0,offset,offset+length);return length}
            override fun close(){closed=true}
        })
        val error=assertThrows(IllegalArgumentException::class.java){open(context,large)}
        assertTrue(error.message!!.contains("32 MB"));assertTrue(closed)
        assertTrue(File(context.cacheDir,"camp3-sources").listFiles().orEmpty().isEmpty())
    }
    @Test fun unsupportedRegionalFormatHasAnExplicitBoundedFallback() {
        val file=File(context.cacheDir,"camp3/fallback.bmp").apply{parentFile!!.mkdirs()}
        javaClass.getResourceAsStream("/ocr/fallback.bmp")!!.use{input->file.outputStream().use{input.copyTo(it)}}
        OriginalPhoto.openWithDecoder(context,uri(file)){_,_,_->null}.use{source->source.decode(PhotoCrop(.25f,0f,.75f,1f)).use{
            assertTrue(it.limited);assertEquals(64,it.bitmap.width);assertEquals(64,it.bitmap.height)
        }}
    }
    @Test fun closingTheDialogDuringDecodingKeepsTheSourceUntilThatReadFinishes() {
        lateinit var source:OriginalPhoto
        var cached:File?=null
        source=OriginalPhoto.openWithDecoder(context,uri(jpeg(192,128))){file,rect,_->
            cached=file
            source.close()
            assertTrue("Originale ancora necessario al decoder",file.exists())
            Bitmap.createBitmap(rect.width(),rect.height(),Bitmap.Config.ARGB_8888)
        }
        source.decode().use{assertFalse(it.bitmap.isRecycled)}
        assertFalse(cached!!.exists())
        assertThrows(IllegalStateException::class.java){source.decode()}
    }

    @Test fun fallbackForLargePhotosHasABoundedBitmapAndAnHonestDetailWarningFlag() {
        OriginalPhoto.openWithDecoder(context,uri(jpeg(6000,3000))){_,_,_->null}.use{source->
            source.decode(PhotoCrop(.25f,.25f,.5f,.5f)).use{crop->
                assertTrue(crop.limited)
                assertEquals(750,crop.bitmap.width);assertEquals(375,crop.bitmap.height)
                assertEquals(375f,crop.columnX(.375f)!!,.01f)
            }
        }
    }
    @Test fun cancellationWhileCopyingAStreamClosesItAndRemovesTheIncompleteSource() {
        val input=jpeg(192,128).readBytes().inputStream()
        val uri=Uri.parse("content://stream-only/cancel")
        org.robolectric.Shadows.shadowOf(context.contentResolver).registerInputStream(uri,input)
        assertThrows(java.util.concurrent.CancellationException::class.java){
            OriginalPhoto.open(context,uri){throw java.util.concurrent.CancellationException("dialog closed")}
        }
        assertTrue(File(context.cacheDir,"camp3-sources").listFiles().orEmpty().isEmpty())
    }

}
