package it.refertoelettronico

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
class SignatureUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun canDrawClearCancelAndPersistANamedPreliminarySignatureWithoutStartingMatch() {
        var journal by mutableStateOf(Journal(config=Demo.config()))
        compose.setContent{VolleyTheme{SignatureWorkflow(journal,true,Demo.start(),onUpdate={journal=it;true},onClose={})}}
        compose.onNodeWithTag("sign-CAPTAIN_HOME").performClick()
        compose.onNodeWithTag("signature-save").assertIsNotEnabled()
        fun draw()=compose.onNodeWithTag("signature-pad").performTouchInput {
            down(Offset(width*.2f,height*.5f));moveTo(Offset(width*.35f,height*.25f),100);moveTo(Offset(width*.5f,height*.7f),100);moveTo(Offset(width*.8f,height*.4f),100);up()
        }
        draw();compose.onNodeWithTag("signature-clear").performClick();compose.onNodeWithTag("signature-save").assertIsNotEnabled()
        draw();compose.onNodeWithTag("signature-cancel").performClick();assertTrue(journal.signatures.isNullOrEmpty())
        compose.onNodeWithTag("sign-CAPTAIN_HOME").performClick();draw();compose.onNodeWithTag("signature-save").assertIsEnabled().performClick()
        compose.waitForIdle()
        assertNotNull("Journal=$journal\n"+compose.onNodeWithText("Firme preliminari").onParent().onParent().printToString(),Signatures.current(journal,SignatureRole.CAPTAIN_HOME))
        assertEquals("Atleta 1-1",Signatures.current(journal,SignatureRole.CAPTAIN_HOME)!!.signer)
        assertTrue(journal.events.isEmpty());assertEquals(1,journal.signatures!!.size)
    }
    @Test fun firstSetPromptsForRequiredSignaturesAndPaperChoiceLeavesFieldsBlank() {
        var journal by mutableStateOf(Journal(config=Demo.config(),openingToss=OpeningToss(0,0)))
        compose.setContent{VolleyTheme{
            val state=remember(journal){MatchEngine.replay(journal.config,journal.events)}
            LiveScreen(journal.config,state,null,onEvent={journal=MatchEngine.add(journal,it)},onUndo={},onRedo={},canUndo=false,canRedo=false,onHistory={},journal=journal,onSignatures={journal=it;true})
        }}
        for(team in 0..1) {
            compose.onNodeWithTag("lineup-team-$team").performScrollTo().performClick()
            for(number in 1..6) {
                compose.onNodeWithTag("lineup-player-$team-$number").performScrollTo().performClick()
                compose.onNodeWithTag("lineup-slot-$team-$number").performScrollTo().performClick()
            }
        }
        compose.onNodeWithTag("start-set").performScrollTo().performClick()
        compose.onNodeWithText("Firme preliminari").assertExists()
        assertTrue(journal.events.isEmpty())
        compose.onNodeWithTag("signatures-paper-start").performClick()
        assertEquals(listOf(Kind.START_SET),journal.events.map{it.kind})
        assertTrue(journal.signatures.isNullOrEmpty())
    }
    @Test fun missingRefereeCanBeNamedInsideApprovalWithoutLosingTeamSignatures() {
        var journal by mutableStateOf(GuideExample.journal(10).let{it.copy(config=it.config.copy(officials=it.config.officials.with(0,"")))})
        val original=journal.signatures
        compose.setContent{VolleyTheme{SignatureWorkflow(journal,false,onUpdate={journal=it;true},onClose={})}}
        compose.onNodeWithTag("signature-official-name-FIRST_REFEREE").performScrollTo().performTextInput("Arbitra di prova")
        compose.onNodeWithTag("save-official-name-FIRST_REFEREE").performScrollTo().performClick()
        assertEquals("Arbitra di prova",journal.config.officials[0])
        assertEquals(original,journal.signatures)
        assertTrue(Signatures.missing(journal,true).isEmpty())
        compose.onNodeWithTag("sign-SCORER").performScrollTo().assertIsEnabled()
    }
    @Test fun legacyInkIsAvailableAfterTheMatchWithoutAnyRecoveryAction() {
        val original=LegacySignatureFixtures.preliminary()
        val journal=original.copy(events=GuideExample.journal(10).events,config=original.config.copy(location="Luogo completato"))
        compose.setContent{VolleyTheme{SignatureWorkflow(journal,false,onUpdate={true},onClose={})}}
        compose.onNodeWithTag("recover-preliminary-signatures").assertDoesNotExist()
        assertTrue(Signatures.missing(journal,true).isEmpty())
        assertEquals(original.signatures!!.map{it.strokes},journal.signatures!!.map{it.strokes})
    }
    @Test fun finalOrderIsEnforcedAndPreliminarySignaturesRemainUntouched() {
        var journal by mutableStateOf(GuideExample.journal(10))
        compose.setContent{VolleyTheme{SignatureWorkflow(journal,false,onUpdate={journal=it;true},onClose={})}}
        compose.onNodeWithTag("sign-FIRST_REFEREE").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("sign-SCORER").performScrollTo().assertIsEnabled()
        assertEquals(2,Signatures.missing(journal,false).size)
        assertEquals(4,journal.signatures!!.size)
    }
}
