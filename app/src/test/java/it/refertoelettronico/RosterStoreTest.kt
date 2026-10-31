package it.refertoelettronico

import com.google.gson.Gson
import it.refertoelettronico.model.*
import it.refertoelettronico.pdf.SheetLayout
import it.refertoelettronico.storage.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class RosterStoreTest {
    private fun catalog():Team {
        val members=(1..20).map{RosterMember("id-$it",Player(it,if(it==20)"ASSENTE_UNICA" else "Atleta $it",captain=it==1),present=it<=6)}
        return Rosters.withMembers(Team("Squadra",emptyList()),members)
    }
    @Test fun savesWholeSeasonRosterButNewMatchesRequireTheirOwnAttendance() {
        val dir=Files.createTempDirectory("rosters").toFile()
        try {
            val store=RosterStore(dir);val saved=store.save("Squadra U14 2026",catalog())
            assertEquals(20,saved.team.players.size)
            val loaded=Rosters.fromTemplate(store.list().single().second.getOrThrow().team)
            assertEquals(20,loaded.roster!!.size);assertTrue(loaded.players.isEmpty())
            val selected=Rosters.withMembers(loaded,loaded.roster!!.mapIndexed{i,m->m.copy(present=i<6)})
            assertEquals((1..6).toList(),selected.players.map{it.number})
            assertEquals(saved.id,store.save("Squadra U14 2026",selected,saved.id).id)
            assertEquals(20,store.list().single().second.getOrThrow().team.players.size)
        }finally{dir.deleteRecursively()}
    }
    @Test fun absenteesNeverEnterLineupsSubstitutionsOrPaperPdf() {
        val team=catalog();val config=Demo.config().copy(teams=listOf(team,Demo.config().teams[1]))
        MatchEngine.validate(config)
        val journal=Journal(config=config,events=listOf(Demo.start()))
        assertThrows(RuleViolation::class.java){MatchEngine.add(journal,Event(Kind.SUBSTITUTION,0,1,20))}
        assertThrows(RuleViolation::class.java){MatchEngine.add(Journal(config=config),Demo.start().copy(lineups=listOf(listOf(1,2,3,4,5,20),(1..6).toList())))}
        val texts=SheetLayout.build(journal).flatMap{it.marks}.joinToString(" "){it.text}
        assertFalse(texts.contains("ASSENTE_UNICA"))
        assertEquals(20,Rosters.template(team).players.size)
    }
    @Test fun backupKeepsFullRosterAndAttendanceWithoutChangingTheAcceptedGame() {
        val dir=Files.createTempDirectory("roster-match").toFile()
        try {
            val config=Demo.config().copy(teams=listOf(catalog(),Demo.config().teams[1]))
            val original=Journal(config=config,events=listOf(Demo.start(),Event(Kind.POINT,1)))
            val store=MatchStore(dir);val loaded=store.decode(store.encode(original))
            assertEquals(original,loaded)
            assertEquals(20,loaded.config.teams[0].roster!!.size)
            assertEquals(6,loaded.config.teams[0].players.size)
            assertEquals(MatchEngine.replay(config,original.events),MatchEngine.replay(loaded.config,loaded.events))
            val legacy=Journal(config=Demo.config(),events=listOf(Demo.start()))
            assertFalse(store.encode(legacy).contains("\"roster\""))
            assertEquals(legacy,store.decode(store.encode(legacy)))
        }finally{dir.deleteRecursively()}
    }
    @Test fun inconsistentAttendanceAndOversizedCatalogCannotBeSavedAsAMatch() {
        val config=Demo.config().copy(teams=listOf(catalog(),Demo.config().teams[1]))
        val wrong=config.teams[0].copy(players=config.teams[0].players.dropLast(1))
        assertThrows(RuleViolation::class.java){MatchEngine.validate(config.copy(teams=listOf(wrong,config.teams[1])))}
        val duplicateIds=Rosters.withMembers(catalog(),catalog().roster!!.map{it.copy(id="same")})
        assertThrows(RuleViolation::class.java){MatchEngine.validate(config.copy(teams=listOf(duplicateIds,config.teams[1])))}
    }
    @Test fun catalogUpdatesAreIndependentFromPastMatchesAndDoNotOverwriteAnotherTeam() {
        val dir=Files.createTempDirectory("roster-independent").toFile()
        try {
            val store=RosterStore(dir);val first=store.save("Prima",catalog())
            val oldMatch=Journal(config=Demo.config().copy(teams=listOf(catalog(),Demo.config().teams[1])))
            store.save("Seconda",Demo.config().teams[1])
            val changed=catalog().copy(name="Nuovo nome")
            store.save("Prima",changed,first.id)
            assertEquals(2,store.list().size)
            assertEquals("Squadra",oldMatch.config.teams[0].name)
            assertEquals("Nuovo nome",store.list().map{it.second.getOrThrow()}.single{it.id==first.id}.team.name)
            assertThrows(IllegalArgumentException::class.java){store.save("Rosa incompleta",catalog().copy(players=listOf(Player(0,"")),roster=null))}
            java.io.File(dir,"damaged.json").writeText("not json")
            assertTrue(store.list().single{it.first=="damaged"}.second.isFailure)
            assertTrue(java.io.File(dir,"damaged.json").exists())
        }finally{dir.deleteRecursively()}
    }
}
