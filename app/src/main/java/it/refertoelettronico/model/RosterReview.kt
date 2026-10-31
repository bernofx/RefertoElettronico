package it.refertoelettronico.model

object RosterReview {
    const val MATCH_LIMIT=14
    fun issues(team:Team):Map<String,String> {
        val members=team.roster ?: return emptyMap()
        val present=members.filter{it.present};val errors=linkedMapOf<String,String>()
        present.forEach{if(it.player.number !in 1..99)errors[it.id]="Completa il numero di maglia (1–99)."}
        present.groupBy{it.player.number}.values.filter{it.size>1}.flatten().forEach{errors[it.id]="Numero duplicato tra le presenti: correggi o deseleziona."}
        if(present.size>MATCH_LIMIT)present.forEach{errors[it.id]="Superato il limite di $MATCH_LIMIT presenti."}
        if(present.count{it.player.libero}>2)present.filter{it.player.libero}.forEach{errors[it.id]="Sono selezionati più di due liberi."}
        if(present.count{it.player.captain}>1)present.filter{it.player.captain}.forEach{errors[it.id]="Scegli una sola capitana tra le presenti."}
        return errors
    }
}
