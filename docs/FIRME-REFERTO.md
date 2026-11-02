# Firme del referto FIPAV · 0.7.0

## Regole seguite

Fonte: [Manuale nazionale del segnapunti addetto al referto cartaceo 2025/26](https://overjoyed-wilson.s3.eu-south-mil.io.cloud.ovh.net/s3fs-public/2026-06/Manuale_Segnapunti_2025_26_v1.pdf?VersionId=1782761693.836059), consultato nuovamente il 2 novembre 2026; disponibile dalla [pagina documenti FIPAV](https://www.federvolley.it/documenti-UG).

- § 1.2 e § 2.1.5, pagina stampata 5: dopo il sorteggio, capitani e allenatori firmano negli spazi FIRME del riquadro squadre.
- § 2.3.3, pagine stampate 36–37, figura 76: dopo i controlli arbitrali ed eventuali termini previsti per la conferma delle istanze, firmano nell’ordine segnapunti, secondo arbitro e primo arbitro.
- La stessa sezione precisa: «Nelle competizioni FIPAV i capitani non firmano il referto a fine gara». L’app lascia quindi vuoti i due campi Capitani nel riquadro APPROVAZIONE.
- Allenatore e secondo arbitro vengono richiesti se presenti nei dati della gara. Non vengono inventati firmatari o sostituti.
- § 2.1.7: i tagliandi delle formazioni sono documenti distinti, firmati dall’allenatore o, in sua assenza, dal capitano. L’app ricorda di verificare la firma sui tagliandi ricevuti; non la sostituisce con la firma preliminare sul referto.

## Flusso nell’app

Dopo aver scelto servizio e lato nel sorteggio, **Raccogli / controlla firme** apre le firme preliminari. Ogni persona controlla il nominativo e traccia la propria firma con dito o penna. Il riquadro firma richiede l’orientamento orizzontale, con intestazione compatta e area di scrittura ampia; alla conferma, ad Annulla o a Indietro viene ripristinata la precedente impostazione di orientamento. Cancella ripulisce il riquadro; Annulla esce senza salvare; Conferma firma salva nel fascicolo della gara. Serve il nominativo nei Dati.

Premendo l’inizio del primo set, se mancano firme aggiornate si apre questo passaggio prima di registrare l’avvio. Si può raccogliere le firme o usare esplicitamente **Inizia · firme da completare sulla stampa**: non viene creata alcuna firma fittizia e gli spazi restano vuoti. Le firme raccolte e il sorteggio associato vengono salvati sul dispositivo; una bozza di firma non confermata non viene salvata. Dalla 0.7.1 il riquadro conserva anche il tratto in corso durante ridimensionamenti e ricomposizioni, mantenendone le proporzioni; Cancella è il comando esplicito per ripulirlo.

Alla conclusione della gara compare **Approvazione finale**. Le firme vengono abilitate nell’ordine previsto; il secondo arbitro viene saltato quando assente. Il pannello si può chiudere per completare le verifiche e riaprire dal pulsante in Gara o dalla scheda **Referto**. Prima del termine della gara le firme finali restano disabilitate. Dopo l’inizio non si raccolgono retroattivamente firme preliminari.

Il PDF e la sua anteprima riportano il tratto nei campi scrivibili originari: capitano/allenatore nelle due colonne delle squadre, segnapunti/secondo/primo arbitro nella colonna Firma di APPROVAZIONE. Il riquadro squadre conserva l’ordine casa/ospiti: le lettere A/B vengono assegnate dopo il sorteggio, senza scambiare i firmatari. Il tratto mantiene le proporzioni ed è ritagliato nel proprio campo, senza coprire le etichette. Nell’allegato figurano ruolo, nominativo, data/ora di raccolta, riferimento dei dati approvati e firme mancanti o da rifare.

## Correzioni e conservazione

Dalla 0.7.2 le firme preliminari si riferiscono all’identità della gara, alla distinta e allo staff confermati della propria squadra e al sorteggio. I punti e gli altri eventi non le invalidano. I nominativi e le città degli ufficiali, l’addetto all’arbitro e le note amministrative non fanno parte di questa approvazione preliminare. Modificare una squadra invalida le firme preliminari di quella squadra; modificare i dati identificativi della gara o il sorteggio interessa entrambe. Cambiare soltanto colori o candidati della rosa storica non invalida le firme.

L’approvazione finale si riferisce anche alla cronologia e alle firme precedenti. Correzioni successive, annotazioni o la rimozione di una firma precedente rendono obsolete le approvazioni interessate. L’app mostra **Da rifirmare** e omette tali tratti dal PDF. Annulla/Ripristina continuano a correggere la cronologia; una ricostruzione esattamente identica dei contenuti approvati ritrova le firme corrispondenti.

Il JSON della gara conserva le firme come tratti vettoriali normalizzati e include il sorteggio associato. I vecchi archivi senza questi campi restano leggibili. Vengono verificati ruoli, duplicati, dimensioni, limiti dei punti e coordinate finite. I file sono locali nell’archivio privato dell’app e vengono inclusi nelle copie della gara esportate dall’utente; non vengono inviate firme a servizi remoti.

Le firme tracciate sul dispositivo e riprodotte nel PDF **non sono firme digitali qualificate**. Il riferimento SHA-256 serve a riconoscere modifiche ai dati, non a certificare l’identità o l’autenticità del firmatario. La disponibilità di questa funzione non attesta l’accettazione federale delle firme riprodotte: per la consegna del cartaceo valgono le indicazioni dell’arbitro e del Comitato; quando richiesto si firmano gli spazi sulla stampa.

## Guida

Il tutorial alla revisione 3 include i passaggi delle firme preliminari e dell’approvazione finale e genera il PDF con firme esclusivamente dimostrative, senza utilizzare nominativi o firme delle gare reali.

## Anteprima dell’interfaccia

[Accesso, elenco e riquadro firma](ui/firme-preview/anteprima-firme-preliminari.png) · [Accesso dal Referto e approvazione finale](ui/firme-preview/anteprima-firme-finali.png). Rendering delle schermate reali dell’Activity Android e delle sue finestre in Robolectric, con dati fittizi.

[Riquadro firma aggiornato in orizzontale](ui/firme-preview/3-riquadro-firma-landscape.png). Il test verifica dimensioni orizzontali, acquisizione del tratto e ripristino della precedente richiesta di orientamento sia dopo Annulla sia dopo Conferma. La rotazione effettiva resta da provare sul telefono.

## Input della firma · 0.7.1

Il riquadro usa una View Android nativa incorporata nella schermata. Gli eventi DOWN/MOVE/UP vengono acquisiti direttamente, senza attendere il superamento della soglia di trascinamento. Sono inclusi i campioni storici dei movimenti e i dati della penna; viene seguito il puntatore che ha iniziato il tratto, senza unirlo a un secondo tocco. Il primo contatto lascia un punto visibile. Un pallino con alone segue il contatto durante la scrittura e sparisce al rilascio o all’annullamento del gesto: è un’indicazione visiva, esclusa dai tratti esportati.

Ricomporre o ridimensionare la schermata non ripulisce il riquadro. I tratti restano nel sistema di coordinate originale e vengono adattati senza deformazioni; per cancellarli serve il pulsante Cancella. Conferma si abilita quando sono presenti punti sufficienti; la normale validazione impedisce di salvare una firma vuota o un semplice punto. Nessun cambiamento alla firma APK o al formato degli archivi.

## Completamento ufficiali e recupero · 0.7.2

Il nominativo mancante di primo arbitro o segnapunti si può completare direttamente in **Approvazione finale**, inserendo Nome e cognome e premendo **Salva nominativo**. La modifica usa lo stesso percorso di aggiornamento dei Dati e conserva le firme preliminari quando i contenuti approvati dalle squadre non sono cambiati. Le firme finali restano riferite al documento completo e possono richiedere una nuova raccolta dopo correzioni.

Le firme delle versioni 0.7.0/0.7.1 legavano anche tutti i dati degli ufficiali. Quando sono ancora valide, l’aggiornamento dei Dati conserva in modo verificato le firme non interessate dalla modifica usando il nuovo ambito di approvazione. Gli archivi già interamente firmati e invariati rimangono validi, senza migrazioni che ne cambino i riferimenti.

Se le firme preliminari risultano già obsolete per il vecchio comportamento, aprire **Referto → Controlla e raccogli firme → Recupera firme preliminari**. Il pulsante compare in presenza di vecchie firme non aggiornate e ancora conservate nell’archivio. La verifica cerca la configurazione originale modificando soltanto campi degli ufficiali attualmente compilati in campi vuoti; devono corrispondere esattamente il precedente hash, il firmatario e il sorteggio. Solo allora viene aggiornato l’ambito dell’approvazione, conservando tratti, nominativo, data di raccolta e hash originale. Nessuna firma viene ridisegnata o retrodatata.

Il recupero copre il caso del nome dell’arbitro originariamente vuoto e completato dopo la partita. Non può ricostruire firme rimosse, dimostrare modifiche di cui mancano i dati originali o riapprovare automaticamente una distinta o un sorteggio cambiati. Se i dati della gara cambiano mentre la verifica è in corso, il risultato non viene applicato e occorre riprovare.

## Addetto all’arbitro e ufficiali di gara

L’addetto all’arbitro è un incarico di assistenza della società, distinto dal direttore di gara. La selezione di un addetto compila soltanto Addetto di società all’arbitro; il campo viene disabilitato perché il nome è ripreso dall’incarico selezionato, modificabile sopra. Non compila 1° o 2° arbitro e non rende quella persona firmataria dell’approvazione finale.

La selezione del segnapunti, invece, compila Segnapunti e viene usata per la relativa firma finale. Il nome del direttore di gara va nel campo 1° arbitro e l’eventuale secondo nel campo 2° arbitro. La scheda Ufficiali ora separa Assistenza della società da Arbitri e segnapunti e spiega da dove provengono i campi precompilati.
