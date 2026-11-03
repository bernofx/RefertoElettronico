package it.refertoelettronico.pdf

import it.refertoelettronico.model.*
import kotlin.math.max

/** Coordinates refer to the unchanged official blank (1190 x 730). Independent of Android. */
data class Mark(val type: String, val x: Float, val y: Float, val text: String = "", val size: Float = 7f,
    val x2: Float = 0f, val y2: Float = 0f, val width: Float = 0f, val cell: TextCell? = null, val color: Int = -1,val signature:MatchSignature?=null)
data class SheetPage(val template: Boolean, val title: String, val marks: List<Mark>)
object SheetLayout {
    fun build(journal: Journal): List<SheetPage> {
        Signatures.validate(journal)
        val config = journal.config
        val state = MatchEngine.replay(config, journal.events)
        val marks = mutableListOf<Mark>()
        fun cell(x:Number,y:Number,w:Number,h:Number,value:Any?,size:Float=6f,center:Boolean=false) {
            val box=TextCell(x.toFloat(),y.toFloat(),x.toFloat()+w.toFloat(),y.toFloat()+h.toFloat(),if(center)CellAlign.CENTER else CellAlign.LEFT)
            marks+=Mark("text",box.left,box.bottom-2,value?.toString().orEmpty(),size,width=box.width,cell=box)
        }
        fun score(x:Float,y:Float,w:Float,h:Float,values:List<Int>,team:Int) {
            cell(x+1,y+1,w/2-3,h-2,values[team],5.5f,true)
            cell(x+w/2+2,y+1,w/2-3,h-2,values[1-team],5.5f,true)
        }
        fun line(x: Float,y: Float,x2: Float,y2: Float) { marks += Mark("line",x,y,x2=x2,y2=y2) }
        fun circle(x: Float,y: Float,r: Float) { marks += Mark("circle",x,y,size=r) }
        fun wipe(x: Float,y: Float,w: Float,h: Float) { marks += Mark("white",x,y,x2=w,y2=h) }
        cell(110,6,99,10,config.competition,7f);cell(264,6,49,10,config.matchNumber,7f)
        cell(413,6,202,10,config.season,7f);cell(264,20,351,10,config.phase,7f)
        cell(197,20,7,8,"X",6f,true)
        cell(56,33,153,10,config.location,7f);cell(264,33,144,10,config.venue,7f)
        cell(456,33,70,10,config.date,7f);cell(560,33,55,10,config.scheduledTime,7f)
        val teamA = Signatures.toss(journal)?.left ?: 0
        fun letter(team:Int)=if(team==teamA)"A"else"B"
        val teamB = 1-teamA
        cell(43,47,239,10,config.teams[0].name,8f);cell(345,47,243,10,config.teams[1].name,8f)
        cell(23,48,10,12,letter(0),7f,true);cell(594,48,10,12,letter(1),7f,true)
        val overflow = mutableListOf<String>()
        val changeSnapshots=mutableMapOf<Int,SetState>()
        var replayed=MatchEngine.initial(config)
        journal.events.forEach { e ->
            val previous=replayed.current
            replayed=MatchEngine.apply(config,replayed,e)
            replayed.current?.let { next ->
                if(next.changedEndsAt!=null && previous?.changedEndsAt==null)changeSnapshots[next.number]=next
            }
        }
        fun teamBlock(set: SetState, team: Int, x: Float, top: Float, duplicate: Boolean = false) {
            val dx=x-93f;val dy=top-71f
            val deciding=MatchEngine.isDeciding(config,set.number)
            val checkpoint=changeSnapshots[set.number]
            val initialPanel=deciding && x==93f
            val shown=if(initialPanel && checkpoint!=null)checkpoint else set
            val rightHeader=duplicate || x==359f || x==920f
            val nameLeft=x+if(duplicate)44 else if(rightHeader)45 else 73
            val nameRight=x+if(duplicate)118 else if(rightHeader)116 else 146
            cell(nameLeft,73+dy,nameRight-nameLeft,14,config.teams[team].name,6f)
            for(slot in 0..5)cell(x+slot*29.5f+2,105+dy,25.5f,10,set.initial[team][slot],8f,true)
            val letterX=x+if(duplicate || rightHeader)20 else 158
            // Sets 1–4 already contain the correct A/B letters on the printed sheet.
            if(deciding)cell(letterX-6,73+dy,12,14,letter(team),8f,true)
            val first=team==set.firstServer
            if(!duplicate) {
                val flagX=x+if(rightHeader)5 else 172
                val flagY=(if(first)75 else 85)+dy
                line(flagX-2,flagY-2,flagX+2,flagY+2);line(flagX-2,flagY+2,flagX+2,flagY-2)
            }
            shown.changes.filter{it.team==team && !it.exceptional}.groupBy{it.slot}.forEach { (slot,changes) ->
                changes.firstOrNull()?.let { sub ->
                    cell(x+slot*29.5f+2,119+dy,25.5f,10,sub.incoming,7f,true)
                    score(x+slot*29.5f,131+dy,29.5f,12f,sub.score,team)
                    changes.getOrNull(1)?.let { back ->
                        score(x+slot*29.5f,144+dy,29.5f,12f,back.score,team)
                        circle(x+14.75f+slot*29.5f,124+dy,5f)
                    }
                }
            }
            val counts=IntArray(6)
            if(!first){if(!duplicate){line(x+3,162+dy,x+9,168+dy);line(x+3,168+dy,x+9,162+dy)};counts[0]=1}
            val completedBeforeChange=if(duplicate)checkpoint?.turns?.get(team)?.count{it.endScore!=null}?:0 else 0
            shown.turns[team].forEachIndexed { index,turn ->
                val round=counts[turn.slot]++
                if(!duplicate || index>=completedBeforeChange) {
                    val halfLeft=x+turn.slot*29.5f+(if(round<4)0f else 14.75f)
                    val cx=halfLeft+7.375f
                    val rowTop=157+dy+(round%4)*13.2f
                    val cy=rowTop+8.5f
                    if(round<8) {
                        val markHere=when {
                            checkpoint==null -> true
                            initialPanel -> turn.startAtTotal < checkpoint.score.sum()
                            duplicate -> turn.startAtTotal >= checkpoint.score.sum()
                            else -> true
                        }
                        if(turn.served && markHere)line(halfLeft+11,rowTop+1,halfLeft+14,rowTop+4)
                        if(turn.endScore!=null) {
                            cell(halfLeft+1,rowTop+4,12.75f,8,turn.endScore,5.8f,true)
                            if(shown.endedAt!=null && index==shown.turns[team].lastIndex)circle(cx,cy,4.5f)
                        }
                    } else if(!duplicate)overflow+="Set ${set.number} sq. ${letter(team)} turno ${round+1}, ordine ${turn.slot+1}, n. ${turn.player}: ${turn.endScore?:"in corso"}"
                }
            }
            val scoreX=(if(initialPanel)279.5f else 272f)+dx
            val finalScore=shown.score[team]
            val priorPoints=if(duplicate)checkpoint?.score?.get(team)?:0 else 0
            val rows=if(initialPanel)8 else 12
            val scoreBase=if(initialPanel)400f else 96f+dy
            val step=6.65f
            val capacity=if(initialPanel)13 else 48
            for(p in 1..minOf(finalScore,capacity)) {
                val cy=scoreBase+((p-1)%rows)*step
                val cx=scoreX+((p-1)/rows)*7.2f
                if(p>priorPoints)line(cx,cy,cx+4,cy-4)
                if(p in shown.penaltyPoints[team])circle(cx+2,cy-2,3f)
                if(shown.endedAt!=null && p==finalScore)line(cx-1,cy+1,cx+5,cy+1)
            }
            if(priorPoints>0) {
                val lastY=scoreBase+(minOf(priorPoints,12)-1)*step
                line(scoreX+2,scoreBase-4,scoreX+2,lastY)
            }
            if(finalScore>capacity && !duplicate)overflow+="Set ${set.number} sq. ${letter(team)} punti oltre $capacity: ${capacity+1}-$finalScore"
            if(shown.endedAt!=null)for(col in 0 until if(initialPanel)2 else 4) {
                val firstUnused=max(finalScore+1,col*rows+1)
                val last=minOf((col+1)*rows,capacity)
                if(firstUnused<=last) {
                    val cx=scoreX+col*7.2f+2
                    line(cx,scoreBase-3+((firstUnused-1)%rows)*step,cx,scoreBase-3+((last-1)%rows)*step)
                }
            }
            shown.timeouts.filter{it.team==team}.forEachIndexed { i,t ->score(270f+dx,184+dy+i*13.2f,29f,12f,t.score,team) }
            val logs=shown.liberoLog.filter{it.team==team}
            val firstLog=if(duplicate)checkpoint?.liberoLog?.count{it.team==team && it.exited}?:0 else 0
            logs.drop(firstLog).forEachIndexed { i,l ->
                val cx=301f+dx+(i/9)*29.7f;val rowTop=91f+dy+(i%9)*13.2f
                val value=if(config.teams[team].players.count{it.libero}>1 || l.chain.size>1)
                    "${l.chain.first()}-${l.replaced}"+l.chain.drop(1).joinToString(""){"-$it"} else l.replaced.toString()
                if(i<18){cell(cx,rowTop,25.7f,11,value,6f,true);if(l.exited)line(cx,rowTop+9,cx+25,rowTop+2)}
                else if(!duplicate)overflow+="Set ${set.number} sq. ${letter(team)} libero $value al ${scoreLabel(l.score,team)}${if(l.exited)", rientrata" else""}"
            }
        }
        state.sets.forEach { set ->
            val deciding=MatchEngine.isDeciding(config,set.number)
            val panel=if(deciding)5 else set.number
            val top=when(panel){1,2->71f;3,4->217f;else->362f}
            val leftX=if(panel%2==0)654f else 93f
            val rightX=leftX+266f
            val left=set.initialLeft;val right=1-left
            teamBlock(set,left,leftX,top);teamBlock(set,right,rightX,top)
            val dy=top-71
            val startTime=clock(set.startedAt).split(':')
            cell(leftX+26,73+dy,12,14,startTime[0],6f,true);cell(leftX+44,73+dy,13,14,startTime[1],6f,true)
            set.endedAt?.let{clock(it).split(':')}?.let { time ->
                cell(rightX+141,73+dy,13,14,time[0],6f,true);cell(rightX+161,73+dy,13,14,time[1],6f,true)
            }
            if(deciding && set.changedEndsAt!=null){teamBlock(set,left,654f,362f,true);cell(806,366,12,12,set.changedEndsAt[left],7f,true)}
            if(deciding && set.number!=5){
                marks+=Mark("fill",75f,461f,x2=13f,y2=19f,color=0xFF21B1D1.toInt())
                cell(76,463,11,14,set.number,12f,true)
            }
        }
        listOf(0,1).forEachIndexed { side,team ->
            val x=if(side==0)928f else 1058f
            val regular=config.teams[team].players.filter { !it.libero }.sortedBy { it.number }
            cell(if(side==0)932 else 1168,365,12,13,letter(team),7f,true)
            regular.take(12).forEachIndexed { i,p ->
                val y=396f+i*13.15f
                cell(x+2,y,18,10,p.number,6f,true);if(p.captain) circle(x+11,y+5,5f)
                cell(x+25,y,101,10,p.name,6f)
            }
            regular.drop(12).forEach { overflow += "ELENCO AGGIUNTIVO Sq. ${letter(team)} n. ${it.number} ${it.name}${if(it.captain) " (K)" else ""}" }
            config.teams[team].players.filter { it.libero }.forEachIndexed { i,p ->
                cell(x+2,568f+i*13f,18,11,p.number,6f,true);cell(x+25,568f+i*13f,101,11,p.name,6f)
            }
            config.teams[team].staff.take(5).forEachIndexed { i,name ->cell(if(side==0)x+7 else 1070f,608f+i*13.15f,if(side==0)107 else 114,10,name,6f) }
        }
        // The summary stays in A:B order; printed parentheses and h/min labels remain visible.
        cell(685,530,88,14,config.teams[teamA].name,6f);cell(820,530,96,14,config.teams[teamB].name,6f)
        val summaryColumns=listOf(669f,691f,713f,735f,830f,852f,875f,897f)
        fun summaryRow(top:Float,values:List<Int>) {
            summaryColumns.forEachIndexed { index,x->cell(x+2,top+2,17,15,values[index],6f,true) }
        }
        state.sets.forEachIndexed { i,set ->
            val top=567f+i*20f
            summaryRow(top,listOf(set.timeouts.count{it.team==teamA},set.changes.count{it.team==teamA},
                if(set.winner==teamA)1 else 0,set.score[teamA],set.score[teamB],if(set.winner==teamB)1 else 0,
                set.changes.count{it.team==teamB},set.timeouts.count{it.team==teamB}))
            cell(803,top+3,17,13,set.endedAt?.let{(it/60000-set.startedAt/60000).coerceAtLeast(0)} ?: "",6f,true)
        }
        if(state.finished(config)) {
            summaryRow(667f,listOf(state.sets.sumOf{set->set.timeouts.count{it.team==teamA}},state.sets.sumOf{set->set.changes.count{it.team==teamA}},
                state.wins(teamA),state.sets.sumOf{it.score[teamA]},state.sets.sumOf{it.score[teamB]},state.wins(teamB),
                state.sets.sumOf{set->set.changes.count{it.team==teamB}},state.sets.sumOf{set->set.timeouts.count{it.team==teamB}}))
            cell(780,675,22,8,state.sets.sumOf{set->set.endedAt?.let{it/60000-set.startedAt/60000}?:0},6f,true)
        }
        state.sets.firstOrNull()?.let { set ->clock(set.startedAt).split(':').let{
            cell(692,695,14,8,it[0],6f,true);cell(714,695,15,8,it[1],6f,true)
        } }
        state.sets.lastOrNull()?.endedAt?.let { at ->
            clock(at).split(':').let{cell(773,695,15,8,it[0],6f,true);cell(795,695,15,8,it[1],6f,true)}
            val minutes=(at/60000-state.sets.first().startedAt/60000).coerceAtLeast(0)
            cell(849,695,12,8,minutes/60,6f,true);cell(869,695,15,8,minutes%60,6f,true)
        }
        if(state.finished(config) && !state.endedEarly) {
            val winner=if(state.wins(0)>state.wins(1))0 else 1
            cell(714,709,168,13,config.teams[winner].name,7f)
            if(config.format!=Format.BEST_OF_FIVE){wipe(885f,708f,13f,16f);cell(886,709,11,13,state.wins(winner),10f,true)}
            cell(902,709,13,13,state.wins(1-winner),10f,true)
        }
        state.sanctions.filter { it.card != Card.IMPROPER }.take(13).forEachIndexed { i,san ->
            val x=when(san.card) {Card.WARNING,Card.DELAY_WARNING->6f;Card.PENALTY,Card.DELAY_PENALTY->28f;Card.EXPULSION->50f;else->72f}
            val y=548f+i*13.1f
            cell(x+2,y,17,10,if(san.card==Card.DELAY_WARNING || san.card==Card.DELAY_PENALTY)"R" else san.recipient,6f,true)
            cell(96,y,17,10,letter(san.team),6f,true);cell(117,y,17,10,san.set,6f,true)
            cell(138,y,40,10,scoreLabel(san.score,san.team),6f,true)
        }
        state.sanctions.filter { it.card == Card.IMPROPER }.forEach { circle(if(letter(it.team)=="A")131f else 175f,521f,5f) }
        state.sanctions.filter {it.card!=Card.IMPROPER}.drop(13).forEach { overflow += "SANZIONE Set ${it.set} ${scoreLabel(it.score,it.team)} Sq. ${letter(it.team)} ${it.card.label}: ${it.recipient}" }
        val observations=mutableListOf<String>()
        if(config.initialObservations.isNotBlank()) observations+=config.initialObservations
        if(config.fallbackReason.isNotBlank()) observations+="Referto cartaceo: ${config.fallbackReason}"
        observations+=state.notes.map { "${clock(it.at)} ${if(it.set>0) "${it.set}° set ${scoreLabel(it.score,teamA)} " else ""}${it.text}" }
        if(overflow.isNotEmpty())observations+="Vedi allegato per registrazioni oltre gli spazi del modello."
        val observationLines=observations.flatMap { wrap(it,102) }
        (observationLines.take(6)+"Dati completi e registrazioni negli allegati.").forEachIndexed { i,s->cell(192,523f+i*13.15f,465,10,s,6.5f) }
        config.officials.take(3).forEachIndexed { i,name ->
            cell(221,649f+i*13.15f,186,10,name,6f)
            cell(413,649f+i*13.15f,98,10,config.officialCities.getOrElse(i){""},6f)
        }
        config.officials.drop(3).take(4).forEachIndexed { i,name ->
            // Printed line-judge order is 1/3 in the left column and 2/4 on the right.
            cell(if(i%2==0)192 else 449,688f+(i/2)*13.15f,210,10,name,6f)
        }
        SignatureRole.entries.forEach { role ->
            Signatures.current(journal,role)?.let { signature->
                val box=SignatureGeometry.cell(role,signature)
                marks+=Mark("signature",box.left,box.top,cell=box,signature=signature)
            }
        }
        val missingSignatures=Signatures.required(config,true).plus(Signatures.required(config,false)).filter{Signatures.current(journal,it)==null}
        // Date/identifiers remain human-readable even if an unusually long value was fitted in the form.
        val status=if(state.finished(config) && !state.endedEarly) "GARA CONCLUSA" else if(state.endedEarly) "GARA TERMINATA ANTICIPATAMENTE" else "PROVVISORIO - GARA IN CORSO"
        val pages=mutableListOf(SheetPage(true,status+(if(config.missingMetadata().isNotEmpty())" · DATI DA COMPLETARE" else "")+(if(state.finished(config) && missingSignatures.isNotEmpty())" · FIRME DA COMPLETARE" else ""),marks))
        val appendix=mutableListOf<String>()
        appendix += "${config.competition} · stagione ${config.season} · gara ${config.matchNumber}"
        appendix += "${config.teams[teamA].name} (A) - ${config.teams[teamB].name} (B) · ${config.date} · ${config.location}"
        appendix += "DATI COMPLETI DELLA GARA"
        appendix += "Fase/girone: ${config.phase} · campo: ${config.venue} · ora prevista: ${config.scheduledTime}"
        appendix += "Formula: ${config.format.label} · limite sostituzioni: ${config.maxSubstitutions}"
        config.officials.forEachIndexed { i,name->appendix+="${listOf("1° arbitro","2° arbitro","Segnapunti","Giudice linea 1","Giudice linea 2","Giudice linea 3","Giudice linea 4").getOrElse(i){"Ufficiale"}}: $name · città: ${config.officialCities.getOrElse(i){""}}" }
        if(!config.refereeAttendant.isNullOrBlank())appendix += "Addetto all’arbitro: ${config.refereeAttendant}"
        appendix += "ELENCHI COMPLETI (numeri, capitana K, liberi L)"
        listOf(teamA,teamB).forEach { t ->
            appendix += "Squadra ${letter(t)}: ${config.teams[t].name} · codice società ${config.teams[t].clubCode}"
            config.teams[t].staff.forEachIndexed{i,name->appendix+="${listOf("Allenatore","Vice allenatore","Fisioterapista","Medico","Dirigente").getOrElse(i){"Panchina"}}: $name"}
            appendix += config.teams[t].players.sortedBy { it.number }.joinToString("; ") { "${it.number} ${it.name}${if(it.captain) " K" else ""}${if(it.libero) " L" else ""}" }
        }
        appendix += "OSSERVAZIONI COMPLETE";appendix+=observations
        appendix += "REGISTRAZIONI AGGIUNTIVE";appendix+=overflow
        appendix += "FIRME DEL REFERTO"
        Signatures.required(config,true).plus(Signatures.required(config,false)).forEach { role->
            val signature=Signatures.current(journal,role)
            appendix+=if(signature!=null)"${role.label}: ${signature.signer} - raccolta il ${java.time.Instant.ofEpochMilli(signature.signedAt).atZone(java.time.ZoneId.of("Europe/Rome")).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))} - riferimento originale ${signature.contentHash}${if(Signatures.needsReview(journal,role))" - CONTENUTO AGGIORNATO DOPO LA FIRMA, TRATTO CONSERVATO"else""}"
                else "${role.label}: ${if(journal.signatures.orEmpty().any{it.role==role.name})"FIRMA CONSERVATA IN ARCHIVIO - verificare firmatario e fase" else "NON RACCOLTA IN APP - spazio da completare sulla stampa"}"
        }
        appendix += "Firme tracciate sul dispositivo e riprodotte nel modello; non firme digitali qualificate. Consegna secondo le indicazioni dell'arbitro e del Comitato."
        appendix += "FIPAV: firme preliminari dopo il sorteggio; a fine gara segnapunti, 2° arbitro se presente, 1° arbitro. I capitani non firmano a fine gara."
        appendix += "CRONOLOGIA - punteggio indicato nell'ordine A:B"
        var progress=MatchEngine.initial(config)
        journal.events.forEachIndexed { i,e ->
            progress=MatchEngine.apply(config,progress,e)
            val set=progress.current
            val score=set?.let { "${it.score[teamA]}:${it.score[teamB]}" } ?: "0:0"
            appendix += "${i+1}. ${clock(e.at)} · set ${set?.number?:0} · $score · ${e.description(config)}"
        }
        appendix += "Completare sulla stampa le firme non raccolte in app. Il registro allegato è di supporto; il foglio principale riproduce la figura 82 del Manuale FIPAV del segnapunti 2025/26."
        val lines=appendix.flatMap { wrap(it,150) }
        lines.chunked(47).forEachIndexed { i,chunk ->
            pages += SheetPage(false,"ALLEGATO ${i+1} - REGISTRO DI SUPPORTO",chunk.mapIndexed { n,s -> Mark("text",24f,20f+n*14.5f,s,9f,width=1140f,cell=TextCell(24f,10f+n*14.5f,1164f,23f+n*14.5f)) })
        }
        return pages
    }
    private fun wrap(value: String, limit: Int): List<String> {
        if(value.isBlank())return emptyList()
        val result=mutableListOf<String>();var rest=value.replace('\n',' ')
        while(rest.length>limit) {
            val cut=rest.lastIndexOf(' ',limit).takeIf{it>limit/2}?:limit
            result+=rest.take(cut);rest=rest.drop(cut).trimStart()
        }
        if(rest.isNotEmpty())result+=rest
        return result
    }
}
