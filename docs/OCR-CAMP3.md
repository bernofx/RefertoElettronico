# Foto della lista giocatrici e dello staff

Dalla versione **0.5.0** l'acquisizione cerca automaticamente la tabella, mantiene più dettagli della fotografia e può usare una colonna di maglie indicata dal refertista. Le due squadre restano indipendenti.

## Fotografie consigliate

Per **ogni squadra**:

1. Una foto ravvicinata alla **lista delle giocatrici**, comprendendo tutte le righe e la colonna dei numeri di maglia. Includere possibilmente le intestazioni **Cognome e Nome** e **N° maglia**. Non occorre fotografare tutto il CAMP3.
2. Una seconda foto **facoltativa** allo **staff**, comprendendo le etichette dei ruoli e i relativi nominativi. Non serve se i dati della panchina sono già disponibili o si preferisce inserirli a mano.

Le foto devono essere ferme, a fuoco, ben illuminate e con il foglio disteso. Se il documento intero rende i caratteri piccoli, avvicinare la fotocamera alla sola tabella: un ritaglio successivo non può ricostruire dettagli già sfocati o mancanti.

**Intestazioni:** sono consigliate per la lista, perché distinguono i numeri di maglia dai progressivi, dalle tessere e dalle date. Sono riconosciute anche le intestazioni brevi **N° / N. / N**, quando compaiono a sinistra dei nomi accanto a colonne come Matricola o Nascita. Se l’intestazione non è riconoscibile, si può indicare la colonna manualmente. Per lo staff i nomi dei ruoli permettono l'associazione automatica; se non vengono letti, i nomi si completano nella revisione.

## Lista giocatrici

1. Prima dell'inizio della gara aprire **Dati → Modifica i dati della gara → Squadra di casa/ospite → Foto lista giocatrici**.
2. Usare **Fotografa** oppure **Carica foto**. Il formato è un'immagine, non un PDF.
3. L'app cerca le intestazioni e la sezione successiva, propone il ritaglio della tabella e rilegge quella zona. Il limite inferiore non coincide con l'ultima atleta riconosciuta: usa la sezione successiva, quando identificabile, o il bordo della foto, per non escludere le righe che l'OCR ha mancato.
4. Quando non individua la tabella con sufficiente evidenza, mostra la foto: trascinare per selezionare il rettangolo oppure spostarne gli angoli. Usare **Leggi lista giocatrici**. Il ritaglio automatico è un'euristica e va corretto se esclude righe o colonne.
5. Se non legge nessuna maglia, l'app propone direttamente **Colonna maglie**, con la zona selezionata ingrandita. Toccare il centro della colonna dei numeri effettivi; non il progressivo o la tessera. Premere Leggi. È possibile tornare a **Ritaglia**, **Foto intera** e **Colonna automatica**.
6. Controllare tutte le atlete: nome, numero, righe mancanti o estranee. I numeri duplicati, incerti e i nomi parziali sono segnalati. **Mostra riga originale** aiuta a confrontare il risultato con il testo letto. Non vengono inventati nomi o numeri mancanti.
7. Verificare **K** per la capitana e **L** per i liberi. Dalla 0.7.0, nel modello CAMP3 con colonna N°, gli indicatori espliciti **K/L1/L2** separati dai nomi possono essere proposti automaticamente; restano modificabili e segnalati da controllare. Se i simboli non sono leggibili, indicare i ruoli manualmente. Aggiungere manualmente eventuali righe mancanti o scegliere una foto più vicina con **Ritaglia e rileggi**.
8. Confermare **Sostituisci rosa** e poi **Salva modifiche** nella gara. L'importazione riguarda soltanto la squadra indicata nel titolo.

Per confermare la rosa servono 6–14 giocatrici, numeri unici tra 1 e 99, una capitana, almeno sei non liberi e al massimo due liberi. Con più di 12 giocatrici serve almeno un libero. Chiudere senza importare mantiene la rosa precedente. Dopo l'inizio del primo set i numeri e i ruoli sono bloccati: per cambiare la rosa occorre annullare fino all'inizio.

## Staff facoltativo

