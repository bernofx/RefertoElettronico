package it.refertoelettronico

import com.google.gson.Gson
import it.refertoelettronico.ocr.*
import org.junit.Assert.*
import org.junit.Test

/** Only anonymized words/geometry; the user's document and personal data are never bundled. */
class Camp3ComoTest {
    private fun words():List<OcrWord> = javaClass.getResourceAsStream("/ocr/camp3-como-anonymized.json")!!.bufferedReader().use {
        Gson().fromJson(it,Array<OcrWord>::class.java).toList()
    }
    @Test fun camp3NumberHeadingFindsAllFourteenJerseysWithoutUsingDatesOrMembershipNumbers() {
        val rows=Camp3Rows.suggest(words())
        assertEquals(14,rows.size)
        assertEquals((1..14).toList(),rows.map{it.number})
        assertTrue(rows.all{it.name.startsWith("ALFA BETA")})
        assertTrue(rows.none{it.name.contains("SIGMA") || it.name.contains("7654321") || it.name.contains("AI ")})
    }
    @Test fun explicitCaptainAndBothLiberoMarkersAreSuggestedForReview() {
        val rows=Camp3Rows.suggest(words())
        assertEquals(1,rows.count{it.captain});assertEquals(2,rows.count{it.libero})
        assertEquals(10,rows.single{it.captain}.number)
        assertEquals(listOf(13,14),rows.filter{it.libero}.map{it.number})
        assertTrue(rows.filter{it.captain || it.libero}.all{row->row.issues.any{it.startsWith("Ruolo")}})
    }
    @Test fun automaticCropContainsAllAthletesAndStopsAtTheStaffTableHeading() {
        val photo=words()
        val crop=Camp3Table.detect(photo,595,842,Camp3Section.PLAYERS)!!
        assertTrue(crop.top*842<201)
        assertTrue(crop.bottom*842>453 && crop.bottom*842<477)
        assertTrue(crop.left*595<28)
    }
    @Test fun aMatchNumberElsewhereOnThePageIsNotAPlayerColumn() {
        val matchNumber=OcrWord("N°",10f,100f,20f,110f)
        val rows=Camp3Rows.suggest((words()-words().filter{it.text=="N°"})+matchNumber)
        assertTrue(rows.all{it.number==null})
    }
    @Test fun croppedAndScaledPhotoCoordinatesStillKeepJerseysAndRows() {
        val shifted=words().map{it.copy(left=(it.left-10)*3,top=(it.top-190)*3,right=(it.right-10)*3,bottom=(it.bottom-190)*3)}
        val rows=Camp3Rows.suggest(shifted)
        assertEquals((1..14).toList(),rows.map{it.number})
    }
}
