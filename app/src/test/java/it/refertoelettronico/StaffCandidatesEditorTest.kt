package it.refertoelettronico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
class StaffCandidatesEditorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun unknownManualCandidateCanBeNamedMappedAndExplicitlyConfirmed() {
        var config by mutableStateOf(Demo.config().copy(teams=Demo.config().teams.mapIndexed{i,t->if(i==0)t.copy(staffRoster=listOf(StaffCandidate("local",roleCode="CUSTOM",roleLabel="Proposta sconosciuta")))else t}))
        compose.setContent{VolleyTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StaffCandidatesEditor(config,teamIndex=0,onChange={t,people->config=config.copy(teams=config.teams.mapIndexed{i,item->if(i==t)item.copy(staffRoster=people)else item})})
        }}}
        compose.onNodeWithText("Da verificare").assertExists()
        compose.onNodeWithTag("staff-presence-local").assertIsOff()
        compose.onNodeWithTag("staff-edit-local").performClick()
        compose.onNodeWithText("Nome incaricato").performScrollTo().performTextReplacement("Èva confermata")
        compose.onNode(hasText("Da mappare") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Allenatore",substring=false).performClick()
        compose.onNodeWithTag("staff-presence-local").performScrollTo().performClick()
        compose.runOnIdle {
            val person=config.teams[0].staffRoster!!.single()
            assertEquals("BENCH",person.scope);assertEquals("COACH",person.assignment);assertEquals("CUSTOM",person.roleCode)
            assertNull(person.sourceId);assertEquals("Èva confermata",StaffReview.confirm(config).teams[0].staff[0])
        }
    }
    @Test fun manualServiceEntryOnlyNeedsNameAndRoleAndIsImmediatelySelected() {
        var config by mutableStateOf(Demo.config())
        compose.setContent{VolleyTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StaffCandidatesEditor(config,serviceOnly=true,onChange={t,people->config=config.copy(teams=config.teams.with(t,config.teams[t].copy(staffRoster=people)))})
        }}}
        compose.onNodeWithTag("staff-add").performClick()
        compose.onNodeWithTag("staff-entry-confirm").assertIsNotEnabled()
        compose.onNodeWithText("Cognome e nome").performTextReplacement("  Persona di prova  ")
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle {
            val view=org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView
            val image=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.invalidate();view.draw(android.graphics.Canvas(image))
            val file=java.io.File("build/outputs/verification/staff-entry-0.9.3.png");file.parentFile!!.mkdirs()
            file.outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};image.recycle()
        }
        compose.onNodeWithTag("staff-entry-confirm").performClick()
        compose.runOnIdle {
            val person=config.teams[0].staffRoster!!.single()
            assertTrue(person.selected)
            assertEquals("SCORER",person.assignment)
            assertEquals("SERVICE",person.scope)
            assertEquals("Persona di prova",StaffReview.confirm(config).officials[2])
        }
    }
    @Test fun existingBenchNameCanBeEditedAndRemovedWithoutOtherRolesChanging() {
        var team by mutableStateOf(Demo.config().teams[0])
        compose.setContent{VolleyTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            TeamEditor(team,0,"2026/27",true,onChange={team=it})
        }}}
        compose.onNodeWithText("Panchina e codice società").performScrollTo().performClick()
        compose.onNodeWithText("Modifica",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Cognome e nome").performTextReplacement("Allenatrice corretta")
        compose.onNodeWithTag("staff-entry-confirm").performClick()
        compose.runOnIdle{assertEquals("Allenatrice corretta",team.staff[0])}
        compose.onNodeWithText("Modifica",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Rimuovi incaricato").performClick()
        compose.runOnIdle{assertEquals(List(5){""},team.staff)}
    }
    @Test fun twoServiceProposalsShareASingleGameAssignmentAndShowConflicts() {
        var config by mutableStateOf(Demo.config().copy(teams=Demo.config().teams.mapIndexed{i,t->t.copy(staffRoster=listOf(StaffCandidate("scorer-$i",name="Proposta $i",scope="SERVICE",assignment="SCORER")))}))
        compose.setContent{VolleyTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            StaffCandidatesEditor(config,serviceOnly=true,onChange={t,people->config=config.copy(teams=config.teams.mapIndexed{i,item->if(i==t)item.copy(staffRoster=people)else item})})
        }}}
        for(t in 0..1)compose.onNodeWithTag("staff-presence-scorer-$t").performScrollTo().performClick()
        compose.onAllNodesWithText("Scegli una sola persona per questo incarico.").assertCountEquals(2)
        compose.onNodeWithTag("staff-presence-scorer-0").performClick()
        compose.runOnIdle{assertEquals("Proposta 1",StaffReview.confirm(config).officials[2])}
    }
}
