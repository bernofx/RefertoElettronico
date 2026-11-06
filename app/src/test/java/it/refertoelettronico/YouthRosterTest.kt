package it.refertoelettronico

import it.refertoelettronico.model.*
import org.junit.Assert.*
import org.junit.Test

class YouthRosterTest {
    private fun config(category:String,count:Int)=Demo.config().copy(competition=category,teams=List(2){t->
        Team("Squadra $t",(1..count).map{Player(it,"Atleta $it",captain=it==1)})
    })
    @Test fun youngerTeamsCanStartWithFourteenPlayersAndNoLibero() {
        listOf("U13 F Como","Under 12 femminile","U-13 Maschile").forEach { category->
            val c=config(category,14)
            MatchEngine.validate(c)
            val state=MatchEngine.apply(c,MatchEngine.initial(c),Event(Kind.START_SET,lineups=List(2){(1..6).toList()}))
            assertTrue(state.designatedLiberos.all{it.isEmpty()})
            assertNotNull(state.current)
        }
    }
    @Test fun higherAndUnknownCategoriesKeepExistingRosterConstraint() {
        listOf("U14 F Como","Under 15 maschile","U18","Campionato 13 girone A","U130").forEach { category->
            assertFalse(config(category,14).belowUnder14())
            assertThrows(IllegalArgumentException::class.java){MatchEngine.validate(config(category,14))}
            MatchEngine.validate(config(category,12))
        }
    }
}
