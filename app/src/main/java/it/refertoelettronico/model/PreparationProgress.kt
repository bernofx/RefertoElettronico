package it.refertoelettronico.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Completion guides data entry; saving an incomplete but playable draft remains possible. */
object PreparationProgress {
    data class Issue(val label:String,val target:String)
    data class Step(val index:Int,val title:String,val summary:String,val issues:List<Issue>) {
        val complete get()=issues.isEmpty()
    }
    fun steps(config:MatchConfig):List<Step> {
        val final=runCatching{StaffReview.confirm(config)}.getOrDefault(config)
        val staffErrors=StaffReview.issues(config)
        return buildList {
            val game=buildList {
                if(config.matchNumber.isBlank())add(Issue("Numero gara","match-number"))
                if(config.date.isBlank() || runCatching{LocalDate.parse(config.date,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))}.isFailure)add(Issue("Data valida (gg/mm/aaaa)","date"))
                if(config.scheduledTime.isBlank() || runCatching{LocalTime.parse(config.scheduledTime,DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT))}.isFailure)add(Issue("Ora prevista valida","time"))
                if(config.location.isBlank())add(Issue("Località","location"))
                if(config.venue.isBlank())add(Issue("Palestra / campo","venue"))
                if(config.competition.isBlank())add(Issue("Campionato","competition"))
                if(config.season.isBlank())add(Issue("Stagione","season"))
            }
            add(Step(0,"Gara",if(game.isEmpty())"Dati principali completi"else"${game.size} dati da completare",game))
            config.teams.forEachIndexed { teamIndex,team->
                val missing=buildList {
                    if(team.name.isBlank())add(Issue("Nome squadra","team-name-$teamIndex"))
                    if(team.players.count{!it.libero}<6)add(Issue("Seleziona almeno 6 atlete di ruolo","roster-$teamIndex"))
                    if(team.players.any{it.name.isBlank()})add(Issue("Completa i nomi delle presenti","roster-$teamIndex"))
                    if(team.players.count{it.captain}!=1)add(Issue("Indica una capitana","roster-$teamIndex"))
                    if(team.players.size>RosterReview.MATCH_LIMIT || team.players.any{it.number !in 1..99} ||
                        team.players.map{it.number}.distinct().size!=team.players.size || team.players.count{it.libero}>2 || RosterReview.issues(team).isNotEmpty())
                        add(Issue("Controlla numeri e ruoli","roster-$teamIndex"))
                    if(team.staffRoster.orEmpty().any{it.selected && it.scope!="SERVICE" && it.id in staffErrors})add(Issue("Verifica lo staff","staff-$teamIndex"))
                }
                add(Step(teamIndex+1,if(teamIndex==0)"Squadra di casa"else"Squadra ospite",
                    "${team.players.size} presenti · ${team.players.count{it.libero}} liberi",missing))
            }
            val officials=buildList {
                if(staffErrors.any{(id,_)->config.teams.flatMap{it.staffRoster.orEmpty()}.any{it.id==id && it.scope=="SERVICE"}})
                    add(Issue("Verifica gli incarichi selezionati","service"))
                if(final.officials.getOrElse(0){""}.isBlank())add(Issue("Nome del 1° arbitro","official-0"))
                if(final.officials.getOrElse(2){""}.isBlank()) {
                    val proposals=config.teams.flatMap{it.staffRoster.orEmpty()}.any{it.scope=="SERVICE" && it.assignment=="SCORER"}
                    add(if(proposals)Issue("Scegli il segnapunti dagli incarichi ricevuti","service")else Issue("Nome del segnapunti","official-2"))
                }
            }
            add(Step(3,"Ufficiali",if(officials.isEmpty())"Arbitro e segnapunti indicati"else"${officials.size} dati da completare",officials))
        }
    }
}
