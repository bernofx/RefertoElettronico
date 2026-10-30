package it.refertoelettronico.pdf

import android.content.Context
import android.graphics.*
import it.refertoelettronico.model.Journal

/** Small-screen preview from the same marks, without decoding the 30-megapixel print image. */
object PreviewRenderer {
    fun render(context:Context,journal:Journal):Bitmap {
        val layout=SheetLayout.build(journal).first()
        val result=Bitmap.createBitmap(1190,842,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(result);canvas.drawColor(Color.WHITE)
        val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;typeface=Typeface.createFromAsset(context.assets,"DejaVuSans.ttf")}
        paint.textSize=11f;canvas.drawText("Referto Volley · ${layout.title}",18f,26f,paint)
        context.assets.open("referto-preview.png").use { input ->
            val base=BitmapFactory.decodeStream(input) ?: error("Anteprima del modello non disponibile.")
            canvas.drawBitmap(base,0f,48f,paint);base.recycle()
        }
        canvas.save();canvas.translate(0f,48f)
        layout.marks.forEach { m ->
            paint.style=Paint.Style.FILL;paint.color=Color.BLACK;paint.strokeWidth=.65f
            when(m.type) {
                "text" -> {
                    val box=m.cell
                    if(box==null){paint.textSize=m.size;canvas.drawText(m.text,m.x,m.y,paint)}
                    else {
                        paint.textSize=1000f
                        val metrics=paint.fontMetrics
                        val fitted=TextFit.fit(m.text,box,m.size,-metrics.ascent/1000,-metrics.descent/1000){paint.measureText(it)/1000}
                        paint.textSize=fitted.size
                        canvas.save();canvas.clipRect(box.left,box.top,box.right,box.bottom)
                        canvas.drawText(fitted.text,fitted.x,fitted.baseline,paint);canvas.restore()
                    }
                }
                "line" ->canvas.drawLine(m.x,m.y,m.x2,m.y2,paint)
                "circle" ->{paint.style=Paint.Style.STROKE;canvas.drawCircle(m.x,m.y,m.size,paint)}
                "white", "fill" ->{paint.color=if(m.type=="white")Color.WHITE else m.color;canvas.drawRect(m.x,m.y,m.x+m.x2,m.y+m.y2,paint)}
            }
        }
        canvas.restore();paint.style=Paint.Style.FILL;paint.color=Color.BLACK;paint.textSize=8f
        canvas.drawText("Referto cartaceo di supporto · Firme sul documento stampato · Pagina 1",18f,815f,paint)
        return result
    }
}
