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
    @Test fun manuallyChosenJerseyColumnWorksWithoutHeadersAndIgnoresProgressiveAndDateFragments() {
        val words=listOf(word("1",0f,0f),word("ROSSI",120f,0f),word("MARIA",200f,0f),
            word("12",350f,0f),word("04",390f,0f),word("2013",430f,0f),word("8",510f,0f))
        val row=Camp3Rows.suggest(words,numberColumnX=514f).single()
        assertEquals(8,row.number);assertEquals("ROSSI MARIA",row.name)
    }
    @Test fun centredNameHeadersDoNotCutOffSurnamesThatStartFurtherLeft() {
        val words=listOf(word("TESSERA",0f,0f),word("COGNOME",240f,0f),word("NOME",310f,0f),word("MAGLIA",480f,0f),
            word("123456",0f,30f),word("DE",110f,30f),word("ANGELIS",140f,30f),word("GIULIA",220f,30f),word("7",500f,30f))
        val row=Camp3Rows.suggest(words).single()
        assertEquals("DE ANGELIS GIULIA",row.name);assertEquals(7,row.number)
    }
    @Test fun slopingRowsKeepNamesAndNumbersTogetherAcrossTheTable() {
        fun tilted(text:String,x:Float,y:Float)=word(text,x,y+.12f*x)
        val words=listOf(tilted("COGNOME",100f,0f),tilted("NOME",220f,0f),tilted("MAGLIA",500f,0f))+
            (1..8).flatMap{i->listOf(tilted("ROSSI",100f,i*30f),tilted("MARIA",220f,i*30f),tilted(i.toString(),520f,i*30f))}
        val rows=Camp3Rows.suggest(words,skewSlope=.12f)
        assertEquals(8,rows.size);assertEquals((1..8).toList(),rows.map{it.number})
        assertTrue(rows.all{it.name=="ROSSI MARIA"})
    }
    @Test fun aReadableJerseyHeaderIsEnoughEvenIfTheNameHeaderWasMissed() {
        val rows=Camp3Rows.suggest(listOf(word("MAGLIA",460f,0f),word("ROSSI",100f,30f),word("MARIA",200f,30f),word("12",478f,30f)))
        assertEquals(12,rows.single().number)
    }
    @Test fun partialNamesAreShownForReviewInsteadOfDroppingTheAthlete() {
        val row=Camp3Rows.suggest(listOf(word("COGNOME",100f,0f),word("MAGLIA",460f,0f),word("ROSSI",100f,30f),word("7",478f,30f))).single()
        assertEquals("ROSSI",row.name);assertEquals(7,row.number)
        assertTrue(row.issues.any{it.contains("Nome letto")})
    }
    @Test fun ambiguousDigitsAreCorrectedOnlyInTheSelectedColumnAndAlwaysFlagged() {
        val rows=Camp3Rows.suggest(listOf(word("ROSSI",100f,0f),word("MARIA",200f,0f),word("O8",500f,0f)),numberColumnX=508f)
        assertEquals(8,rows.single().number);assertTrue(rows.single().issues.any{it.contains("O8")})
        assertNull(Camp3Rows.suggest(listOf(word("ROSSI",100f,0f),word("MARIA",200f,0f),word("L",500f,0f)),numberColumnX=504f).single().number)
    }
    @Test fun twoDistinctCandidatesAreNotSilentlyAssignedAndDuplicatesAreFlagged() {
        val header=listOf(word("COGNOME",100f,0f),word("MAGLIA",460f,0f))
        val uncertain=Camp3Rows.suggest(header+listOf(word("ROSSI",100f,30f),word("MARIA",200f,30f),word("7",462f,30f),word("9",486f,30f))).single()
        assertNull(uncertain.number)
        val duplicate=Camp3Rows.suggest(header+(1..2).flatMap{i->listOf(word("ROSSI",100f,i*30f),word("MARIA",200f,i*30f),word("7",478f,i*30f))})
        assertEquals(2,duplicate.size);assertTrue(duplicate.all{r->r.issues.any{it.contains("altra riga")}})
    }
    @Test fun athleteSurnamedMedicoIsNotMistakenForTheStaffHeading() {
        val rows=Camp3Rows.suggest(listOf(word("COGNOME",100f,0f),word("MAGLIA",460f,0f),word("MEDICO",100f,30f),word("GIULIA",200f,30f),word("7",478f,30f)))
        assertEquals("MEDICO GIULIA",rows.single().name)
    }

}
