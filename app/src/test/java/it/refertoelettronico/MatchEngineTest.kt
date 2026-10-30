package it.refertoelettronico

import it.refertoelettronico.model.*
import it.refertoelettronico.pdf.SheetLayout
import it.refertoelettronico.storage.MatchStore
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class MatchEngineTest {
    private val config=Demo.config()
    private val start=Demo.start(1_800_000_000_000)
    private fun state(vararg events:Event) = MatchEngine.replay(config,listOf(start)+events)
    private fun point(team:Int)=Event(Kind.POINT,team,at=start.at+1000)
    private fun rejects(block:()->Unit) {try{block();fail("Operazione irregolare accettata")}catch(_:RuleViolation){}}
    private fun finish(s:MatchState,team:Int):MatchState {
        var result=s
        while(result.current!!.endedAt==null)result=MatchEngine.apply(config,result,point(team))
        return result
    }
    @Test fun receiverWinningRallyRotatesClockwiseAndServes() {
        val s=state(point(1));assertEquals(listOf(0,1),s.current!!.score)
        assertEquals(listOf(2,3,4,5,6,1),s.current!!.court[1]);assertEquals(1,s.current!!.server)
        assertEquals(listOf(1,2,3,4,5,6),s.current!!.court[0]);assertEquals(1,s.current!!.turns[1].single().slot)
        assertEquals(0,s.current!!.turns[0].single().endScore)
    }
    @Test fun winningOnOwnServeDoesNotRotate() {
        val s=state(point(0),point(0));assertEquals(start.lineups,s.current!!.court);assertEquals(1,s.current!!.turns[0].size)
    }
    @Test fun sixChangesOfServeReturnToOriginalFormation() {
        var s=state()
        repeat(6) {s=MatchEngine.apply(config,s,point(1));s=MatchEngine.apply(config,s,point(0))}
        assertEquals(start.lineups,s.current!!.court);assertEquals(listOf(6,6),s.current!!.rotations)
    }
    @Test fun setNeedsTwoPointMargin() {
        var s=state()
        repeat(24){s=MatchEngine.apply(config,s,point(0));s=MatchEngine.apply(config,s,point(1))}
        s=MatchEngine.apply(config,s,point(0));assertNull(s.current!!.endedAt)
        s=MatchEngine.apply(config,s,point(1));s=MatchEngine.apply(config,s,point(0));assertNull(s.current!!.endedAt)
        s=MatchEngine.apply(config,s,point(0));assertEquals(listOf(27,25),s.current!!.score);assertEquals(0,s.current!!.winner)
        val ended=s;rejects{MatchEngine.apply(config,ended,point(1))}
    }
    @Test fun finalSetChangesEndsAtEightAndEndsAtFifteen() {
        var s=MatchEngine.initial(config)
        repeat(4){i ->
            s=MatchEngine.apply(config,s,start.copy(server=i%2,left=i%2))
            s=finish(s,i%2)
        }
        s=MatchEngine.apply(config,s,start.copy(server=1,left=1))
        repeat(8){s=MatchEngine.apply(config,s,point(1))}
        assertEquals(listOf(0,8),s.current!!.changedEndsAt)
        repeat(7){s=MatchEngine.apply(config,s,point(1))}
        assertEquals(listOf(0,15),s.current!!.score);assertTrue(s.finished(config));assertEquals(3,s.wins(1))
        val ended=s;rejects{MatchEngine.apply(config,ended,start)}
    }
    @Test fun nextSetAlternatesServiceAndCourtEnds() {
        val s=finish(state(),0)
        rejects{MatchEngine.apply(config,s,start)}
        val next=MatchEngine.apply(config,s,start.copy(server=1,left=1))
        assertEquals(2,next.current!!.number);assertEquals(listOf(0,0),next.current!!.score)
    }
    @Test fun matchEndsAtThreeSetsAndDoesNotRequireFifth() {
        var s=state()
        repeat(3){i ->
            if(i>0)s=MatchEngine.apply(config,s,start.copy(server=i%2,left=i%2))
            s=finish(s,0)
        }
        assertTrue(s.finished(config));assertEquals(3,s.sets.size)
    }
    @Test fun substitutionFollowsPairAcrossRotationsAndReturnsStarter() {
        var s=state(Event(Kind.SUBSTITUTION,0,1,7),point(1),point(0))
        assertEquals(listOf(2,3,4,5,6,7),s.current!!.court[0])
        s=MatchEngine.apply(config,s,Event(Kind.SUBSTITUTION,0,7,1))
        assertEquals(listOf(2,3,4,5,6,1),s.current!!.court[0]);assertEquals(2,s.current!!.countSubs(0))
        val after=s;rejects{MatchEngine.apply(config,after,Event(Kind.SUBSTITUTION,0,1,7))}
    }
    @Test fun substituteCanOnlyBeReplacedByPairedStarter() {
        val s=state(Event(Kind.SUBSTITUTION,0,1,7))
        rejects{MatchEngine.apply(config,s,Event(Kind.SUBSTITUTION,0,7,8))}
        rejects{MatchEngine.apply(config,s,Event(Kind.SUBSTITUTION,0,2,1))}
    }
    @Test fun eightChangesArePermittedButNinthRejected() {
        val twelve=config.copy(teams=config.teams.map{t ->t.copy(players=t.players.filter{!it.libero}+(9..12).map{Player(it,"Riserva $it")})})
        var s=MatchEngine.replay(twelve,listOf(start))
        for(i in 1..4){s=MatchEngine.apply(twelve,s,Event(Kind.SUBSTITUTION,0,i,i+6));s=MatchEngine.apply(twelve,s,Event(Kind.SUBSTITUTION,0,i+6,i))}
        assertEquals(8,s.current!!.countSubs(0));val after=s
        rejects{MatchEngine.apply(twelve,after,Event(Kind.SUBSTITUTION,0,5,11))}
    }
    @Test fun timeoutsArePerTeamAndResetEachSet() {
        val t=Event(Kind.TIMEOUT,0)
        val s=state(t,t,Event(Kind.TIMEOUT,1))
        rejects{MatchEngine.apply(config,s,t)};assertEquals(3,s.current!!.timeouts.size)
        val next=MatchEngine.apply(config,finish(s,0),start.copy(server=1,left=1))
        assertEquals(0,next.current!!.timeouts.size)
    }
    @Test fun liberoReplacementDoesNotConsumeSubstitutionAndRestoresOriginal() {
        var s=state(Event(Kind.LIBERO_IN,0,12,5));assertEquals(12,s.current!!.visible(0)[4]);assertEquals(0,s.current!!.countSubs(0))
        s=MatchEngine.apply(config,s,point(0));s=MatchEngine.apply(config,s,Event(Kind.LIBERO_OUT,0))
        assertEquals(5,s.current!!.visible(0)[4]);assertTrue(s.current!!.liberoLog.single().exited)
    }
    @Test fun liberoCannotEnterFrontRowOrServe() {
        rejects{state(Event(Kind.LIBERO_IN,0,12,2))}
        rejects{state(Event(Kind.LIBERO_IN,0,12,1))}
    }
    @Test fun liberoMustExitWhenRotationMovesIntoFrontRow() {
        var s=state(Event(Kind.LIBERO_IN,0,12,5),point(1),point(0))
        assertNotNull(MatchEngine.rallyProblem(s));val bad=s
        rejects{MatchEngine.apply(config,bad,point(0))}
        s=MatchEngine.apply(config,s,Event(Kind.LIBERO_OUT,0));assertNull(MatchEngine.rallyProblem(s))
    }
    @Test fun liberoRequiresCompletedRallyBetweenReplacements() {
        val s=state(Event(Kind.LIBERO_IN,0,12,5))
        rejects{MatchEngine.apply(config,s,Event(Kind.LIBERO_OUT,0))}
        val forced=MatchEngine.apply(config,s,Event(Kind.LIBERO_OUT,0,text="Infortunio, autorizzazione arbitro"))
        assertNull(forced.current!!.liberos[0]);assertTrue(forced.notes.isNotEmpty())
    }
    @Test fun penaltyGivesOpponentPointAndServiceButIsNotCompletedRally() {
        val s=state(Event(Kind.SANCTION,0,text="A",card=Card.PENALTY))
        assertEquals(listOf(0,1),s.current!!.score);assertEquals(1,s.current!!.server);assertEquals(0,s.current!!.rallies)
        assertEquals(listOf(1),s.current!!.penaltyPoints[1]);assertEquals(listOf(0,0),s.sanctions.single().score)
    }
    @Test fun expulsionPreventsPlayUntilPlayerIsReplacedAndExpiresNextSet() {
        var s=state(Event(Kind.SANCTION,0,1,text="1",card=Card.EXPULSION))
        val suspended=s;rejects{MatchEngine.apply(config,suspended,point(0))}
        s=MatchEngine.apply(config,s,Event(Kind.SUBSTITUTION,0,1,7));s=finish(s,0)
        s=MatchEngine.apply(config,s,start.copy(server=1,left=1));assertTrue(s.current!!.expelled.all{it.isEmpty()})
    }
    @Test fun disqualificationPersistsAcrossSets() {
        var s=state(Event(Kind.SANCTION,0,1,text="1",card=Card.DISQUALIFICATION))
        s=MatchEngine.apply(config,s,Event(Kind.SUBSTITUTION,0,1,7));s=finish(s,0)
        val after=s;rejects{MatchEngine.apply(config,after,start.copy(server=1,left=1))}
    }
    @Test fun interruptionBlocksScoreAndIsUndoable() {
        val s=state(Event(Kind.INTERRUPT,text="Blackout"));assertTrue(s.interrupted)
        rejects{MatchEngine.apply(config,s,point(0))}
        val resumed=MatchEngine.apply(config,s,Event(Kind.RESUME,text="Impianto ripristinato"));assertFalse(resumed.interrupted)
    }
    @Test fun multiStepUndoRestoresScoreCourtLiberoChangesAndTimeoutsExactly() {
        val base=Journal(config=config,events=listOf(start,Event(Kind.LIBERO_IN,0,12,6)))
        var j=base
        val events=listOf(point(1),point(0),Event(Kind.TIMEOUT,1),Event(Kind.SUBSTITUTION,0,2,7),Event(Kind.SANCTION,1,text="A",card=Card.PENALTY))
        events.forEach{j=MatchEngine.add(j,it)}
        val beforeUndo=MatchEngine.replay(config,j.events)
        j=MatchEngine.undo(j,events.size);assertEquals(base.events,j.events)
        assertEquals(MatchEngine.replay(config,base.events),MatchEngine.replay(config,j.events))
        repeat(events.size){j=MatchEngine.redo(j)}
        assertEquals(beforeUndo,MatchEngine.replay(config,j.events))
    }
    @Test fun undoAcrossSetBoundaryRestoresPreviousUnfinishedSet() {
        var j=Journal(config=config,events=listOf(start))
        repeat(25){j=MatchEngine.add(j,point(0))}
        j=MatchEngine.add(j,start.copy(server=1,left=1));j=MatchEngine.add(j,point(1))
        j=MatchEngine.undo(j,3)
        val s=MatchEngine.replay(config,j.events);assertEquals(1,s.sets.size);assertEquals(listOf(24,0),s.current!!.score);assertNull(s.current!!.endedAt)
    }
    @Test fun newActionAfterUndoDiscardsRedoBranch() {
        val j=MatchEngine.add(MatchEngine.undo(Journal(config=config,events=listOf(start,point(0)))),point(1))
        assertTrue(j.redo.isEmpty());assertEquals(listOf(0,1),MatchEngine.replay(config,j.events).current!!.score)
    }
    @Test fun invalidRostersAndLineupsAreRejected() {
        rejects{MatchEngine.validate(config.copy(teams=config.teams.with(0,config.teams[0].copy(players=config.teams[0].players+Player(1,"Duplicato")))))}
        rejects{MatchEngine.replay(config,listOf(start.copy(lineups=listOf(listOf(1,1,2,3,4,5),(1..6).toList()))))}
        rejects{MatchEngine.replay(config,listOf(start.copy(lineups=listOf(listOf(12,2,3,4,5,6),(1..6).toList()))))}
    }
    @Test fun backupRoundTripPreservesUndoAndMultipleMatches() {
        val dir=Files.createTempDirectory("referto-store").toFile()
        try {
            val store=MatchStore(dir);val first=MatchEngine.undo(Journal(config=config,events=listOf(start,point(1))))
            val id=store.create(first);assertEquals(first,store.load(id));assertEquals(first,store.decode(store.encode(first)))
            store.create(Journal(config=config.copy(matchNumber="altra")));assertEquals(2,store.list().size)
            store.save(id,MatchEngine.redo(first));assertEquals(listOf(0,1),MatchEngine.replay(config,store.load(id).events).current!!.score)
        } finally {dir.deleteRecursively()}
    }
    @Test fun unsupportedBackupVersionAndBrokenSequenceAreRejected() {
        val dir=Files.createTempDirectory("referto-store").toFile()
        try {
            val store=MatchStore(dir)
            try{store.decode(store.encode(Journal(version=99,config=config)));fail()}catch(_:IllegalArgumentException){}
            rejects{store.decode(store.encode(Journal(config=config,events=listOf(point(0)))))}
        } finally{dir.deleteRecursively()}
    }
    @Test fun exportIncludesAllEventsWithoutClippingHistoryOrLongObservations() {
        val long="Osservazione lunga ".repeat(500)
        val j=Journal(config=config,events=listOf(start,Event(Kind.NOTE,text=long),point(1)))
        val pages=SheetLayout.build(j);assertTrue(pages.first().template);assertTrue(pages.size>2)
        assertTrue(pages.drop(1).flatMap{it.marks}.any{it.text.contains("+1 Stella Como")})
        assertTrue(pages.all{it.marks.all{m->m.x>=0 && m.y>=0 && m.x<=1190 && m.y<=730}})
    }
    @Test fun liberoExchangeIsRecordedInOneCellUntilRegularPlayerReturns() {
        var s=state(Event(Kind.LIBERO_IN,0,12,6),point(0),Event(Kind.LIBERO_IN,0,13,6),point(0),Event(Kind.LIBERO_OUT,0))
        assertEquals(1,s.current!!.liberoLog.size)
        assertEquals(listOf(12,13),s.current!!.liberoLog.single().chain)
        assertTrue(s.current!!.liberoLog.single().exited)
    }
    @Test fun injuredLiberosCanBeRedesignatedOnlyAfterBothAreUnavailable() {
        var s=state(Event(Kind.UNAVAILABLE,0,12,text="Infortunio confermato dall'arbitro"))
        val one=s
        rejects{MatchEngine.apply(config,one,Event(Kind.REDESIGNATE_LIBERO,0,12,7,text="Richiesta allenatore"))}
        s=MatchEngine.apply(config,s,Event(Kind.UNAVAILABLE,0,13,text="Inabilità confermata dall'arbitro"))
        s=MatchEngine.apply(config,s,Event(Kind.REDESIGNATE_LIBERO,0,12,7,text="Autorizzazione arbitro"))
        assertTrue(7 in s.designatedLiberos[0]);assertTrue(12 in s.retired[0])
        s=MatchEngine.apply(config,s,Event(Kind.LIBERO_IN,0,7,6))
        assertEquals(7,s.current!!.visible(0)[5])
    }
    @Test fun finalPointWonInReceptionDoesNotMarkAnUnservedTurnAsServed() {
        var s=state()
        repeat(24){s=MatchEngine.apply(config,s,point(1))}
        s=MatchEngine.apply(config,s,point(0))
        s=MatchEngine.apply(config,s,point(1))
        assertNotNull(s.current!!.endedAt)
        assertFalse(s.current!!.turns[1].last().served)
        assertEquals(25,s.current!!.turns[1].last().endScore)
    }

    @Test fun invalidDatesAndTimesDoNotBecomeOfficialMetadata() {
        rejects{MatchEngine.validate(config.copy(date="31/02/2026"))}
        rejects{MatchEngine.validate(config.copy(scheduledTime="25:00"))}
        MatchEngine.validate(config.copy(date="29/02/2028",scheduledTime="23:59"))
    }

    @Test fun invalidRedoCannotOverwriteAnExistingSavedMatch() {
        val dir=Files.createTempDirectory("referto-store").toFile()
        try {
            val store=MatchStore(dir)
            val good=Journal(config=config,events=listOf(start))
            val id=store.create(good)
            val changed=config.copy(teams=config.teams.with(0,config.teams[0].copy(players=config.teams[0].players.map{if(it.number==7)it.copy(number=9)else it})))
            rejects{store.save(id,good.copy(config=changed,redo=listOf(Event(Kind.SUBSTITUTION,0,1,7))))}
            assertEquals(good,store.load(id))
        } finally{dir.deleteRecursively()}
    }

}
