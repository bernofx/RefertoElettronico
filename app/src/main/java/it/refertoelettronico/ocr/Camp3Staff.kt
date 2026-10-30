package it.refertoelettronico.ocr

import java.util.Locale
import kotlin.math.abs

enum class Camp3Section { PLAYERS, STAFF }
data class StaffSuggestion(val index:Int,val name:String,val source:String)

object Camp3Staff {
    val labels=listOf("Allenatore","Vice allenatore","Fisioterapista","Medico","Dirigente")
    /** Only explicit role labels are used. Unrecognized roles remain for manual entry. */
    fun suggest(words:List<OcrWord>,skewSlope:Float=0f):List<StaffSuggestion> {
        if(words.isEmpty())return emptyList()
        fun norm(s:String)=s.uppercase(Locale.ROOT).filter{it.isLetterOrDigit()}
        fun y(w:OcrWord)=w.cy-skewSlope*w.cx
        val height=words.map{it.height}.sorted()[words.size/2]
        val groups=mutableListOf<MutableList<OcrWord>>()
        words.sortedBy(::y).forEach { w->
            val group=groups.minByOrNull{abs(it.map(::y).average()-y(w))}
            if(group!=null && abs(group.map(::y).average()-y(w))<height*.72f)group+=w else groups+=mutableListOf(w)
        }
        return groups.mapNotNull { group->
            val row=group.sortedBy{it.left};val normalized=row.map{norm(it.text)}
            val coach=normalized.indexOfFirst{it.contains("ALLENATORE")}
            val prefix=normalized.getOrNull(coach-1)
            val index=when {
                coach>=0 -> if(prefix in setOf("2","2O","2A","SECONDO","VICE") || normalized[coach].startsWith("VICE") || normalized[coach].startsWith("2"))1 else 0
                normalized.any{it.startsWith("FISIOTERAP") || it.startsWith("MASSAGGIAT") } ->2
                "MEDICO" in normalized ->3
                normalized.any{it.startsWith("DIRIGENTE")} ->4
                else ->return@mapNotNull null
            }
            val name=row.filter { w->
                val n=norm(w.text)
                w.text.matches(Regex("[\\p{L}][\\p{L}'’.-]*")) && n !in setOf("VICE","PRIMO","SECONDO","ACCOMPAGNATORE","ADDETTO","ARBITRO","COGNOME","NOME","E") &&
                    listOf("ALLENATORE","FISIOTERAP","MASSAGGIAT","MEDICO","DIRIGENTE").none{n.startsWith(it)}
            }.joinToString(" "){it.text}
            if(name.split(' ').count{it.isNotBlank()}<2)null else StaffSuggestion(index,name,row.joinToString(" "){it.text})
        }
    }
}
