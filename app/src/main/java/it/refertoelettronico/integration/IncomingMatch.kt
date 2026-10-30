package it.refertoelettronico.integration

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import it.refertoelettronico.model.MatchConfig

object IncomingMatch {
    fun accepts(intent:Intent)=intent.action in listOf(Intent.ACTION_SEND,Intent.ACTION_VIEW,MatchSetupImport.ACTION)

    fun read(context:Context,intent:Intent):MatchConfig {
        require(accepts(intent)){"Richiesta non supportata."}
        val uri=if(intent.action==Intent.ACTION_SEND) {
            if(Build.VERSION.SDK_INT>=33)intent.getParcelableExtra(Intent.EXTRA_STREAM,Uri::class.java)
            else @Suppress("DEPRECATION") (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
        } else intent.data
        if(intent.action==Intent.ACTION_VIEW && uri?.scheme==MatchSetupLink.SCHEME) {
            return MatchSetupLink.decode(uri.toString())
        }
        require(uri!=null && uri.scheme=="content"){"Condividi un file JSON tramite un URI content:// con permesso di lettura."}
        return context.contentResolver.openInputStream(uri)?.use{MatchSetupImport.read(it)}
            ?: error("Impossibile leggere i dati della gara.")
    }
}
