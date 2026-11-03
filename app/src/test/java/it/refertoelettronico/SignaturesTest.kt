package it.refertoelettronico

import it.refertoelettronico.model.*
import it.refertoelettronico.storage.MatchStore
import it.refertoelettronico.ui.GuideExample
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class SignaturesTest {
    private val ink=LegacySignatureFixtures.ink
    private val toss=Demo.start(1_000_000).copy(server=1,left=1)
    private fun pre():Journal {
        var j=Journal(config=Demo.config().copy(officials=Demo.config().officials.with(1,"Secondo arbitro di prova")))
        Signatures.required(j.config,true).forEach{j=Signatures.capture(j,it,ink,3f,toss,at=900_000)}
        return j
    }
    private fun finished():Journal {
        var j=Journal(config=pre().config)
        Signatures.required(j.config,true).forEach{j=Signatures.capture(j,it,ink,3f,Demo.start(),at=900_000)}
        return j.copy(events=GuideExample.journal(10).events)
    }
    private fun signed():Journal {var j=finished();Signatures.required(j.config,false).forEach{j=Signatures.capture(j,it,ink,3f,at=2_000_000)};return j}
    @Test fun rolesFollowFipavAndOmitAbsentCoachesAndSecondReferee() {
        val config=Demo.config().copy(teams=Demo.config().teams.map{it.copy(staff=List(5){""})})
        assertEquals(listOf(SignatureRole.CAPTAIN_HOME,SignatureRole.CAPTAIN_AWAY),Signatures.required(config,true))
        assertEquals(listOf(SignatureRole.SCORER,SignatureRole.FIRST_REFEREE),Signatures.required(config,false))
        assertFalse(Signatures.required(config,false).any{it.team>=0})
    }
    @Test fun descriptiveMatchEditsPreserveEverySignatureWithoutReviewWarnings() {
        val j=signed()
        val updated=Signatures.withConfig(j,j.config.copy(location="Località completata",venue="Palestra corretta",date="03/11/2026",scheduledTime="20:30",
            competition="U14F",phase="Girone A",matchNumber="00042",season="2026/2027",initialObservations="Nota amministrativa",fallbackReason="Portale indisponibile",
            officialCities=List(7){"Como"},refereeAttendant="Addetto di prova"))
        assertEquals(j.signatures,updated.signatures)
        SignatureRole.entries.forEach { role->assertNotNull(Signatures.current(updated,role));assertFalse(Signatures.needsReview(updated,role))}
    }
    @Test fun rosterOrTossEditsKeepInkAndFlagOnlyRelevantContent() {
        val j=pre()
        val started=MatchEngine.add(j,toss)
        assertFalse(Signatures.needsReview(MatchEngine.add(started,Event(Kind.POINT,team=1)),SignatureRole.CAPTAIN_HOME))
        val team=j.config.teams[0].copy(players=j.config.teams[0].players.map{if(it.number==2)it.copy(name="Nome corretto")else it})
        val changed=Signatures.withConfig(j,j.config.copy(teams=j.config.teams.with(0,team)))
        assertTrue(Signatures.missing(changed,true).isEmpty())
        assertEquals(j.signatures,changed.signatures)
        assertTrue(Signatures.needsReview(changed,SignatureRole.CAPTAIN_HOME));assertTrue(Signatures.needsReview(changed,SignatureRole.COACH_HOME))
        assertFalse(Signatures.needsReview(changed,SignatureRole.CAPTAIN_AWAY))
        assertTrue(Signatures.needsReview(j,SignatureRole.CAPTAIN_AWAY,toss.copy(left=0)))
    }
    @Test fun displayColourAndLibraryCandidatesDoNotChangeApprovalContent() {
        val j=pre();val changed=Signatures.withConfig(j,j.config.copy(teams=j.config.teams.map{it.copy(colorKey="white",historicalRoster=true,roster=listOf(RosterMember("candidate",Player(99,"Assente"),false)))}))
        assertTrue(Signatures.missing(changed,true).isEmpty());assertFalse(Signatures.needsReview(changed,SignatureRole.CAPTAIN_HOME))
    }
    @Test fun finalCaptureRequiresFinishedMatchAndScorerSecondFirstOrder() {
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(pre(),SignatureRole.SCORER,ink,3f)}
        val j=finished()
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(j,SignatureRole.FIRST_REFEREE,ink,3f)}
        val scorer=Signatures.capture(j,SignatureRole.SCORER,ink,3f)
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(scorer,SignatureRole.FIRST_REFEREE,ink,3f)}
        assertTrue(Signatures.missing(signed(),false).isEmpty())
    }
    @Test fun correctionsRetainFinalInkAndExposeReviewWithoutClearingIt() {
        val j=signed();val corrected=MatchEngine.add(j,Event(Kind.NOTE,text="Correzione dell’arbitro"))
        assertEquals(j.signatures,corrected.signatures);assertTrue(Signatures.missing(corrected,false).isEmpty())
        Signatures.required(j.config,false).forEach{assertTrue(Signatures.needsReview(corrected,it))}
        assertEquals(3,Signatures.missing(MatchEngine.undo(j),false).size)
        val restored=MatchEngine.redo(MatchEngine.undo(j))
        assertTrue(Signatures.missing(restored,false).isEmpty());assertFalse(Signatures.needsReview(restored,SignatureRole.FIRST_REFEREE))
        assertEquals(1,Signatures.missing(Signatures.remove(j,SignatureRole.SCORER),false).size)
        assertTrue(Signatures.needsReview(Signatures.remove(j,SignatureRole.SCORER),SignatureRole.FIRST_REFEREE))
    }
    @Test fun changingAnActualSignerRequiresOnlyThatActorsNewSignature() {
        val j=signed();val changed=Signatures.withConfig(j,j.config.copy(officials=j.config.officials.with(0,"Altra arbitra")))
        assertNull(Signatures.current(changed,SignatureRole.FIRST_REFEREE))
        assertEquals(j.signatures,changed.signatures)
        assertNotNull(Signatures.current(changed,SignatureRole.SCORER));assertNotNull(Signatures.current(changed,SignatureRole.SECOND_REFEREE))
        assertFalse(Signatures.needsReview(changed,SignatureRole.SCORER))
    }
    @Test fun archivesPreserveInkAndOlderUnsignedArchivesStillLoad() {
        val store=MatchStore(Files.createTempDirectory("signed-match").toFile());val j=signed()
        assertEquals(j,store.load(store.create(j)));assertEquals(j,store.decode(store.encode(j)))
        assertTrue(store.decode(store.encode(Journal(config=Demo.config()))).signatures.isNullOrEmpty())
    }
    @Test fun existingLegacyInkReturnsWithoutRecoveryEvenIfLocationWasAlreadyCompleted() {
        val original=LegacySignatureFixtures.preliminary()
        val edited=original.copy(events=GuideExample.journal(10).events,config=original.config.copy(location="Località nuova"))
        assertTrue(Signatures.missing(edited,true).isEmpty());assertEquals(original.signatures,edited.signatures)
        assertTrue(Signatures.needsReview(edited,SignatureRole.CAPTAIN_HOME))
        assertEquals(4,it.refertoelettronico.pdf.SheetLayout.build(edited).first().marks.count{it.type=="signature"})
    }
    @Test fun verifiedLegacySignaturesAcquireReviewBaselineBeforeDescriptiveEdit() {
        val j=LegacySignatureFixtures.preliminary().copy(events=GuideExample.journal(10).events)
        val next=Signatures.withConfig(j,j.config.copy(location="Luogo completato",venue="Campo aggiornato",officials=j.config.officials.with(0,"Arbitra di prova")))
        assertTrue(Signatures.missing(next,true).isEmpty());assertFalse(Signatures.needsReview(next,SignatureRole.CAPTAIN_HOME))
        assertEquals(j.signatures!!.map{it.contentHash to it.signedAt},next.signatures!!.map{it.contentHash to it.signedAt})
        assertEquals(j.signatures!!.map{it.strokes},next.signatures!!.map{it.strokes})
    }
    @Test fun legacyFinalSignaturesRemainUsableAcrossAdministrativeCorrections() {
        val j=LegacySignatureFixtures.fullySigned(GuideExample.journal(10).events)
        assertFalse(Signatures.needsReview(j,SignatureRole.FIRST_REFEREE))
        val next=Signatures.withConfig(j,j.config.copy(location="Luogo completato",venue="Palestra aggiornata"))
        assertTrue(Signatures.missing(next,false).isEmpty());assertFalse(Signatures.needsReview(next,SignatureRole.FIRST_REFEREE))
        assertEquals(j.signatures!!.map{it.signedAt to it.contentHash},next.signatures!!.map{it.signedAt to it.contentHash})
    }
    @Test fun version072FinalApprovalsSurviveCompletingAnOriginallyBlankPlace() {
        val j=LegacySignatureFixtures.fullySigned(GuideExample.journal(10).events,Demo.config().copy(location=""),2)
        assertFalse(Signatures.needsReview(j,SignatureRole.FIRST_REFEREE))
        val updated=Signatures.withConfig(j,j.config.copy(location="Como"))
        assertTrue(Signatures.missing(updated,true).isEmpty());assertTrue(Signatures.missing(updated,false).isEmpty())
        assertEquals(j.signatures!!.map{it.strokes to it.signedAt},updated.signatures!!.map{it.strokes to it.signedAt})
        assertFalse(Signatures.needsReview(updated,SignatureRole.FIRST_REFEREE))
    }
    @Test fun rejectEmptyDotsOversizedAndNonFiniteInk() {
        val j=Journal(config=Demo.config())
        listOf(emptyList(),listOf(listOf(SignaturePoint(.1f,.1f))),listOf(List(4001){SignaturePoint(.1f,.2f)}),listOf(listOf(SignaturePoint(Float.NaN,0f),SignaturePoint(1f,1f)))).forEach { strokes->
            assertThrows(IllegalArgumentException::class.java){Signatures.capture(j,SignatureRole.CAPTAIN_HOME,strokes,3f,toss)}
        }
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(j,SignatureRole.CAPTAIN_HOME,ink,Float.POSITIVE_INFINITY,toss)}
    }
}
