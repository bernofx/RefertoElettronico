# Verifica prima dell'uso

La build e i test automatici esercitano il motore, le schermate Compose e il PDF con le librerie effettivamente usate dall'app. Robolectric esegue i test Android in modalità grafica nativa, senza dispositivo o emulatore. La suite corrente contiene 112 test, inclusi i controlli di geometria dal tavolo del segnapunti e dei fotogrammi intermedi di rotazione/annullamento, compatibilità dei colori con le gare precedenti, contrasto dei testi, silenziamento e conferme delle sole azioni accettate. I fotogrammi della registrazione sono generati dal test delle schermate, non da un disegno simulato.

Per la prova su telefono:

1. Installare l'APK di debug su Android 8 o successivo e aprire la gara dimostrativa.
2. Assegnare il servizio a una squadra e verificare che il primo punto conquistato dall'altra provochi la rotazione e identifichi il nuovo battitore.
3. Inserire timeout, un cambio con rientro e il libero. Provare l'uscita richiesta quando il libero raggiunge la prima linea o dovrebbe servire.
4. Annullare più eventi, ripristinarli e controllare insieme punteggio, formazioni, servizio e contatori.
5. Chiudere il processo dell'app e riaprire la gara dall'archivio. Verificare che sia presente anche la possibilità di ripristinare gli annullamenti.
6. Generare il PDF, salvarlo tramite il selettore di Android e condividerlo tramite il foglio di condivisione. Stampare su A3 orizzontale e confrontare allineamento e leggibilità con il referto del Comitato.
7. Esportare il JSON, importarlo su una nuova installazione e verificare che la gara sia ricostruita senza modificare l'originale.
8. Far controllare al segnapunti/arbitro il referto di una gara simulata completa, inclusi quinto set, sanzione e rientro di una sostituzione, prima dell'uso come supporto in una gara reale.

I dialoghi di sistema per condivisione e salvataggio, la stampa fisica e l'installazione su telefono non sono coperti da Robolectric. Il foglio principale e gli allegati devono essere controllati dagli ufficiali di gara come previsto dal Manuale.

Verifiche aggiunte nella 0.3.0: pulsanti dei punti coerenti con i lati nel secondo set e nel quinto (inclusi ottavo punto, Annulla e Ripristina); importazione JSON parziale da VolleyAlerts tramite content URI con revisione senza sovrascrivere gare; numeri senza nominativi e segnalazione nel PDF; parser delle righe CAMP3 che distingue la colonna MAGLIA da tessera e data; decodifica, riduzione e rotazione delle foto; assenza del permesso Internet nel manifest finale.

La fotocamera, il riconoscitore ML Kit effettivo su foto reali e i permessi fra VolleyAlerts e Referto Volley richiedono la verifica su telefono. I test OCR del parser usano parole e coordinate sintetiche; non misurano l'accuratezza del riconoscitore su CAMP3 reali.

Verifiche aggiunte nella **0.4.0**: misurazione dei testi con il font PDF effettivo e controllo che restino nelle aree assegnate; confronto dei pixel delle etichette sensibili del modello (ufficiali, firme, parentesi, unità di misura, intestazioni e lettere prestampate) prima e dopo la compilazione. I casi comprendono nomi lunghi, panchine e ufficiali completi, dodici atlete più due liberi per squadra, sanzioni, sostituzione con rientro, sequenza libero-libero-rientro, quinto set ai vantaggi e un set a 54:52. Verificati anche ordine casa/ospite dopo il sorteggio e riquadro decisivo nella formula al meglio di tre set.

Esempi generati: `referto-gara-completa.pdf` per la gara dimostrativa e `referto-allineamenti.pdf` per i dati lunghi, con le relative immagini PNG e l'anteprima Android. Le immagini sono state ispezionate. Le verifiche coprono i casi descritti e i campi misurati; resta la prova fisica A3 indicata nel [piano](PIANO.md).

Verifiche aggiunte nella **0.5.0**: mantenimento dei dettagli nelle foto da 3200 pixel e ridimensionamento esatto fino a 3600 sui dispositivi recenti; ritaglio dentro i bordi della foto; selezione touch della colonna anche dopo l'ingrandimento; riconoscimento delle intestazioni e dei limiti della tabella senza eliminare le ultime righe non lette; raggruppamento di righe inclinate; cognomi a sinistra di intestazioni centrate; nomi parziali proposti in revisione; colonna manuale senza intestazioni; numeri ambigui, duplicati e tessere/date distinti dalle maglie; ruoli dello staff associati soltanto alle etichette; importazione dei soli ruoli selezionati. Il riconoscitore ML Kit effettivo e le foto CAMP3 reali restano da verificare sul dispositivo. Non è stata misurata una percentuale di accuratezza OCR.

Verifiche 0.6.0: tre casi di validazione dei link pre-gara e apertura del link nell'interfaccia. Verifiche 0.6.1: cinque casi sul modello CAMP3 con dati fittizi (14 righe e maglie, K/L1/L2, confine con lo staff, distinzione dal numero gara e coordinate ritagliate/ridimensionate) e un caso UI di conferma della rosa con i ruoli proposti. Il fixture deriva soltanto dalla geometria del modulo; nessun dato personale del documento ricevuto è pubblicato. Rimane necessaria la verifica del riconoscitore effettivo sulle fotografie del dispositivo.

Ritaglio originale 0.6.1: dieci test aggiuntivi verificano la risoluzione effettiva della regione rispetto all'anteprima, assenza di ingrandimenti, limite del ritaglio, tutte le otto trasformazioni EXIF con quattro rotazioni manuali, bordi inclusi, colonna normalizzata, percorsi giocatrici/staff, annullamento ed errori, provider solo-stream, file non valido/oltre 32 MB, chiusura durante lettura e fallback limitato. L'anteprima non è passata al riconoscitore definitivo: tutti i chiamanti del dialogo usano `readOriginal` e una propria regione decodificata.

**Limite dell'ambiente di test:** lo shadow BitmapRegionDecoder di Robolectric restituisce bitmap vuoti e ignora il campionamento. Le verifiche pixel/geometria usano quindi un decoder regionale JVM indipendente (ImageIO), mentre le trasformazioni Bitmap/Matrix e la lettura alternativa usano la grafica Android nativa di Robolectric. L'adattatore di produzione usa BitmapRegionDecoder Android; la sua verifica finale sul dispositivo e l'accuratezza di ML Kit sulle foto reali restano necessarie. Non si dichiara una percentuale di accuratezza OCR.
