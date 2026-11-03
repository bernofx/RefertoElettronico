# Firme del referto · versione 0.8.0

## Regole e uso del documento

Fonte: [Manuale nazionale del segnapunti addetto al referto cartaceo 2025/26](https://overjoyed-wilson.s3.eu-south-mil.io.cloud.ovh.net/s3fs-public/2026-06/Manuale_Segnapunti_2025_26_v1.pdf?VersionId=1782761693.836059), consultato il 2 novembre 2026; collegato dalla [pagina documenti FIPAV](https://www.federvolley.it/documenti-UG).

- § 1.2 e § 2.1.5, pagina stampata 5: dopo il sorteggio, capitani e allenatori firmano negli spazi FIRME del riquadro squadre.
- § 2.3.3, pagine stampate 36–37: dopo i controlli arbitrali ed eventuali termini previsti per le istanze, firmano segnapunti, secondo arbitro e primo arbitro, nell’ordine. La stessa sezione precisa che nelle competizioni FIPAV i capitani non firmano a fine gara.
- Allenatore e secondo arbitro vengono richiesti se presenti nei dati. I tagliandi delle formazioni sono documenti distinti: va verificata la firma dell’allenatore o, in sua assenza, del capitano (§ 2.1.7).

Il riferimento al modello non attesta approvazione federale dell’app. Le firme tracciate sul dispositivo e riprodotte nel PDF non sono firme digitali qualificate. Per l’impiego del cartaceo e le modalità di firma valgono le disposizioni dell’arbitro e del Comitato; la stampa prodotta da software non viene dichiarata automaticamente ammessa né automaticamente invalida.

## Raccolta

Dopo il sorteggio, **Raccogli / controlla firme** apre le firme preliminari. La persona controlla il nominativo e firma con dito o penna nel riquadro orizzontale. Cancella ripulisce il riquadro; Annulla esce senza salvare; Conferma firma salva. Alla chiusura viene ripristinata l’impostazione di orientamento precedente.

Prima del primo set, se mancano firme il pannello propone di raccoglierle oppure di usare esplicitamente **Inizia · firme da completare sulla stampa**: non viene inventata alcuna firma. A gara conclusa **Approvazione finale** permette di raccogliere segnapunti, secondo arbitro se presente e primo arbitro. Il pannello si riapre da Gara o Referto; il nome mancante di primo arbitro o segnapunti si può completare direttamente qui.

L’addetto all’arbitro è una persona di assistenza della società: la sua selezione non compila il campo 1° arbitro e non lo rende firmatario finale. La selezione del segnapunti compila invece Segnapunti.

## Correzioni senza perdita delle firme

Dalla **0.8.0** la firma è conservata come raccolta storica della persona. Completare o correggere località, palestra, data/ora prevista, numero gara, campionato, nominativi degli altri ufficiali o note amministrative non cancella il tratto e non impone una nuova firma.

Le variazioni a distinta, ruoli, sorteggio o cronologia di gara conservano il tratto, con una segnalazione di contenuto aggiornato da controllare con il firmatario. Questa segnalazione non blocca il salvataggio e non esclude la firma dal PDF. Una nuova raccolta aggiorna la data e il riferimento di controllo; l’app non dichiara che una firma precedente approvi automaticamente contenuti aggiunti dopo.

Se cambia il nominativo della persona assegnata a un incarico, la vecchia firma rimane in archivio ma non viene attribuita alla nuova persona. Le firme finali sono riportate nel PDF di gara conclusa; annullare fino a una gara in corso non elimina i relativi tratti, che tornano disponibili dopo la chiusura.

Il riferimento SHA-256 originale rimane conservato. Un riferimento separato serve soltanto a mostrare variazioni dei contenuti sportivi; non certifica identità o autenticità del firmatario e non viene usato per cancellare firme storiche.

## Gare e firme delle versioni precedenti

Le firme raccolte con la 0.7.x restano leggibili. Quelle ancora presenti nell’archivio e associate allo stesso nominativo tornano disponibili anche se una vecchia versione le aveva indicate come non aggiornate dopo aver completato il luogo o gli ufficiali. **Non occorre più premere Recupera firme preliminari.**

Quando il vecchio riferimento è ancora verificabile, l’aggiornamento dei Dati stabilisce anche il nuovo riferimento di controllo prima della correzione descrittiva. Se il vecchio contenuto non è ricostruibile, la firma rimane comunque raccolta storicamente, con indicazione di contenuto aggiornato. Non vengono ricostruiti tratti eliminati dall’utente.

## Riquadro e PDF

L’input Android nativo disegna dal primo contatto, acquisisce movimenti piccoli e campioni storici e segue il puntatore attivo senza unirlo a un secondo contatto. Il pallino segue dito o penna durante la scrittura e sparisce al rilascio: resta escluso da archivio e PDF. Ricomposizione e ridimensionamento non ripuliscono il tratto.

Il PDF e la sua anteprima riportano firme vettoriali nei campi originari. La 0.8.0 usa più altezza delle righe e sceglie l’area scrivibile più ampia che rispetta le etichette stampate, mantenendo le proporzioni. Nell’allegato figurano nominativo, data originale, riferimento storico ed eventuale segnalazione di contenuto aggiornato.

I file rimangono nell’archivio privato dell’app e nelle copie JSON esportate dall’utente. Nessuna firma viene inviata a servizi remoti. Installazione, rotazione e uso su telefono reale restano da verificare sul dispositivo.
