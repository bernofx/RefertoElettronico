package it.refertoelettronico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w360dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TeamChoiceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun commonPrefixNamesRemainFullyRenderedAndChoicesStayIndependentAtLargeFontSize() {
        val config=Demo.config().copy(teams=Demo.config().teams.mapIndexed{i,t->t.copy(name=if(i==0)"POLISPORTIVA INVERIGO" else "POLISPORTIVA SAN GIUSEPPE COMO",colorKey=if(i==0)"YELLOW" else "WHITE")})
        var view:android.view.View?=null
        compose.setContent {
            view=LocalView.current.rootView
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.3f)) {
                VolleyTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                    StartSetPanel(config,MatchEngine.initial(config),{})
                }}
            }
        }
        for(prefix in listOf("toss-serve","toss-left","lineup-team"))for(t in 0..1) {
            val layouts=mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("$prefix-$t-name",useUnmergedTree=true).performScrollTo().assertTextEquals(config.teams[t].name)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult){get->get(layouts)}
            assertTrue(layouts.isNotEmpty());assertFalse(layouts.single().hasVisualOverflow)
            assertTrue(layouts.single().lineCount>=2)
            compose.onNodeWithTag("$prefix-$t").performScrollTo().performClick().assertIsSelected()
            compose.onNodeWithTag("$prefix-${1-t}").assertIsNotSelected()
        }
        // Selecting an editor tab must not change either toss choice.
        compose.onNodeWithTag("toss-serve-1").assertIsSelected()
        compose.onNodeWithTag("toss-left-1").assertIsSelected()
        compose.onNodeWithTag("toss-serve-0").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("toss-left-1").assertIsSelected()
        compose.runOnIdle {
            val root=view!!
            val bitmap=android.graphics.Bitmap.createBitmap(root.width,root.height,android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bitmap))
            val dir=File("build/outputs/verification").apply{mkdirs()}
            File(dir,"ui-nomi-squadre-0.6.6.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        }
    }
}
