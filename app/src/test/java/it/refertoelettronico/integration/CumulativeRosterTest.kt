package it.refertoelettronico.integration

import com.google.gson.Gson
import it.refertoelettronico.model.*
import it.refertoelettronico.storage.*
import it.refertoelettronico.pdf.SheetLayout
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Base64

class CumulativeRosterTest {
    private val gson=Gson()
    fun payload(count:Int=21,staff:Any?=null):String {
        val players=(1..count).map{mapOf("number" to if(it<=12)it else if(it==13)0 else 7,"name" to if(it==13)"" else "Atleta è $it","captain" to (it==1 || it>13),"libero" to (it>13))}
        val home=mutableMapOf<String,Any>("name" to "Casa","players" to players)
        if(staff!=null)home["staff"]=staff
        return gson.toJson(mapOf("schema" to MatchSetupImport.SCHEMA,"version" to 1,"config" to mapOf("matchNumber" to "00123","teams" to listOf(home,mapOf("name" to "Ospiti","players" to players.take(12))))))
    }
    fun candidate(code:String,id:String="1",name:String="Nome è",scope:String=when(code){"DIRS","DIRAA"->"SERVICE";"TEC1","TEC2","DIRAU"->"BENCH";else->"UNKNOWN"})=mapOf("sourceId" to id,"name" to name,"roleCode" to code,"roleLabel" to "Ruolo $code","scope" to scope)
    private fun valid(config:MatchConfig)=config.copy(teams=config.teams.map{Rosters.withMembers(it,it.roster!!.mapIndexed{i,m->m.copy(present=i<12)})})
    @Test fun cumulativeCandidatesAreNotTruncatedOrConfirmedAndOnlySelectedPlayersEnterPdf() {
        val raw=MatchSetupImport.decode(payload())
        assertEquals("00123",raw.matchNumber)
        assertEquals(21,raw.teams[0].roster!!.size)
        assertTrue(raw.teams.all{it.players.isEmpty()})
        assertEquals(21,raw.teams[0].roster!!.map{it.id}.distinct().size)
        val confirmed=valid(raw);MatchEngine.validate(confirmed)
        assertEquals(12,confirmed.teams[0].players.size)
        val texts=SheetLayout.build(Journal(config=confirmed)).flatMap{it.marks}.joinToString(" "){it.text}
        assertFalse(texts.contains("Atleta è 21"))
        assertEquals(21,confirmed.teams[0].roster!!.size)
    }
    @Test fun selectedDuplicateNumbersRolesAndMissingNumberAreCorrectableWithoutDeletingCandidates() {
        val raw=valid(MatchSetupImport.decode(payload()))
        fun select(index:Int):MatchConfig {
            val t=raw.teams[0];val next=Rosters.withMembers(t,t.roster!!.mapIndexed{i,m->m.copy(present=m.present || i==index)})
            return raw.copy(teams=listOf(next,raw.teams[1]))
        }
        for(index in listOf(12,13)) {
            val cfg=select(index);assertThrows(IllegalArgumentException::class.java){MatchEngine.validate(cfg)}
            assertTrue(RosterReview.issues(cfg.teams[0]).isNotEmpty());assertEquals(21,cfg.teams[0].roster!!.size)
        }
        val over=raw.copy(teams=raw.teams.map{Rosters.withMembers(it,it.roster!!.map{m->m.copy(present=true)})})
        assertThrows(IllegalArgumentException::class.java){MatchEngine.validate(over)}
    }
    @Test fun identicalEntriesHaveDistinctLocalIdsAndFileAndLinkPreserveAllUnicodeData() {
        val root=com.google.gson.JsonParser.parseString(payload(staff=listOf(candidate("TEC1"),candidate("TEC1")))).asJsonObject
        val athletes=root.getAsJsonObject("config").getAsJsonArray("teams")[0].asJsonObject.getAsJsonArray("players")
        athletes.set(1,athletes[0].deepCopy())
        val json=root.toString()
        val file=MatchSetupImport.read(json.byteInputStream())
        val link=MatchSetupLink.decode("refertovolley://import-match?data="+Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray()))
        assertEquals(file.teams[0].roster!!.map{it.player},link.teams[0].roster!!.map{it.player})
        assertEquals(file.teams[0].roster!![0].player,file.teams[0].roster!![1].player)
        assertNotEquals(file.teams[0].roster!![0].id,file.teams[0].roster!![1].id)
        assertEquals(2,file.teams[0].staffRoster!!.map{it.id}.distinct().size)
        assertEquals(file.teams[0].staffRoster!!.map{it.name},link.teams[0].staffRoster!!.map{it.name})
        assertTrue(link.teams[0].staffRoster!!.none{it.selected})
    }
    @Test fun legacyStaffStringsRemainSupportedAndMissingOrEmptyStaffCreatesNoPeople() {
        val old=MatchSetupImport.decode(payload(staff=listOf("Coach","Vice")))
        assertEquals("Coach",old.teams[0].staff[0]);assertNull(old.teams[0].staffRoster)
        for(raw in listOf(payload(),payload(staff=emptyList<String>()))) {
            val cfg=MatchSetupImport.decode(raw);assertTrue(cfg.teams[0].staff.all{it.isEmpty()});assertNull(cfg.teams[0].staffRoster)
        }
    }
    @Test fun unknownRolesAndSameSourceWithDifferentRolesRemainUnselectedAndUnmerged() {
        val cfg=MatchSetupImport.decode(payload(staff=listOf(candidate("TEC1"),candidate("TEC2"),candidate("ALIEN",id="2"))))
        val people=cfg.teams[0].staffRoster!!;assertEquals(3,people.size)
        assertEquals("ALIEN",people[2].roleCode);assertEquals("UNKNOWN",people[2].scope);assertEquals("UNMAPPED",people[2].assignment)
        assertTrue(StaffReview.issues(cfg).isEmpty())
        val selected=cfg.copy(teams=cfg.teams.mapIndexed{i,t->if(i==0)t.copy(staffRoster=people.map{it.copy(selected=true)})else t})
        assertEquals(3,StaffReview.issues(selected).size)
        assertThrows(IllegalArgumentException::class.java){StaffReview.confirm(selected)}
    }
    @Test fun serviceAssignmentsNeverEnterBenchAndOnlyConfirmedNamesAreExported() {
        val cfg=valid(MatchSetupImport.decode(payload(staff=listOf(candidate("TEC1",name="COACH_SELECTED"),candidate("DIRS","2","SCORER_SELECTED"),candidate("DIRAA","3","ATTENDANT_SELECTED"),candidate("DIRAU","4","UNSELECTED_PERSON")))))
        val selected=cfg.copy(teams=cfg.teams.mapIndexed{i,t->if(i==0)t.copy(staffRoster=t.staffRoster!!.mapIndexed{j,p->p.copy(selected=j<3)})else t})
        val result=StaffReview.confirm(selected);MatchEngine.validate(result)
        assertEquals("COACH_SELECTED",result.teams[0].staff[0]);assertEquals("SCORER_SELECTED",result.officials[2]);assertEquals("ATTENDANT_SELECTED",result.refereeAttendant)
        assertFalse(result.teams[0].staff.contains("SCORER_SELECTED"));assertFalse(result.teams[0].staff.contains("ATTENDANT_SELECTED"))
        val text=SheetLayout.build(Journal(config=result)).flatMap{it.marks}.joinToString(" "){it.text}
        assertFalse(text.contains("UNSELECTED_PERSON"));assertTrue(text.contains("ATTENDANT_SELECTED"))
        assertEquals(selected,StaffReview.prepare(result))
    }
    @Test fun scorerIsUniqueAcrossTeamsAndMissingNamesAndUnmappedSelectedRolesBlockConfirmation() {
        var cfg=MatchSetupImport.decode(payload(staff=listOf(candidate("DIRS"))))
        cfg=cfg.copy(teams=cfg.teams.map{it.copy(staffRoster=listOf(StaffCandidate(java.util.UUID.randomUUID().toString(),name="Éva",roleCode="DIRS",scope="SERVICE",assignment="SCORER",selected=true)))})
        assertEquals(2,StaffReview.issues(cfg).size)
        val invalid=cfg.copy(teams=listOf(cfg.teams[0].copy(staffRoster=listOf(StaffCandidate("empty",selected=true))),cfg.teams[1].copy(staffRoster=null)))
        assertTrue(StaffReview.issues(invalid).containsKey("empty"))
    }
    @Test fun draftRestoreKeepsIncompleteValuesIdsSelectionsAndChangesSeparateFromMatches() {
        val dir=Files.createTempDirectory("setup-draft").toFile()
        try {
            val raw=MatchSetupImport.decode(payload(staff=listOf(candidate("TEC1"))))
            val member=raw.teams[0].roster!![12]
            val team=Rosters.withMembers(raw.teams[0],raw.teams[0].roster!!.map{if(it.id==member.id)it.copy(player=it.player.copy(name="Corretto è"),present=true)else it})
            val draft=SetupDraft(raw.copy(teams=listOf(team,raw.teams[1])))
            val store=DraftStore(File(dir,"draft.json"));store.save(draft)
            assertEquals(draft,DraftStore(File(dir,"draft.json")).load())
            assertEquals(0,MatchStore(File(dir,"matches")).list().size)
            store.clear();assertNull(store.load())
        }finally{dir.deleteRecursively()}
    }
    @Test fun explicitCardinalityAndByteLimitsRejectEntirePayloadWithoutTruncation() {
        assertEquals(60,MatchSetupImport.decode(payload(60)).teams[0].roster!!.size)
        assertThrows(IllegalArgumentException::class.java){MatchSetupImport.decode(payload(61))}
        assertThrows(IllegalArgumentException::class.java){MatchSetupImport.decode(payload(staff=List(61){candidate("TEC1")}))}
        assertThrows(IllegalArgumentException::class.java){MatchSetupImport.decode(payload(staff=listOf(candidate("TEC1"),"mixed")))}
        assertThrows(IllegalArgumentException::class.java){MatchSetupImport.read(ByteArray(MatchSetupImport.MAX_BYTES+1).inputStream())}
        assertThrows(IllegalArgumentException::class.java){MatchSetupLink.decode("refertovolley://import-match?data="+"x".repeat(16_001))}
    }
    @Test fun sevenStaffSampleKeepsHomeAwayAssociationsAndDoesNotMergeNames() {
        val root=com.google.gson.JsonParser.parseString(payload(staff=listOf(candidate("TEC1","h1"),candidate("TEC2","h2"),candidate("DIRAU","h3"),candidate("DIRS","h4"),candidate("DIRAA","h5")))).asJsonObject
        root.getAsJsonObject("config").getAsJsonArray("teams")[1].asJsonObject.add("staff",gson.toJsonTree(listOf(candidate("TEC1","v1"),candidate("DIRAU","v2"))))
        val cfg=MatchSetupImport.decode(root.toString())
        assertEquals(3,cfg.teams[0].staffRoster!!.count{it.scope=="BENCH"})
        assertEquals(2,cfg.teams[0].staffRoster!!.count{it.scope=="SERVICE"})
        assertEquals(2,cfg.teams[1].staffRoster!!.count{it.scope=="BENCH"})
        assertEquals(7,cfg.teams.flatMap{it.staffRoster!!}.map{it.id}.distinct().size)
        assertEquals(1,cfg.teams.flatMap{it.staffRoster!!}.map{it.name}.distinct().size)
        assertTrue(cfg.teams.all{it.staff.all{value->value.isBlank()}})
    }
    @Test fun manualSourceLessPeopleAndUnknownFieldsAreSupportedButMalformedStaffIsRejected() {
        val manual=mapOf("name" to "Èva","roleCode" to "CUSTOM","roleLabel" to "Personalizzato","scope" to "UNKNOWN","ignored" to 123)
        val cfg=MatchSetupImport.decode(payload(staff=listOf(manual)))
        assertNull(cfg.teams[0].staffRoster!!.single().sourceId)
        for(invalid in listOf(mapOf("sourceId" to 123),mapOf("name" to 123))) {
            assertThrows(RuntimeException::class.java){MatchSetupImport.decode(payload(staff=listOf(invalid)))}
        }
    }

}
