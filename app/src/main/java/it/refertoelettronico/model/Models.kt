package it.refertoelettronico.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Positions are indexed 0..5 for zones 1..6. A team identity never changes with court ends. */
data class Player(val number: Int, val name: String, val libero: Boolean = false, val captain: Boolean = false)
data class RosterMember(val id:String,val player:Player,val present:Boolean=true)
data class Team(val name: String, val players: List<Player>, val staff: List<String> = List(5) { "" }, val clubCode: String = "", val colorKey: String? = null, val roster:List<RosterMember>?=null, val staffRoster:List<StaffCandidate>?=null, val historicalRoster:Boolean?=null)
enum class Format(val label: String, val maxSets: Int, val wins: Int) {
    BEST_OF_FIVE("Al meglio di 5 set", 5, 3), BEST_OF_THREE("Al meglio di 3 set", 3, 2),
    THREE_FIXED("3 set obbligatori a 25", 3, 3)
}
data class MatchConfig(
    val teams: List<Team>, val competition: String = "U14 Femminile - FIPAV Como",
    val season: String = "2026/27", val matchNumber: String = "", val phase: String = "",
    val location: String = "", val venue: String = "", val date: String = "", val scheduledTime: String = "",
    val officials: List<String> = List(7) { "" }, val officialCities: List<String> = List(7) { "" },
    val format: Format = Format.BEST_OF_FIVE, val maxSubstitutions: Int = 8,
    val initialObservations: String = "", val fallbackReason: String = "", val refereeAttendant:String? = null
)
enum class Kind { START_SET, POINT, TIMEOUT, SUBSTITUTION, LIBERO_IN, LIBERO_OUT, NOTE, SANCTION,
    EXCEPTIONAL_SUB, REDESIGNATE_LIBERO, CAPTAIN, INTERRUPT, RESUME, END_MATCH, UNAVAILABLE }
enum class Card(val label: String, val penalty: Boolean = false) {
    WARNING("Avvertimento (giallo)"), PENALTY("Penalizzazione (rosso)", true),
    EXPULSION("Espulsione (giallo/rosso insieme)"), DISQUALIFICATION("Squalifica (giallo/rosso separati)"),
    DELAY_WARNING("Avvertimento per ritardo"), DELAY_PENALTY("Penalizzazione per ritardo", true),
    IMPROPER("Richiesta impropria")
}
data class Event(
    val kind: Kind, val team: Int = -1, val player: Int = 0, val other: Int = 0,
    val text: String = "", val card: Card = Card.WARNING,
    val lineups: List<List<Int>> = emptyList(), val server: Int = 0, val left: Int = 0,
    val at: Long = System.currentTimeMillis()
)
data class Turn(val slot: Int, val player: Int, val endScore: Int? = null, val served: Boolean = true, val startAtTotal: Int = 0)
data class Change(val team: Int, val outgoing: Int, val incoming: Int, val score: List<Int>,
    val slot: Int, val returnOfStarter: Boolean, val exceptional: Boolean = false, val reason: String = "")
data class Timeout(val team: Int, val score: List<Int>, val libero: Int?)
data class LiberoEntry(val team: Int, val libero: Int, val replaced: Int, val score: List<Int>, val exited: Boolean = false, val chain: List<Int> = listOf(libero))
data class ActiveLibero(val number: Int, val replaced: Int)
data class Sanction(val team: Int, val recipient: String, val card: Card, val set: Int, val score: List<Int>, val at: Long)
data class Note(val set: Int, val score: List<Int>, val text: String, val at: Long)
data class SetState(
    val number: Int, val initial: List<List<Int>>, val court: List<List<Int>>, val firstServer: Int,
    val initialLeft: Int, val startedAt: Long, val endedAt: Long? = null,
    val score: List<Int> = listOf(0, 0), val server: Int = firstServer,
    val rotations: List<Int> = listOf(0, 0), val turns: List<List<Turn>> = listOf(emptyList(), emptyList()),
    val changes: List<Change> = emptyList(), val timeouts: List<Timeout> = emptyList(),
    val liberos: List<ActiveLibero?> = listOf(null, null), val liberoLog: List<LiberoEntry> = emptyList(),
    val lastLiberoRally: List<Int> = listOf(-1, -1), val rallies: Int = 0,
    val changedEndsAt: List<Int>? = null, val penaltyPoints: List<List<Int>> = listOf(emptyList(), emptyList()),
    val expelled: List<List<Int>> = listOf(emptyList(), emptyList())
) {
    val winner: Int? get() = if (endedAt == null) null else if (score[0] > score[1]) 0 else 1
    fun visible(team: Int): List<Int> = court[team].map { n -> if (liberos[team]?.replaced == n) liberos[team]!!.number else n }
    fun countSubs(team: Int) = changes.count { it.team == team && !it.exceptional }
}
data class MatchState(
    val sets: List<SetState> = emptyList(), val notes: List<Note> = emptyList(), val sanctions: List<Sanction> = emptyList(),
    val disqualified: List<List<Int>> = listOf(emptyList(), emptyList()),
    val retired: List<List<Int>> = listOf(emptyList(), emptyList()),
    val designatedLiberos: List<List<Int>> = listOf(emptyList(), emptyList()),
    val captains: List<Int> = listOf(0, 0), val interrupted: Boolean = false, val endedEarly: Boolean = false
) {
    val current: SetState? get() = sets.lastOrNull()
    fun wins(team: Int) = sets.count { it.winner == team }
    fun finished(config: MatchConfig) = endedEarly || (config.format == Format.THREE_FIXED && sets.size == 3 && current?.endedAt != null) ||
        (config.format != Format.THREE_FIXED && (wins(0) >= config.format.wins || wins(1) >= config.format.wins))
    fun letter(team: Int): String = if ((sets.firstOrNull()?.initialLeft ?: 0) == team) "A" else "B"
}
data class Journal(val version: Int = 1, val config: MatchConfig, val events: List<Event> = emptyList(), val redo: List<Event> = emptyList(), val signatures:List<MatchSignature>?=null,val openingToss:OpeningToss?=null)

fun <T> List<T>.with(index: Int, value: T): List<T> = mapIndexed { i, t -> if (i == index) value else t }
fun clock(at: Long): String = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Rome")).format(Instant.ofEpochMilli(at))
fun scoreLabel(score: List<Int>, team: Int = 0): String = "${score[team]}:${score[1-team]}"

fun MatchConfig.missingMetadata(): List<String> = buildList {
    teams.forEach { team ->
        val missing=team.players.filter { it.name.isBlank() }.map { it.number }
        if(missing.isNotEmpty())add("cognome e nome · ${team.name} · n. ${missing.joinToString()}")
    }
    if(competition.isBlank())add("campionato")
    if(matchNumber.isBlank())add("numero gara")
    if(date.isBlank())add("data")
    if(scheduledTime.isBlank())add("ora prevista")
    if(location.isBlank())add("località")
    if(venue.isBlank())add("palestra/campo")
    if(officials.getOrElse(0){""}.isBlank())add("1° arbitro")
    if(officials.getOrElse(2){""}.isBlank())add("segnapunti")
}

/** Recognize the explicit youth category, not unrelated numbers in the competition name. */
fun MatchConfig.belowUnder14(): Boolean {
    val age = Regex("(?i)(?<![\\p{L}\\d])(?:U|UNDER)\\s*[-–]?\\s*(\\d{1,2})(?!\\d)")
        .find(competition)?.groupValues?.get(1)?.toIntOrNull()
    return age != null && age in 1..13
}
