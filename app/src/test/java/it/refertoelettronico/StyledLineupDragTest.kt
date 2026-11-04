package it.refertoelettronico

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import it.refertoelettronico.model.*
import it.refertoelettronico.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyledLineupDragTest {
    @get:Rule val compose=createComposeRule()
    @Test fun jerseyDragUsesTheRealZoneTargetsAndOutsideDropsKeepTheFormation() {
        val config=Demo.config()
        compose.setContent{VolleyTheme(Appearance(layout=InterfaceStyle.COURT,mode=ThemeMode.DARK)){Surface(Modifier.fillMaxSize()){
            StartSetPanel(config,MatchEngine.initial(config),{},fillViewport=true)
        }}}
        compose.onNodeWithTag("lineup-player-0-1").performScrollTo()
        val source=compose.onNodeWithTag("lineup-player-0-1").fetchSemanticsNode().boundsInRoot
        val target=compose.onNodeWithTag("lineup-slot-0-4").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("lineup-player-0-1").performTouchInput{down(center);advanceEventTime(700);moveTo(center+target.center-source.center,500);up()}
        compose.onNodeWithTag("lineup-slot-0-4").assertContentDescriptionEquals("Zona 4, numero 1")
        compose.onNodeWithTag("lineup-player-0-1").assertDoesNotExist()
        compose.onNodeWithTag("lineup-player-0-2").performTouchInput{down(center);advanceEventTime(700);moveTo(center+Offset(6f,5f),500);up()}
        compose.onNodeWithTag("lineup-slot-0-4").assertContentDescriptionEquals("Zona 4, numero 1")
        compose.onNodeWithTag("lineup-slot-0-3").assertContentDescriptionEquals("Zona 3, vuota")
    }
}
