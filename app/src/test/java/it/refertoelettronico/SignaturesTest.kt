package it.refertoelettronico

import it.refertoelettronico.model.*
import it.refertoelettronico.storage.MatchStore
import it.refertoelettronico.ui.GuideExample
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class SignaturesTest {
    private val ink=listOf(listOf(SignaturePoint(.1f,.5f),SignaturePoint(.3f,.1f),SignaturePoint(.45f,.8f),SignaturePoint(.9f,.3f)))
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
    @Test fun preliminarySignaturesSurviveRalliesButNotRosterOrTossChanges() {
        val j=pre();assertTrue(Signatures.missing(j,true).isEmpty())
        val started=MatchEngine.add(j,toss)
        val point=MatchEngine.add(started,Event(Kind.POINT,team=1))
        assertTrue(Signatures.missing(point,true).isEmpty())
        assertEquals(4,Signatures.missing(j,true,toss.copy(left=0)).size)
        val changed=j.copy(config=j.config.copy(teams=j.config.teams.with(0,j.config.teams[0].copy(name="Altra squadra"))))
        assertEquals(2,Signatures.missing(changed,true).size)
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(started,SignatureRole.CAPTAIN_HOME,ink,3f,toss)}
    }
    @Test fun displayColourAndLibraryCandidatesDoNotInvalidateSignedData() {
        val j=pre();val changed=j.copy(config=j.config.copy(teams=j.config.teams.map{it.copy(colorKey="white",historicalRoster=true,roster=listOf(RosterMember("candidate",Player(99,"Assente"),false)))}))
        assertTrue(Signatures.missing(changed,true).isEmpty())
    }
    @Test fun finalSignaturesRequireFinishedMatchAndScorerSecondFirstOrder() {
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(pre(),SignatureRole.SCORER,ink,3f)}
        val j=finished()
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(j,SignatureRole.FIRST_REFEREE,ink,3f)}
        val withScorer=Signatures.capture(j,SignatureRole.SCORER,ink,3f)
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(withScorer,SignatureRole.FIRST_REFEREE,ink,3f)}
        assertTrue(Signatures.missing(signed(),false).isEmpty())
    }
    @Test fun resultCorrectionOrPriorSignatureRemovalInvalidatesApproval() {
        val j=signed()
        assertEquals(3,Signatures.missing(MatchEngine.undo(j),false).size)
        assertEquals(3,Signatures.missing(MatchEngine.add(j,Event(Kind.NOTE,text="Correzione comunicata dall’arbitro")),false).size)
        assertEquals(3,Signatures.missing(Signatures.remove(j,SignatureRole.SCORER),false).size)
        assertEquals(3,Signatures.missing(Signatures.remove(j,SignatureRole.CAPTAIN_HOME),false).size)
        assertTrue(Signatures.missing(j,true).isEmpty())
    }
    @Test fun changedPreliminaryTossInvalidatesOtherSignaturesBeforeFirstSet() {
        val j=pre();val changed=Signatures.capture(j,SignatureRole.CAPTAIN_HOME,ink,3f,toss.copy(left=0))
        assertNotNull(Signatures.current(changed,SignatureRole.CAPTAIN_HOME))
        assertEquals(3,Signatures.missing(changed,true).size)
    }
    @Test fun archivesPreserveInkAndLegacyArchivesWithoutSignaturesStillLoad() {
        val store=MatchStore(Files.createTempDirectory("signed-match").toFile());val j=signed()
        val id=store.create(j);assertEquals(j,store.load(id));assertEquals(j,store.decode(store.encode(j)))
        val old=store.decode(store.encode(Journal(config=Demo.config())))
        assertTrue(old.signatures.isNullOrEmpty());assertEquals(4,Signatures.missing(old,true).size)
    }
    @Test fun addingRefereeAfterTheMatchPreservesPreliminarySignaturesAndAllowsApproval() {
        var j=Journal(config=Demo.config().copy(officials=Demo.config().officials.with(0,"")))
        Signatures.required(j.config,true).forEach{j=Signatures.capture(j,it,ink,3f,Demo.start())}
        j=j.copy(events=GuideExample.journal(10).events)
        val original=j.signatures
        j=Signatures.withConfig(j,j.config.copy(officials=j.config.officials.with(0,"Arbitra di prova"),officialCities=j.config.officialCities.with(0,"Como")))
        assertEquals(original,j.signatures);assertTrue(Signatures.missing(j,true).isEmpty())
        Signatures.required(j.config,false).forEach{j=Signatures.capture(j,it,ink,3f)}
        assertTrue(Signatures.missing(j,false).isEmpty())
    }
    @Test fun officialAndAdministrativeNotesDoNotAffectPreliminaryTeamApproval() {
        val j=pre()
        val changed=j.copy(config=j.config.copy(officials=j.config.officials.with(0,"Nuovo arbitro"),
            officialCities=List(7){"Città aggiornata"},refereeAttendant="Addetto di prova",
            initialObservations="Nota amministrativa",fallbackReason="Portale indisponibile"))
        assertTrue(Signatures.missing(changed,true).isEmpty())
        val roster=changed.config.teams[0].copy(players=changed.config.teams[0].players.map{if(it.number==2)it.copy(name="Nome corretto")else it})
        assertEquals(listOf(SignatureRole.CAPTAIN_HOME,SignatureRole.COACH_HOME),
            Signatures.missing(changed.copy(config=changed.config.copy(teams=changed.config.teams.with(0,roster))),true))
    }
    @Test fun validLegacySignaturesUpgradeBeforeUnrelatedOfficialEdit() {
        val j=LegacySignatureFixtures.preliminary().copy(events=GuideExample.journal(10).events)
        assertTrue(Signatures.missing(j,true).isEmpty())
        val next=Signatures.withConfig(j,j.config.copy(officials=j.config.officials.with(0,"Arbitra di prova")))
        assertTrue(Signatures.missing(next,true).isEmpty())
        assertTrue(next.signatures!!.all{it.bindingVersion==2})
        assertEquals(j.signatures!!.map{it.signedAt to it.strokes},next.signatures!!.map{it.signedAt to it.strokes})
    }
    @Test fun existingInvalidLegacySignaturesRecoverOnlyAfterOriginalHashVerification() {
        val original=LegacySignatureFixtures.preliminary()
        val broken=original.copy(events=GuideExample.journal(10).events,config=original.config.copy(
            officials=original.config.officials.with(0,"Arbitra di prova"),officialCities=original.config.officialCities.with(0,"Como")))
        assertEquals(4,Signatures.missing(broken,true).size)
        assertTrue(Signatures.hasLegacyPreliminariesToRecover(broken))
        val recovered=Signatures.recoverLegacyPreliminaries(broken)
        assertTrue(Signatures.missing(recovered,true).isEmpty())
        assertEquals(original.signatures!!.map{it.signer to it.signedAt},recovered.signatures!!.map{it.signer to it.signedAt})
        assertEquals(original.signatures!!.map{it.strokes},recovered.signatures!!.map{it.strokes})
        val store=MatchStore(Files.createTempDirectory("recovered-signatures").toFile())
        assertEquals(recovered,store.decode(store.encode(recovered)))
        assertEquals(4,it.refertoelettronico.pdf.SheetLayout.build(recovered).first().marks.count{it.type=="signature"})
    }
    @Test fun legacyRecoveryDoesNotRevalidateChangedTeamsOrToss() {
        val j=LegacySignatureFixtures.preliminary()
        val changed=j.copy(config=j.config.copy(officials=j.config.officials.with(0,"Arbitra di prova"),
            teams=j.config.teams.with(0,j.config.teams[0].copy(name="Squadra modificata"))))
        assertEquals(changed,Signatures.recoverLegacyPreliminaries(changed))
        val toss=changed.copy(config=j.config.copy(officials=j.config.officials.with(0,"Arbitra di prova")),events=listOf(Demo.start().copy(left=1)))
        assertEquals(toss,Signatures.recoverLegacyPreliminaries(toss))
    }
    @Test fun unchangedLegacyApprovalsAndColourOnlyEditsRemainValidWithoutMigration() {
        var j=LegacySignatureFixtures.preliminary(Demo.config()).copy(events=GuideExample.journal(10).events)
        Signatures.required(j.config,false).forEach{j=Signatures.capture(j,it,ink,3f)}
        val store=MatchStore(Files.createTempDirectory("legacy-approvals").toFile())
        val decoded=store.decode(store.encode(j))
        assertTrue(Signatures.missing(decoded,false).isEmpty())
        val next=Signatures.withConfig(decoded,decoded.config.copy(teams=decoded.config.teams.map{it.copy(colorKey="white")}))
        assertEquals(j.signatures,next.signatures)
        assertTrue(Signatures.missing(next,false).isEmpty())
    }
    @Test fun rejectEmptyDotsOversizedAndNonFiniteInk() {
        val j=Journal(config=Demo.config())
        listOf(emptyList(),listOf(listOf(SignaturePoint(.1f,.1f))),listOf(List(4001){SignaturePoint(.1f,.2f)}),listOf(listOf(SignaturePoint(Float.NaN,0f),SignaturePoint(1f,1f)))).forEach { strokes->
            assertThrows(IllegalArgumentException::class.java){Signatures.capture(j,SignatureRole.CAPTAIN_HOME,strokes,3f,toss)}
        }
        assertThrows(IllegalArgumentException::class.java){Signatures.capture(j,SignatureRole.CAPTAIN_HOME,ink,Float.POSITIVE_INFINITY,toss)}
    }
}