Nella stessa scheda squadra premere **Foto staff (facoltativa)** e fotografare solo questa sezione. L'app propone i nominativi quando riconosce esplicitamente allenatore, vice/secondo allenatore, fisioterapista/massaggiatore, medico e dirigente. Il riconoscimento di codici abbreviati o modelli diversi non è garantito.

Rivedere i nomi e selezionare i ruoli da importare. Se più nominativi corrispondono allo stesso ruolo, non ne viene scelto uno arbitrariamente: si deve completare il campo. Confermare **Importa staff selezionato**, poi salvare la gara. I ruoli non selezionati restano invariati; la lista delle giocatrici e l'altra squadra non vengono modificate. Lo staff può essere corretto anche dopo l'inizio della gara.

## Miglioramenti e limiti del riconoscimento

- Modello latino ML Kit incluso nell'APK; analisi locale, senza permesso Internet.
- Fotografie decodificate fino a 3600 pixel sul lato lungo, senza il precedente dimezzamento indiscriminato (una foto da 3200 pixel prima poteva scendere a 1600). Su Android 8 la decodifica limita anche la memoria delle foto molto grandi. Su Android 9 e successivi la dimensione viene impostata durante la decodifica; i file codificati oltre 32 MB sono rifiutati.
- Gestione orientamento EXIF e rotazione manuale di 90°. Le righe sono raggruppate considerando l'inclinazione stimata dal testo; non è una correzione completa della prospettiva del foglio.
- Intestazioni su più righe e intestazioni dei nomi centrate non devono tagliare i cognomi che iniziano più a sinistra. Le righe con un solo frammento di nome vengono proposte da controllare quando c'è evidenza della tabella.
- Il numero viene cercato nella colonna MAGLIA, nella colonna N° riconosciuta dal contesto della tabella CAMP3, oppure in quella indicata dall'utente. La sigla N° di altri riquadri, come il numero gara, non viene usata come intestazione delle maglie. Valori ambigui non vengono assegnati arbitrariamente. Alcuni errori come `O8`/`l2` sono interpretati come 08/12 solo dentro quella colonna e segnalati esplicitamente per la revisione.
- L'intestazione **Qualifica** della tabella dello staff costituisce un confine della lista delle atlete. Nomi dello staff e tessere non devono diventare righe della rosa.
- Il ritaglio automatico non è garantito per tutti i modelli CAMP3, fogli inclinati o intestazioni illeggibili. L'immagine resta correggibile; non si salva nessuna rosa senza conferma.
- I test usano immagini e coordinate sintetiche per controllare geometria, righe, numeri, ritaglio, staff e interfaccia. Non sono una misura dell'accuratezza del riconoscitore ML Kit sui CAMP3 reali. Per tarare il riconoscimento sul modulo usato a Como serve un campione fotografico rappresentativo e una prova sul telefono.

## Conservazione dei dati

La foto della fotocamera resta temporaneamente nella cache privata ed è eliminata alla chiusura del dialogo; se il processo viene terminato, può restare un file di cache eliminabile dal sistema. Le immagini scelte dall'archivio non vengono modificate. Non si allegano immagini alla gara o al PDF. Il testo OCR integrale serve solo alla revisione e non viene salvato: la gara conserva soltanto atlete e ruoli dello staff confermati dall'utente.

## Verifica del modello CAMP3 Como nella 0.7.0

Il campione fornito dall'utente usa N° per le maglie, Cognome e Nome nello stesso ampio riquadro, Data Nascita e Matricola nelle colonne successive e Qualifica come inizio dello staff. I dati nel PDF sono in Courier-Bold da 7 punti, ma ML Kit non viene vincolato a un font: l'adattamento riguarda il significato delle intestazioni, le posizioni e le righe.

Una fixture con dati fittizi e la stessa geometria verifica 14 righe, tutte le maglie, una capitana e due liberi, anche dopo ritaglio/ridimensionamento. Nel repository non sono presenti il documento ricevuto, i nomi, le date o le tessere originali. Questi test verificano l'interpretazione dei risultati OCR; non certificano la lettura fotografica su un telefono. Le foto restano il percorso principale e il PDF non è necessario per importare una squadra.
