# Referto Volley

App Android offline per assistere il segnapunti e produrre il **referto cartaceo classico FIPAV** quando eScoreVolley non è utilizzabile. Profilo iniziale: **Under 14 femminile, FIPAV Como, stagione 2026/27**.

[Scarica l'APK Android 0.3.0](https://raw.githubusercontent.com/bernofx/RefertoElettronico/main/downloads/RefertoVolley-0.3.0.apk). È una versione di prova firmata con chiave debug; richiede Android 8.0 o successivo.

## Utilizzo

1. **Prepara una nuova gara**: dati gara, distinte (numero, capitana e liberi), panchine e ufficiali. Puoi iniziare con i soli numeri e completare cognome e nome in seguito; il PDF segnala i nominativi mancanti. Registra il motivo dell'uso del cartaceo. Per ciascuna squadra puoi usare **Leggi CAMP3 da foto**, fotografando o caricando il suo documento e revisionando la rosa prima di sostituirla.
2. Scegli un colore per ciascuna squadra nei suoi dati: pulsanti dei punti e numeri in campo useranno quel colore, anche al cambio campo. La scelta è modificabile durante la gara e inclusa nella copia JSON.
3. Conferma il sorteggio e le sei posizioni iniziali di ciascuna squadra. La squadra inizialmente a sinistra è A sul referto; l'identità A/B resta invariata durante i cambi campo.
4. Usa **+1 punto**. Il campo mostra la formazione, ruota la squadra che conquista il servizio ed evidenzia il battitore. Il campo è visto dal tavolo del segnapunti, con rete verticale e formazioni opposte. Il pallone identifica la battitrice e le rotazioni sono animate; Annulla anima lo spostamento inverso.
5. Registra timeout, sostituzioni e ingressi/uscite del libero. I rimpiazzi del libero non consumano sostituzioni. Un messaggio segnala quando il libero deve uscire prima della prossima azione.
6. **Annulla** ripristina immediatamente l'ultima operazione. Dal **Registro**, “Annulla da qui” permette di tornare indietro di più eventi, anche oltre la fine di un set. **Ripristina** recupera gli eventi; registrare un nuovo evento scarta il ramo annullato.
7. Conferma le formazioni a ogni set. Il primo servizio e i lati si alternano; nel quinto è previsto un nuovo sorteggio e il cambio campo a 8.
8. In **Referto**, genera e salva/condividi il PDF. La prima pagina riproduce il foglio FIPAV; seguono registro e osservazioni completi. Le firme vanno apposte sulla stampa. Una gara non conclusa produce un PDF esplicitamente provvisorio.

Ogni operazione accettata viene salvata sul dispositivo con scrittura atomica. Dopo la chiusura dell'app la gara si riapre da **Le tue gare**. La copia JSON include dati, cronologia e operazioni ripristinabili; importarla crea una nuova gara senza sovrascrivere l'originale. I numeri, i ruoli delle distinte e la formula sono modificabili prima dell'inizio; in seguito è necessario annullare fino all'inizio. I nomi e gli altri dati della gara restano correggibili.

Sono disponibili sanzioni (con punto automatico per penalizzazione), richieste improprie, sostituzioni eccezionali, inabilità, ridesignazione del libero, nuova capitana, osservazioni, interruzione/ripresa e termine anticipato per decisione arbitrale. Le decisioni eccezionali richiedono una motivazione. Il termine anticipato conserva il risultato acquisito e le osservazioni, senza inventare un risultato a tavolino.

La gara dimostrativa nella schermata iniziale usa esclusivamente dati fittizi.

I suoni confermano le operazioni registrate con successo: punto breve, timeout e conferme, annullamento distinto. L'icona dell'altoparlante nella barra superiore li attiva/disattiva; la scelta resta sul dispositivo. L'app rispetta le modalità silenziosa/vibrazione e il volume di sistema. Un'operazione rifiutata non emette il suono di successo. I suoni non richiedono rete né autorizzazioni per il microfono.

La versione **0.2.0** ha introdotto colori e audio. Le gare e le copie JSON della versione 0.1.0 restano compatibili; in assenza di colori salvati vengono usati blu e rosso. L'aggiornamento usa lo stesso application ID e la stessa firma di debug; non disinstallare l'app se vuoi conservare gli archivi locali.

La versione **0.3.0** fa seguire ai pulsanti dei punti e al riepilogo dei set i lati del campo, anche nel quinto set e con Annulla/Ripristina. Accetta dati pre-gara da VolleyAlerts tramite un JSON condiviso da Android: [specifica di integrazione e codice Kotlin](docs/INTEGRAZIONE-VOLLEYALERTS.md). VolleyAlerts può omettere le rose; si completano nell'app.

L'OCR usa un modello latino incluso nell'APK e funziona senza connessione. Ogni squadra ha la propria acquisizione CAMP3. I risultati devono essere revisionati: verificare numeri di maglia, correggere i nomi, escludere righe estranee e indicare capitana e liberi. La colonna numerica viene usata automaticamente solo se identificata dall'intestazione MAGLIA; altrimenti i numeri restano da inserire. La foto acquisita dalla fotocamera viene eliminata dalla cache alla chiusura dell'importazione; le immagini scelte dall'archivio non vengono modificate o copiate permanentemente. Le bozze OCR e il testo integrale non vengono salvati nella gara: solo le giocatrici confermate. Vedi [uso e limiti OCR](docs/OCR-CAMP3.md).

Gli allineamenti e le sovrapposizioni del PDF finale sono un problema aperto da correggere: [piano degli interventi](docs/PIANO.md).

## Regole e documenti

Il [regolamento territoriale U14F](https://www.fipav.como.it/data-files/fipavcomo/dropbox/Regolamento%20U14F%20Stagione%202026-2027.pdf) prescrive tre set vinti su cinque, libero e secondo libero, rete a 2,15 m, e ammette l'eccezione al referto elettronico in assenza totale di connessione o per problemi del portale. Le modifiche federali in vigore dal 28 settembre 2026 portano a **otto sostituzioni per set**, mantenendo due timeout e i vincoli di abbinamento titolare/riserva.

Il PDF usa il modulo della figura 82 del Manuale nazionale del segnapunti cartaceo 2025/26, attualmente pubblicato dalla FIPAV. Elenchi, eventi e osservazioni che eccedono gli spazi del modulo sono conservati negli allegati. Il quinto set separa le registrazioni prima e dopo il cambio campo. Vedi [fonti e dettagli](docs/REGOLAMENTO.md).

Non è un'integrazione con eScoreVolley e non invia dati al Comitato. La procedura di consegna e il controllo finale restano quelli previsti dal Comitato e dagli ufficiali di gara. L'app registra le decisioni arbitrali; non sostituisce la direzione di gara, il CAMP3 o il rapporto dell'arbitro. Ripresa da una gara iniziata su un altro sistema: al momento si ricostruiscono gli eventi nel Registro; non è previsto l'import automatico da eScoreVolley. Gli esiti disciplinari a tavolino, i set annullati da rigiocare e le decisioni tecniche particolari vanno gestiti dall'arbitro sui documenti previsti, usando le osservazioni dell'app come supporto.

## Sviluppo

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
