package it.refertoelettronico.storage

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.refertoelettronico.model.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Each match has its own file. A failed write never replaces the in-memory accepted journal. */
class MatchStore(private val directory: File) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    init { check(directory.mkdirs() || directory.isDirectory) }
    fun encode(journal: Journal): String = gson.toJson(journal)
    fun decode(json: String): Journal {
        require(json.length <= 20_000_000) { "Archivio troppo grande." }
        val journal = gson.fromJson(json, Journal::class.java) ?: error("Archivio vuoto.")
        require(journal.version == 1) { "Versione archivio non supportata." }
        require(journal.events.size + journal.redo.size <= 100_000) { "Troppi eventi nell'archivio." }
        Signatures.validate(journal)
        MatchEngine.replay(journal.config, journal.events + journal.redo)
        return journal
    }
    fun create(journal: Journal): String = UUID.randomUUID().toString().also { save(it, journal) }
    fun save(id: String, journal: Journal) {
        require(id.matches(Regex("[a-zA-Z0-9-]+")))
        require(journal.version==1) { "Versione archivio non supportata." }
        Signatures.validate(journal)
        MatchEngine.replay(journal.config,journal.events+journal.redo)
        val target = File(directory, "$id.json")
        val temp = File(directory, "$id.tmp")
        FileOutputStream(temp).use { stream ->
            stream.write(encode(journal).toByteArray(Charsets.UTF_8)); stream.fd.sync()
        }
        try { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING) }
    }
    fun delete(id:String) {
        require(id.matches(Regex("[a-zA-Z0-9-]+"))){"Identificativo gara non valido."}
        Files.deleteIfExists(File(directory,"$id.json").toPath())
    }
    fun load(id: String): Journal = decode(File(directory, "$id.json").readText())
    fun list(): List<Pair<String, Result<Journal>>> = directory.listFiles().orEmpty()
        .filter { it.extension == "json" }.sortedByDescending { it.lastModified() }.map { it.nameWithoutExtension to runCatching { load(it.nameWithoutExtension) } }
}
