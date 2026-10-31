package it.refertoelettronico.storage

import com.google.gson.GsonBuilder
import it.refertoelettronico.model.MatchConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class SetupDraft(val config:MatchConfig,val matchId:String?=null,val version:Int=1)

/** Incomplete data live outside the match archive and never start a match. */
class DraftStore(private val file:File) {
    private val gson=GsonBuilder().create()
    fun load():SetupDraft? {
        if(!file.exists())return null
        require(file.length()<=2_000_000){"Bozza troppo grande."}
        val value=gson.fromJson(file.readText(),SetupDraft::class.java)
        require(value.version==1 && value.config.teams.size==2){"Bozza non valida."}
        return value
    }
    fun save(draft:SetupDraft) {
        val data=gson.toJson(draft).toByteArray(Charsets.UTF_8)
        require(data.size<=2_000_000){"Bozza troppo grande."}
        file.parentFile!!.mkdirs()
        val temp=File(file.parentFile,file.name+".tmp")
        FileOutputStream(temp).use{it.write(data);it.fd.sync()}
        try{Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE)}
        catch(_:java.nio.file.AtomicMoveNotSupportedException){Files.move(temp.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING)}
    }
    fun clear(){Files.deleteIfExists(file.toPath())}
}
