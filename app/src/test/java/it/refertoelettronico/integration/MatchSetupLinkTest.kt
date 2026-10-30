package it.refertoelettronico.integration

import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class MatchSetupLinkTest {
    private val json = """{"schema":"it.refertoelettronico.match-setup","version":1,"config":{"matchNumber":"00123","date":"31/10/2026","scheduledTime":"15:30","venue":"Palestra – Cagnò","teams":[{"name":"Casa è"},{"name":"Ospiti"}],"format":"BEST_OF_FIVE","maxSubstitutions":8}}"""
    private fun link(value:String=json) = "refertovolley://import-match?data=" + Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

    @Test fun utf8SetupKeepsHomeAwayOrderAndLeadingZerosWithoutPlayers() {
        val config=MatchSetupLink.decode(link())
        assertEquals("00123",config.matchNumber)
        assertEquals("Casa è",config.teams[0].name)
        assertEquals("Ospiti",config.teams[1].name)
        assertEquals("Palestra – Cagnò",config.venue)
        assertTrue(config.teams.all{it.players.isEmpty()})
    }
    @Test fun rejectsOtherTargetsParametersFragmentsAndOversizedPayloads() {
        listOf(link().replace("refertovolley:","https:"),link().replace("import-match","other"),
            link()+"&data=second",link()+"#fragment",link().replace("?data=","/path?data="),
            "refertovolley://user@import-match?data=e30", "refertovolley://import-match:123?data=e30",
            "refertovolley://import-match?data="+"a".repeat(16_001), "refertovolley://import-match?data=%20")
            .forEach{address->assertThrows(RuntimeException::class.java){MatchSetupLink.decode(address)}}
    }
    @Test fun keepsTheExistingSchemaAndPreMatchValidation() {
        listOf(json.replace("\"version\":1","\"version\":2"),json.replace("\"config\":{","\"config\":{\"events\":[],"),"not json")
            .forEach{value->assertThrows(RuntimeException::class.java){MatchSetupLink.decode(link(value))}}
    }
}
