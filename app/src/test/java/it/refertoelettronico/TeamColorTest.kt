package it.refertoelettronico

import it.refertoelettronico.model.*
import it.refertoelettronico.storage.MatchStore
import it.refertoelettronico.ui.teamInk
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class TeamColorTest {
    @Test fun versionOneBackupsWithoutColorsStillLoadWithContrastingDefaults() {
        val directory=Files.createTempDirectory("color-legacy").toFile()
        try {
            val store=MatchStore(directory)
            val old=Journal(config=Demo.config(),events=listOf(Demo.start()))
            val json=store.encode(old)
            assertFalse(json.contains("colorKey"))
            val loaded=store.decode(json)
            assertEquals(TeamColor.BLUE,TeamColor.of(loaded.config.teams[0],0))
            assertEquals(TeamColor.RED,TeamColor.of(loaded.config.teams[1],1))
            assertEquals(MatchEngine.replay(old.config,old.events),MatchEngine.replay(loaded.config,loaded.events))
        } finally{directory.deleteRecursively()}
    }
    @Test fun selectedColorsSurviveSavingAndDoNotAlterTheGameOrUndoBranch() {
        val directory=Files.createTempDirectory("color-match").toFile()
        try {
            val store=MatchStore(directory)
            val original=Journal(config=Demo.config(),events=listOf(Demo.start(),Event(Kind.POINT,1)))
            val j=MatchEngine.undo(original)
            val colored=j.copy(config=j.config.copy(teams=j.config.teams.mapIndexed{t,team->team.copy(colorKey=if(t==0)"WHITE" else "GREEN")}))
            val key=store.create(colored)
            val loaded=store.load(key)
            assertEquals(colored,loaded)
            assertEquals(TeamColor.WHITE,TeamColor.of(loaded.config.teams[0],0))
            assertEquals(TeamColor.GREEN,TeamColor.of(loaded.config.teams[1],1))
            assertEquals(MatchEngine.replay(original.config,original.events),MatchEngine.replay(loaded.config,MatchEngine.redo(loaded).events))
        } finally{directory.deleteRecursively()}
    }
    @Test fun everyPaletteColorHasReadableNumbersAndPointLabels() {
        TeamColor.entries.forEach { choice ->
            val color=Color(choice.argb)
            val ink=teamInk(color)
            val l1=color.luminance();val l2=ink.luminance()
            val ratio=(maxOf(l1,l2)+.05f)/(minOf(l1,l2)+.05f)
            assertTrue("${choice.label}: contrasto $ratio",ratio>=4.5f)
        }
    }
    @Test fun futureOrUnrecognizedColorKeysFallBackWithoutBreakingAnImport() {
        assertEquals(TeamColor.BLUE,TeamColor.of(Demo.config().teams[0].copy(colorKey="FUTURE_COLOR"),0))
    }
}
