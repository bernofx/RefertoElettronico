package it.refertoelettronico

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
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
@Config(sdk=[35],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreparationUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun missingLocationCanBeReachedFromTheLastStepAndSavedWithoutLosingSignatures() {
        var journal by mutableStateOf(GuideExample.journal(11).let{it.copy(config=it.config.copy(location=""))})
        val signatures=journal.signatures
        var root:android.view.View?=null
        compose.setContent{root=LocalView.current.rootView;VolleyTheme{MatchEditor(journal.config,
            onSave={journal=Signatures.withConfig(journal,it)},rosterLocked=true)}}
        compose.onNodeWithTag("setup-progress").assertTextEquals("3/4 complete")
        compose.runOnIdle {
            val view=root!!;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
            val file=File("build/outputs/verification/preparazione-prossimo-passo-0.8.0.png");file.parentFile!!.mkdirs()
            file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
        compose.onNodeWithTag("setup-step-0").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Da completare"))
        compose.onNodeWithTag("setup-step-3").assertIsDisplayed().performClick()
        compose.onNodeWithTag("setup-suggestion").assertTextEquals("Località")
        compose.onNodeWithTag("setup-suggested-action").performClick()
        compose.onNodeWithTag("setup-field-location").assertIsFocused().performTextInput("Como")
        compose.onNodeWithTag("setup-progress").assertTextEquals("4/4 complete")
        compose.runOnIdle {
            val view=root!!;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
            val file=File("build/outputs/verification/preparazione-0.8.0.png");file.parentFile!!.mkdirs()
            file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
        compose.onNodeWithText("Salva modifiche").performClick()
        assertEquals("Como",journal.config.location);assertEquals(signatures,journal.signatures)
        (Signatures.required(journal.config,true)+Signatures.required(journal.config,false)).forEach {
            assertNotNull(Signatures.current(journal,it));assertFalse(Signatures.needsReview(journal,it))
        }
    }
    @Test fun secondaryFieldsAreCollapsedAndCanBeOpenedWhenNeeded() {
        compose.setContent{VolleyTheme{MatchEditor(Demo.config(),{},false)}}
        compose.onNodeWithText("Campionato",substring=false).assertDoesNotExist()
        compose.onNodeWithText("Motivo dell'uso del cartaceo").assertDoesNotExist()
        compose.onNodeWithTag("setup-advanced").performScrollTo().performClick()
        compose.onNodeWithText("Campionato",substring=false).assertExists()
        compose.onNodeWithTag("setup-notes").performScrollTo().performClick()
        compose.onNodeWithText("Motivo dell'uso del cartaceo").assertExists()
    }
    @Test fun nextActionNavigatesToTheIncompleteRosterWithoutBlockingDraftSave() {
        val original=Demo.config();var saved=false
        val config=original.copy(teams=original.teams.with(0,original.teams[0].copy(players=original.teams[0].players.map{if(it.number==2)it.copy(name="")else it})))
        compose.setContent{VolleyTheme{MatchEditor(config,{saved=true},false)}}
        compose.onNodeWithTag("setup-progress").assertTextEquals("3/4 complete")
        compose.onNodeWithTag("setup-next").performClick()
        compose.onNodeWithTag("setup-field-roster-0").assertIsDisplayed()
        compose.onNodeWithTag("setup-suggestion").assertTextEquals("Completa i nomi delle presenti")
        compose.onNodeWithText("Salva modifiche").performClick()
        assertTrue(saved)
    }
}
