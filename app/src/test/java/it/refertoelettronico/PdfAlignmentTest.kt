package it.refertoelettronico

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import it.refertoelettronico.pdf.*
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
class PdfAlignmentTest {
    @Test fun longMatchFitsWritingAreasAndPreservesPreprintedLabelsInTheActualPdf() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        PDFBoxResourceLoader.init(context)
        val journal=PdfFixtures.completeWithLongFields()
        val layouts=SheetLayout.build(journal)
        // Independent rectangles around printed labels and signature spaces, measured on the blank.
        val protected=listOf(
            TextCell(224f,33f,258f,45f), // CAMPO
            TextCell(242f,72f,261f,89f),TextCell(371f,72f,388f,89f), // preprinted A/B
            TextCell(803f,72f,822f,89f),TextCell(932f,72f,949f,89f),
            TextCell(188f,620f,662f,647f), // approval headings
            TextCell(188f,647f,218f,685f), // 1st/2nd/scorer labels
            TextCell(514f,647f,662f,685f), // signatures remain blank
            TextCell(758f,567f,774f,667f), // preprinted set numbers
            TextCell(795f,567f,802f,667f), // left parentheses
            TextCell(824f,567f,829f,667f), // right parentheses
            TextCell(708f,700f,711f,705f),TextCell(730f,700f,740f,705f),
            TextCell(789f,700f,792f,705f),TextCell(811f,700f,821f,705f),
            TextCell(863f,700f,866f,705f),TextCell(885f,700f,895f,705f),
            TextCell(929f,382f,1186f,395f), // player-column headings
            TextCell(929f,553f,1186f,566f), // LIBERO
            TextCell(929f,592f,1186f,605f) // staff heading
        )
        PDDocument().use { doc->
            val font=context.assets.open("DejaVuSans.ttf").use{PDType0Font.load(doc,it)}
            val ascent=font.fontDescriptor.ascent/1000;val descent=font.fontDescriptor.descent/1000
            layouts.forEachIndexed { page,layout->layout.marks.filter{it.type=="text" && it.text.isNotBlank()}.forEach { mark->
                val box=mark.cell!!
                val fitted=TextFit.fit(mark.text,box,mark.size,ascent,descent){font.getStringWidth(it)/1000}
                assertTrue(fitted.x>=box.left-.001f && fitted.x+fitted.width<=box.right+.001f)
                assertTrue(fitted.baseline-ascent*fitted.size>=box.top-.001f)
                assertTrue(fitted.baseline-descent*fitted.size<=box.bottom+.001f)
                if(page==0)protected.forEach { label->
                    val overlaps=fitted.x<label.right && fitted.x+fitted.width>label.left &&
                        fitted.baseline-ascent*fitted.size<label.bottom && fitted.baseline-descent*fitted.size>label.top
                    assertFalse("Testo sovrapposto a un'etichetta prestampata: ${mark.text}",overlaps)
                }
            }}
        }
        val file=PdfExporter.export(context,journal)
        val image=PDDocument.load(file).use { doc->
            val text=PDFTextStripper().getText(doc)
            assertTrue(text.contains("Comune residenza 6"))
            assertTrue(text.contains("Panchina 1-4 CognomeComposto"))
            assertTrue(text.contains("punti oltre 48"))
            PDFRenderer(doc).renderImageWithDPI(0,72f)
        }
        val blank=context.assets.open("referto-fipav.pdf").use{input->PDDocument.load(input).use{PDFRenderer(it).renderImageWithDPI(0,72f)}}
        protected.forEach { rect->
            var changes=0;var total=0
            for(y in rect.top.toInt() until rect.bottom.toInt())for(x in rect.left.toInt() until rect.right.toInt()) {
                val before=blank.getPixel(x,y);val after=image.getPixel(x,y+48)
                fun darkness(c:Int)=((c shr 16 and 255)+(c shr 8 and 255)+(c and 255))/3
                if(kotlin.math.abs(darkness(before)-darkness(after))>32)changes++
                total++
            }
            assertTrue("Etichetta modificata in $rect: $changes/$total pixel",changes<=maxOf(2,total/100))
        }
        blank.recycle()
        val outputs=File("build/outputs/verification").apply{mkdirs()}
        file.copyTo(File(outputs,"referto-allineamenti.pdf"),overwrite=true)
        File(outputs,"referto-allineamenti.png").outputStream().use{image.compress(Bitmap.CompressFormat.PNG,100,it)}
        image.recycle()
        val preview=PdfExporter.preview(context,journal)
        File(outputs,"anteprima-allineamenti.png").outputStream().use{preview.compress(Bitmap.CompressFormat.PNG,100,it)}
        preview.recycle()
    }
}
