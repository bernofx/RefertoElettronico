package it.refertoelettronico.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import it.refertoelettronico.model.SignaturePoint
import kotlin.math.min

/** Handwriting consumes native DOWN/MOVE/UP events immediately, without a drag/slop recognizer. */
internal class SignatureInkView(context:Context):View(context) {
    var onInkChanged:(List<List<SignaturePoint>>,Float,Boolean)->Unit={_,_,_->}
    private val strokes=mutableListOf<MutableList<SignaturePoint>>()
    private var basisAspect=1f
    private var activePointer=-1
    private var cursorPoint:SignaturePoint?=null
    private var pointCount=0
    private var truncated=false
    private var clearToken=Int.MIN_VALUE
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color=Color.BLACK;strokeWidth=2.5f*resources.displayMetrics.density
        style=Paint.Style.STROKE;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND
    }
    private val cursorPaint=Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.FILL}
    init { setBackgroundColor(Color.WHITE);isClickable=true;isFocusable=true }
    fun clearIfRequested(token:Int) {
        if(clearToken==token)return
        clearToken=token;strokes.clear();pointCount=0;activePointer=-1;cursorPoint=null;truncated=false
        basisAspect=width.toFloat().coerceAtLeast(1f)/height.coerceAtLeast(1)
        changed()
    }
    private fun changed() {
        invalidate()
        onInkChanged(strokes.map{it.toList()},basisAspect,truncated)
    }
    // Fit the original writing surface into the new bounds; resizes never erase or stretch ink.
    private fun contentRect():RectF {
        val fit=min(width/basisAspect,height.toFloat()).coerceAtLeast(1f)
        val w=fit*basisAspect
        return RectF((width-w)/2,(height-fit)/2,(width+w)/2,(height+fit)/2)
    }
    private fun append(x:Float,y:Float) {
        val rect=contentRect()
        val point=SignaturePoint(((x-rect.left)/rect.width()).coerceIn(0f,1f),((y-rect.top)/rect.height()).coerceIn(0f,1f))
        cursorPoint=point
        if(truncated || strokes.isEmpty())return
        if(strokes.last().lastOrNull()==point)return
        if(pointCount>=4000){truncated=true;return}
        strokes.last().add(point);pointCount++
    }
    override fun onTouchEvent(event:MotionEvent):Boolean {
        when(event.actionMasked) {
            MotionEvent.ACTION_DOWN->{
                parent?.requestDisallowInterceptTouchEvent(true)
                if(strokes.size>=128 || pointCount>=4000){truncated=true;changed();return true}
                if(strokes.isEmpty())basisAspect=width.toFloat().coerceAtLeast(1f)/height.coerceAtLeast(1)
                activePointer=event.getPointerId(0)
                strokes.add(mutableListOf());append(event.x,event.y);changed()
            }
            MotionEvent.ACTION_MOVE->{
                val index=event.findPointerIndex(activePointer)
                if(index>=0) {
                    for(history in 0 until event.historySize)append(event.getHistoricalX(index,history),event.getHistoricalY(index,history))
                    append(event.getX(index),event.getY(index));changed()
                }
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP->{
                val index=event.actionIndex
                if(event.getPointerId(index)==activePointer) {
                    append(event.getX(index),event.getY(index));activePointer=-1
                    parent?.requestDisallowInterceptTouchEvent(false);changed()
                    if(event.actionMasked==MotionEvent.ACTION_UP)performClick()
                }
            }
            MotionEvent.ACTION_CANCEL->{activePointer=-1;parent?.requestDisallowInterceptTouchEvent(false);changed()}
        }
        return true
    }
    override fun performClick():Boolean {super.performClick();return true}
    override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        if(strokes.isEmpty())basisAspect=w.toFloat().coerceAtLeast(1f)/h.coerceAtLeast(1)
        changed()
    }
    override fun onDraw(canvas:Canvas) {
        super.onDraw(canvas)
        val rect=contentRect()
        strokes.forEach { stroke->
            if(stroke.size==1) {
                val point=stroke.first();paint.style=Paint.Style.FILL
                canvas.drawCircle(rect.left+point.x*rect.width(),rect.top+point.y*rect.height(),paint.strokeWidth/2,paint)
                paint.style=Paint.Style.STROKE
            } else {
                val path=Path()
                stroke.forEachIndexed { i,p->
                    val x=rect.left+p.x*rect.width();val y=rect.top+p.y*rect.height()
                    if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
                }
                canvas.drawPath(path,paint)
            }
        }
        // Live cursor is deliberately separate from the exported strokes.
        if(activePointer>=0)cursorPoint?.let { point->
            val x=rect.left+point.x*rect.width();val y=rect.top+point.y*rect.height()
            val dp=resources.displayMetrics.density
            cursorPaint.color=Color.argb(65,22,109,94);canvas.drawCircle(x,y,10f*dp,cursorPaint)
            cursorPaint.color=Color.WHITE;canvas.drawCircle(x,y,4f*dp,cursorPaint)
            cursorPaint.color=Color.rgb(22,109,94);canvas.drawCircle(x,y,3f*dp,cursorPaint)
        }
    }
}
