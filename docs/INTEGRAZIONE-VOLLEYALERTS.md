# VolleyAlerts → Referto Volley: apertura di una nuova gara

Contratto di integrazione **versione 1**, disponibile da Referto Volley **0.3.0**.
Package Android destinatario: `it.refertoelettronico`. Android minimo: 8.0 (API 26).

## Obiettivo e comportamento

VolleyAlerts fornisce i dati della gara e i nomi delle squadre. **Non serve il CAMP3 e non servono i nominativi delle atlete per l'integrazione.** Referto Volley apre una bozza, mostra un riepilogo e chiede al refertista di rivederla. I dati vengono salvati soltanto quando il refertista preme Salva e supera i controlli della gara.

- È sempre una nuova gara: non si modifica né si sovrascrive una gara esistente.
- Non vengono importati punti, eventi, formazioni, sostituzioni o cronologie. Non vengono avviati set automaticamente.
- Le rose possono essere omesse. Il refertista inserisce poi i numeri di maglia e indica capitana e liberi. Servono due squadre con nome diverso e 6–14 giocatrici ciascuna, almeno sei non liberi, una capitana per squadra e al massimo due liberi. Con più di 12 giocatrici serve almeno un libero.
- I cognomi e nomi delle giocatrici sono facoltativi per iniziare e segnare la gara; si possono completare in seguito. Il PDF segnala i nominativi mancanti tra i dati da completare. Il modello cartaceo classico FIPAV prevede anche i nominativi nel riquadro SQUADRE; non si genera un CAMP3.
- Non ci sono accessi al server VolleyAlerts, credenziali condivise o permessi Internet richiesti dall'app destinataria. Il trasporto è locale tramite Android.
- Non c'è callback, restituzione del risultato o deduplicazione per ID VolleyAlerts. Ogni importazione accettata prepara una nuova gara. Il salvataggio/condivisione del PDF rimane un'azione del refertista.

## Trasporto Android

Scrivere un **file JSON UTF-8** (massimo **1.000.000 byte**) in un percorso esposto da un `FileProvider` di VolleyAlerts. Passare il suo URI **`content://`**, con permesso temporaneo di lettura. Non usare `file://`, URL HTTP/HTTPS, percorsi assoluti o `Intent.EXTRA_TEXT`.

Sono supportati:

| Metodo | Action | Dove passare l'URI |
| --- | --- | --- |
| Apertura diretta consigliata | `it.refertoelettronico.IMPORT_MATCH` | `intent.data` tramite `setDataAndType` |
| Condivisione Android | `android.intent.action.SEND` | `Intent.EXTRA_STREAM` |
| Apri con | `android.intent.action.VIEW` | `intent.data` tramite `setDataAndType` |

MIME accettati dai filtri Android: `application/vnd.it.refertoelettronico.match-setup+json` (consigliato) oppure `application/json`.

Per l'apertura diretta impostare `setPackage("it.refertoelettronico")`. Non serve conoscere il nome dell'Activity. Impostare anche `ClipData` con lo stesso URI per propagare correttamente il permesso.

### Codice Kotlin in VolleyAlerts

Il codice seguente presuppone un `FileProvider` di VolleyAlerts, non quello di Referto Volley. L'authority d'esempio `${applicationId}.files` va allineata a quella dichiarata da VolleyAlerts.

```kotlin
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

fun apriRefertoVolley(context: Context, jsonUtf8: String): Boolean {
    val bytes = jsonUtf8.toByteArray(Charsets.UTF_8)
    require(bytes.size <= 1_000_000) { "Dati gara troppo grandi" }
    val directory = File(context.cacheDir, "referto-setup").apply { mkdirs() }
    val file = File(directory, "gara-${UUID.randomUUID()}.json")
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files", // authority del provider di VolleyAlerts
        file
    )
    val intent = Intent("it.refertoelettronico.IMPORT_MATCH").apply {
        setPackage("it.refertoelettronico")
        setDataAndType(uri, "application/vnd.it.refertoelettronico.match-setup+json")
        clipData = ClipData.newRawUri("Dati gara", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        // Mostrare all'utente che deve installare/aggiornare Referto Volley >= 0.3.0.
        false
    }
}
```

