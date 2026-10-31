package it.refertoelettronico.model

class RuleViolation(message: String) : IllegalArgumentException(message)
private fun rule(condition: Boolean, message: String) { if (!condition) throw RuleViolation(message) }

/** Pure reducer. Undo removes events, then replays this same reducer; no score-only corrections. */
object MatchEngine {
    fun validate(config: MatchConfig) {
        rule(config.teams.size == 2, "Servono due squadre.")
        rule(config.teams.map { it.name.trim().lowercase() }.distinct().size == 2, "Le squadre devono avere nomi diversi.")
        config.teams.forEach { team ->
            team.roster?.let { catalog ->
                rule(catalog.size<=60 && catalog.map{it.id}.distinct().size==catalog.size && catalog.all{it.id.isNotBlank()},"Rosa salvata non valida.")
                rule(catalog.filter{it.present}.map{it.player}==team.players,"La distinta non corrisponde alle presenze selezionate.")
            }
            rule(team.name.isNotBlank(), "Inserisci il nome di entrambe le squadre.")
            rule(team.players.size in 6..14, "${team.name}: seleziona da 6 a 14 giocatrici presenti.")
            rule(team.players.all { it.number in 1..99 }, "${team.name}: il numero di maglia deve essere compreso tra 1 e 99.")
            rule(team.players.distinctBy { it.number }.size == team.players.size, "${team.name}: numeri di maglia duplicati.")
            rule(team.players.count { it.libero } <= 2, "Sono consentiti al massimo due liberi.")
            rule(team.players.count { !it.libero } >= 6, "${team.name}: servono almeno sei giocatrici non libero.")
            rule(team.players.count { it.captain } == 1, "${team.name}: indica una sola capitana.")
            rule(team.players.size <= 12 || team.players.any { it.libero }, "Con più di 12 giocatrici è richiesto almeno un libero.")
        }
        rule(config.maxSubstitutions in listOf(6, 8), "Limite sostituzioni non valido.")
        if(config.date.isNotBlank())rule(runCatching {
            java.time.LocalDate.parse(config.date,java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT))
        }.isSuccess,"Data non valida: usa gg/mm/aaaa.")
        if(config.scheduledTime.isNotBlank())rule(runCatching {
            java.time.LocalTime.parse(config.scheduledTime,java.time.format.DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT))
        }.isSuccess,"Ora non valida: usa hh:mm.")
    }
    fun initial(config: MatchConfig): MatchState {
        validate(config)
        return MatchState(designatedLiberos = config.teams.map { t -> t.players.filter { it.libero }.map { it.number } },
            captains = config.teams.map { t -> t.players.single { it.captain }.number })
    }
    fun replay(config: MatchConfig, events: List<Event>): MatchState = events.fold(initial(config)) { s, e -> apply(config, s, e) }
    fun add(journal: Journal, event: Event): Journal {
        apply(journal.config, replay(journal.config, journal.events), event)
        return journal.copy(events = journal.events + event, redo = emptyList())
    }
    fun undo(journal: Journal, count: Int = 1): Journal {
        rule(count in 1..journal.events.size, "Non ci sono operazioni da annullare.")
        return journal.copy(events = journal.events.dropLast(count), redo = journal.events.takeLast(count) + journal.redo)
    }
    fun redo(journal: Journal): Journal {
        rule(journal.redo.isNotEmpty(), "Non ci sono operazioni da ripristinare.")
        val e = journal.redo.first()
        apply(journal.config, replay(journal.config, journal.events), e)
        return journal.copy(events = journal.events + e, redo = journal.redo.drop(1))
    }
    private fun active(s: MatchState): SetState {
        rule(!s.endedEarly, "Gara terminata.")
        val set = s.current ?: throw RuleViolation("Inserisci prima le formazioni del set.")
        rule(set.endedAt == null, "Set terminato: inserisci le formazioni del prossimo set.")
        return set
    }
    private fun roster(config: MatchConfig, team: Int, number: Int): Player =
        config.teams[team].players.find { it.number == number } ?: throw RuleViolation("Giocatrice n. $number non in distinta.")
    private fun blocked(s: MatchState, team: Int, number: Int, set: SetState? = s.current): Boolean =
        number in s.disqualified[team] || number in s.retired[team] || number in (set?.expelled?.get(team) ?: emptyList())
    fun rallyProblem(s: MatchState): String? {
        val set = s.current ?: return "Scegli le formazioni iniziali."
        if (s.interrupted) return "Gara interrotta: registra la ripresa."
        for (t in 0..1) {
            if (set.visible(t).any { blocked(s, t, it) }) return "Sostituisci la giocatrice non più disponibile prima di riprendere."
            set.liberos[t]?.let { l ->
                val zone = set.court[t].indexOf(l.replaced) + 1
                if (zone in 2..4 || (zone == 1 && set.server == t)) return "Registra l'uscita del libero prima della prossima azione."
            }
        }
        return null
    }
    fun apply(config: MatchConfig, s: MatchState, e: Event): MatchState {
        if (e.kind in listOf(Kind.POINT, Kind.TIMEOUT, Kind.SUBSTITUTION, Kind.LIBERO_IN, Kind.LIBERO_OUT,
                Kind.SANCTION, Kind.EXCEPTIONAL_SUB, Kind.REDESIGNATE_LIBERO, Kind.CAPTAIN, Kind.UNAVAILABLE)) rule(e.team in 0..1, "Squadra non valida.")
        if (e.kind == Kind.START_SET) {
            rule(!s.finished(config), "La gara è già terminata.")
            rule(!s.interrupted, "Registra prima la ripresa della gara.")
            rule(s.current == null || s.current!!.endedAt != null, "Il set corrente non è terminato.")
            rule(e.lineups.size == 2 && e.lineups.all { it.size == 6 && it.distinct().size == 6 }, "Seleziona sei giocatrici distinte per squadra.")
            rule(e.server in 0..1 && e.left in 0..1, "Sorteggio non valido.")
            e.lineups.forEachIndexed { team, lineup -> lineup.forEach { n ->
                roster(config, team, n)
                rule(n !in s.designatedLiberos[team], "Il libero non può essere nella formazione iniziale.")
                rule(!blocked(s, team, n, null), "La giocatrice n. $n non è disponibile.")
            } }
            val n = s.sets.size + 1
            rule(n <= config.format.maxSets, "Numero massimo di set raggiunto.")
            val deciding = isDeciding(config, n)
            if (n > 1 && !deciding) {
                rule(e.server == 1 - s.sets.last().firstServer, "Il primo servizio si alterna tra i set.")
                rule(e.left == 1 - s.sets.last().initialLeft, "I lati del campo si scambiano tra i set.")
            }
            val set = SetState(n, e.lineups, e.lineups, e.server, e.left, e.at,
                turns = listOf(emptyList<Turn>(), emptyList()).with(e.server, listOf(Turn(0, e.lineups[e.server][0]))))
            return s.copy(sets = s.sets + set)
        }
        if (e.kind == Kind.UNAVAILABLE) {
            roster(config,e.team,e.player)
            rule(e.text.isNotBlank(), "Indica il motivo e la decisione dell'arbitro.")
            rule(!s.finished(config), "Gara già terminata.")
            return s.copy(retired=s.retired.with(e.team,(s.retired[e.team]+e.player).distinct()),
                notes=s.notes+Note(s.current?.number?:0,s.current?.score?:listOf(0,0),
                    "Sq. ${s.letter(e.team)} n. ${e.player} INABILE AL GIOCO: ${e.text}",e.at))
        }
        if (e.kind == Kind.NOTE || e.kind == Kind.CAPTAIN) {
            rule(e.text.isNotBlank() || e.kind == Kind.CAPTAIN, "Inserisci un'annotazione.")
            if (e.kind == Kind.CAPTAIN) {
                roster(config, e.team, e.player)
                rule(!blocked(s, e.team, e.player), "Giocatrice non disponibile.")
            }
            return s.copy(notes = s.notes + Note(s.current?.number ?: 0, s.current?.score ?: listOf(0,0),
                if (e.kind == Kind.CAPTAIN) "Sq. ${s.letter(e.team)} n. ${e.player} NUOVO K. ${e.text}" else e.text, e.at),
                captains = if (e.kind == Kind.CAPTAIN) s.captains.with(e.team, e.player) else s.captains)
        }
        if (e.kind == Kind.END_MATCH) {
            rule(!s.finished(config), "Gara già terminata.")
            rule(e.text.isNotBlank(), "Indica il motivo e la decisione dell'arbitro.")
            return s.copy(endedEarly = true, notes = s.notes + Note(s.current?.number ?: 0, s.current?.score ?: listOf(0,0), "GARA TERMINATA: ${e.text}", e.at))
        }
        if (e.kind == Kind.INTERRUPT || e.kind == Kind.RESUME) {
            rule(e.text.isNotBlank(), "Indica il motivo o la decisione dell'arbitro.")
            rule(s.interrupted == (e.kind == Kind.RESUME), if (e.kind == Kind.RESUME) "La gara non è interrotta." else "La gara è già interrotta.")
            return s.copy(interrupted = e.kind == Kind.INTERRUPT, notes = s.notes + Note(s.current?.number ?: 0,
                s.current?.score ?: listOf(0,0), (if (e.kind == Kind.INTERRUPT) "INIZIO INTERR.: " else "FINE INTERR.: ") + e.text, e.at))
        }
        var set = active(s)
        when (e.kind) {
            Kind.POINT -> {
                rule(rallyProblem(s) == null, rallyProblem(s) ?: "")
                set = point(config, set, e.team, e.at, false)
            }
            Kind.TIMEOUT -> {
                rule(!s.interrupted, "Gara interrotta.")
                rule(set.timeouts.count { it.team == e.team } < 2, "Entrambi i timeout sono già stati utilizzati.")
                set = set.copy(timeouts = set.timeouts + Timeout(e.team, set.score, set.liberos[e.team]?.number))
            }
            Kind.SUBSTITUTION, Kind.EXCEPTIONAL_SUB -> {
                val team = e.team
                rule(e.player != e.other, "Scegli due giocatrici diverse.")
                roster(config, team, e.other)
                val pos = set.court[team].indexOf(e.player)
                rule(pos >= 0 && set.liberos[team]?.replaced != e.player, "La giocatrice uscente deve essere in campo. Fai prima uscire il libero.")
                rule(e.other !in set.court[team] && e.other !in s.designatedLiberos[team] && !blocked(s, team, e.other), "Giocatrice entrante non disponibile.")
                val slot = (pos + set.rotations[team]) % 6
                val starter = set.initial[team][slot]
                val prior = set.changes.filter { it.team == team && it.slot == slot && !it.exceptional }
                val returning = e.other == starter
                if (e.kind == Kind.SUBSTITUTION) {
                    rule(set.countSubs(team) < config.maxSubstitutions, "Limite di ${config.maxSubstitutions} sostituzioni raggiunto.")
                    if (e.player == starter) {
                        rule(prior.isEmpty(), "La titolare è già uscita e rientrata in questo set.")
                        rule(e.other !in set.initial[team] && set.changes.none { it.team == team && (it.incoming == e.other || it.outgoing == e.other) }, "La riserva è già abbinata o è una titolare.")
                    } else {
                        rule(returning && prior.size == 1 && prior.single().incoming == e.player, "Può rientrare soltanto la titolare abbinata, una sola volta.")
                    }
                } else {
                    rule(e.text.isNotBlank(), "La sostituzione eccezionale richiede la decisione dell'arbitro e il motivo.")
                }
                set = set.copy(court = set.court.with(team, set.court[team].with(pos, e.other)),
                    changes = set.changes + Change(team, e.player, e.other, set.score, slot, returning, e.kind == Kind.EXCEPTIONAL_SUB, e.text))
                var next = s.copy(sets = s.sets.dropLast(1) + set)
                if (e.kind == Kind.EXCEPTIONAL_SUB) next = next.copy(retired = next.retired.with(team, next.retired[team] + e.player),
                    notes = next.notes + Note(set.number, set.score, "Sq. ${s.letter(team)} SOST. ECC. ${e.player} -> ${e.other}: ${e.text}", e.at))
                return next
            }
            Kind.LIBERO_IN -> {
                val team = e.team
                rule(e.player in s.designatedLiberos[team] && !blocked(s, team, e.player), "Libero non disponibile o non designato.")
                rule(e.player != set.liberos[team]?.number, "Il libero è già in campo.")
                val replaced = set.liberos[team]?.replaced ?: e.other
                val zone = set.court[team].indexOf(replaced) + 1
                rule(zone in listOf(1,5,6) && !(zone == 1 && set.server == team), "Il libero può rimpiazzare soltanto una giocatrice in seconda linea e non può servire.")
                rule(set.rallies > set.lastLiberoRally[team] || e.text.isNotBlank(), "Deve esserci un'azione completata fra due rimpiazzi del libero.")
                var log = set.liberoLog
                if (set.liberos[team] != null) {
                    val i = log.indexOfLast { it.team == team && !it.exited }
                    if (i >= 0) log = log.with(i, log[i].copy(libero = e.player, chain = log[i].chain + e.player))
                } else log = log + LiberoEntry(team, e.player, replaced, set.score)
                set = set.copy(liberos = set.liberos.with(team, ActiveLibero(e.player, replaced)),
                    lastLiberoRally = set.lastLiberoRally.with(team, set.rallies),
                    liberoLog = log)
                if (e.text.isNotBlank()) return s.copy(sets = s.sets.dropLast(1) + set,
                    notes = s.notes + Note(set.number, set.score, "Sq. ${s.letter(e.team)} INGRESSO LIBERO: ${e.text}", e.at))
            }
            Kind.LIBERO_OUT -> {
                rule(set.liberos[e.team] != null, "Nessun libero in campo.")
                rule(set.rallies > set.lastLiberoRally[e.team] || e.text.isNotBlank(), "Serve un'azione completata; per un rimpiazzo forzato indica la decisione dell'arbitro.")
                val i = set.liberoLog.indexOfLast { it.team == e.team && !it.exited }
                set = set.copy(liberos = set.liberos.with(e.team, null), lastLiberoRally = set.lastLiberoRally.with(e.team, set.rallies),
                    liberoLog = if (i >= 0) set.liberoLog.with(i, set.liberoLog[i].copy(exited = true)) else set.liberoLog)
                if (e.text.isNotBlank()) return s.copy(sets = s.sets.dropLast(1) + set,
                    notes = s.notes + Note(set.number, set.score, "Sq. ${s.letter(e.team)} USCITA LIBERO: ${e.text}", e.at))
            }
            Kind.REDESIGNATE_LIBERO -> {
                rule(e.text.isNotBlank(), "Indica dichiarazione di inabilità e decisione dell'arbitro.")
                rule(e.player in s.designatedLiberos[e.team], "Indica il libero dichiarato inabile.")
                roster(config, e.team, e.other)
                rule(e.other !in set.visible(e.team) && e.other !in s.designatedLiberos[e.team] && e.other != set.liberos[e.team]?.replaced && !blocked(s,e.team,e.other), "La ridesignata deve essere fuori campo e disponibile, diversa dalla rimpiazzata.")
                rule(s.designatedLiberos[e.team].filter { it != e.player }.all { blocked(s,e.team,it) }, "Con due liberi, prima deve risultare inabile anche l'altro libero.")
                rule(set.liberos[e.team] == null, "Registra prima l'uscita forzata del libero.")
                return s.copy(designatedLiberos = s.designatedLiberos.with(e.team, s.designatedLiberos[e.team].filter { it != e.player } + e.other),
                    retired = s.retired.with(e.team, s.retired[e.team] + e.player),
                    notes = s.notes + Note(set.number, set.score, "Sq. ${s.letter(e.team)} RIDESIGNAZIONE LIBERO ${e.player} -> ${e.other}: ${e.text}", e.at))
            }
            Kind.SANCTION -> {
                rule(e.text.isNotBlank(), "Indica il destinatario (numero di maglia o ruolo in panchina).")
                if (e.player > 0) roster(config, e.team, e.player)
                val sanction = Sanction(e.team, e.text, e.card, set.number, set.score, e.at)
                if (e.card == Card.EXPULSION && e.player > 0) set = set.copy(expelled = set.expelled.with(e.team, set.expelled[e.team] + e.player))
                val ds = if (e.card == Card.DISQUALIFICATION && e.player > 0) s.disqualified.with(e.team, s.disqualified[e.team] + e.player) else s.disqualified
                if (e.card.penalty) set = point(config, set, 1-e.team, e.at, true)
                return s.copy(sets = s.sets.dropLast(1) + set, sanctions = s.sanctions + sanction, disqualified = ds)
            }
            else -> throw RuleViolation("Operazione non supportata.")
        }
        return s.copy(sets = s.sets.dropLast(1) + set)
    }
    fun isDeciding(config: MatchConfig, number: Int) = config.format != Format.THREE_FIXED && number == config.format.maxSets
    private fun point(config: MatchConfig, before: SetState, team: Int, at: Long, penalty: Boolean): SetState {
        var set = before
        var turns = set.turns
        if (team != set.server) {
            val old = turns[set.server]
            rule(old.isNotEmpty() && old.last().endScore == null, "Registro dei turni non valido.")
            turns = turns.with(set.server, old.dropLast(1) + old.last().copy(endScore = set.score[set.server]))
            val rotated = set.court[team].drop(1) + set.court[team].first()
            val rotations = set.rotations.with(team, set.rotations[team] + 1)
            turns = turns.with(team, turns[team] + Turn(rotations[team] % 6, rotated[0], startAtTotal = set.score.sum() + 1))
            set = set.copy(court = set.court.with(team, rotated), rotations = rotations, server = team)
        }
        val score = set.score.with(team, set.score[team] + 1)
        val target = if (isDeciding(config, set.number)) 15 else 25
        val ended = score[team] >= target && score[team] - score[1-team] >= 2
        if (ended) {
            val ts = turns[team]
            turns = turns.with(team, ts.dropLast(1) + ts.last().copy(endScore = score[team], served = team == before.server))
        }
        return set.copy(score = score, turns = turns, endedAt = if (ended) at else null,
            rallies = set.rallies + if (penalty) 0 else 1,
            changedEndsAt = set.changedEndsAt ?: if (isDeciding(config, set.number) && score[team] == 8) score else null,
            penaltyPoints = if (penalty) set.penaltyPoints.with(team, set.penaltyPoints[team] + score[team]) else set.penaltyPoints)
    }
}

fun Event.description(config: MatchConfig): String {
    val name = config.teams.getOrNull(team)?.name ?: ""
    return when (kind) {
        Kind.START_SET -> "Inizio set - servizio ${config.teams[server].name}"
        Kind.POINT -> "+1 $name"
        Kind.TIMEOUT -> "Timeout $name"
        Kind.SUBSTITUTION -> "$name: $player esce, $other entra"
        Kind.EXCEPTIONAL_SUB -> "$name: cambio eccezionale $player -> $other · $text"
        Kind.LIBERO_IN -> "$name: libero $player entra per $other"
        Kind.LIBERO_OUT -> "$name: uscita libero"
        Kind.SANCTION -> "$name: ${card.label} · $text"
        Kind.REDESIGNATE_LIBERO -> "$name: ridesignazione libero $player -> $other · $text"
        Kind.CAPTAIN -> "$name: nuova capitana $player"
        Kind.UNAVAILABLE -> "$name: n. $player inabile al gioco · $text"
        Kind.INTERRUPT -> "Interruzione · $text"
        Kind.RESUME -> "Ripresa · $text"
        Kind.END_MATCH -> "Termine anticipato · $text"
        Kind.NOTE -> text
    }
}
