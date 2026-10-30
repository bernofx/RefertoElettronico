package it.refertoelettronico

import it.refertoelettronico.model.*
import it.refertoelettronico.ui.*
import org.junit.Assert.*
import org.junit.Test

class CourtGeometryTest {
    @Test fun formationsAreOppositeWithoutMirroringTheAthletesNumbers() {
        for(zone in 1..6) {
            val l=CourtGeometry.position(CourtEnd.LEFT,zone)
            val r=CourtGeometry.position(CourtEnd.RIGHT,zone)
            assertEquals(1f-l.x,r.x,.0001f);assertEquals(1f-l.y,r.y,.0001f)
        }
    }
    @Test fun serviceZonesAreOppositeCornersFromTheScorerTable() {
        val l=CourtGeometry.position(CourtEnd.LEFT,1);val r=CourtGeometry.position(CourtEnd.RIGHT,1)
        assertTrue(l.x<.25f && l.y>.7f);assertTrue(r.x>.75f && r.y<.3f)
    }
    @Test fun frontRowsFaceTheNetAndBackRowsFaceTheirBaseline() {
        for(zone in 2..4) {
            assertTrue(CourtGeometry.position(CourtEnd.LEFT,zone).x>.3f)
            assertTrue(CourtGeometry.position(CourtEnd.RIGHT,zone).x<.7f)
        }
        for(zone in listOf(1,5,6)) {
            assertTrue(CourtGeometry.position(CourtEnd.LEFT,zone).x<.2f)
            assertTrue(CourtGeometry.position(CourtEnd.RIGHT,zone).x>.8f)
        }
    }
    @Test fun lineupSelectorsUseTheSameTopToBottomAndLeftToRightOrderAsTheCourt() {
        for(end in CourtEnd.entries) {
            val rows=CourtGeometry.rows(end)
            assertEquals((1..6).toList(),rows.flatten().sorted())
            rows.forEach { row ->
                val a=CourtGeometry.position(end,row[0]);val b=CourtGeometry.position(end,row[1])
                assertTrue(a.x<b.x);assertEquals(a.y,b.y,.0001f)
            }
            assertTrue(CourtGeometry.position(end,rows.first()[0]).y<CourtGeometry.position(end,rows.last()[0]).y)
        }
    }
    @Test fun fifthSetChangeAndItsUndoSwapEndsWithoutChangingTeamIdentity() {
        val state=MatchEngine.replay(Demo.config(),listOf(Demo.start()))
        val before=state.current!!
        val changed=before.copy(changedEndsAt=listOf(8,6))
        assertEquals(0,CourtGeometry.leftTeam(before));assertEquals(1,CourtGeometry.leftTeam(changed))
        assertEquals(0,CourtGeometry.leftTeam(changed.copy(changedEndsAt=null)))
        assertEquals("A",state.letter(0));assertEquals("B",state.letter(1))
    }
    @Test fun rotationFollowsClockwisePerimeterOnBothEnds() {
        for(end in CourtEnd.entries) {
            // Jersey in zone 2 moves to zone 1, then to 6, 5, 4, 3, and back to 2.
            val route=listOf(2,1,6,5,4,3).map { CourtGeometry.position(end,it) }
            for(i in route.indices) {
                val a=route[i];val b=route[(i+1)%route.size]
                assertTrue(a.x==b.x || a.y==b.y)
                assertNotEquals(a,b)
            }
        }
    }
}