Aggiungere al file XML dei percorsi del FileProvider di VolleyAlerts:

```xml
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <cache-path name="referto_setup" path="referto-setup/" />
</paths>
```

Se VolleyAlerts non ha già un FileProvider, dichiararlo dentro `<application>` (unire questa configurazione a quella esistente, evitando authority duplicate):

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.files"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

Mantenere il file disponibile mentre Referto Volley lo legge; non eliminarlo subito dopo `startActivity`. Usare nomi univoci; pulire i file vecchi in un momento successivo. Non è necessaria una configurazione `<queries>` se si chiama direttamente `startActivity` e si gestisce `ActivityNotFoundException`; se si vuole prima usare `resolveActivity`/`getPackageInfo`, dichiarare il package destinatario in `<queries>` secondo le regole di visibilità Android.

Per `ACTION_SEND`, impostare `type` al MIME indicato, aggiungere `putExtra(Intent.EXTRA_STREAM, uri)`, `clipData`, permesso di lettura e package destinatario. Non usare `setDataAndType` come sostituto di `EXTRA_STREAM` per SEND.

## JSON minimo per VolleyAlerts (senza rose)

```json
{
  "schema": "it.refertoelettronico.match-setup",
  "version": 1,
  "config": {
    "competition": "U14 Femminile - FIPAV Como",
    "season": "2026/27",
    "matchNumber": "123",
    "phase": "Girone A",
    "date": "31/10/2026",
    "scheduledTime": "15:30",
    "location": "Como",
    "venue": "Palestra di esempio",
    "teams": [
      { "name": "Squadra di casa", "clubCode": "" },
      { "name": "Squadra ospite", "clubCode": "" }
    ]
  }
}
```

L'array `teams` è **sempre nell'ordine casa, ospite**, indipendentemente dal sorteggio o dal lato del campo. Non assegnare A/B: Referto Volley lo fa al sorteggio. Per preparare una bozza il parser richiede soltanto `schema`, `version`, `config` e `config.teams` con esattamente due oggetti. Inviare i nomi delle squadre quando disponibili; i campi mancanti possono essere completati nel modulo.

Non inviare `null` nei campi conosciuti: omettere il campo se sconosciuto. I campi sconosciuti sono ignorati per consentire estensioni future; non hanno alcun effetto. Non inviare eventi dentro `config`: vengono rifiutati.

## Campi di config

| Campo | Tipo | Default se omesso / significato |
| --- | --- | --- |
| `teams` | array di 2 oggetti | Obbligatorio, casa poi ospite; struttura sotto |
| `competition` | stringa | `U14 Femminile - FIPAV Como` |
| `season` | stringa | `2026/27` |
| `matchNumber` | stringa | Vuota. Anche se numerico, usare una stringa per preservare eventuali zeri iniziali |
| `phase` | stringa | Vuota; girone/fase |
| `date` | stringa | Vuota; `gg/mm/aaaa`, es. `31/10/2026` |
| `scheduledTime` | stringa | Vuota; orario locale italiano a 24 ore, `HH:mm`, es. `15:30` |
| `location` | stringa | Vuota; comune/località |
| `venue` | stringa | Vuota; nome palestra/campo |
| `format` | stringa enum | `BEST_OF_FIVE`; anche `BEST_OF_THREE` oppure `THREE_FIXED` |
| `maxSubstitutions` | intero | `8`; ammessi soltanto `6` o `8` |
| `officials` | array di stringhe | Sette stringhe vuote; ordine: 1° arbitro, 2° arbitro, segnapunti, giudici di linea 1–4 |
| `officialCities` | array di stringhe | Sette stringhe vuote; stessi indici di `officials` |
| `initialObservations` | stringa | Vuota; osservazioni iniziali |
| `fallbackReason` | stringa | Vuota; motivo effettivo dell'uso del cartaceo al posto dell'elettronico, se già noto |

