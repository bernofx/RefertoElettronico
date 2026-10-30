package it.refertoelettronico.ocr

import java.util.Locale
import kotlin.math.*

/** Conservative table locator. Never use the last recognized athlete as the bottom of the crop. */
object Camp3Table {
    fun detect(words:List<OcrWord>,width:Int,height:Int,section:Camp3Section,skewSlope:Float=0f):PhotoCrop? {
        if(words.isEmpty())return null
        fun norm(w:OcrWord)=w.text.uppercase(Locale.ROOT).filter{it.isLetterOrDigit()}
        fun y(w:OcrWord)=w.cy-skewSlope*w.cx
        val h=words.map{it.height}.sorted()[words.size/2]
        val staff=words.filter { w->norm(w).let{it in setOf("TECNICI","STAFF","TESSERATI","FIRME") || it.contains("ALLENATORE") || it.startsWith("DIRIGENTE")}}
        if(section==Camp3Section.STAFF) {
            val first=staff.filter{norm(it)!="FIRME"}.minByOrNull(::y) ?: return null
            val top=(first.top-h*1.2f).coerceAtLeast(0f)/height
            return if(top<.9f)PhotoCrop(0f,top,1f,1f) else null
        }
        val anchors=words.filter{norm(it).let{s->s.contains("COGNOME") || s.contains("NOMINATIVO") || s.contains("MAGLIA")}}
        val choices=anchors.map { anchor->
            val band=words.filter{abs(y(it)-y(anchor))<h*1.8f}
            val labels=band.map(::norm)
            val hasName=labels.any{it.contains("COGNOME") || it.contains("NOMINATIVO")}
            val hasOther=labels.any{it.contains("MAGLIA") || it in setOf("TESSERA","TESSERAMENTO","NASCITA","DATA")}
            val score=(if(hasName)2 else 0)+(if(hasOther)2 else 0)+labels.count{it in setOf("NOME","NASCITA","TESSERA")}
            Triple(anchor,band,if(hasName && hasOther)score else 0)
        }
        val choice=choices.filter{it.third>=4}.maxWithOrNull(compareBy<Triple<OcrWord,List<OcrWord>,Int>>{it.third}.thenBy{-y(it.first)}) ?: return null
        val headerY=y(choice.first)
        val stop=staff.filter{y(it)>headerY+h*2}.minOfOrNull(::y)
        val body=words.filter{y(it)>headerY+h && (stop==null || y(it)<stop)}
        if(body.count{it.text.any(Char::isLetter)}<3)return null
        val left=(choice.second.minOf{it.left}-h*2).coerceAtLeast(0f)
        val right=(choice.second.maxOf{it.right}+h*2).coerceAtMost(width.toFloat())
        val top=(choice.second.minOf{it.top}-h*.8f).coerceAtLeast(0f)
        // A missing OCR row must remain in the next pass: stop at the next section, or the image edge.
        val bottom=if(stop!=null)(stop+max(skewSlope*left,skewSlope*right)-h*.6f).coerceAtMost(height.toFloat()) else height.toFloat()
        if(right-left<width*.3f || bottom-top<height*.1f)return null
        return PhotoCrop(left/width,top/height,right/width,bottom/height)
    }
}
