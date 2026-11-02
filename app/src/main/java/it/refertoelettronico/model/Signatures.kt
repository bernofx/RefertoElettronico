package it.refertoelettronico.model

import com.google.gson.Gson
import java.security.MessageDigest

/** A drawn signature, not a cryptographic or qualified digital signature. Coordinates are normalized. */
data class OpeningToss(val server:Int,val left:Int)
data class SignaturePoint(val x:Float,val y:Float)
data class MatchSignature(val role:String,val signer:String,val signedAt:Long,val contentHash:String,
    val aspectRatio:Float,val strokes:List<List<SignaturePoint>>,val firstServer:Int=0,val initialLeft:Int=0)
enum class SignatureRole(val label:String,val preliminary:Boolean,val team:Int=-1) {
    CAPTAIN_HOME("Capitana · casa",true,0),COACH_HOME("Allenatore · casa",true,0),
    CAPTAIN_AWAY("Capitana · ospiti",true,1),COACH_AWAY("Allenatore · ospiti",true,1),
    SCORER("Segnapunti",false),SECOND_REFEREE("2° arbitro",false),FIRST_REFEREE("1° arbitro",false)
}
object Signatures {
    private val gson=Gson()
    fun required(config:MatchConfig,preliminary:Boolean):List<SignatureRole> = SignatureRole.entries.filter { role->
        role.preliminary==preliminary && when(role) {
            SignatureRole.COACH_HOME,SignatureRole.COACH_AWAY->config.teams[role.team].staff.getOrElse(0){""}.isNotBlank()
            SignatureRole.SECOND_REFEREE->config.officials.getOrElse(1){""}.isNotBlank()
            else->true
        }
    }
    fun signer(config:MatchConfig,role:SignatureRole):String=when(role) {
        SignatureRole.CAPTAIN_HOME,SignatureRole.CAPTAIN_AWAY->config.teams[role.team].players.singleOrNull{it.captain}?.name.orEmpty()
        SignatureRole.COACH_HOME,SignatureRole.COACH_AWAY->config.teams[role.team].staff.getOrElse(0){""}
        SignatureRole.SCORER->config.officials.getOrElse(2){""}
        SignatureRole.SECOND_REFEREE->config.officials.getOrElse(1){""}
        SignatureRole.FIRST_REFEREE->config.officials.getOrElse(0){""}
    }.trim()
    fun start(journal:Journal):Event?=journal.events.firstOrNull{it.kind==Kind.START_SET}
    fun toss(journal:Journal):Event?=start(journal) ?: journal.openingToss?.let{Event(Kind.START_SET,server=it.server,left=it.left)}
    // Exclude display colours, library candidates and OCR/import bookkeeping from signed match data.
    private fun configPayload(config:MatchConfig)=gson.toJson(config.copy(teams=config.teams.map{
        it.copy(colorKey=null,roster=null,staffRoster=null,historicalRoster=null)
    }))
    private fun hash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    private fun fingerprint(journal:Journal,role:SignatureRole,toss:Event?):String {
        val config=configPayload(journal.config)
        if(role.preliminary) {
            require(toss!=null && toss.server in 0..1 && toss.left in 0..1){"Completa il sorteggio prima delle firme."}
            return hash("pre-v1|${role.name}|$config|${toss.server}|${toss.left}")
        }
        val initial=required(journal.config,true).map{current(journal,it)}
        val previous=required(journal.config,false).takeWhile{it!=role}.map{current(journal,it)}
        return hash("post-v1|${role.name}|$config|${gson.toJson(journal.events)}|${gson.toJson(initial)}|${gson.toJson(previous)}")
    }
    fun current(journal:Journal,role:SignatureRole,toss:Event?=toss(journal)):MatchSignature? {
        if(role !in required(journal.config,role.preliminary))return null
        if(!role.preliminary && !MatchEngine.replay(journal.config,journal.events).finished(journal.config))return null
        if(!role.preliminary && required(journal.config,false).takeWhile{it!=role}.any{current(journal,it)==null})return null
        val record=journal.signatures.orEmpty().singleOrNull{it.role==role.name} ?: return null
        val actualToss=toss
        return record.takeIf{it.signer==signer(journal.config,role) && it.contentHash==fingerprint(journal,role,actualToss)}
    }
    fun missing(journal:Journal,preliminary:Boolean,toss:Event?=toss(journal))=required(journal.config,preliminary).filter{current(journal,it,toss)==null}
    fun capture(journal:Journal,role:SignatureRole,strokes:List<List<SignaturePoint>>,aspectRatio:Float,toss:Event?=toss(journal),at:Long=System.currentTimeMillis()):Journal {
        require(role in required(journal.config,role.preliminary)){"Firma non prevista per questa gara."}
        val name=signer(journal.config,role)
        require(name.isNotBlank()){ "Completa il nominativo nei Dati prima di raccogliere la firma." }
        if(role.preliminary)require(start(journal)==null){"Le firme preliminari si raccolgono prima dell’inizio della gara."}
        else {
            require(MatchEngine.replay(journal.config,journal.events).finished(journal.config)){"Le firme finali si raccolgono a gara terminata."}
            require(required(journal.config,false).takeWhile{it!=role}.all{current(journal,it)!=null}){"Raccogli prima le firme precedenti nell’ordine indicato."}
        }
        val record=MatchSignature(role.name,name,at,fingerprint(journal,role,toss),aspectRatio,strokes,toss?.server?:0,toss?.left?:0)
        val next=journal.copy(signatures=journal.signatures.orEmpty().filter{it.role!=role.name}+record,openingToss=if(role.preliminary)OpeningToss(toss!!.server,toss.left)else journal.openingToss)
        validate(next)
        return next
    }
    fun remove(journal:Journal,role:SignatureRole)=journal.copy(signatures=journal.signatures.orEmpty().filter{it.role!=role.name})
    fun validate(journal:Journal) {
        journal.openingToss?.let{require(it.server in 0..1 && it.left in 0..1){"Sorteggio non valido."}}
        val records=journal.signatures.orEmpty()
        require(records.size<=SignatureRole.entries.size && records.map{it.role}.distinct().size==records.size){"Firme duplicate o troppe firme."}
        records.forEach { r->
            require(r.role in SignatureRole.entries.map{it.name} && !r.signer.isNullOrBlank() && r.signer.length<=200){"Firmatario non valido."}
            require(r.contentHash?.matches(Regex("[a-f0-9]{64}"))==true && r.signedAt>0 && r.firstServer in 0..1 && r.initialLeft in 0..1){"Dati firma non validi."}
            require(r.aspectRatio.isFinite() && r.aspectRatio in .1f..20f){"Proporzioni firma non valide."}
            require(!r.strokes.isNullOrEmpty() && r.strokes.size<=128){"Firma vuota o troppo complessa."}
            require(r.strokes.all{!it.isNullOrEmpty()} && r.strokes.sumOf{it.size} in 2..4000){"Firma vuota o troppo lunga."}
            val points=r.strokes.flatten()
            require(points.all{it.x.isFinite() && it.y.isFinite() && it.x in 0f..1f && it.y in 0f..1f}){"Coordinate firma non valide."}
            require(points.maxOf{it.x}-points.minOf{it.x}+points.maxOf{it.y}-points.minOf{it.y}>.05f){"Traccia una firma nel riquadro."}
        }
    }
}
