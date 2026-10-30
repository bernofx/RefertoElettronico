package it.refertoelettronico

import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import it.refertoelettronico.model.*
import it.refertoelettronico.ui.LiveScreen
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
class PointSidesTest {
    @get:Rule val compose=createComposeRule()
    private fun setup(journal:MutableState<Journal>) {
        compose.setContent { MaterialTheme {
            val j=journal.value
            LiveScreen(j.config,MatchEngine.replay(j.config,j.events),j.events.lastOrNull(),
                onEvent={journal.value=MatchEngine.add(journal.value,it)},
                onUndo={journal.value=MatchEngine.undo(journal.value)},onRedo={journal.value=MatchEngine.redo(journal.value)},
                canUndo=true,canRedo=j.redo.isNotEmpty(),onHistory={})
        } }
    }
    private fun assertLeft(team:Int) {
        val a=compose.onNodeWithTag("point-button-$team").fetchSemanticsNode().boundsInRoot
        val b=compose.onNodeWithTag("point-button-${1-team}").fetchSemanticsNode().boundsInRoot
        assertTrue("Il pulsante deve seguire il campo",a.left<b.left)
    }
    @Test fun secondSetAndUndoKeepButtonsWithTheTeamOnThatSide() {
        var initial=Journal(config=Demo.config(),events=listOf(Demo.start()))
        repeat(25){initial=MatchEngine.add(initial,Event(Kind.POINT,0))}
        initial=MatchEngine.add(initial,Demo.start().copy(left=1,server=1))
        val journal=mutableStateOf(initial)
        setup(journal)
        assertLeft(1)
        compose.onNodeWithTag("point-button-1").performClick()
        compose.onNodeWithTag("score-1").assertTextEquals("1")
        compose.onNodeWithTag("score-0").assertTextEquals("0")
        compose.onNodeWithText("↶ Annulla").performClick()
        assertLeft(1)
        compose.onNodeWithTag("score-1").assertTextEquals("0")
    }
    @Test fun fifthSetEighthPointAndUndoMoveButtonsToTheCorrectEnds() {
        var initial=Journal(config=Demo.config())
        repeat(4){set->
            initial=MatchEngine.add(initial,Demo.start().copy(left=set%2,server=set%2))
            repeat(25){initial=MatchEngine.add(initial,Event(Kind.POINT,set%2))}
        }
        initial=MatchEngine.add(initial,Demo.start().copy(left=1,server=0))
        repeat(7){initial=MatchEngine.add(initial,Event(Kind.POINT,0))}
        val journal=mutableStateOf(initial)
        setup(journal)
        assertLeft(1)
        compose.onNodeWithTag("point-button-0").performClick()
        assertLeft(0)
        compose.onNodeWithTag("score-0").assertTextEquals("8")
        compose.onNodeWithText("↶ Annulla").performClick()
        assertLeft(1)
        compose.onNodeWithTag("score-0").assertTextEquals("7")
        compose.onNodeWithText("Ripristina").performClick()
        assertLeft(0)
    }
}