`BEST_OF_FIVE`: tre set vinti su cinque, quinto a 15. `BEST_OF_THREE`: due set vinti su tre, terzo a 15. `THREE_FIXED`: tre set obbligatori a 25. Tutti i set richiedono due punti di vantaggio. Per U14F Como usare `BEST_OF_FIVE`, otto sostituzioni. I default sono quelli del profilo iniziale U14F Como, non vengono dedotti dal testo di `competition`: specificare esplicitamente i valori per altre competizioni.

Per gli array di ufficiali sono ammessi fino a sette elementi; quelli mancanti in fondo diventano stringhe vuote. Per saltare un indice usare `""`, mai `null`. Limite per ogni stringa: 4096 caratteri.

## Campi di ciascuna squadra

| Campo | Tipo | Default / significato |
| --- | --- | --- |
| `name` | stringa | Vuota; denominazione della squadra |
| `clubCode` | stringa | Vuota; codice società |
| `colorKey` | stringa | Se assente/non riconosciuto: blu per casa, rosso per ospite |
| `players` | array di oggetti | Vuoto: nessuna atleta inventata. Massimo 14 |
| `staff` | array di stringhe | Cinque stringhe vuote; allenatore, vice allenatore, fisioterapista, medico, dirigente |

Colori: `BLUE`, `RED`, `GREEN`, `ORANGE`, `PURPLE`, `PINK`, `YELLOW`, `WHITE`, `BLACK`, `GREY`. La scelta si può correggere nell'app e segue la squadra nei cambi campo.

Ogni elemento di `players`, **soltanto se disponibile**, ha questa forma:

```json
{ "number": 7, "name": "Rossi Maria", "captain": false, "libero": false }
```

`number` è un intero, non una stringa; se omesso vale 0 da correggere nel modulo. Per salvare e iniziare la gara dev'essere tra 1 e 99, unico all'interno della squadra. `name` è una stringa libera cognome e nome (default vuota); non serve separarli. `captain` e `libero` sono booleani JSON (`true`/`false`), default `false`. Non dedurre o inventare nominativi, numeri e ruoli se VolleyAlerts non li possiede.

## Errori e verifica dell'integrazione

File non leggibile, formato/schema/versione errati, tipi incompatibili e dimensioni oltre 1 MB producono un messaggio nell'app; nessuna gara viene salvata. Dati incompleti o correggibili (ad esempio rose assenti, data errata, numeri duplicati) si aprono nel modulo e vengono controllati quando l'utente salva. Il limite sostituzioni e la formula non riconosciuti sono invece rifiutati già in lettura.

Da verificare tra le due app su un dispositivo reale:

1. Installare Referto Volley >= 0.3.0, chiamarlo da VolleyAlerts con il JSON minimo e un vero URI del suo FileProvider.
2. Controllare numero gara, casa/ospite, data, ora e palestra nel modulo. Le rose devono essere vuote se non inviate.
3. Ignorare l'importazione: nessuna nuova gara deve comparire nell'archivio.
4. Ripetere mentre una gara è aperta: accettare deve aprire una nuova bozza lasciando intatta quella salvata.
5. Completare i numeri di maglia e i ruoli, salvare e iniziare senza nomi. Completare eventualmente i nominativi in seguito.
6. Provare un JSON non valido e un URI senza permesso: l'app deve mostrare l'errore e non creare gare.
7. Controllare il comportamento con Referto Volley assente o con la vecchia versione (il chiamante deve proporre installazione/aggiornamento).

I test automatici del destinatario verificano decodifica, limiti, lettura tramite content URI, apertura della revisione e conservazione delle gare esistenti. Non sostituiscono la verifica dei permessi fra i due distinti package su telefono.
