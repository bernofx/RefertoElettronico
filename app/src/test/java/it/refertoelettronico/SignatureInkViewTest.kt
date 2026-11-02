package it.refertoelettronico

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.model.*
import it.refertoelettronico.ui.SignatureInkView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignatureInkViewTest {
    private class Pad {
        val view=SignatureInkView(ApplicationProvider.getApplicationContext())
        var ink:List<List<SignaturePoint>> = emptyList()
        var ratio=1f
        init {
            view.onInkChanged={points,aspect,_->ink=points;ratio=aspect}
            view.clearIfRequested(0);view.layout(0,0,1800,300)
        }
        fun touch(action:Int,x:Float,y:Float,time:Long=10) {
            MotionEvent.obtain(1,time,action,x,y,0).also{assertTrue(view.dispatchTouchEvent(it));it.recycle()}
        }
        fun blackPixels():Int {
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            var count=0
            for(y in 0 until bitmap.height)for(x in 0 until bitmap.width)if(Color.red(bitmap.getPixel(x,y))<100)count++
            bitmap.recycle();return count
        }
    }
    @Test fun nativeDownDrawsImmediatelyAndSmallMotionCanBeCapturedWithoutTouchSlop() {
        val pad=Pad()
        pad.touch(MotionEvent.ACTION_DOWN,900f,150f)
        assertEquals(1,pad.ink.flatten().size)
        assertTrue("Il primo contatto deve essere visibile",pad.blackPixels()>0)
        pad.touch(MotionEvent.ACTION_MOVE,910f,166f,20)
        pad.touch(MotionEvent.ACTION_UP,910f,166f,30)
        assertEquals(2,pad.ink.flatten().size)
        val saved=Signatures.capture(Journal(config=Demo.config()),SignatureRole.CAPTAIN_HOME,pad.ink,pad.ratio,Demo.start())
        assertNotNull(Signatures.current(saved,SignatureRole.CAPTAIN_HOME))
    }
    @Test fun cursorFollowsTheFingerAndDisappearsOnLiftWithoutChangingStoredInk() {
        val pad=Pad()
        fun pixel(x:Int,y:Int):Int {
            val image=Bitmap.createBitmap(pad.view.width,pad.view.height,Bitmap.Config.ARGB_8888)
            pad.view.draw(Canvas(image));val color=image.getPixel(x,y);image.recycle();return color
        }
        pad.touch(MotionEvent.ACTION_DOWN,300f,100f)
        assertEquals(Color.rgb(22,109,94),pixel(300,100))
        pad.touch(MotionEvent.ACTION_MOVE,900f,180f,20)
        assertEquals(Color.rgb(22,109,94),pixel(900,180))
        assertEquals(Color.BLACK,pixel(300,100))
        val ink=pad.ink
        pad.touch(MotionEvent.ACTION_UP,900f,180f,30)
        assertEquals(ink,pad.ink)
        assertEquals(Color.BLACK,pixel(900,180))
    }
    @Test fun batchedMoveSamplesAndSeparateStrokesArePreserved() {
        val pad=Pad();pad.touch(MotionEvent.ACTION_DOWN,100f,100f)
        val move=MotionEvent.obtain(1,20,MotionEvent.ACTION_MOVE,200f,130f,0)
        move.addBatch(30,400f,180f,1f,1f,0)
        pad.view.dispatchTouchEvent(move);move.recycle()
        pad.touch(MotionEvent.ACTION_UP,600f,200f,40)
        assertEquals(4,pad.ink.single().size)
        pad.touch(MotionEvent.ACTION_DOWN,800f,100f,50)
        pad.touch(MotionEvent.ACTION_UP,1000f,200f,60)
        assertEquals(listOf(4,2),pad.ink.map{it.size})
    }
    @Test fun resizeCancelAndRecompositionDoNotClearInkButClearCommandDoes() {
        val pad=Pad();pad.touch(MotionEvent.ACTION_DOWN,300f,100f)
        pad.touch(MotionEvent.ACTION_MOVE,900f,180f,20)
        pad.touch(MotionEvent.ACTION_CANCEL,900f,180f,30)
        val before=pad.ink;val aspect=pad.ratio
        pad.view.clearIfRequested(0) // AndroidView update after recomposition is not a clear command.
        pad.view.layout(0,0,1200,600)
        assertEquals(before,pad.ink);assertEquals(aspect,pad.ratio)
        assertTrue(pad.blackPixels()>0)
        pad.view.clearIfRequested(1)
        assertTrue(pad.ink.isEmpty());assertEquals(0,pad.blackPixels())
    }
    @Test fun stylusPointerIsTrackedAndSecondaryTouchDoesNotJoinTheStroke() {
        val pad=Pad()
        fun stylus(action:Int,time:Long,x:Float,y:Float,second:Boolean=false) {
            val props=arrayOf(MotionEvent.PointerProperties().apply{id=7;toolType=MotionEvent.TOOL_TYPE_STYLUS},
                MotionEvent.PointerProperties().apply{id=9;toolType=MotionEvent.TOOL_TYPE_FINGER})
            val coords=arrayOf(MotionEvent.PointerCoords().apply{this.x=x;this.y=y;pressure=1f},
                MotionEvent.PointerCoords().apply{this.x=1700f;this.y=290f;pressure=1f})
            val event=MotionEvent.obtain(1,time,action,if(second)2 else 1,props,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_STYLUS,0)
            pad.view.dispatchTouchEvent(event);event.recycle()
        }
        stylus(MotionEvent.ACTION_DOWN,10,200f,100f)
        stylus(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),20,200f,100f,true)
        stylus(MotionEvent.ACTION_MOVE,30,600f,200f,true)
        stylus(MotionEvent.ACTION_POINTER_UP,40,800f,180f,true)
        stylus(MotionEvent.ACTION_MOVE,50,1000f,200f,true)
        assertEquals(3,pad.ink.single().size)
        assertTrue(pad.ink.flatten().all{it.x<.5f})
    }
}
