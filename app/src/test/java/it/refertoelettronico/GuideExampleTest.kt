package it.refertoelettronico

import it.refertoelettronico.model.*
import it.refertoelettronico.ui.GuideExample
import org.junit.Assert.*
import org.junit.Test

class GuideExampleTest {
    private fun state(phase:Int)=GuideExample.journal(phase).let{MatchEngine.replay(it.config,it.events)}
    @Test fun receptionPointRotatesAndChangesServer() {
        assertEquals(0,state(1).current!!.server)
        val after=state(2).current!!
        assertEquals(listOf(0,1),after.score)
        assertEquals(1,after.server)
        assertEquals(listOf(0,1),after.rotations)
    }
    @Test fun timeoutSubstitutionAndLiberoShowValidSequentialStates() {
        assertEquals(1,state(3).current!!.timeouts.size)
        assertEquals(1,state(4).current!!.countSubs(1))
        assertEquals(7,state(4).current!!.court[1].first())
        assertEquals(12,state(5).current!!.liberos[1]!!.number)
        assertTrue(12 in state(5).current!!.visible(1))
        assertEquals(1,state(5).current!!.countSubs(1))
    }
    @Test fun undoRedoReconstructsFullCourtState() {
        assertNull(state(6).current!!.liberos[1])
        assertEquals(state(4),state(6))
        assertEquals(state(5),state(7))
        assertEquals(1,GuideExample.journal(6).redo.size)
    }
    @Test fun finalExampleIsCompletedAndIncludesDemonstratedOperations() {
        val journal=GuideExample.journal(10);val final=state(10)
        assertTrue(final.finished(journal.config))
        assertEquals(3,final.wins(0));assertEquals(0,final.wins(1))
        assertEquals(listOf(listOf(25,1),listOf(25,0),listOf(25,0)),final.sets.map{it.score})
        assertEquals(1,final.notes.size);assertEquals(1,final.sanctions.size)
        assertTrue(journal.events.any{it.kind==Kind.SUBSTITUTION})
        assertTrue(journal.events.any{it.kind==Kind.LIBERO_IN})
        assertEquals("TUTORIAL",journal.config.matchNumber)
        assertTrue(journal.config.initialObservations.contains("DATI FITTIZI"))
    }
}
