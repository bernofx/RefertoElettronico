package it.refertoelettronico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*
import it.refertoelettronico.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LineupPanelTest {
    @get:Rule val compose=createComposeRule()
    private val config=Demo.config()
    private var rootView:android.view.View?=null
    private fun setup(state:MatchState=MatchEngine.initial(config),onStart:(Event)->Unit={}) {
        compose.setContent{rootView=LocalView.current.rootView;VolleyTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){StartSetPanel(config,state,onStart)}}}
    }
    private fun assign(team:Int,number:Int,zone:Int) {
        compose.onNodeWithTag("lineup-player-$team-$number").performScrollTo().performClick()
        compose.onNodeWithTag("lineup-slot-$team-$zone").performScrollTo().performClick()
    }
    @Test fun coachSheetIsOrderedIndependentlyOfTossAndStartsWithEmptyZones() {
        setup()
        fun bounds(z:Int)=compose.onNodeWithTag("lineup-slot-0-$z").fetchSemanticsNode().boundsInRoot
        assertTrue(bounds(4).left<bounds(3).left);assertTrue(bounds(3).left<bounds(2).left)
        assertTrue(bounds(5).left<bounds(6).left);assertTrue(bounds(6).left<bounds(1).left)
        assertTrue(bounds(4).top<bounds(5).top)
        compose.onNodeWithTag("lineup-slot-0-1").assertContentDescriptionEquals("Zona 1, vuota")
        compose.onNodeWithTag("start-set").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("toss-left-1").performScrollTo().performClick()
        assertTrue(bounds(4).left<bounds(3).left)
        assertTrue(bounds(3).left<bounds(2).left)
    }
    @Test fun tappingSupportsSwapsExcludesLiberosAndRequiresBothTossChoicesBeforeStarting() {
        var event:Event?=null
        setup(onStart={event=it})
        val libero=config.teams[0].players.first{it.libero}.number
        compose.onNodeWithTag("lineup-player-0-$libero").assertDoesNotExist()
        for(t in 0..1) {
            compose.onNodeWithTag("lineup-team-$t").performScrollTo().performClick()
            for(z in 1..6)assign(t,z,z)
        }
        compose.onNodeWithTag("lineup-team-0").performScrollTo().performClick()
        assign(0,1,2)
        compose.onNodeWithTag("lineup-slot-0-2").assertContentDescriptionEquals("Zona 2, numero 1")
        compose.onNodeWithTag("lineup-slot-0-1").assertContentDescriptionEquals("Zona 1, numero 2")
        compose.onNodeWithTag("start-set").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("toss-serve-1").performScrollTo().performClick()
        compose.onNodeWithTag("start-set").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("toss-left-0").performScrollTo().performClick()
        compose.onNodeWithTag("lineup-slot-0-4").performScrollTo()
        compose.runOnIdle {
            val view=rootView!!
            val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val folder=java.io.File("build/outputs/verification").apply{mkdirs()}
            java.io.File(folder,"ui-formazioni-0.6.4.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        }
        compose.onNodeWithTag("start-set").performScrollTo().assertIsEnabled().performClick()
        assertEquals(1,event!!.server);assertEquals(0,event!!.left)
        assertEquals(listOf(2,1,3,4,5,6),event!!.lineups[0])
        val state=MatchEngine.apply(config,MatchEngine.initial(config),event!!)
        assertEquals(event!!.lineups,state.current!!.court)
    }
    @Test fun aLongPressDragFromRosterDropsIntoTheCorrectZoneAndOutsideDropChangesNothing() {
        setup()
        compose.onNodeWithTag("lineup-player-0-1").performScrollTo()
        val source=compose.onNodeWithTag("lineup-player-0-1").fetchSemanticsNode().boundsInRoot
        val target=compose.onNodeWithTag("lineup-slot-0-4").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("lineup-player-0-1").performTouchInput {
            down(center);advanceEventTime(700);moveTo(center+target.center-source.center,500);up()
        }
        compose.onNodeWithTag("lineup-slot-0-4").assertContentDescriptionEquals("Zona 4, numero 1")
        compose.onNodeWithTag("lineup-player-0-2").performTouchInput {
            down(center);advanceEventTime(700);moveTo(center+Offset(15f,5f),500);up()
        }
        compose.onNodeWithTag("lineup-slot-0-4").assertContentDescriptionEquals("Zona 4, numero 1")
        compose.onNodeWithTag("lineup-slot-0-3").assertContentDescriptionEquals("Zona 3, vuota")
    }
    @Test fun nextSetAlternatesServiceAndSidesAutomatically() {
        var journal=Journal(config=config,events=listOf(Demo.start().copy(left=1,server=0)))
        repeat(25){journal=MatchEngine.add(journal,Event(Kind.POINT,0))}
        val state=MatchEngine.replay(config,journal.events)
        var event:Event?=null
        setup(state){event=it}
        compose.onNodeWithTag("toss-serve-0").assertDoesNotExist()
        for(t in 0..1) {
            compose.onNodeWithTag("lineup-team-$t").performScrollTo().performClick()
            compose.onNodeWithText("Copia formazione del set precedente").performScrollTo().performClick()
        }
        compose.onNodeWithTag("start-set").performScrollTo().performClick()
        assertEquals(1,event!!.server);assertEquals(0,event!!.left)
        assertEquals(2,MatchEngine.apply(config,state,event!!).current!!.number)
    }
    @Test fun tieBreakRequiresNewServiceAndSideChoices() {
        var journal=Journal(config=config)
        repeat(4){set->
            journal=MatchEngine.add(journal,Demo.start().copy(left=set%2,server=set%2))
            repeat(25){journal=MatchEngine.add(journal,Event(Kind.POINT,set%2))}
        }
        setup(MatchEngine.replay(config,journal.events))
        compose.onNodeWithText("Nuovo sorteggio · quinto set").assertExists()
        compose.onNodeWithTag("toss-serve-0").assertIsNotSelected()
        compose.onNodeWithTag("toss-left-0").assertIsNotSelected()
        compose.onNodeWithTag("start-set").performScrollTo().assertIsNotEnabled()
    }

}
