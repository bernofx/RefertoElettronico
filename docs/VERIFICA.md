# Verifica prima dell'uso

La build e i test automatici esercitano il motore, le schermate Compose e il PDF con le librerie effettivamente usate dall'app. Robolectric esegue i test Android in modalità grafica nativa, senza dispositivo o emulatore. La suite corrente contiene 67 test, inclusi i controlli di geometria dal tavolo del segnapunti e dei fotogrammi intermedi di rotazione/annullamento, compatibilità dei colori con le gare precedenti, contrasto dei testi, silenziamento e conferme delle sole azioni accettate. I fotogrammi della registrazione sono generati dal test delle schermate, non da un disegno simulato.

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

Il problema degli allineamenti del PDF è aperto e registrato nel [piano](PIANO.md). I test attuali verificano produzione e contenuto del PDF; non garantiscono l'assenza di sovrapposizioni grafiche con il modello.
