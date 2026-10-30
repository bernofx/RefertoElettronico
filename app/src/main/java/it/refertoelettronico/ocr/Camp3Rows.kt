package it.refertoelettronico.ocr

import it.refertoelettronico.model.Player
import java.util.Locale

/** Coordinates are from the upright photo. Jersey numbers are used only under a MAGLIA header. */
data class OcrWord(val text:String,val left:Float,val top:Float,val right:Float,val bottom:Float) {
    val cx get()=(left+right)/2
    val cy get()=(top+bottom)/2
    val height get()=(bottom-top).coerceAtLeast(1f)
}
data class RosterSuggestion(val name:String,val number:Int?,val source:String)
data class Camp3Reading(val rows:List<RosterSuggestion>,val text:String)

object Camp3Rows {
    private fun norm(text:String)=text.uppercase(Locale.ROOT).trim(' ','.',':','°','º')
    private val stop=setOf("ALLENATORE","ALLENATORI","DIRIGENTE","DIRIGENTI","TECNICI","FIRME","MEDICO","FISIOTERAPISTA")
    private val excluded=setOf("COGNOME","NOME","NOMINATIVO","DATA","NASCITA","TESSERA","TESSERAMENTO","MAGLIA","ATLETI","ATLETE","GIOCATORI","GIOCATRICI","ELENCO","NUMERO","SQUADRA","SOCIETA","SOCIETÀ","CODICE","QUALIFICA","RUOLO","CAMP3","FEDERAZIONE","PALLAVOLO")
    private val nameWord=Regex("[\\p{L}][\\p{L}'’.-]*")

    fun suggest(words:List<OcrWord>):List<RosterSuggestion> {
        if(words.isEmpty())return emptyList()
        val groups=mutableListOf<MutableList<OcrWord>>()
        words.sortedBy{it.cy}.forEach { word ->
            val row=groups.lastOrNull()
            if(row!=null && kotlin.math.abs(row.map{it.cy}.average()-word.cy)<=minOf(row.map{it.height}.average().toFloat(),word.height)*.55f)row+=word
            else groups+= mutableListOf(word)
        }
        val rows=groups.map{it.sortedBy{w->w.left}}
        val header=rows.indexOfFirst{row->row.any{norm(it.text) in setOf("COGNOME","NOMINATIVO")}}
        val headers=if(header>=0)rows.take(header+2).takeLast(3).flatten() else emptyList()
        val nameX=headers.firstOrNull{norm(it.text) in setOf("COGNOME","NOMINATIVO")}?.left
        val numberX=headers.firstOrNull{norm(it.text).contains("MAGLIA")}?.cx
        val rightEdge=if(nameX!=null)headers.filter{it.left>nameX && norm(it.text) in setOf("DATA","NASCITA","TESSERA","TESSERAMENTO","QUALIFICA")}.minOfOrNull{it.left} else null
        val result=mutableListOf<RosterSuggestion>()
        for((index,row) in rows.withIndex()) {
            if(header>=0 && index<=header)continue
            if(header>=0 && row.any{norm(it.text) in stop})break
            if(row.any{norm(it.text) in excluded})continue
            val nameTokens=row.filter { w ->
                (nameX==null || w.right>=nameX) && (rightEdge==null || w.left<rightEdge) &&
                    nameWord.matches(w.text) && norm(w.text) !in setOf("K","C","L","L1","L2")
            }
            if(nameTokens.size<2)continue
            // Without a name-column header, only table-like rows with a numeric cell are proposed.
            if(header<0 && row.none{it.text.any(Char::isDigit)})continue
            val numeric=row.filter{it.text.matches(Regex("[0-9]{1,2}")) && it.text.toInt() in 1..99}
            val number=if(numberX!=null)numeric.minByOrNull{kotlin.math.abs(it.cx-numberX)}
                ?.takeIf{kotlin.math.abs(it.cx-numberX)<=maxOf(it.height*3,30f)}?.text?.toInt() else null
            result+=RosterSuggestion(nameTokens.joinToString(" "){it.text},number,row.joinToString(" "){it.text})
            if(result.size>=40)break
        }
        return result
    }

    fun validate(players:List<Player>):String? = when {
        players.size !in 6..14 -> "Seleziona da 6 a 14 giocatrici."
        players.any{it.number !in 1..99} -> "Controlla tutti i numeri di maglia (1–99)."
        players.distinctBy{it.number}.size!=players.size -> "Ci sono numeri di maglia duplicati."
        players.any{it.name.isBlank()} -> "Completa i nomi delle righe da importare."
        players.count{it.captain}!=1 -> "Indica una sola capitana."
        players.count{it.libero}>2 -> "Indica al massimo due liberi."
        players.count{!it.libero}<6 -> "Servono almeno sei giocatrici non libero."
        players.size>12 && players.none{it.libero} -> "Con più di 12 giocatrici serve almeno un libero."
        else -> null
    }
}
