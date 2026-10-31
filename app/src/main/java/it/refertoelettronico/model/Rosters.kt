package it.refertoelettronico.model

import java.util.UUID

object Rosters {
    fun members(team:Team):List<RosterMember> = team.roster ?: team.players.map{RosterMember(UUID.randomUUID().toString(),it)}
    fun withMembers(team:Team,members:List<RosterMember>):Team = team.copy(players=members.filter{it.present}.map{it.player},roster=members)
    fun fromTemplate(team:Team):Team {
        val clean=StaffReview.prepare(MatchConfig(teams=listOf(team))).teams.single()
        return withMembers(clean.copy(roster=null,staffRoster=clean.staffRoster?.map{it.copy(id=UUID.randomUUID().toString(),selected=false)}),team.players.map{RosterMember(UUID.randomUUID().toString(),it,false)})
    }
    fun template(team:Team):Team = team.copy(players=(team.roster?.map{it.player} ?: team.players),roster=null)
    fun importedPlayers(team:Team,players:List<Player>):Team = withMembers(team,players.map{RosterMember(UUID.randomUUID().toString(),it)})
}
