package it.refertoelettronico.storage

import com.google.gson.GsonBuilder
import it.refertoelettronico.model.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

data class SavedRoster(val version:Int=1,val id:String,val label:String,val team:Team)

/** Separate from matches. Attendance is never reused silently in a new match. */
class RosterStore(private val directory:File) {
    private val gson=GsonBuilder().setPrettyPrinting().create()
    init{check(directory.mkdirs() || directory.isDirectory)}
    private fun validate(saved:SavedRoster) {
        require(saved.version==1 && saved.id.matches(Regex("[a-zA-Z0-9-]+"))){"Rosa non valida."}
        require(saved.label.isNotBlank() && saved.label.length<=120){"Inserisci un nome per la rosa (massimo 120 caratteri)."}
        require(saved.team.name.isNotBlank()){"Inserisci il nome della squadra."}
        require(saved.team.players.size in 1..60){"La rosa deve contenere da 1 a 60 giocatrici."}
        require(saved.team.players.all{it.number in 1..99}){"Completa i numeri di maglia prima di salvare la rosa."}
        require(saved.team.roster==null){"Formato rosa non valido."}
    }
    fun list():List<Pair<String,Result<SavedRoster>>> = directory.listFiles().orEmpty().filter{it.extension=="json"}.map{file->
        file.nameWithoutExtension to runCatching {
            require(file.length()<=1_000_000){"Rosa troppo grande."}
            val value=gson.fromJson(file.readText(),SavedRoster::class.java) ?: error("Rosa vuota.")
            validate(value);require(value.id==file.nameWithoutExtension);value
        }
    }.sortedBy{it.second.getOrNull()?.label?.lowercase().orEmpty()}
    fun save(label:String,team:Team,replaceId:String?=null):SavedRoster {
        val saved=SavedRoster(id=replaceId ?: UUID.randomUUID().toString(),label=label.trim(),team=Rosters.template(team))
        validate(saved)
        val encoded=gson.toJson(saved).toByteArray(Charsets.UTF_8)
        require(encoded.size<=1_000_000){"Rosa troppo grande."}
        val target=File(directory,"${saved.id}.json");val temp=File(directory,"${saved.id}.tmp")
        FileOutputStream(temp).use{stream->stream.write(encoded);stream.fd.sync()}
        try{Files.move(temp.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)}
        catch(_:java.nio.file.AtomicMoveNotSupportedException){Files.move(temp.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING)}
        return saved
    }
}
