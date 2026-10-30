package it.refertoelettronico

import it.refertoelettronico.ocr.*
import it.refertoelettronico.model.*
import org.junit.Assert.*
import org.junit.Test

class Camp3RowsTest {
    private fun word(text:String,x:Float,y:Float)=OcrWord(text,x,y,x+text.length*8,y+16)
    @Test fun jerseyColumnIsDistinguishedFromSequenceMembershipAndBirthDate() {
        val words=listOf(word("N°",0f,0f),word("TESSERA",40f,0f),word("COGNOME",160f,0f),word("NOME",230f,0f),word("NASCITA",360f,0f),word("MAGLIA",460f,0f),
            word("1",0f,30f),word("123456",40f,30f),word("ROSSI",160f,30f),word("MARIA",230f,30f),word("12/04/2013",360f,30f),word("7",476f,30f))
        val row=Camp3Rows.suggest(words).single()
        assertEquals("ROSSI MARIA",row.name);assertEquals(7,row.number)
    }
    @Test fun withoutJerseyHeaderNumbersAreLeftForReviewEvenIfThereIsASequenceNumber() {
        val row=Camp3Rows.suggest(listOf(word("COGNOME",100f,0f),word("NOME",200f,0f),
            word("1",0f,30f),word("D’ANGELO",100f,30f),word("GIULIA",200f,30f))).single()
        assertEquals("D’ANGELO GIULIA",row.name);assertNull(row.number)
    }
    @Test fun staffRowsAfterTheAthleteTableAreNotImported() {
        val rows=Camp3Rows.suggest(listOf(word("COGNOME",100f,0f),word("NOME",200f,0f),
            word("ROSSI",100f,30f),word("MARIA",200f,30f),word("ALLENATORE",0f,60f),
            word("BIANCHI",100f,90f),word("LUCA",200f,90f)))
        assertEquals(listOf("ROSSI MARIA"),rows.map{it.name})
    }
    @Test fun unrecognizedLayoutStillRequiresManualNumbersAndNeverInfersRoles() {
        val rows=Camp3Rows.suggest(listOf(word("123456",0f,0f),word("ROSSI",100f,0f),word("MARIA",200f,0f)))
        assertEquals(1,rows.size);assertNull(rows[0].number)
        assertNotNull(Camp3Rows.validate(rows.map{Player(it.number ?: 0,it.name)}))
    }
    @Test fun importRequiresCheckedNumbersAndRolesAndDoesNotModifyTheOtherTeam() {
        val original=Demo.config()
        val roster=(1..6).map{Player(it,"Atleta $it",captain=it==1)}
        assertNull(Camp3Rows.validate(roster))
        assertNotNull(Camp3Rows.validate(roster.map{it.copy(captain=false)}))
        assertNotNull(Camp3Rows.validate(roster.map{it.copy(number=1)}))
        assertNotNull(Camp3Rows.validate(roster.map{it.copy(number=0)}))
        val changed=original.copy(teams=original.teams.with(1,original.teams[1].copy(players=roster)))
        assertEquals(original.teams[0],changed.teams[0])
        assertEquals(roster,changed.teams[1].players)
    }
}
