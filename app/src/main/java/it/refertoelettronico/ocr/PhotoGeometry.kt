package it.refertoelettronico.ocr

import android.graphics.Matrix
import android.graphics.Rect
import kotlin.math.*

data class PhotoPoint(val x:Double,val y:Double)

/** Boundary coordinates, not pixel centres. All eight EXIF transforms plus a manual clockwise turn. */
class PhotoGeometry(val rawWidth:Int,val rawHeight:Int,val exif:Int=1,quarterTurns:Int=0) {
    private val turns=((quarterTurns%4)+4)%4
    private val swapped=exif in 5..8
    private val exifWidth=if(swapped)rawHeight else rawWidth
    private val exifHeight=if(swapped)rawWidth else rawHeight
    val width=if(turns%2==0)exifWidth else exifHeight
    val height=if(turns%2==0)exifHeight else exifWidth
    init {require(rawWidth>0 && rawHeight>0 && exif in 1..8)}
    fun forward(x:Double,y:Double):PhotoPoint {
        val p=when(exif) {
            2->PhotoPoint(rawWidth-x,y)
            3->PhotoPoint(rawWidth-x,rawHeight-y)
            4->PhotoPoint(x,rawHeight-y)
            5->PhotoPoint(y,x)
            6->PhotoPoint(rawHeight-y,x)
            7->PhotoPoint(rawHeight-y,rawWidth-x)
            8->PhotoPoint(y,rawWidth-x)
            else->PhotoPoint(x,y)
        }
        return when(turns) {
            1->PhotoPoint(exifHeight-p.y,p.x)
            2->PhotoPoint(exifWidth-p.x,exifHeight-p.y)
            3->PhotoPoint(p.y,exifWidth-p.x)
            else->p
        }
    }
    private val origin by lazy{forward(0.0,0.0)}
    private val axisX by lazy{forward(1.0,0.0).let{PhotoPoint(it.x-origin.x,it.y-origin.y)}}
    private val axisY by lazy{forward(0.0,1.0).let{PhotoPoint(it.x-origin.x,it.y-origin.y)}}
    fun inverse(x:Double,y:Double):PhotoPoint {
        val dx=x-origin.x;val dy=y-origin.y
        val determinant=axisX.x*axisY.y-axisY.x*axisX.y
        return PhotoPoint((dx*axisY.y-dy*axisY.x)/determinant,(dy*axisX.x-dx*axisX.y)/determinant)
    }
    fun rawRect(upright:Rect):Rect {
        val points=listOf(inverse(upright.left.toDouble(),upright.top.toDouble()),inverse(upright.right.toDouble(),upright.top.toDouble()),
            inverse(upright.left.toDouble(),upright.bottom.toDouble()),inverse(upright.right.toDouble(),upright.bottom.toDouble()))
        return Rect(floor(points.minOf{it.x}).toInt().coerceIn(0,rawWidth-1),floor(points.minOf{it.y}).toInt().coerceIn(0,rawHeight-1),
            ceil(points.maxOf{it.x}).toInt().coerceIn(1,rawWidth),ceil(points.maxOf{it.y}).toInt().coerceIn(1,rawHeight))
    }
    fun matrix(scaleX:Float=1f,scaleY:Float=1f)=Matrix().apply {
        setValues(floatArrayOf((axisX.x*scaleX).toFloat(),(axisY.x*scaleX).toFloat(),(origin.x*scaleX).toFloat(),
            (axisX.y*scaleY).toFloat(),(axisY.y*scaleY).toFloat(),(origin.y*scaleY).toFloat(),0f,0f,1f))
    }
}
