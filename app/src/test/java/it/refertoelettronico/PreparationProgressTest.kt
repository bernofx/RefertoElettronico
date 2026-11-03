package it.refertoelettronico

import it.refertoelettronico.model.*
import org.junit.Assert.*
import org.junit.Test

class PreparationProgressTest {
    @Test fun completeDemoShowsAllFourSectionsReady() {
        val steps=PreparationProgress.steps(Demo.config())
        assertEquals(4,steps.size);assertTrue(steps.all{it.complete})
    }
    @Test fun missingPlaceIsTheNextActionEvenWhenOtherSectionsAreComplete() {
        val steps=PreparationProgress.steps(Demo.config().copy(location=""))
        assertEquals(3,steps.count{it.complete})
        assertEquals(PreparationProgress.Issue("Località","location"),steps.first{!it.complete}.issues.first())
    }
    @Test fun attendanceAndNamesMustBeCheckedWhileUnusedHistoricalCandidatesAreOptional() {
        val original=Demo.config();val team=original.teams[0].copy(players=original.teams[0].players.take(5).map{it.copy(name="")})
        val steps=PreparationProgress.steps(original.copy(teams=original.teams.with(0,team)))
        assertFalse(steps[1].complete);assertTrue(steps[2].complete)
        assertEquals("roster-0",steps[1].issues.first().target)
    }
    @Test fun confirmedScorerProposalCompletesOfficialsBeforeProjectionIsSaved() {
        val original=Demo.config()
        val team=original.teams[0].copy(staffRoster=listOf(StaffCandidate("proposal",name="Segnapunti di prova",scope="SERVICE",assignment="SCORER",selected=true)))
        val config=original.copy(teams=original.teams.with(0,team),officials=original.officials.with(2,""))
        assertTrue(PreparationProgress.steps(config)[3].complete)
    }
    @Test fun unmappedSelectedStaffRoutesBackToTheTeamWhereItCanBeCorrected() {
        val original=Demo.config();val team=original.teams[0].copy(staffRoster=listOf(StaffCandidate("unknown",name="Persona di prova",selected=true)))
        val steps=PreparationProgress.steps(original.copy(teams=original.teams.with(0,team)))
        assertEquals("staff-0",steps[1].issues.first().target)
    }
    @Test fun invalidDateAndTimeAreNotMarkedComplete() {
        val step=PreparationProgress.steps(Demo.config().copy(date="31/02/2026",scheduledTime="29:00"))[0]
        assertEquals(listOf("date","time"),step.issues.map{it.target})
    }
}
