package it.refertoelettronico.model

import com.google.gson.Gson
import java.security.MessageDigest

/** A drawn signature, not a cryptographic or qualified digital signature. Coordinates are normalized. */
data class OpeningToss(val server:Int,val left:Int)
data class SignaturePoint(val x:Float,val y:Float)
data class MatchSignature(val role:String,val signer:String,val signedAt:Long,val contentHash:String,
    val aspectRatio:Float,val strokes:List<List<SignaturePoint>>,val firstServer:Int=0,val initialLeft:Int=0,val bindingVersion:Int?=null,val originalContentHash:String?=null,val reviewHash:String?=null)
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
    private fun legacyPreliminaryFingerprintV2(config:MatchConfig,role:SignatureRole,toss:Event?):String {
        require(toss!=null && toss.server in 0..1 && toss.left in 0..1){"Completa il sorteggio prima delle firme."}
        val team=config.teams[role.team].copy(colorKey=null,roster=null,staffRoster=null,historicalRoster=null)
        // A captain/coach approves their own team and match/toss data, not the officials' administration.
        val scope=listOf(config.competition,config.season,config.matchNumber,config.phase,config.location,config.venue,
            config.date,config.scheduledTime,config.format.name,config.maxSubstitutions,team,toss.server,toss.left)
        return hash("pre-v2|${role.name}|${gson.toJson(scope)}")
    }
    private fun legacyPreliminaryFingerprint(config:MatchConfig,role:SignatureRole,toss:Event):String =
        hash("pre-v1|${role.name}|${configPayload(config)}|${toss.server}|${toss.left}")
    private fun teamData(team:Team)=team.copy(colorKey=null,roster=null,staffRoster=null,historicalRoster=null)
    private fun evidence(record:MatchSignature?)=record?.let{listOf(it.role,it.signer,it.signedAt,it.aspectRatio,it.strokes,it.firstServer,it.initialLeft)}
    private fun reviewFingerprint(journal:Journal,role:SignatureRole,toss:Event?):String {
        val config=journal.config
        val scope=if(role.preliminary) {
            require(toss!=null && toss.server in 0..1 && toss.left in 0..1){"Completa il sorteggio prima delle firme."}
            listOf(teamData(config.teams[role.team]),config.format.name,config.maxSubstitutions,toss.server,toss.left)
        } else listOf(config.teams.map(::teamData),config.format.name,config.maxSubstitutions,journal.events,
            required(config,true).map{evidence(current(journal,it))},
            required(config,false).takeWhile{it!=role}.map{evidence(current(journal,it))})
        return hash("review-v3|${role.name}|${signer(config,role)}|${gson.toJson(scope)}")
    }
    /** Historical ink remains usable for its named actor. Content changes are review notices, not deletion. */
    fun current(journal:Journal,role:SignatureRole,toss:Event?=toss(journal)):MatchSignature? {
        if(role !in required(journal.config,role.preliminary))return null
        if(!role.preliminary && !MatchEngine.replay(journal.config,journal.events).finished(journal.config))return null
        return journal.signatures.orEmpty().singleOrNull{it.role==role.name && it.signer==signer(journal.config,role)}
    }
    fun needsReview(journal:Journal,role:SignatureRole,toss:Event?=toss(journal)):Boolean {
        val record=current(journal,role,toss) ?: return false
        return if(record.reviewHash!=null)record.reviewHash!=reviewFingerprint(journal,role,toss)
            else legacyCurrent(journal,role,toss)==null
    }
    /** Verify old references solely to establish a baseline; never discard the underlying ink. */
    private fun legacyCurrent(journal:Journal,role:SignatureRole,toss:Event?):MatchSignature? {
        val record=current(journal,role,toss) ?: return null
        if(role.preliminary && toss==null)return null
        if(!role.preliminary && required(journal.config,false).takeWhile{it!=role}.any{legacyCurrent(journal,it,toss)==null})return null
        val expected=when {
            record.bindingVersion==3->reviewFingerprint(journal,role,toss)
            role.preliminary && record.bindingVersion==2->legacyPreliminaryFingerprintV2(journal.config,role,toss)
            role.preliminary->legacyPreliminaryFingerprint(journal.config,role,toss!!)
            else->{
                val initial=required(journal.config,true).map{legacyCurrent(journal,it,toss)}
                val previous=required(journal.config,false).takeWhile{it!=role}.map{legacyCurrent(journal,it,toss)}
                hash("post-v1|${role.name}|${configPayload(journal.config)}|${gson.toJson(journal.events)}|${gson.toJson(initial)}|${gson.toJson(previous)}")
            }
        }
        return record.takeIf{it.contentHash==expected}?.copy(reviewHash=null)
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
        val baseline=reviewFingerprint(journal,role,toss)
        val record=MatchSignature(role.name,name,at,baseline,aspectRatio,strokes,toss?.server?:0,toss?.left?:0,bindingVersion=3,reviewHash=baseline)
        val next=journal.copy(signatures=journal.signatures.orEmpty().filter{it.role!=role.name}+record,openingToss=if(role.preliminary)OpeningToss(toss!!.server,toss.left)else journal.openingToss)
        validate(next)
        return next
    }
    /** Preserve every stroke and original reference; descriptive match edits do not require new signatures. */
    fun withConfig(journal:Journal,config:MatchConfig):Journal {
        val toss=toss(journal)
        val records=journal.signatures?.map { record->
            val role=SignatureRole.valueOf(record.role)
            if(record.reviewHash==null && legacyCurrent(journal,role,toss)!=null) {
                record.copy(reviewHash=reviewFingerprint(journal,role,toss))
            } else record
        }
        return journal.copy(config=config,signatures=records)
    }
    fun remove(journal:Journal,role:SignatureRole)=journal.copy(signatures=journal.signatures.orEmpty().filter{it.role!=role.name})
    fun validate(journal:Journal) {
        journal.openingToss?.let{require(it.server in 0..1 && it.left in 0..1){"Sorteggio non valido."}}
        val records=journal.signatures.orEmpty()
        require(records.size<=SignatureRole.entries.size && records.map{it.role}.distinct().size==records.size){"Firme duplicate o troppe firme."}
        records.forEach { r->
            require(r.bindingVersion==null || r.bindingVersion in 1..3){"Versione firma non supportata."}
            require(r.reviewHash==null || r.reviewHash.matches(Regex("[a-f0-9]{64}"))){"Riferimento verifica firma non valido."}
            require(r.bindingVersion!=3 || r.reviewHash!=null){"Riferimento verifica firma mancante."}
            require(r.originalContentHash==null || r.originalContentHash.matches(Regex("[a-f0-9]{64}"))){"Riferimento originale firma non valido."}
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
