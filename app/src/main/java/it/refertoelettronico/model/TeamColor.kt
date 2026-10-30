package it.refertoelettronico.model

/** Stable keys in backups; older matches without a key get contrasting blue/red defaults. */
enum class TeamColor(val label: String, val argb: Long) {
    BLUE("Blu",0xFF2457B2), RED("Rosso",0xFFB72B3A), GREEN("Verde",0xFF18754B),
    ORANGE("Arancione",0xFFB45B13), PURPLE("Viola",0xFF7851A9), PINK("Rosa",0xFFB62E79),
    YELLOW("Giallo",0xFFF2CB49), WHITE("Bianco",0xFFFFFEFA), BLACK("Nero",0xFF232B30), GREY("Grigio",0xFF65717D);

    companion object {
        fun of(team: Team, teamIndex: Int): TeamColor = entries.find { it.name==team.colorKey }
            ?: if(teamIndex==0)BLUE else RED
    }
}
