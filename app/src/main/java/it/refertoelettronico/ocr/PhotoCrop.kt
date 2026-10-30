package it.refertoelettronico.ocr

import kotlin.math.*

/** Normalized coordinates on the upright photo, independent of preview size. */
data class PhotoCrop(val left:Float=0f,val top:Float=0f,val right:Float=1f,val bottom:Float=1f) {
    init {require(left>=0 && top>=0 && right<=1 && bottom<=1 && right-left>=.019f && bottom-top>=.019f)}
    fun pixels(width:Int,height:Int):android.graphics.Rect {
        val x=floor(left*width).toInt().coerceIn(0,width-1)
        val y=floor(top*height).toInt().coerceIn(0,height-1)
        return android.graphics.Rect(x,y,ceil(right*width).toInt().coerceIn(x+1,width),ceil(bottom*height).toInt().coerceIn(y+1,height))
    }
    companion object {
        fun between(x1:Float,y1:Float,x2:Float,y2:Float):PhotoCrop {
            val l=min(x1,x2).coerceIn(0f,.98f);val t=min(y1,y2).coerceIn(0f,.98f)
            return PhotoCrop(l,t,max(max(x1,x2),l+.02f).coerceAtMost(1f),max(max(y1,y2),t+.02f).coerceAtMost(1f))
        }
    }
}
