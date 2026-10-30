# Foto della lista giocatrici e dello staff

La versione **0.6.1** legge il ritaglio dalla **foto originale**, prima di ridimensionarlo. L'anteprima serve a scegliere la zona e la colonna delle maglie; non è la sorgente dell'OCR definitivo. Restano disponibili ricerca automatica della tabella, lettura N° del modello CAMP3 e revisione separata per le due squadre.

## Fotografie consigliate

Per **ogni squadra**:

1. Una foto ravvicinata alla **lista delle giocatrici**, comprendendo tutte le righe e la colonna dei numeri di maglia. Includere possibilmente le intestazioni **Cognome e Nome** e **N° maglia**. Non occorre fotografare tutto il CAMP3.
2. Una seconda foto **facoltativa** allo **staff**, comprendendo le etichette dei ruoli e i relativi nominativi. Non serve se i dati della panchina sono già disponibili o si preferisce inserirli a mano.

Le foto devono essere ferme, a fuoco, ben illuminate e con il foglio disteso. Se il documento intero rende i caratteri piccoli, avvicinare la fotocamera alla sola tabella: un ritaglio successivo non può ricostruire dettagli già sfocati o mancanti.

**Intestazioni:** sono consigliate per la lista, perché distinguono i numeri di maglia dai progressivi, dalle tessere e dalle date. Sono riconosciute anche le intestazioni brevi **N° / N. / N**, quando compaiono a sinistra dei nomi accanto a colonne come Matricola o Nascita. Se l’intestazione non è riconoscibile, si può indicare la colonna manualmente. Per lo staff i nomi dei ruoli permettono l'associazione automatica; se non vengono letti, i nomi si completano nella revisione.

## Lista giocatrici

1. Prima dell'inizio della gara aprire **Dati → Modifica i dati della gara → Squadra di casa/ospite → Foto lista giocatrici**.
2. Usare **Fotografa** oppure **Carica foto**. Il formato è un'immagine, non un PDF.
3. L'app cerca le intestazioni e la sezione successiva su una lettura iniziale ridotta, propone il ritaglio e rilegge **quella zona dall'originale**. Anche il ritaglio manuale, la colonna indicata a mano e lo staff usano l'originale. Il limite inferiore non coincide con l'ultima atleta riconosciuta: usa la sezione successiva, quando identificabile, o il bordo della foto, per non escludere le righe che l'OCR ha mancato.
4. Quando non individua la tabella con sufficiente evidenza, mostra la foto: trascinare per selezionare il rettangolo oppure spostarne gli angoli. Usare **Leggi lista giocatrici**. Il ritaglio automatico è un'euristica e va corretto se esclude righe o colonne.
5. Se non legge nessuna maglia, l'app propone direttamente **Colonna maglie**, con la zona selezionata ingrandita. Toccare il centro della colonna dei numeri effettivi; non il progressivo o la tessera. Premere Leggi. È possibile tornare a **Ritaglia**, **Foto intera** e **Colonna automatica**.
6. Controllare tutte le atlete: nome, numero, righe mancanti o estranee. I numeri duplicati, incerti e i nomi parziali sono segnalati. **Mostra riga originale** aiuta a confrontare il risultato con il testo letto. Non vengono inventati nomi o numeri mancanti.
7. Verificare **K** per la capitana e **L** per i liberi. Dalla 0.6.1, nel modello CAMP3 con colonna N°, gli indicatori espliciti **K/L1/L2** separati dai nomi possono essere proposti automaticamente; restano modificabili e segnalati da controllare. Se i simboli non sono leggibili, indicare i ruoli manualmente. Aggiungere manualmente eventuali righe mancanti o scegliere una foto più vicina con **Ritaglia e rileggi**.
8. Confermare **Sostituisci rosa** e poi **Salva modifiche** nella gara. L'importazione riguarda soltanto la squadra indicata nel titolo.

Per confermare la rosa servono 6–14 giocatrici, numeri unici tra 1 e 99, una capitana, almeno sei non liberi e al massimo due liberi. Con più di 12 giocatrici serve almeno un libero. Chiudere senza importare mantiene la rosa precedente. Dopo l'inizio del primo set i numeri e i ruoli sono bloccati: per cambiare la rosa occorre annullare fino all'inizio.

## Staff facoltativo

Nella stessa scheda squadra premere **Foto staff (facoltativa)** e fotografare solo questa sezione. L'app propone i nominativi quando riconosce esplicitamente allenatore, vice/secondo allenatore, fisioterapista/massaggiatore, medico e dirigente. Il riconoscimento di codici abbreviati o modelli diversi non è garantito.

Rivedere i nomi e selezionare i ruoli da importare. Se più nominativi corrispondono allo stesso ruolo, non ne viene scelto uno arbitrariamente: si deve completare il campo. Confermare **Importa staff selezionato**, poi salvare la gara. I ruoli non selezionati restano invariati; la lista delle giocatrici e l'altra squadra non vengono modificate. Lo staff può essere corretto anche dopo l'inizio della gara.

