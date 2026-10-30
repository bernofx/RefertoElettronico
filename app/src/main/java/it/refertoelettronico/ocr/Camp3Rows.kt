package it.refertoelettronico.ocr

import it.refertoelettronico.model.Player
import java.util.Locale
import kotlin.math.abs

data class OcrWord(val text:String,val left:Float,val top:Float,val right:Float,val bottom:Float) {
    val cx get()=(left+right)/2
    val cy get()=(top+bottom)/2
    val height get()=(bottom-top).coerceAtLeast(1f)
}
data class RosterSuggestion(val name:String,val number:Int?,val source:String,val issues:List<String> = emptyList(),val captain:Boolean=false,val libero:Boolean=false)
data class Camp3Reading(val rows:List<RosterSuggestion>,val text:String,val notices:List<String> = emptyList(),val staff:List<StaffSuggestion> = emptyList(),val detectedCrop:PhotoCrop?=null)

object Camp3Rows {
    private fun norm(text:String)=text.uppercase(Locale.ROOT).filter{it.isLetterOrDigit()}
    private fun header(text:String)=norm(text).replace('1','I').replace('0','O')
    private val stops=setOf("ALLENATORE","ALLENATORI","DIRIGENTE","DIRIGENTI","TECNICI","FIRME","MEDICO","FISIOTERAPISTA")
    private val columns=setOf("TESSERA","TESSERAMENTO","MATRICOLA","NASCITA","DATA","NAZIONALITA","NAZIONALITÀ","CITTADINANZA","QUALIFICA","RUOLO","MAGLIA","NUMERO","CODICE")
    private val excluded=columns+setOf("COGNOME","NOME","NOMINATIVO","ATLETI","ATLETE","GIOCATORI","GIOCATRICI","ELENCO","SQUADRA","SOCIETA","SOCIETÀ","CAMP3","FEDERAZIONE","PALLAVOLO")
    private val nameWord=Regex("[\\p{L}][\\p{L}'’.-]*")
    private fun nameHeader(w:OcrWord)=header(w.text).let{it.contains("COGNOME") || it.contains("NOMINATIVO")}
    private fun jerseyHeader(w:OcrWord)=header(w.text).let{it.contains("MAGLIA") || it=="MAGUA" || it=="MAGL"}
    private fun median(values:List<Float>)=values.sorted().let{it[it.size/2]}

