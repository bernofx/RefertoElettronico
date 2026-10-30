package it.refertoelettronico.pdf

import it.refertoelettronico.model.*
import kotlin.math.max

/** Coordinates refer to the unchanged official blank (1190 x 730). Independent of Android. */
data class Mark(val type: String, val x: Float, val y: Float, val text: String = "", val size: Float = 7f,
    val x2: Float = 0f, val y2: Float = 0f, val width: Float = 0f)
data class SheetPage(val template: Boolean, val title: String, val marks: List<Mark>)
object SheetLayout {
    fun build(journal: Journal): List<SheetPage> {
        val config = journal.config
        val state = MatchEngine.replay(config, journal.events)
        val marks = mutableListOf<Mark>()
        fun text(x: Number,y: Number,s: Any?,size: Float=7f,width: Float=0f) { marks += Mark("text",x.toFloat(),y.toFloat(),s?.toString() ?: "",size,width=width) }
        fun line(x: Float,y: Float,x2: Float,y2: Float) { marks += Mark("line",x,y,x2=x2,y2=y2) }
        fun circle(x: Float,y: Float,r: Float) { marks += Mark("circle",x,y,size=r) }
        fun wipe(x: Float,y: Float,w: Float,h: Float) { marks += Mark("white",x,y,x2=w,y2=h) }
        text(123,13,config.competition,7f,84f);text(264,13,config.matchNumber,7f,55f)
        text(413,13,"${config.season} ${config.phase}",7f,205f);text(200,27,"X")
        text(53,41,config.location,7f,157f);text(255,41,config.venue,7f,150f)
        text(456,41,config.date,7f,70f);text(562,41,config.scheduledTime,7f,50f)
        val teamA = state.sets.firstOrNull()?.initialLeft ?: 0
        val teamB = 1-teamA
        text(47,57,config.teams[teamA].name,8f,235f);text(345,57,config.teams[teamB].name,8f,244f)
        text(25,58,"A");text(597,58,"B")
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
            text(x+(if(rightHeader)55 else 80),83+dy,config.teams[team].name,6f,if(rightHeader)60f else 66f)
            for(slot in 0..5)text(x+11+slot*29.4f,113+dy,set.initial[team][slot],8f)
            text(x+(if(rightHeader)8 else 156),83+dy,state.letter(team),7f)
            val first=team==set.firstServer
            if(!duplicate)text(x+(if(rightHeader)23 else 169),if(first)78+dy else 86+dy,"X",5f)
            shown.changes.filter{it.team==team && !it.exceptional}.groupBy{it.slot}.forEach { (slot,changes) ->
                changes.firstOrNull()?.let { sub ->
                    text(x+8+slot*29.4f,126+dy,sub.incoming,7f)
                    text(x+2+slot*29.4f,139+dy,scoreLabel(sub.score,team),5.5f,27f)
                    changes.getOrNull(1)?.let { back ->
                        text(x+2+slot*29.4f,152+dy,scoreLabel(back.score,team),5.5f,27f)
                        circle(x+11+slot*29.4f,123+dy,5f)
                    }
                }
            }
            val counts=IntArray(6)
            if(!first){if(!duplicate)text(x+8,166+dy,"X",6f);counts[0]=1}
            val completedBeforeChange=if(duplicate)checkpoint?.turns?.get(team)?.count{it.endScore!=null}?:0 else 0
            shown.turns[team].forEachIndexed { index,turn ->
                val round=counts[turn.slot]++
                if(!duplicate || index>=completedBeforeChange) {
                    val cx=x+turn.slot*29.4f+(if(round<4)6 else 21)
                    val cy=166+dy+(round%4)*13.2f
                    if(round<8) {
                        val markHere=when {
                            checkpoint==null -> true
                            initialPanel -> turn.startAtTotal < checkpoint.score.sum()
                            duplicate -> turn.startAtTotal >= checkpoint.score.sum()
                            else -> true
                        }
                        if(turn.served && markHere)line(cx+6,cy-10,cx+10,cy-6)
                        if(turn.endScore!=null) {
                            text(cx-3,cy,turn.endScore,6f)
                            if(shown.endedAt!=null && index==shown.turns[team].lastIndex)circle(cx,cy-2,5f)
                        }
                    } else if(!duplicate)overflow+="Set ${set.number} sq. ${state.letter(team)} turno ${round+1}, ordine ${turn.slot+1}, n. ${turn.player}: ${turn.endScore?:"in corso"}"
                }
            }
            val scoreX=272f+dx
            val finalScore=shown.score[team]
            val priorPoints=if(duplicate)checkpoint?.score?.get(team)?:0 else 0
            val rows=if(initialPanel)8 else 12
            val scoreBase=if(initialPanel)399f else 96f+dy
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
            if(finalScore>capacity && !duplicate)overflow+="Set ${set.number} sq. ${state.letter(team)} punti oltre $capacity: ${capacity+1}-$finalScore"
            if(shown.endedAt!=null)for(col in 0 until if(initialPanel)2 else 4) {
                val firstUnused=max(finalScore+1,col*rows+1)
                val last=minOf((col+1)*rows,capacity)
                if(firstUnused<=last) {
                    val cx=scoreX+col*7.2f+2
                    line(cx,scoreBase-3+((firstUnused-1)%rows)*step,cx,scoreBase-3+((last-1)%rows)*step)
                }
            }
            shown.timeouts.filter{it.team==team}.forEachIndexed { i,t ->text(scoreX,193+dy+i*13.2f,scoreLabel(t.score,team),6f,25f) }
            val logs=shown.liberoLog.filter{it.team==team}
            val firstLog=if(duplicate)checkpoint?.liberoLog?.count{it.team==team && it.exited}?:0 else 0
            logs.drop(firstLog).forEachIndexed { i,l ->
                val cx=302f+dx+(i/9)*27f;val cy=100f+dy+(i%9)*13.2f
                val value=if(config.teams[team].players.count{it.libero}>1 || l.chain.size>1)
                    "${l.chain.first()}-${l.replaced}"+l.chain.drop(1).joinToString(""){"-$it"} else l.replaced.toString()
                if(i<18){text(cx,cy,value,6f,25f);if(l.exited)line(cx,cy-1,cx+22,cy-5)}
                else if(!duplicate)overflow+="Set ${set.number} sq. ${state.letter(team)} libero $value al ${scoreLabel(l.score,team)}${if(l.exited)", rientrata" else""}"
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
            text(leftX+31,83+dy,startTime[0],6f);text(leftX+48,83+dy,startTime[1],6f)
            set.endedAt?.let{clock(it).split(':')}?.let { time ->
                text(rightX+141,83+dy,time[0],6f);text(rightX+158,83+dy,time[1],6f)
            }
            if(deciding && set.changedEndsAt!=null){teamBlock(set,left,654f,362f,true);text(811,377,set.changedEndsAt[left],7f)}
            if(deciding && set.number!=5){wipe(72f,400f,19f,37f);text(77,423,set.number,12f)}
        }
        listOf(teamA,teamB).forEachIndexed { side,team ->
            val x=if(side==0)928f else 1058f
            val regular=config.teams[team].players.filter { !it.libero }.sortedBy { it.number }
            text(if(side==0)x+6 else 1173f,376,state.letter(team),7f)
            regular.take(12).forEachIndexed { i,p ->
                val y=403f+i*13.15f
                text(x+6,y,p.number,6f);if(p.captain) circle(x+9,y-2,5f)
                text(x+27,y,p.name,6f,101f)
            }
            regular.drop(12).forEach { overflow += "ELENCO AGGIUNTIVO Sq. ${state.letter(team)} n. ${it.number} ${it.name}${if(it.captain) " (K)" else ""}" }
            config.teams[team].players.filter { it.libero }.forEachIndexed { i,p ->
                text(x+6,578f+i*13f,p.number,6f);text(x+27,578f+i*13f,p.name,6f,101f)
            }
            config.teams[team].staff.take(5).forEachIndexed { i,name -> text(x+7,616f+i*13.15f,name,6f,109f) }
        }
        // Final summary has A on the left independently of physical ends during each set.
        text(699,540,config.teams[teamA].name,6f,71f);text(839,540,config.teams[teamB].name,6f,90f)
        state.sets.forEachIndexed { i,set ->
            val y=580f+i*20.0f
            text(672,y,set.timeouts.count{it.team==teamA},6f);text(696,y,set.changes.count{it.team==teamA},6f)
            text(718,y,if(set.winner==teamA)"1" else "0",6f);text(741,y,set.score[teamA],6f)
            text(839,y,set.score[teamB],6f);text(861,y,if(set.winner==teamB)"1" else "0",6f)
            text(884,y,set.changes.count{it.team==teamB},6f);text(906,y,set.timeouts.count{it.team==teamB},6f)
            text(794,y,set.endedAt?.let { (it/60000-set.startedAt/60000).coerceAtLeast(0) } ?: "",6f)
        }
        if(state.finished(config)) {
            val totalsA=listOf(state.sets.sumOf{set->set.timeouts.count{it.team==teamA}},state.sets.sumOf{set->set.changes.count{it.team==teamA}},state.wins(teamA),state.sets.sumOf{it.score[teamA]})
            val totalsB=listOf(state.sets.sumOf{it.score[teamB]},state.wins(teamB),state.sets.sumOf{set->set.changes.count{it.team==teamB}},state.sets.sumOf{set->set.timeouts.count{it.team==teamB}})
            listOf(672f,696f,718f,741f).forEachIndexed{i,x->text(x,680,totalsA[i],6f)}
            listOf(839f,861f,884f,906f).forEachIndexed{i,x->text(x,680,totalsB[i],6f)}
            text(790,680,state.sets.sumOf{set->set.endedAt?.let{it/60000-set.startedAt/60000}?:0},6f)
        }
        state.sets.firstOrNull()?.let { set ->clock(set.startedAt).split(':').let{ text(696,702,it[0],6f);text(727,702,it[1],6f) } }
        state.sets.lastOrNull()?.endedAt?.let { at ->
            clock(at).split(':').let{text(775,702,it[0],6f);text(811,702,it[1],6f)}
            val minutes=(at/60000-state.sets.first().startedAt/60000).coerceAtLeast(0)
            text(865,702,minutes/60,6f);text(902,702,minutes%60,6f)
        }
        if(state.finished(config) && !state.endedEarly) {
            val winner=if(state.wins(0)>state.wins(1))0 else 1
            text(716,718,config.teams[winner].name,7f,163f)
            if(config.format!=Format.BEST_OF_FIVE){wipe(884f,707f,15f,17f);text(885,718,"${state.wins(winner)}:",7f)}
            text(903,718,state.wins(1-winner),7f)
        }
        state.sanctions.filter { it.card != Card.IMPROPER }.take(13).forEachIndexed { i,san ->
            val x=when(san.card) {Card.WARNING,Card.DELAY_WARNING->8f;Card.PENALTY,Card.DELAY_PENALTY->29f;Card.EXPULSION->52f;else->73f}
            val y=551f+i*13f
            text(x,y,if(san.card==Card.DELAY_WARNING || san.card==Card.DELAY_PENALTY)"R" else san.recipient,6f,19f)
            text(95,y,state.letter(san.team),6f);text(116,y,san.set,6f);text(139,y,scoreLabel(san.score,san.team),6f,39f)
        }
        state.sanctions.filter { it.card == Card.IMPROPER }.forEach { circle(if(state.letter(it.team)=="A")134f else 178f,521f,5f) }
        state.sanctions.drop(13).forEach { overflow += "SANZIONE Set ${it.set} ${scoreLabel(it.score,it.team)} Sq. ${state.letter(it.team)} ${it.card.label}: ${it.recipient}" }
        val observations=mutableListOf<String>()
        if(config.initialObservations.isNotBlank()) observations+=config.initialObservations
        if(config.fallbackReason.isNotBlank()) observations+="Referto cartaceo: ${config.fallbackReason}"
        observations+=state.notes.map { "${clock(it.at)} ${if(it.set>0) "${it.set}° set ${scoreLabel(it.score,teamA)} " else ""}${it.text}" }
        if(overflow.isNotEmpty())observations+="Vedi allegato per registrazioni oltre gli spazi del modello."
        val observationLines=observations.flatMap { wrap(it,102) }
        observationLines.take(7).forEachIndexed { i,s -> text(192,531f+i*13f,s,6.5f,460f) }
        config.officials.take(3).forEachIndexed { i,name ->
            text(221,651f+i*13f,name,6f,228f);text(456,651f+i*13f,config.officialCities.getOrElse(i){""},6f,90f)
        }
        config.officials.drop(3).take(4).forEachIndexed { i,name -> text(if(i<2)222f else 444f,701f+(i%2)*13f,name,6f,165f) }
        // Date/identifiers remain human-readable even if an unusually long value was fitted in the form.
        val status=if(state.finished(config) && !state.endedEarly) "GARA CONCLUSA" else if(state.endedEarly) "GARA TERMINATA ANTICIPATAMENTE" else "PROVVISORIO - GARA IN CORSO"
        val pages=mutableListOf(SheetPage(true,status+if(config.missingMetadata().isNotEmpty())" · DATI DA COMPLETARE" else "",marks))
        val appendix=mutableListOf<String>()
        appendix += "${config.competition} · stagione ${config.season} · gara ${config.matchNumber}"
        appendix += "${config.teams[teamA].name} (A) - ${config.teams[teamB].name} (B) · ${config.date} · ${config.location}"
        appendix += "ELENCHI COMPLETI (numeri, capitana K, liberi L)"
        listOf(teamA,teamB).forEach { t ->
            appendix += "Squadra ${state.letter(t)}: ${config.teams[t].name} · codice società ${config.teams[t].clubCode}"
            appendix += config.teams[t].players.sortedBy { it.number }.joinToString("; ") { "${it.number} ${it.name}${if(it.captain) " K" else ""}${if(it.libero) " L" else ""}" }
        }
        appendix += "OSSERVAZIONI COMPLETE";appendix+=observations
        appendix += "REGISTRAZIONI AGGIUNTIVE";appendix+=overflow
        appendix += "CRONOLOGIA - punteggio indicato nell'ordine A:B"
        var progress=MatchEngine.initial(config)
        journal.events.forEachIndexed { i,e ->
            progress=MatchEngine.apply(config,progress,e)
            val set=progress.current
            val score=set?.let { "${it.score[teamA]}:${it.score[teamB]}" } ?: "0:0"
            appendix += "${i+1}. ${clock(e.at)} · set ${set?.number?:0} · $score · ${e.description(config)}"
        }
        appendix += "Firme da apporre sul cartaceo. Il registro allegato è di supporto; il foglio principale riproduce la figura 82 del Manuale FIPAV del segnapunti 2025/26."
        val lines=appendix.flatMap { wrap(it,150) }
        lines.chunked(47).forEachIndexed { i,chunk ->
            pages += SheetPage(false,"ALLEGATO ${i+1} - REGISTRO DI SUPPORTO",chunk.mapIndexed { n,s -> Mark("text",24f,20f+n*14.5f,s,9f,width=1140f) })
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
