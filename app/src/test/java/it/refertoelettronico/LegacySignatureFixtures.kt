package it.refertoelettronico

import com.google.gson.Gson
import it.refertoelettronico.model.*
import java.security.MessageDigest

/** Reproduce the exact pre-v1 binding shipped in 0.7.0/0.7.1, independently of the new implementation. */
internal object LegacySignatureFixtures {
    val ink=listOf(listOf(SignaturePoint(.1f,.5f),SignaturePoint(.3f,.1f),SignaturePoint(.45f,.8f),SignaturePoint(.9f,.3f)))
    fun preliminary(config:MatchConfig=Demo.config().copy(officials=Demo.config().officials.with(0,""),officialCities=Demo.config().officialCities.with(0,"")),
                    toss:Event=Demo.start()):Journal {
        var j=Journal(config=config)
        Signatures.required(config,true).forEach{j=Signatures.capture(j,it,ink,3f,toss,at=1_000)}
        val clean=config.copy(teams=config.teams.map{it.copy(colorKey=null,roster=null,staffRoster=null,historicalRoster=null)})
        val payload=Gson().toJson(clean)
        return j.copy(signatures=j.signatures!!.map { record->
            val value="pre-v1|${record.role}|$payload|${toss.server}|${toss.left}"
            val hash=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
            record.copy(contentHash=hash,bindingVersion=null,reviewHash=null)
        })
    }
    fun preliminaryV2(config:MatchConfig=Demo.config(),toss:Event=Demo.start()):Journal {
        val j=preliminary(config,toss);val gson=Gson()
        return j.copy(signatures=j.signatures!!.map{record->
            val role=SignatureRole.valueOf(record.role)
            val team=config.teams[role.team].copy(colorKey=null,roster=null,staffRoster=null,historicalRoster=null)
            val scope=listOf(config.competition,config.season,config.matchNumber,config.phase,config.location,config.venue,
                config.date,config.scheduledTime,config.format.name,config.maxSubstitutions,team,toss.server,toss.left)
            val value="pre-v2|${role.name}|${gson.toJson(scope)}"
            val hash=MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
            record.copy(contentHash=hash,bindingVersion=2)
        })
    }
    fun fullySigned(events:List<Event>,config:MatchConfig=Demo.config(),preliminaryVersion:Int=1):Journal {
        var j=(if(preliminaryVersion==2)preliminaryV2(config)else preliminary(config)).copy(events=events)
        val gson=Gson()
        val clean=j.config.copy(teams=j.config.teams.map{it.copy(colorKey=null,roster=null,staffRoster=null,historicalRoster=null)})
        for(role in Signatures.required(j.config,false)) {
            val initial=Signatures.required(j.config,true).map{Signatures.current(j,it)}
            val previous=Signatures.required(j.config,false).takeWhile{it!=role}.map{Signatures.current(j,it)}
            val payload="post-v1|${role.name}|${gson.toJson(clean)}|${gson.toJson(j.events)}|${gson.toJson(initial)}|${gson.toJson(previous)}"
            val hash=MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
            j=Signatures.capture(j,role,ink,3f,at=2_000_000)
            j=j.copy(signatures=j.signatures!!.map{if(it.role==role.name)it.copy(contentHash=hash,bindingVersion=null,reviewHash=null)else it})
        }
        return j
    }
}
