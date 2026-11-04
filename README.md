# Referto Volley

App Android offline per assistere il segnapunti e produrre il **referto cartaceo classico FIPAV** quando eScoreVolley non è utilizzabile. Profilo iniziale: **Under 14 femminile, FIPAV Como, stagione 2026/27**.

[Scarica l'APK Android 0.9.1](https://raw.githubusercontent.com/bernofx/RefertoElettronico/main/downloads/RefertoVolley-0.9.1.apk). È una versione di prova firmata con chiave debug; richiede Android 8.0 o successivo.

## Utilizzo

1. **Prepara una nuova gara**: dati gara, distinte (numero, capitana e liberi), panchine e ufficiali. Puoi iniziare con i soli numeri e completare cognome e nome in seguito; il PDF segnala i nominativi mancanti. Registra il motivo dell'uso del cartaceo. Per ciascuna squadra puoi usare **Foto lista giocatrici**, fotografando da vicino la tabella e revisionando la rosa prima di sostituirla. Lo staff può essere letto con una seconda foto facoltativa.
2. Scegli un colore per ciascuna squadra nei suoi dati: pulsanti dei punti e numeri in campo useranno quel colore, anche al cambio campo. La scelta è modificabile durante la gara e inclusa nella copia JSON.
3. Scegli l’esito del sorteggio: **Chi serve per primo?** e **Chi è a sinistra del tavolo?**. Per ciascuna squadra riporta il tagliando visto dal fondo verso la rete: **4–3–2 / 5–6–1**. Tieni premuto un numero e trascinalo nella zona, oppure tocca numero e zona. Compila entrambe le formazioni e conferma l’inizio del set. La squadra inizialmente a sinistra è A sul referto; l'identità A/B resta invariata durante i cambi campo.
4. Usa **+1 punto**. Il campo mostra la formazione, ruota la squadra che conquista il servizio ed evidenzia il battitore. Il campo è visto dal tavolo del segnapunti, con rete verticale e formazioni opposte. Il pallone identifica la battitrice e le rotazioni sono animate; Annulla anima lo spostamento inverso.
5. Registra timeout, sostituzioni e ingressi/uscite del libero. I rimpiazzi del libero non consumano sostituzioni. Un messaggio segnala quando il libero deve uscire prima della prossima azione.
6. **Annulla** ripristina immediatamente l'ultima operazione. Dal **Registro**, “Annulla da qui” permette di tornare indietro di più eventi, anche oltre la fine di un set. **Ripristina** recupera gli eventi; registrare un nuovo evento scarta il ramo annullato.
7. Dopo il sorteggio raccogli le firme preliminari di capitane e allenatori presenti con **Raccogli / controlla firme**. In alternativa scegli esplicitamente di completarle sulla stampa. A gara terminata raccogli segnapunti, secondo arbitro se presente e primo arbitro nell’ordine proposto. Le firme raccolte vengono inserite nel PDF. Completare dati descrittivi conserva i tratti; variazioni a distinta o risultato sono segnalate senza cancellarli. [Regole e funzionamento delle firme](docs/FIRME-REFERTO.md).
8. Conferma le formazioni a ogni set. Il primo servizio e i lati si alternano; nel quinto è previsto un nuovo sorteggio e il cambio campo a 8.
9. In **Referto**, genera e salva/condividi il PDF. La prima pagina riproduce il foglio FIPAV; seguono registro e osservazioni completi. Le firme raccolte nell’app compaiono nei campi previsti; gli spazi mancanti restano da completare sulla stampa. Una gara non conclusa produce un PDF esplicitamente provvisorio.

Ogni operazione accettata viene salvata sul dispositivo con scrittura atomica. Dopo la chiusura dell'app la gara si riapre da **Le tue gare**. La copia JSON include dati, cronologia, operazioni ripristinabili e firme raccolte; importarla crea una nuova gara senza sovrascrivere l'originale. I numeri, i ruoli delle distinte e la formula sono modificabili prima dell'inizio; in seguito è necessario annullare fino all'inizio. I nomi e gli altri dati della gara restano correggibili.

Sono disponibili sanzioni (con punto automatico per penalizzazione), richieste improprie, sostituzioni eccezionali, inabilità, ridesignazione del libero, nuova capitana, osservazioni, interruzione/ripresa e termine anticipato per decisione arbitrale. Le decisioni eccezionali richiedono una motivazione. Il termine anticipato conserva il risultato acquisito e le osservazioni, senza inventare un risultato a tavolino.

La gara dimostrativa nella schermata iniziale usa esclusivamente dati fittizi.

I suoni confermano le operazioni registrate con successo: punto breve, timeout e conferme, annullamento distinto. L'icona dell'altoparlante nella barra superiore li attiva/disattiva; la scelta resta sul dispositivo. L'app rispetta le modalità silenziosa/vibrazione e il volume di sistema. Un'operazione rifiutata non emette il suono di successo. I suoni non richiedono rete né autorizzazioni per il microfono.

La versione **0.2.0** ha introdotto colori e audio. Le gare e le copie JSON della versione 0.1.0 restano compatibili; in assenza di colori salvati vengono usati blu e rosso. L'aggiornamento usa lo stesso application ID e la stessa firma di debug; non disinstallare l'app se vuoi conservare gli archivi locali.

La versione **0.3.0** fa seguire ai pulsanti dei punti e al riepilogo dei set i lati del campo, anche nel quinto set e con Annulla/Ripristina. Accetta dati pre-gara da VolleyAlerts tramite un JSON condiviso da Android: [specifica di integrazione e codice Kotlin](docs/INTEGRAZIONE-VOLLEYALERTS.md). VolleyAlerts può omettere le rose; si completano nell'app.

La versione **0.5.0** migliora l'OCR: foto con più dettagli, ricerca automatica della tabella e seconda lettura del ritaglio, righe raggruppate considerando l'inclinazione, selezione manuale della colonna delle maglie quando l'intestazione non viene riconosciuta. I nomi parziali e i numeri incerti/duplicati sono proposti da controllare. Per ogni squadra si può acquisire una foto ravvicinata della lista e una seconda foto facoltativa dello staff. Le intestazioni sono consigliate; senza quella delle maglie si può indicare la colonna a mano. Il riconoscimento va sempre revisionato, con capitana e liberi indicati manualmente. Funziona sul dispositivo senza connessione e non conserva le immagini nella gara. [Guida OCR e limiti](docs/OCR-CAMP3.md).

La versione **0.4.0** corregge il posizionamento dei campi del PDF: nomi e città degli ufficiali, giudici di linea, orari, durate, formazioni, punteggi e rose. Ogni testo ha un'area di scrittura delimitata; se un valore è troppo lungo viene abbreviato con puntini di sospensione, mantenendolo completo negli allegati. Le lettere A/B già stampate nei set 1–4 non vengono riscritte. Intestazione e distinte mantengono l'ordine casa/ospite anche quando il sorteggio assegna il lato destro alla squadra di casa. Il modello originale rimane alla sua risoluzione di stampa. [Esempio PDF di una gara dimostrativa completa](downloads/Esempio-referto-0.4.0.pdf). Stato delle verifiche nel [piano](docs/PIANO.md).

## Regole e documenti

Il [regolamento territoriale U14F](https://www.fipav.como.it/data-files/fipavcomo/dropbox/Regolamento%20U14F%20Stagione%202026-2027.pdf) prescrive tre set vinti su cinque, libero e secondo libero, rete a 2,15 m, e ammette l'eccezione al referto elettronico in assenza totale di connessione o per problemi del portale. Le modifiche federali in vigore dal 28 settembre 2026 portano a **otto sostituzioni per set**, mantenendo due timeout e i vincoli di abbinamento titolare/riserva.

Il PDF usa il modulo della figura 82 del Manuale nazionale del segnapunti cartaceo 2025/26, attualmente pubblicato dalla FIPAV. Elenchi, eventi e osservazioni che eccedono gli spazi del modulo sono conservati negli allegati. Il quinto set separa le registrazioni prima e dopo il cambio campo. Vedi [fonti e dettagli](docs/REGOLAMENTO.md).

Non è un'integrazione con eScoreVolley e non invia dati al Comitato. La procedura di consegna e il controllo finale restano quelli previsti dal Comitato e dagli ufficiali di gara. L'app registra le decisioni arbitrali; non sostituisce la direzione di gara, il CAMP3 o il rapporto dell'arbitro. Ripresa da una gara iniziata su un altro sistema: al momento si ricostruiscono gli eventi nel Registro; non è previsto l'import automatico da eScoreVolley. Gli esiti disciplinari a tavolino, i set annullati da rigiocare e le decisioni tecniche particolari vanno gestiti dall'arbitro sui documenti previsti, usando le osservazioni dell'app come supporto.

## Sviluppo

Dalla versione **0.6.0** è disponibile anche l’apertura da VolleyAlerts tramite collegamento browser/PWA (`refertovolley://import-match`), oltre al file JSON.

Android 8.0/API 26 o successivo. Kotlin 2.2.21, Compose, AGP 8.13.2, Gradle 8.13, JDK 21 e SDK 36. Nessuna chiave, servizio remoto o permesso Internet richiesto dall'app.

Nell'ambiente cloud predisposto:

```sh
source /workspace/android-setup/activate.sh
cd /workspace/RefertoElettronico
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
```

Fuori dal cloud: installare JDK e SDK, impostare `ANDROID_HOME` oppure il proprio `local.properties` con `sdk.dir`, poi usare il wrapper. Il wrapper verifica il checksum della distribuzione Gradle. Maven Google viene raggiunto tramite il suo endpoint `dl.google.com`; i repository sono Google Maven e Maven Central.

APK: `app/build/outputs/apk/debug/app-debug.apk`. I test JVM verificano il motore, la persistenza e le esportazioni; Robolectric verifica anche schermate Compose, creazione e rendering PDF. Gli esempi generati sono in `app/build/outputs/verification/`. La verifica su un telefono reale, compresi dialoghi di sistema per salvataggio/condivisione e stampa fisica, resta necessaria prima dell'uso a bordo campo.

Il codice del motore è separato dall'interfaccia: `model/MatchEngine.kt` applica gli eventi senza mutare lo stato precedente; undo/redo usano lo stesso replay. `storage/MatchStore.kt` conserva un file per gara. `pdf/SheetLayout.kt` costruisce le registrazioni e `PdfExporter.kt` le sovrappone al modello senza ricampionare l'immagine originale, incorporando il font nel documento.

Le gare contengono dati personali: l'app le conserva localmente, esclude il backup automatico e la migrazione del dispositivo, e condivide soltanto su comando dell'utente. Disinstallare l'app elimina gli archivi locali; salvare la copia JSON per conservarli.

La versione **0.6.1** rilegge la zona selezionata dalla **foto originale** e ridimensiona solo il ritaglio (fino a 3600 pixel), mentre l'anteprima può essere ridotta a 1800 pixel. Questo vale per rilettura automatica/manuale, colonna maglie e staff. Se il formato non consente lettura regionale, l'app usa una lettura alternativa a memoria limitata e segnala la limitazione. Inoltre adatta la lettura delle foto al CAMP3 con intestazione **N°**: non richiede la parola MAGLIA per riconoscere la prima colonna. La tabella dello staff, introdotta da Qualifica, resta distinta dalla rosa. Gli indicatori espliciti K/L1/L2 possono essere proposti nella revisione e vanno controllati. Il test del modello usa esclusivamente dati anonimizzati e non incorpora il CAMP3 fornito dall'utente.


La versione **0.6.2** rende compatta la distinta: ogni giocatrice occupa una riga, con spunta **Presente** e dettagli sotto **Modifica**. Solo le presenti sono disponibili nelle formazioni e nel referto; la rosa completa può superare le 14 giocatrici, mentre la distinta della gara mantiene i vincoli del regolamento. **Salva rosa** conserva una copia riutilizzabile sul dispositivo, comprese le assenti; **Carica rosa** permette di recuperarla oppure partire da una gara precedente. Il caricamento sostituisce i dati della squadra e lascia tutte le presenze da selezionare. Per aggiornare una rosa salvata usa lo stesso nome e conferma **Aggiorna rosa**. Le partite precedenti rimangono invariate.

Le vecchie gare mantengono tutte le giocatrici come presenti. La copia JSON di una gara include rosa completa e presenze; l’archivio separato delle rose è locale e viene eliminato disinstallando l’app. Dopo l’inizio della gara le presenze sono bloccate come numeri e ruoli. Nell’editor foto il ritaglio parte bloccato: attiva **Ritaglia** solo per modificarlo, oppure **Maglie** per indicare la colonna. Il pulsante **Leggi** rimane visibile sotto la foto senza dover scorrere. Colore e panchina sono espandibili per lasciare più spazio alla lista.


La versione **0.6.3** aggiorna la UI con un tema Material 3 coerente: palette verde/avorio, tipografia più leggibile, schede punteggio con numeri grandi e colori squadra, selettori tonali, icone vettoriali e dettagli della rosa accessibili dalla matita. La schermata iniziale separa nuova gara, dimostrazione e archivio. La foto usa un controllo a segmenti per blocco, ritaglio e maglie. [Anteprime dell’interfaccia](docs/UI.md).


La versione **0.6.4** introduce il tagliando formazioni con trascinamento e alternativa a tocchi. Le zone partono vuote; una giocatrice già assegnata scambia posto con quella nella zona di destinazione. Solo le presenti non libero e disponibili sono proposte. Nei set successivi puoi copiare esplicitamente la formazione del set precedente e correggerla secondo il nuovo tagliando. Primo servizio e lato del campo si scelgono in testa al primo e al quinto set; negli altri si alternano automaticamente. Il campo durante la gara resta visto dal tavolo del segnapunti.

La barra superiore e le schede punteggio sono compatte, con ultima azione e avvisi nella parte scorrevole. In **Le tue gare**, il cestino apre una conferma con squadre e data: **Elimina gara** rimuove soltanto la gara scelta e la sua cronologia dal dispositivo. Le rose salvate separatamente e i file JSON già esportati rimangono disponibili; una copia JSON permette di importare nuovamente una gara eliminata. Si possono eliminare anche gli archivi non leggibili.


La versione **0.6.5** adatta l’importazione alle rose cumulative VolleyAlerts: fino a 60 candidate per squadra, mai troncate né fuse, tutte inizialmente non presenti. Ricerca per nome/numero, errori accanto alle voci e controlli sulle sole selezionate. Supporta inoltre le proposte staff strutturate con revisione di panchina, incarichi di servizio e ruoli sconosciuti; il segnapunti è unico per gara. La preparazione viene salvata separatamente e può essere ripresa senza perdere chiavi, selezioni o modifiche. File JSON e link usano lo stesso parser e mantengono le soglie precedenti. Lo staff richiede anche l’esportazione corrispondente in VolleyAlerts, dipendenza ancora da verificare. [Contratto aggiornato e limiti](docs/INTEGRAZIONE-VOLLEYALERTS.md).


La versione **0.6.6** evita selettori indistinguibili quando le squadre condividono l’inizio del nome: sorteggio e scelta della formazione mostrano il nome completo su più righe, Casa/Ospiti, il colore e la selezione esplicita. La scelta selezionata usa come sfondo il colore assegnato alla squadra, con testo a contrasto, spunta e bordo per distinguere anche il bianco. Il conteggio della formazione è separato dal nome. Nelle schede punteggio compatte Casa/Ospiti precede il nome, così l’identità resta leggibile anche quando il nome deve essere abbreviato. I nomi originali nel JSON e nel PDF restano invariati.


La versione **0.6.7** aggiunge **Correggi dati e rose** nella preparazione del primo set. Anche Indietro di Android torna alla compilazione prima dell’inizio; nell’editor è presente un tasto Indietro visibile. **Salva modifiche** applica le correzioni alla stessa gara e ritorna alle formazioni, mantenendo sorteggio e posizioni ancora valide. Le zone con numeri non più disponibili vengono svuotate. Uscire dall’editor con modifiche non applicate richiede una scelta esplicita: la bozza rimane recuperabile. Dopo l’inizio restano i vincoli già previsti su numeri, ruoli e presenze.


Nella **0.6.7** la preparazione delle formazioni mantiene leggibili rete, zone e numeri anche per squadre bianche o con colori chiari: i colori restano quelli scelti, mentre testo e tratti usano un contrasto adeguato e i numeri trascinabili hanno un bordo. La pagina iniziale mostra le gare come schede con fascia colorata, raggruppate per data e ordinate per ora: campo, squadre su righe separate, categoria, numero gara e stato. In corso vengono mostrati punti e numero del set, a gara terminata i set vinti. Il cestino mantiene la conferma. La categoria è un’etichetta descrittiva e non modifica il regolamento.


La versione **0.8.0** guida la preparazione in quattro sezioni con stato di completamento e prossimo dato suggerito. I campi secondari sono raccolti in pannelli espandibili. Completare luogo o palestra conserva le firme; modifiche sportive vengono segnalate mantenendo il tratto. Le firme nel PDF usano più spazio, mentre i nomi della panchina destra rimangono fuori dalla colonna dei ruoli. [Firme e correzioni](docs/FIRME-REFERTO.md) · [Guida aggiornata](docs/GUIDA-E-TUTORIAL.md).

Per chi modifica l’app: aggiornare `AppGuide.steps` e `updatedFor` quando cambiano sostanzialmente i flussi; incrementare `AppGuide.revision` per riproporre una volta i contenuti aggiornati. Le patch estetiche non devono riattivare il tutorial.

La **0.8.1** compatta la barra della preparazione e mantiene leggibili le quattro schede anche sui telefoni stretti. Include tre palette, ognuna chiara o scura, e la modalità che segue il telefono. [Anteprime e scelta dei temi](docs/TEMI.md).

La **0.9.0** aggiunge i layout selezionabili **B · Cruscotto gara** e **C · Campo al centro**. Ogni layout ricorda il proprio tema chiaro/scuro/automatico, la palette e i colori principale e di accento. [Stili, istruzioni e schermate native](docs/TEMI.md).

La **0.9.1** ricostruisce le schermate B e C sui concept selezionati: dettagli B in pannelli, formazioni C con maglie e conferma fissa, punteggi e contatori dedicati e riepilogo firme/referto. Nei due layout le sezioni si raggiungono dal menu ⋮, con una barra superiore compatta. [Confronto B](docs/ui/confronto-B-0.9.1.png) · [Confronto C](docs/ui/confronto-C-0.9.1.png).
