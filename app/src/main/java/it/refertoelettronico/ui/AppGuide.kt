package it.refertoelettronico.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Update these steps and increment revision when a substantial user flow changes. */
object AppGuide {
    const val revision=1
    const val updatedFor="0.6.8"
    data class Step(val title:String,val body:String,val tip:String,val symbol:UiSymbol)
    val steps=listOf(
        Step("Prepara o importa la gara","Dalla pagina iniziale scegli Prepara una nuova gara. Inserisci dati e squadre, oppure apri i dati ricevuti da VolleyAlerts e premi Rivedi dati.","Puoi provare i comandi su una gara dimostrativa, separata dalle tue partite.",UiSymbol.COURT),
        Step("Scegli le presenti","Spunta Presente solo per le atlete di oggi. Controlla numeri, capitana K e liberi L. Le rose storiche sono candidate: non sono già una convocazione. Conferma separatamente lo staff e gli incarichi di servizio.","La matita apre le correzioni. La ricerca trova nome o numero; la bozza viene salvata automaticamente. Poi premi Salva gara o Salva modifiche per passare alle formazioni.",UiSymbol.PEOPLE),
        Step("Sorteggio e formazioni","Indica chi serve per primo e chi è a sinistra guardando dal tavolo del segnapunti. Riporta il tagliando nelle zone 4–3–2 / 5–6–1, con la rete in alto, per entrambe le squadre.","Tieni premuto un numero e trascinalo, oppure tocca numero e zona. Poi conferma l’inizio del set.",UiSymbol.SWAP),
        Step("Segna i punti","Tocca +1 punto dalla parte della squadra che ha vinto l’azione. Colori e pulsanti seguono i lati del campo. Il pallone indica chi serve e le rotazioni avvengono automaticamente.","Durante la gara il campo è visto dal tavolo del segnapunti. Il resto della pagina scorre sotto i punteggi.",UiSymbol.ADD),
        Step("Timeout, cambi e libero","Usa i comandi sotto il campo per la squadra interessata. I contatori mostrano timeout e sostituzioni usati. Libero registra ingressi e uscite: i suoi rimpiazzi non consumano sostituzioni.","Controlla gli avvisi: può essere necessario far uscire il libero prima di segnare la prossima azione.",UiSymbol.CLOCK),
        Step("Correggi un errore","Annulla ripristina anche servizio, formazioni e rotazioni; Ripristina recupera l’azione. Nel Registro, Annulla da qui torna indietro di più operazioni. Prima del primo set puoi riaprire Correggi dati e rose.","Dopo l’inizio, per cambiare numeri, presenze o ruoli occorre annullare fino all’inizio. I nomi restano correggibili da Dati.",UiSymbol.EDIT),
        Step("Riprendi e salva il referto","Le gare si salvano sul dispositivo e si riaprono dalle schede iniziali. In Referto genera il PDF, poi salvalo o condividilo. Se la gara è ancora in corso, il documento sarà provvisorio.","Esporta anche la copia JSON per conservarla o trasferirla. Disinstallare elimina gli archivi locali; le firme vanno apposte sulla stampa.",UiSymbol.DOCUMENT)
    )
}

class GuideProgress(context:Context) {
    private val preferences=context.getSharedPreferences("app-guide",Context.MODE_PRIVATE)
    val needsIntroduction:Boolean get()=preferences.getInt("seen-revision",0)<AppGuide.revision
    fun markSeen(){preferences.edit().putInt("seen-revision",AppGuide.revision).apply()}
}

@Composable
fun GuideOverlay(onClose:()->Unit) {
    var step by rememberSaveable{mutableIntStateOf(0)}
    val item=AppGuide.steps[step.coerceIn(AppGuide.steps.indices)]
    val scroll=rememberScrollState()
    LaunchedEffect(step){scroll.scrollTo(0)}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.widthIn(max=520.dp).fillMaxWidth(.94f).heightIn(max=640.dp).fillMaxHeight(.88f),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("Come usare Referto Volley",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    TextButton(onClick=onClose,modifier=Modifier.testTag("guide-close")){Text("Salta")}
                }
                LinearProgressIndicator(progress={ (step+1f)/AppGuide.steps.size },modifier=Modifier.fillMaxWidth())
                Text("${step+1} di ${AppGuide.steps.size}",style=MaterialTheme.typography.labelMedium)
                Column(Modifier.weight(1f).verticalScroll(scroll),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.medium) {
                        Box(Modifier.size(64.dp),contentAlignment=Alignment.Center){UiGlyph(item.symbol,Modifier.size(32.dp),MaterialTheme.colorScheme.primary)}
                    }
                    Text(item.title,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.testTag("guide-step-title"))
                    Text(item.body,style=MaterialTheme.typography.bodyLarge)
                    Notice(item.tip)
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(step>0)OutlinedButton(onClick={step--},modifier=Modifier.weight(1f).testTag("guide-previous")){Text("Indietro")}
                    Button(onClick={if(step<AppGuide.steps.lastIndex)step++ else onClose()},modifier=Modifier.weight(1f).testTag("guide-next")) {
                        Text(if(step==AppGuide.steps.lastIndex)"Ho capito" else "Avanti")
                    }
                }
            }
        }
    }
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
