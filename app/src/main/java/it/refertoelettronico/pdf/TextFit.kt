package it.refertoelettronico.pdf

import kotlin.math.min

enum class CellAlign { LEFT, CENTER }
data class TextCell(val left:Float,val top:Float,val right:Float,val bottom:Float,val align:CellAlign=CellAlign.LEFT) {
    val width get()=right-left
    val height get()=bottom-top
    init {require(width>0 && height>0)}
}
data class FittedText(val text:String,val x:Float,val baseline:Float,val size:Float,val width:Float)

/** Font metrics and measurement are supplied by the renderer; no horizontal distortion. */
object TextFit {
    fun fit(value:String,cell:TextCell,size:Float,ascent:Float,descent:Float,measure:(String)->Float):FittedText {
        val clean=value.replace(Regex("[\\p{Cc}]")," ")
        val heightLimit=cell.height/(ascent-descent)
        val preferred=min(size,heightLimit)
        val minimum=min(preferred,5.5f)
        val unitWidth=measure(clean)
        val fittedSize=if(unitWidth>0)min(preferred,cell.width/unitWidth).coerceAtLeast(minimum) else preferred
        var result=clean
        if(unitWidth*fittedSize>cell.width) {
            // Code points keep surrogate pairs intact while abbreviating unusually long values.
            val points=clean.codePoints().toArray()
            var low=0;var high=points.size
            while(low<high) {
                val middle=(low+high+1)/2
                val candidate=String(points,0,middle).trimEnd()+"…"
                if(measure(candidate)*fittedSize<=cell.width)low=middle else high=middle-1
            }
            result=String(points,0,low).trimEnd()+"…"
            if(measure(result)*fittedSize>cell.width)result=""
        }
        val width=measure(result)*fittedSize
        val x=cell.left+if(cell.align==CellAlign.CENTER)(cell.width-width)/2 else 0f
        val baseline=cell.top+(cell.height-(ascent-descent)*fittedSize)/2+ascent*fittedSize
        return FittedText(result,x,baseline,fittedSize,width)
    }
}
