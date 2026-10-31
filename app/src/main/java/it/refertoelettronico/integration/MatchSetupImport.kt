package it.refertoelettronico.integration

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import it.refertoelettronico.model.*
import java.io.InputStream
import java.util.UUID

/** Versioned pre-match exchange. Missing fields remain editable; no events or existing match IDs. */
object MatchSetupImport {
    const val ACTION = "it.refertoelettronico.IMPORT_MATCH"
    const val MIME = "application/vnd.it.refertoelettronico.match-setup+json"
    const val SCHEMA = "it.refertoelettronico.match-setup"
    const val MAX_BYTES = 1_000_000
    const val MAX_CANDIDATES = 60

    fun read(input: InputStream): MatchConfig {
        val bytes=java.io.ByteArrayOutputStream()
        val block=ByteArray(8192)
        while(true) {
            val n=input.read(block); if(n<0)break
            require(bytes.size()+n<=MAX_BYTES){"Dati gara troppo grandi (massimo 1 MB)."}
            bytes.write(block,0,n)
        }
        return decode(bytes.toString("UTF-8"))
    }

    fun decode(json: String): MatchConfig {
        require(json.toByteArray(Charsets.UTF_8).size<=MAX_BYTES){"Dati gara troppo grandi (massimo 1 MB)."}
        val root=JsonParser.parseString(json).asJsonObject
        require(root.text("schema")==SCHEMA){"Formato dati gara non riconosciuto."}
        require(root.integer("version",0)==1){"Versione dati gara non supportata."}
        val config=root.getAsJsonObject("config") ?: error("Dati della gara mancanti.")
        require(!config.has("events") && !config.has("redo")){"L'integrazione accetta solo dati prima della gara."}
        val teams=config.getAsJsonArray("teams") ?: error("Squadre mancanti.")
        require(teams.size()==2){"Servono due squadre, nell'ordine casa e ospite."}
        val parsed=teams.map { entry ->
            val team=entry.asJsonObject
            val players=team.getAsJsonArray("players")
            require(players==null || players.size()<=MAX_CANDIDATES){"Limite tecnico: massimo 60 candidate per squadra. Nessuna voce è stata importata."}
            val parsedPlayers=players?.map { item ->
                val p=item.asJsonObject
                Player(p.integer("number",0).also{require(it in 0..99){"Numero candidata fuori dall’intervallo 0–99."}},p.text("name"),p.flag("libero"),p.flag("captain"))
            }.orEmpty()
            val staff=team.getAsJsonArray("staff")
            val structured=staff!=null && staff.size()>0 && staff[0].isJsonObject
            require(staff==null || staff.size()<=if(structured)MAX_CANDIDATES else 5){"Troppi candidati staff (massimo 60) o incarichi tradizionali (massimo 5)."}
            val candidates=if(structured)staff!!.map{entry->
                require(entry.isJsonObject){"Non mescolare persone e testi nell’array staff."}
                val p=entry.asJsonObject;val code=p.text("roleCode");val scope=p.text("scope","UNKNOWN")
                StaffCandidate(id=UUID.randomUUID().toString(),sourceId=p.text("sourceId").takeIf{it.isNotBlank()},name=p.text("name"),
                    roleCode=code,roleLabel=p.text("roleLabel"),scope=scope,assignment=StaffReview.suggested(code,scope))
            }else null
            val result=Team(name=team.text("name"),players=emptyList(),staff=if(structured)List(5){""}else team.texts("staff",5),clubCode=team.text("clubCode"),
                colorKey=team.text("colorKey").takeIf{it.isNotBlank()},staffRoster=candidates,historicalRoster=players!=null)
            Rosters.withMembers(result,parsedPlayers.map{RosterMember(UUID.randomUUID().toString(),it,false)})
        }
        val defaults=MatchConfig(teams=parsed)
        val format=config.text("format",defaults.format.name)
        require(Format.entries.any{it.name==format}){"Formula della gara non riconosciuta."}
        return defaults.copy(competition=config.text("competition",defaults.competition),season=config.text("season",defaults.season),
            matchNumber=config.text("matchNumber"),phase=config.text("phase"),location=config.text("location"),venue=config.text("venue"),
            date=config.text("date"),scheduledTime=config.text("scheduledTime"),officials=config.texts("officials",7),
            officialCities=config.texts("officialCities",7),format=Format.valueOf(format),
            maxSubstitutions=config.integer("maxSubstitutions",8).also{require(it in listOf(6,8)){"Limite sostituzioni non valido."}},
            initialObservations=config.text("initialObservations"),fallbackReason=config.text("fallbackReason"))
    }

    private fun JsonObject.text(key:String,default:String=""):String {
        if(!has(key))return default
        val value=get(key)
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString){"$key deve essere un testo."}
        return value.asString.also{require(it.length<=4096){"$key troppo lungo."}}
    }
    private fun JsonObject.integer(key:String,default:Int):Int {
        if(!has(key))return default
        val value=get(key)
        require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber && value.asString.matches(Regex("-?[0-9]{1,9}"))){"$key deve essere un numero intero."}
        return value.asInt
    }
    private fun JsonObject.flag(key:String):Boolean {
        if(!has(key))return false
        val value=get(key)
        require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean){"$key deve essere true o false."}
        return value.asBoolean
    }
    private fun JsonObject.texts(key:String,size:Int):List<String> {
        if(!has(key))return List(size){""}
        val values=getAsJsonArray(key)
        require(values.size()<=size){"Troppi elementi in $key."}
        return List(size){i -> if(i>=values.size())"" else {
            val value=values[i]
            require(value.isJsonPrimitive && value.asJsonPrimitive.isString && value.asString.length<=4096){"Valore non valido in $key."}
            value.asString
        }}
    }
}
