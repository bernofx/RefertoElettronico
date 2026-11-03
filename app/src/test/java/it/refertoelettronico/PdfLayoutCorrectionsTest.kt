package it.refertoelettronico

import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import it.refertoelettronico.model.*
import it.refertoelettronico.pdf.*
import it.refertoelettronico.ui.GuideExample
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PdfLayoutCorrectionsTest {
    @Test fun rightBenchNamesStayPastTheRoleColumnInTheActualPdf() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>();PDFBoxResourceLoader.init(context)
        val base=GuideExample.journal(10).copy(signatures=null)
        val names=List(5){"PANCHINA_DX_${it+1} Cognome e nome molto lunghi per verificare i limiti della colonna"}
        val named=base.copy(config=base.config.copy(teams=base.config.teams.with(1,base.config.teams[1].copy(staff=names))))
        val marks=SheetLayout.build(named).first().marks
        names.forEach{value->
            val mark=marks.single{it.text==value};assertEquals(1070f,mark.cell!!.left,.001f)
            assertTrue(mark.cell!!.right<=1184f)
        }
        val blank=named.copy(config=named.config.copy(teams=named.config.teams.with(1,named.config.teams[1].copy(staff=List(5){""}))))
        val image=PDDocument.load(PdfExporter.export(context,named)).use{PDFRenderer(it).renderImageWithDPI(0,72f)}
        val reference=PDDocument.load(PdfExporter.export(context,blank)).use{PDFRenderer(it).renderImageWithDPI(0,72f)}
        for(x in 1045..1068)for(y in 606+48..671+48)assertEquals("Colonna ruoli alterata in $x,$y",reference.getPixel(x,y),image.getPixel(x,y))
        image.recycle();reference.recycle()
    }
    @Test fun signatureUsesMoreHeightWithoutStretchingOrCoveringCaptions() {
        val journal=LegacySignatureFixtures.fullySigned(GuideExample.journal(10).events,
            Demo.config().copy(officials=Demo.config().officials.with(1,"Secondo arbitro di prova")))
        val oldHeights=mapOf(SignatureRole.CAPTAIN_HOME to 9f,SignatureRole.CAPTAIN_AWAY to 9f,
            SignatureRole.COACH_HOME to 9f,SignatureRole.COACH_AWAY to 9f,
            SignatureRole.FIRST_REFEREE to 7f,SignatureRole.SECOND_REFEREE to 7f,SignatureRole.SCORER to 7f)
        SignatureRole.entries.forEach { role->
            val record=Signatures.current(journal,role)!!;val area=SignatureGeometry.cell(role,record)
            val points=SignatureGeometry.fit(record,area).flatten()
            val width=points.maxOf{it.x}-points.minOf{it.x};val height=points.maxOf{it.y}-points.minOf{it.y}
            val original=record.strokes.flatten();val ratio=record.aspectRatio*(original.maxOf{it.x}-original.minOf{it.x})/(original.maxOf{it.y}-original.minOf{it.y})
            assertTrue("Firma non ingrandita: $role",height>oldHeights.getValue(role)*1.3f)
            assertEquals(ratio,width/height,.002f)
        }
    }
}