## Miglioramenti e limiti del riconoscimento

- Modello latino ML Kit incluso nell'APK; analisi locale, senza permesso Internet.
- L'anteprima ha lato lungo massimo di 1800 pixel per contenere la memoria. L'OCR definitivo decodifica prima la regione dalla foto originale e riduce solo quella se supera il budget di 3600 pixel sul lato lungo, senza ingrandire le regioni più piccole. Il campionamento del decoder mantiene anche un limite di circa 14 milioni di pixel intermedi: casi molto grandi possono risultare sotto il limite massimo di 3600.
- La sorgente codificata viene copiata integralmente nella cache privata (massimo 32 MB). Questo permette di usare anche provider che espongono solo stream, rileggere e ruotare la stessa foto senza perdere qualità o richiedere nuovamente il documento.
- Per i formati/dispositivi supportati, BitmapRegionDecoder evita di caricare tutta la foto in un bitmap ARGB ad alta risoluzione. Se il decoder regionale non è disponibile, la lettura alternativa riduce l'intera foto con memoria controllata e poi la ritaglia: in quel caso l'app segnala che il dettaglio originale non è disponibile e consiglia una foto JPEG ravvicinata. Non si promette la stessa qualità della decodifica regionale per ogni formato.
- Gestione orientamento EXIF e rotazione manuale di 90°. Le righe sono raggruppate considerando l'inclinazione stimata dal testo; non è una correzione completa della prospettiva del foglio.
- Intestazioni su più righe e intestazioni dei nomi centrate non devono tagliare i cognomi che iniziano più a sinistra. Le righe con un solo frammento di nome vengono proposte da controllare quando c'è evidenza della tabella.
- Il numero viene cercato nella colonna MAGLIA, nella colonna N° riconosciuta dal contesto della tabella CAMP3, oppure in quella indicata dall'utente. La sigla N° di altri riquadri, come il numero gara, non viene usata come intestazione delle maglie. Valori ambigui non vengono assegnati arbitrariamente. Alcuni errori come `O8`/`l2` sono interpretati come 08/12 solo dentro quella colonna e segnalati esplicitamente per la revisione.
- L'intestazione **Qualifica** della tabella dello staff costituisce un confine della lista delle atlete. Nomi dello staff e tessere non devono diventare righe della rosa.
- Il ritaglio automatico non è garantito per tutti i modelli CAMP3, fogli inclinati o intestazioni illeggibili. L'immagine resta correggibile; non si salva nessuna rosa senza conferma.
- I test usano immagini e coordinate sintetiche per controllare geometria, righe, numeri, ritaglio, staff e interfaccia. Non sono una misura dell'accuratezza del riconoscitore ML Kit sui CAMP3 reali. Per tarare il riconoscimento sul modulo usato a Como serve un campione fotografico rappresentativo e una prova sul telefono.

## Conservazione dei dati

La copia codificata dell'originale, l'eventuale foto della fotocamera, l'anteprima e i bitmap di riconoscimento hanno proprietà separate. Alla chiusura si eliminano le copie private quando le letture in corso terminano; anteprime e bitmap OCR vengono rilasciati senza riciclare immagini ancora in uso. Errori e annullamenti eliminano le copie incomplete. Se il processo viene terminato, può restare un file di cache eliminabile dal sistema. Le immagini scelte dall'archivio non vengono modificate. Non si allegano immagini alla gara o al PDF. Il testo OCR integrale serve solo alla revisione e non viene salvato: la gara conserva soltanto atlete e ruoli dello staff confermati dall'utente.

## Verifica del modello CAMP3 Como nella 0.6.1

Il campione fornito dall'utente usa N° per le maglie, Cognome e Nome nello stesso ampio riquadro, Data Nascita e Matricola nelle colonne successive e Qualifica come inizio dello staff. I dati nel PDF sono in Courier-Bold da 7 punti, ma ML Kit non viene vincolato a un font: l'adattamento riguarda il significato delle intestazioni, le posizioni e le righe.

Una fixture con dati fittizi e la stessa geometria verifica 14 righe, tutte le maglie, una capitana e due liberi, anche dopo ritaglio/ridimensionamento. Nel repository non sono presenti il documento ricevuto, i nomi, le date o le tessere originali. Questi test verificano l'interpretazione dei risultati OCR; non certificano la lettura fotografica su un telefono. Le foto restano il percorso principale e il PDF non è necessario per importare una squadra.

## Prova del ritaglio originale sul telefono

Fotografare il foglio intero, lasciare individuare la tabella oppure selezionarla a mano, poi premere **Ritaglia e rileggi** / **Leggi lista giocatrici**. Confrontare numeri e cognomi con la versione precedente. Ripetere dopo una rotazione di 90° e con la foto dello staff. L'aggiornamento firmato con la stessa chiave conserva le gare già salvate; non occorre disinstallare. Una foto sfocata o mossa non recupera dettagli che non sono stati acquisiti.
