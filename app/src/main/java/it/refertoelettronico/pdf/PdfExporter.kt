package it.refertoelettronico.pdf

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.LayerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.util.Matrix
import it.refertoelettronico.model.Journal
import java.io.File

/** Preserve the source form at its original resolution and overlay searchable, embedded-font text. */
object PdfExporter {
    fun export(context: Context, journal: Journal): File {
        PDFBoxResourceLoader.init(context.applicationContext)
        val exportDir=File(context.filesDir,"exports").apply { mkdirs() }
        val output=File(exportDir,"referto-${System.currentTimeMillis()}.pdf")
        try {
            PDDocument().use { doc ->
                val font=context.assets.open("DejaVuSans.ttf").use { PDType0Font.load(doc,it) }
                val background=context.assets.open("referto-fipav.pdf").use { stream ->
                    PDDocument.load(stream).use { base -> LayerUtility(doc).importPageAsForm(base,0) }
                }
                SheetLayout.build(journal).forEachIndexed { i,layout ->
                    val page=PDPage(PDRectangle(1190f,842f));doc.addPage(page)
                    PDPageContentStream(doc,page).use { c ->
                        fun text(x:Float,y:Float,value:String,size:Float,width:Float=0f) {
                            if(value.isEmpty())return
                            val cleaned=value.replace(Regex("[\\p{Cc}]")," ")
                            c.beginText();c.setFont(font,size)
                            val measured=font.getStringWidth(cleaned)/1000*size
                            c.setHorizontalScaling(if(width>0 && measured>width)width/measured*100 else 100f)
                            c.newLineAtOffset(x,y);c.showText(cleaned);c.endText()
                        }
                        c.setNonStrokingColor(0f,0f,0f);c.setStrokingColor(0f,0f,0f);c.setLineWidth(.65f)
                        text(18f,816f,"Referto Volley · ${layout.title}",11f)
                        if(layout.template) {
                            c.saveGraphicsState();c.transform(Matrix.getTranslateInstance(0f,64f));c.drawForm(background);c.restoreGraphicsState()
                        }
                        layout.marks.forEach { m ->
                            val y=794f-m.y
                            when(m.type) {
                                "text" ->text(m.x,y,m.text,m.size,m.width)
                                "line" ->{c.moveTo(m.x,y);c.lineTo(m.x2,794f-m.y2);c.stroke()}
                                "circle" -> {
                                    val r=m.size;val k=r*.55228475f;val x=m.x
                                    c.moveTo(x+r,y);c.curveTo(x+r,y+k,x+k,y+r,x,y+r)
                                    c.curveTo(x-k,y+r,x-r,y+k,x-r,y);c.curveTo(x-r,y-k,x-k,y-r,x,y-r)
                                    c.curveTo(x+k,y-r,x+r,y-k,x+r,y);c.closePath();c.stroke()
                                }
                                "white" -> {c.setNonStrokingColor(1f,1f,1f);c.addRect(m.x,y-m.y2,m.x2,m.y2);c.fill();c.setNonStrokingColor(0f,0f,0f)}
                            }
                        }
                        text(18f,27f,"Referto cartaceo di supporto · Firme sul documento stampato · Pagina ${i+1}",8f)
                    }
                }
                doc.save(output)
            }
            return output
        }catch(t:Throwable){output.delete();throw t}
    }
    fun preview(context:Context,journal:Journal):Bitmap = PreviewRenderer.render(context,journal)
}
