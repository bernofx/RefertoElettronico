package it.refertoelettronico.model

object Demo {
    fun config(): MatchConfig = MatchConfig(
        teams = listOf("Lago Volley", "Stella Como").mapIndexed { t, name -> Team(name,
            (1..8).map { Player(it, "Atleta ${t+1}-$it", captain = it == 1) } +
                Player(12, "Libero ${t+1}-1", libero = true) + Player(13, "Libero ${t+1}-2", libero = true),
            staff = listOf("Allenatore dimostrativo", "", "", "", "")) },
        matchNumber = "DEMO", date = "30/10/2026", scheduledTime = "18:00", location = "Como", venue = "Palestra dimostrativa",
        officials = listOf("Arbitro dimostrativo", "", "Segnapunti dimostrativo", "", "", "", ""),
        officialCities = listOf("Como", "", "Como", "", "", "", ""),
        initialObservations = "GARA DIMOSTRATIVA - dati fittizi", fallbackReason = "Dimostrazione, nessuna gara ufficiale")
    fun start(at: Long = System.currentTimeMillis()) = Event(Kind.START_SET, lineups = listOf((1..6).toList(), (1..6).toList()), at = at)
}
