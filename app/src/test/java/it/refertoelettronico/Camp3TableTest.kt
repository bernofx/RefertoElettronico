package it.refertoelettronico

import it.refertoelettronico.ocr.*
import org.junit.Assert.*
import org.junit.Test

class Camp3TableTest {
    private fun word(text:String,x:Float,y:Float)=OcrWord(text,x,y,x+text.length*8,y+16)
    private val header=listOf(word("TESSERA",50f,200f),word("COGNOME",200f,200f),word("NOME",320f,200f),word("MAGLIA",700f,200f))
    private val players=(1..6).flatMap{i->listOf(word("ROSSI",200f,200+i*30f),word("MARIA",320f,200+i*30f),word(i.toString(),720f,200+i*30f))}
    @Test fun tableIsLocatedFromItsHeadersAndEndsBeforeTheStaffSection() {
        val crop=Camp3Table.detect(header+players+word("TECNICI",50f,600f),1000,1400,Camp3Section.PLAYERS)!!
        assertTrue(crop.top<200/1400f);assertTrue(crop.left<.05f && crop.right>.748f)
        assertTrue(crop.bottom<600/1400f && crop.bottom>500/1400f)
    }
    @Test fun missingLastAthletesAreNotCroppedAwayJustBecauseOcrDidNotReadThem() {
        val crop=Camp3Table.detect(header+players,1000,1400,Camp3Section.PLAYERS)!!
        assertEquals(1f,crop.bottom,.001f)
    }
    @Test fun weakOrMissingHeadersLeaveTheCropToTheUser() {
        assertNull(Camp3Table.detect(players,1000,1400,Camp3Section.PLAYERS))
        assertNull(Camp3Table.detect(listOf(word("COGNOME",200f,200f))+players,1000,1400,Camp3Section.PLAYERS))
    }
    @Test fun separateStaffPhotoDoesNotIncludeThePlayersAboveIt() {
        val crop=Camp3Table.detect(header+players+listOf(word("ALLENATORE",50f,700f),word("ROSSI",240f,700f),word("LUCA",320f,700f)),1000,1400,Camp3Section.STAFF)!!
        assertTrue(crop.top>600/1400f);assertEquals(1f,crop.bottom,.001f)
    }
}
