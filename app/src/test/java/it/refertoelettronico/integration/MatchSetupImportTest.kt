package it.refertoelettronico.integration

import it.refertoelettronico.model.*
import org.junit.Assert.*
import org.junit.Test

class MatchSetupImportTest {
    private fun envelope(config:String)="""{"schema":"${MatchSetupImport.SCHEMA}","version":1,"config":$config}"""
    @Test fun onlyMatchAndTeamDataCanBeImportedWithoutInventingAListOfPlayers() {
        val config=MatchSetupImport.decode(envelope("""{"matchNumber":"123","date":"31/10/2026","teams":[{"name":"Casa"},{"name":"Ospite"}]}"""))
        assertEquals("123",config.matchNumber)
        assertEquals("Casa",config.teams[0].name)
        assertTrue(config.teams.all{it.players.isEmpty()})
        assertEquals(List(7){""},config.officials)
        assertEquals(Format.BEST_OF_FIVE,config.format)
        assertThrows(IllegalArgumentException::class.java){MatchEngine.validate(config)}
    }
    @Test fun numbersOnlyAllowScoringButAreReportedAsMissingOnThePaperSheet() {
        val config=Demo.config().copy(teams=Demo.config().teams.map { team ->team.copy(players=team.players.map{it.copy(name="")}) })
        MatchEngine.validate(config)
        assertEquals(2,config.missingMetadata().count{it.startsWith("cognome e nome")})
        val state=MatchEngine.replay(config,listOf(Demo.start(),Event(Kind.POINT,1)))
        assertEquals(listOf(0,1),state.current!!.score)
        val completed=config.copy(teams=Demo.config().teams)
        assertFalse(completed.missingMetadata().any{it.startsWith("cognome e nome")})
    }
    @Test fun completeSetupPreservesRolesColorsAndCompetition() {
        val json=com.google.gson.Gson().toJson(Demo.config().copy(teams=Demo.config().teams.map{it.copy(colorKey="GREEN")}))
        val config=MatchSetupImport.decode(envelope(json))
        MatchEngine.validate(config)
        assertEquals(TeamColor.GREEN,TeamColor.of(config.teams[0],0))
        assertEquals(2,config.teams[0].players.count{it.libero})
        assertEquals(7,config.officials.size)
    }
    @Test fun malformedVersionsAndWrongTypesAreRejected() {
        val minimal=envelope("""{"teams":[{},{}]}""")
        listOf(minimal.replace("\"version\":1","\"version\":2"),
            minimal.replace(MatchSetupImport.SCHEMA,"other"),
            envelope("""{"teams":[{"players":[{"number":"7"}]},{}]}"""),
            envelope("""{"teams":[{"players":[{"libero":"false"}]},{}]}"""),
            envelope("""{"teams":[{},{}],"officials":[null]}"""),
            envelope("""{"teams":[{}]}"""),
            envelope("""{"teams":[{},{}],"format":"UNKNOWN"}"""),
            envelope("""{"teams":[{},{}],"events":[]}"""),
            envelope("""{"teams":[{},{}],"maxSubstitutions":999}"""))
            .forEach{json->assertThrows(RuntimeException::class.java){MatchSetupImport.decode(json)}}
    }
    @Test fun streamSizeIsLimitedBeforeJsonParsing() {
        val input=java.io.ByteArrayInputStream(ByteArray(MatchSetupImport.MAX_BYTES+1){32})
        assertThrows(IllegalArgumentException::class.java){MatchSetupImport.read(input)}
    }
}
