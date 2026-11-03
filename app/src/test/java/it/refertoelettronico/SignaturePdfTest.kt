package it.refertoelettronico

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import it.refertoelettronico.model.*
import it.refertoelettronico.pdf.*
import it.refertoelettronico.ui.GuideExample
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignaturePdfTest {
    @Test fun actualPdfFitsLargerInkAndPreservesSignaturesAfterCorrections() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>();PDFBoxResourceLoader.init(context)
        val signed=GuideExample.journal(11)
        val roles=Signatures.required(signed.config,true)+Signatures.required(signed.config,false)
        val marks=SheetLayout.build(signed).first().marks.filter{it.type=="signature"}
        assertEquals(roles.size,marks.size)
        marks.forEach { mark ->
            val box=mark.cell!!
            SignatureGeometry.fit(mark.signature!!,box).flatten().forEach { point->
                assertTrue(point.x>box.left && point.x<box.right && point.y>box.top && point.y<box.bottom)
            }
        }
        val blank=signed.copy(signatures=null)
        val blankPdf=PdfExporter.export(context,blank)
        val signedPdf=PdfExporter.export(context,signed)
        val image=PDDocument.load(signedPdf).use{doc->
            val text=PDFTextStripper().getText(doc)
            assertTrue(text.contains("FIRME DEL REFERTO"));assertFalse(text.contains("FIRME DA COMPLETARE"))
            PDFRenderer(doc).renderImageWithDPI(0,72f)
        }
        val blankImage=PDDocument.load(blankPdf).use{PDFRenderer(it).renderImageWithDPI(0,72f)}
        marks.forEach { mark->
            val box=mark.cell!!
            val changed=(box.left.toInt() until box.right.toInt()).sumOf { x->
                (box.top.toInt()+48 until box.bottom.toInt()+48).count{y->image.getPixel(x,y)!=blankImage.getPixel(x,y)}
            }
            assertTrue("Tratto assente nel campo ${mark.signature!!.role}",changed>5)
        }
        // Printed role and signature-column headings remain unchanged.
        for(x in 514..660)for(y in 632+48..646+48)assertEquals(blankImage.getPixel(x,y),image.getPixel(x,y))
        for((left,right) in listOf(928 to 952,1058 to 1083)) {
            for(x in left..right)for(y in 687+48..691+48)assertEquals(blankImage.getPixel(x,y),image.getPixel(x,y))
            for(x in left..right)for(y in 707+48..711+48)assertEquals(blankImage.getPixel(x,y),image.getPixel(x,y))
        }
        val stale=MatchEngine.add(signed,Event(Kind.NOTE,text="Risultato ricontrollato"))
        val staleMarks=SheetLayout.build(stale).first().marks.filter{it.type=="signature"}
        assertEquals(marks.size,staleMarks.size)
        assertTrue(Signatures.needsReview(stale,SignatureRole.FIRST_REFEREE))
        val dir=File("build/outputs/verification").apply{mkdirs()}
        signedPdf.copyTo(File(dir,"referto-firme-0.8.0.pdf"),overwrite=true)
        File(dir,"referto-firme-0.8.0.png").outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        image.recycle();blankImage.recycle()
    }
}
