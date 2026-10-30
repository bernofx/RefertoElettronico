package it.refertoelettronico.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import it.refertoelettronico.model.Event
import it.refertoelettronico.model.Kind

/** Audio is auxiliary: an unavailable output must never reject an already saved match action. */
class ActionFeedback(context: Context) : AutoCloseable {
    private val prefs=context.applicationContext.getSharedPreferences("feedback",Context.MODE_PRIVATE)
    private val manager=context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var generator: ToneGenerator?=null
    var enabled: Boolean=prefs.getBoolean("sounds_enabled",true)
        private set

    fun setEnabled(value: Boolean) {
        check(prefs.edit().putBoolean("sounds_enabled",value).commit()){ "Impossibile salvare la preferenza audio." }
        enabled=value
        if(value)play(Cue.CONFIRM) else runCatching{generator?.stopTone()}
    }
    fun play(cue: Cue) {
        if(!enabled || manager.ringerMode!=AudioManager.RINGER_MODE_NORMAL)return
        runCatching {
            val output=generator ?: ToneGenerator(AudioManager.STREAM_SYSTEM,45).also{generator=it}
            output.startTone(cue.tone,cue.millis)
        }
    }
    override fun close() {
        runCatching{generator?.stopTone();generator?.release()};generator=null
    }
}
enum class Cue(val tone: Int,val millis: Int) {
    POINT(ToneGenerator.TONE_PROP_BEEP,70), CONFIRM(ToneGenerator.TONE_PROP_ACK,100),
    TIMEOUT(ToneGenerator.TONE_PROP_PROMPT,140), UNDO(ToneGenerator.TONE_PROP_BEEP2,110),
    ERROR(ToneGenerator.TONE_PROP_NACK,140);
    companion object {
        fun forEvent(event:Event):Cue=when(event.kind) {
            Kind.POINT->POINT
            Kind.TIMEOUT->TIMEOUT
            else->CONFIRM
        }
    }
}