    /** Project onto the table baseline before grouping: columns need not be level in the photo. */
    fun suggest(words:List<OcrWord>,numberColumnX:Float?=null,skewSlope:Float=0f):List<RosterSuggestion> {
        if(words.isEmpty())return emptyList()
        val height=median(words.map{it.height})
        fun y(word:OcrWord)=word.cy-skewSlope.coerceIn(-.3f,.3f)*word.cx
        val groups=mutableListOf<MutableList<OcrWord>>()
        words.sortedBy{y(it)}.forEach { word->
            val row=groups.minByOrNull{abs(median(it.map(::y))-y(word))}
            if(row!=null && abs(median(row.map(::y))-y(word))<=minOf(height,word.height)*.72f)row+=word
            else groups+=mutableListOf(word)
        }
        val rows=groups.sortedBy{median(it.map(::y))}.map{it.sortedBy{w->w.left}}
        val heading=words.filter(::nameHeader).minByOrNull{y(it)} ?: words.filter(::jerseyHeader).minByOrNull{y(it)}
        val headerY=heading?.let(::y)
        val band=if(headerY!=null)words.filter{abs(y(it)-headerY)<=height*1.8f} else emptyList()
        val nameHead=band.filter{nameHeader(it) || header(it.text)=="NOME"}
        val nameLeft=nameHead.minOfOrNull{it.left}
        val nameRight=nameHead.maxOfOrNull{it.right}
        // Names may start well before their centred header. Do not cut at the word COGNOME.
        val leftEdge=if(nameLeft!=null)band.filter{it.right<nameLeft && header(it.text) in columns}.maxOfOrNull{it.cx} ?: Float.NEGATIVE_INFINITY else Float.NEGATIVE_INFINITY
        val rightEdge=if(nameRight!=null)band.filter{it.left>nameRight && (header(it.text) in columns || jerseyHeader(it))}.minOfOrNull{it.cx} ?: Float.POSITIVE_INFINITY else Float.POSITIVE_INFINITY
        // CAMP3 often calls the jersey column just N°. Accept it only next to the name
        // header and with another roster column; never confuse it with the match number.
        val camp3Columns=band.any{header(it.text) in setOf("MATRICOLA","NASCITA","TESSERA","TESSERAMENTO")}
        val shortNumberHeader=if(nameLeft!=null && camp3Columns)band.filter {
            it.right<nameLeft && header(it.text) in setOf("N","NO","NR","NUM","NUMERO")
        }.maxByOrNull{it.right} else null
        val jersey=band.firstOrNull(::jerseyHeader) ?: shortNumberHeader
        val numberX=numberColumnX ?: jersey?.cx
        val explicitColumn=numberColumnX!=null
        val columnRadius=if(explicitColumn)height*1.3f else maxOf(height*1.5f,(jersey?.let{it.right-it.left} ?: 0f)*.65f)
        val result=mutableListOf<RosterSuggestion>()
        for(row in rows) {
            val rowY=median(row.map(::y))
            if(headerY!=null && rowY<=headerY+height*.7f)continue
            if(row.any{header(it.text)=="QUALIFICA"} && row.any{nameHeader(it) || header(it.text)=="TESSERA"})break
            // Stop at a section heading, not when an athlete happens to be called MEDICO.
            if(row.any{header(it.text) in stops} && (row.size<=2 || row.none{it.text.any(Char::isDigit)}))break
            if(row.any{nameHeader(it) || header(it.text) in setOf("NASCITA","TESSERA","MAGLIA","ATLETI","ATLETE","COGNOME","NOME")})continue
            val nameTokens=row.filter { w->
                w.cx>leftEdge && w.cx<rightEdge && nameWord.matches(w.text) &&
                    norm(w.text) !in setOf("K","C","L","L1","L2") && header(w.text) !in excluded &&
                    (numberX==null || abs(w.cx-numberX)>columnRadius || number(w.text)==null)
            }
            if(nameTokens.isEmpty() || (nameTokens.size<2 && headerY==null && !explicitColumn))continue
            if(headerY==null && numberColumnX==null && row.none{it.text.any(Char::isDigit)})continue
            val candidates=if(numberX==null)emptyList() else row.filter{abs(it.cx-numberX)<=columnRadius && number(it.text)!=null}
            val issues=mutableListOf<String>()
            if(nameTokens.size==1)issues+="Nome letto solo in parte o come unica parola: confrontalo con la foto."
            var raw:String?=null
            val value=when {
                candidates.size==1 -> {raw=candidates[0].text;number(raw!!)}
                candidates.size==2 && candidates.all{it.text.matches(Regex("[0-9]"))} && candidates[1].left-candidates[0].right in 0f..height*.35f -> {
                    raw=candidates.joinToString(""){it.text};issues+="Numero letto in due parti: controllalo.";number(raw!!)
                }
                else -> null
            }
            if(value==null)issues+=if(candidates.size>1)"Più numeri possibili nella colonna: inserisci quello corretto." else "Numero di maglia da completare."
            if(raw!=null && raw!!.any{it.isLetter()})issues+="Numero interpretato da «$raw»: controllalo."
            val nameEnd=nameTokens.maxOf{it.right}
            val roleMarkers=if(shortNumberHeader!=null)row.filter {
                it.left>nameEnd+height*.75f && it.cx<rightEdge && norm(it.text) in setOf("K","L1","L2")
            }.map{norm(it.text)} else emptyList()
            val captain="K" in roleMarkers
            val libero=roleMarkers.any{it=="L1" || it=="L2"}
            if(captain || libero)issues+="Ruolo ${roleMarkers.joinToString()} letto dalla foto: controllalo."
            result+=RosterSuggestion(nameTokens.joinToString(" "){it.text},value,row.joinToString(" "){it.text},issues,captain,libero)
            if(result.size>=40)break
        }
        val duplicated=result.mapNotNull{it.number}.groupingBy{it}.eachCount().filterValues{it>1}.keys
        return result.map{if(it.number in duplicated)it.copy(issues=it.issues+"Numero presente anche in un'altra riga: controllalo.")else it}
    }

    private fun number(raw:String):Int? {
        val text=raw.trim(' ','.','(',')','[',']')
        if(text.length !in 1..2 || text.none{it.isDigit()})return null
        val digits=text.uppercase(Locale.ROOT).map{when(it){'O'->'0';'I','L','|'->'1';else->it}}.joinToString("")
        return digits.toIntOrNull()?.takeIf{it in 1..99}
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
