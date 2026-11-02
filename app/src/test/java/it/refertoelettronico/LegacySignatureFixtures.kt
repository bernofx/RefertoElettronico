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
            record.copy(contentHash=hash,bindingVersion=null)
        })
    }
}
