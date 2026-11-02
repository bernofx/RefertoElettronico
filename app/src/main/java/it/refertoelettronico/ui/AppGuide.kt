package it.refertoelettronico.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Update the shared walkthrough and increment revision when a substantial user flow changes. */
object AppGuide {
    const val revision=2
    const val updatedFor="0.6.9"
    enum class Screen { HOME, DATA, ROSTER, LINEUP, LIVE, TIMEOUT, SUB, LIBERO, HISTORY, NOTE, SANCTION, PDF }
    data class Step(val title:String,val body:String,val tip:String,val screen:Screen,val target:String,val phase:Int=0)
    val steps=listOf(
        Step("Prepara una gara","Questo è il pulsante reale da cui iniziare: inserisci i dati oppure apri quelli ricevuti da VolleyAlerts.","Qui useremo soltanto dati dimostrativi. Le tue gare e le tue bozze restano al sicuro.",Screen.HOME,"home-new",0),
        Step("Controlla i dati gara","Verifica numero, data, ora e palestra. Puoi importare questi dati da VolleyAlerts senza ricopiarli.","Nelle altre schede trovi le due squadre e gli ufficiali.",Screen.DATA,"match-data",0),
        Step("Spunta le presenti","Per ogni squadra seleziona chi partecipa oggi. Controlla numeri, capitana K e liberi L; la matita apre le correzioni.","Puoi riutilizzare una rosa salvata e cambiare solo le differenze, oppure importare i CAMP3. Verifica sempre il risultato OCR.",Screen.ROSTER,"presence",0),
        Step("Conferma dati e rose","Salva dopo aver controllato entrambe le squadre e lo staff. Passerai alla preparazione del primo set.","Prima di iniziare il primo set puoi tornare a Correggi dati e rose. La preparazione viene salvata come bozza.",Screen.ROSTER,"save-setup",0),
        Step("Chi serve per primo?","Riporta la scelta comunicata dall’arbitro. Il colore, il nome completo e Casa/Ospiti distinguono le squadre.","Per il nostro esempio serve per prima Lago Volley.",Screen.LINEUP,"toss-serve",0),
        Step("Chi è a sinistra?","Guarda dal tavolo del segnapunti e indica la squadra inizialmente alla tua sinistra.","Nel primo set sarà A; l’altra sarà B. Non confondere questa vista con il tagliando delle formazioni.",Screen.LINEUP,"toss-left",0),
        Step("Riporta il tagliando","Le zone seguono il foglietto dell’allenatore: 4–3–2 davanti, 5–6–1 dietro, con la rete in alto.","Trascina i numeri della rosa negli slot, oppure tocca prima il numero e poi la zona. Un numero già in campo scambia posto. Ripeti per entrambe le squadre.",Screen.LINEUP,"lineup-grid",0),
        Step("Inizia il set","Dopo aver completato sorteggio e sei posizioni per squadra, conferma l’inizio.","Nel tutorial le formazioni sono già compilate: ora entriamo in una gara di esempio.",Screen.LINEUP,"start-set",0),
        Step("Assegna un punto","Premi +1 punto dalla parte di chi ha vinto l’azione. I pulsanti e i colori seguono il lato delle squadre in campo.","Con Avanti assegniamo un punto a Stella Como, che finora era in ricezione.",Screen.LIVE,"point-1",1),
        Step("Servizio e rotazione","Stella Como ora serve: osserva la rotazione e il pallone sulla battitrice. Il campo è visto dal tavolo del segnapunti.","Le rotazioni sono automatiche a ogni cambio di servizio. Non ruotare manualmente dopo un punto della squadra già al servizio.",Screen.LIVE,"court",2),
        Step("Registra un timeout","Usa il Timeout della squadra che lo richiede. Il contatore mostra quelli già utilizzati nel set.","Per l’esempio apriamo la richiesta di Stella Como.",Screen.LIVE,"timeout-1",2),
        Step("Conferma il timeout","Controlla squadra e punteggio, poi premi Registra. L’app conserva il punteggio della richiesta e mostra il conto alla rovescia.","Il timeout non cambia servizio né posizioni. Sono disponibili due timeout per set.",Screen.TIMEOUT,"operation-form",2),
        Step("Esegui una sostituzione","Apri Cambi per la squadra interessata. Il contatore indica le sostituzioni già registrate.","Distingui le sostituzioni ordinarie dai rimpiazzi del libero.",Screen.LIVE,"sub-1",3),
        Step("Scegli chi esce e chi entra","Nel modulo seleziona il numero che esce dal campo e quello che entra dalla panchina; controlla e registra.","L’app verifica i vincoli e memorizza il punteggio del cambio. Nell’esempio entra il numero 7.",Screen.SUB,"operation-form",3),
        Step("Gestisci il libero","Libero apre ingressi e uscite della squadra scelta. I rimpiazzi non consumano sostituzioni.","Nel prossimo passaggio entra il libero numero 12 di Stella Como.",Screen.LIVE,"libero-1",4),
        Step("Conferma il rimpiazzo","Seleziona il libero e la giocatrice rimpiazzata in seconda linea. Per il rientro usa Registra uscita del libero.","Gli avvisi aiutano a rispettare posizione e intervallo tra i rimpiazzi. Segui sempre le decisioni dell’arbitro.",Screen.LIBERO,"operation-form",4),
        Step("Il libero è in campo","Il numero 12 con L occupa la posizione della giocatrice rimpiazzata. La formazione di base resta memorizzata.","Il pallone continua a indicare la battitrice; i colori identificano le squadre.",Screen.LIVE,"court",5),
        Step("Annulla un errore","Annulla torna indietro di un’operazione, ricostruendo anche punteggio, servizio, posizioni e libero.","Con Avanti annulliamo l’ultimo ingresso del libero: tornerà la giocatrice precedente.",Screen.LIVE,"undo",5),
        Step("Ripristina se necessario","L’ingresso è stato annullato. Ripristina permette di recuperarlo, insieme al resto dello stato della gara.","Con Avanti ripristiniamo l’ingresso. Se registri una nuova azione dopo Annulla, abbandoni le operazioni annullate.",Screen.LIVE,"redo",6),
        Step("Correggi più operazioni","Il Registro contiene la cronologia. Annulla da qui elimina da quel punto in avanti; poi reinserisci le operazioni corrette.","Servizio e rotazioni vengono ricostruiti. Controlla sempre fino a quale momento stai tornando.",Screen.HISTORY,"history",7),
        Step("Aggiungi un’osservazione","Trascrivi le annotazioni e le decisioni comunicate dall’arbitro e premi Registra.","L’esempio qui sotto finirà nelle osservazioni del PDF, con il contesto della gara.",Screen.NOTE,"operation-form",7),
        Step("Registra una sanzione","Da Altre operazioni apri Sanzione: scegli squadra, provvedimento dell’arbitro e numero o ruolo del destinatario.","Un avvertimento giallo non assegna punti. Una penalizzazione assegna automaticamente punto e servizio agli avversari. Qui usiamo un avvertimento di esempio.",Screen.SANCTION,"operation-form",8),
        Step("Altre decisioni arbitrali","Qui trovi anche sostituzione eccezionale, ridesignazione libero, nuova capitana, inabilità, interruzione e ripresa, termine anticipato.","Usa queste operazioni quando richiesto dall’arbitro, riportando la motivazione. Nel tutorial completiamo ora i restanti set con azioni simulate.",Screen.LIVE,"other",9),
        Step("Genera il referto finale","La gara dimostrativa è terminata 3–0. Nella scheda Referto, Genera referto PDF compila il modello cartaceo classico FIPAV.","Con Avanti generiamo davvero il PDF di questa esercitazione. I dati sono fittizi; nessuna partita viene aggiunta al tuo archivio.",Screen.PDF,"pdf-generate",10),
        Step("Ecco il PDF compilato","Questa è l’anteprima effettiva del referto appena generato: riporta squadre, formazioni, punteggi e operazioni dell’esercitazione.","Il PDF completo include il registro cronologico e gli allegati necessari. Le firme si appongono sulla stampa; una gara ancora in corso produce un documento provvisorio.",Screen.PDF,"pdf-preview",10),
        Step("Salva o condividi","Al termine della tua gara usa Salva PDF o Condividi. Salva anche la gara modificabile per conservarne una copia JSON.","L’esercitazione finisce qui: tornerai alla tua schermata senza modifiche. La guida si riapre dalla voce Guida e viene aggiornata quando cambiano i flussi principali.",Screen.PDF,"pdf-export",10),
    )
}

class GuideProgress(context:Context) {
    private val preferences=context.getSharedPreferences("app-guide",Context.MODE_PRIVATE)
    val needsIntroduction:Boolean get()=preferences.getInt("seen-revision",0)<AppGuide.revision
    fun markSeen(){preferences.edit().putInt("seen-revision",AppGuide.revision).apply()}
}

@Composable
fun GuideScreen(onStart:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Guida a Referto Volley",style=MaterialTheme.typography.headlineSmall)
        Text("Aggiornata per la versione ${AppGuide.updatedFor}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick=onStart,modifier=Modifier.fillMaxWidth().testTag("guide-restart")){Text("Avvia tutorial guidato")}
        AppGuide.steps.forEachIndexed{i,item->
            Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("${i+1}. ${item.title}",style=MaterialTheme.typography.titleMedium)
                    Text(item.body,style=MaterialTheme.typography.bodyMedium)
                    Text(item.tip,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
