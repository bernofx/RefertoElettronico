package it.refertoelettronico.ui

import it.refertoelettronico.model.*

/** A deterministic, memory-only exercise. Never handed to MatchStore or DraftStore. */
internal object GuideExample {
    val config=Demo.config().copy(matchNumber="TUTORIAL",initialObservations="ESERCITAZIONE GUIDATA - DATI FITTIZI",
        fallbackReason="Esempio del tutorial, nessuna gara ufficiale")
    private const val start=1793383200000L
    fun journal(phase:Int):Journal {
        var j=Journal(config=config)
        fun add(e:Event){j=MatchEngine.add(j,e.copy(at=start+j.events.size*30_000L))}
        if(phase==0)return j
        val ink=listOf(listOf(SignaturePoint(.12f,.6f),SignaturePoint(.35f,.2f),SignaturePoint(.3f,.75f),SignaturePoint(.65f,.35f),SignaturePoint(.85f,.6f)))
        Signatures.required(config,true).forEach { role->j=Signatures.capture(j,role,ink,2.8f,Demo.start(start),at=start-10_000) }
        add(Demo.start(start))
        if(phase==1)return j
        add(Event(Kind.POINT,team=1))
        if(phase==2)return j
        add(Event(Kind.TIMEOUT,team=1))
        if(phase==3)return j
        val court=MatchEngine.replay(config,j.events).current!!.court[1]
        add(Event(Kind.SUBSTITUTION,team=1,player=court.first(),other=7))
        if(phase==4)return j
        add(Event(Kind.LIBERO_IN,team=1,player=12,other=court[4]))
        if(phase==5)return j
        j=MatchEngine.undo(j)
        if(phase==6)return j
        j=MatchEngine.redo(j)
        if(phase==7)return j
        add(Event(Kind.NOTE,text="Esempio guidato: annotazione comunicata dall’arbitro."))
        if(phase==8)return j
        add(Event(Kind.SANCTION,team=1,player=1,text="1",card=Card.WARNING))
        if(phase==9)return j
        // Finish by playing regular rallies and starting sets; keep all demonstrated operations.
        while(!MatchEngine.replay(config,j.events).finished(config)) {
            val set=MatchEngine.replay(config,j.events).current!!
            if(set.endedAt!=null)add(Demo.start().copy(server=1-set.firstServer,left=1-set.initialLeft))
            else add(Event(Kind.POINT,team=0))
        }
        if(phase>=11)Signatures.required(config,false).forEach { role->j=Signatures.capture(j,role,ink,2.8f,at=start+(j.events.size+1)*30_000L) }
        return j
    }
}
