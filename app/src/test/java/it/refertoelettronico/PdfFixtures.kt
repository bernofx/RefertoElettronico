package it.refertoelettronico

import it.refertoelettronico.model.*

object PdfFixtures {
    fun completeWithLongFields():Journal {
        val base=Demo.config()
        val config=base.copy(competition="Campionato territoriale Under 14 femminile FIPAV Como - denominazione estesa",
            phase="Girone territoriale di qualificazione con una denominazione particolarmente lunga",
            venue="Palazzetto comunale delle attività sportive giovanili - campo principale della palestra scolastica",
            location="Comune con denominazione estesa della provincia di Como",
            officials=(0..6).map{"Ufficiale $it CognomeComposto MoltoLungo con più nomi e un cognome aggiuntivo"},
            officialCities=(0..6).map{"Comune residenza $it con denominazione particolarmente estesa"},
            initialObservations="Osservazione lunga di controllo degli allineamenti e delle eccedenze. ".repeat(25),
            teams=base.teams.mapIndexed { t,team->team.copy(name="Società sportiva pallavolistica $t con denominazione estesa per prova",
                staff=(0..4).map{"Panchina $t-$it CognomeComposto Nome MoltoLungo"},
                players=(team.players+(9..11).map{Player(it,"Giocatrice aggiuntiva $t-$it")}+Player(14,"Giocatrice aggiuntiva $t-14"))
                    .map{it.copy(name="${it.name} CognomeComposto con un secondo nome aggiuntivo")})})
        var journal=Journal(config=config)
        var time=java.time.Instant.parse("2026-10-30T17:00:00Z").toEpochMilli()
        fun add(event:Event){time+=25_000;journal=MatchEngine.add(journal,event.copy(at=time))}
        fun current()=MatchEngine.replay(config,journal.events).current!!
        fun point(team:Int){add(Event(Kind.POINT,team))}
        val results=listOf(25 to 18,22 to 25,54 to 52,19 to 25,18 to 16)
        results.forEachIndexed { index,(a,b)->
            time+=180_000
            add(Demo.start().copy(left=1-index%2,server=index%2))
            if(index==0) {
                add(Event(Kind.LIBERO_IN,0,12,6));point(0)
                add(Event(Kind.LIBERO_IN,0,13,6));point(0);add(Event(Kind.LIBERO_OUT,0))
                add(Event(Kind.SUBSTITUTION,0,1,7));add(Event(Kind.SUBSTITUTION,0,7,1))
                add(Event(Kind.TIMEOUT,0));add(Event(Kind.TIMEOUT,1))
                add(Event(Kind.SANCTION,1,text="A",card=Card.WARNING))
                add(Event(Kind.SANCTION,1,text="A",card=Card.IMPROPER))
                add(Event(Kind.NOTE,text="Nota aggiuntiva con un testo molto lungo da conservare integralmente negli allegati. ".repeat(15)))
            }
            val common=minOf(a,b)
            while(current().score[0]<common || current().score[1]<common) {
                if(current().score[0]<common)point(0)
                if(current().score[1]<common)point(1)
            }
            while(current().score[0]<a)point(0)
            while(current().score[1]<b)point(1)
        }
        return journal
    }
}
