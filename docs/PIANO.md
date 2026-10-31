# Piano dei prossimi interventi

## PDF finale: correzioni implementate nella 0.4.0

- Ricalibrate le aree di scrittura di intestazione, formazioni, sostituzioni, turni di servizio, liberi, timeout, rose, panchine, sanzioni, osservazioni, ufficiali e riepilogo.
- Nomi e città degli ufficiali restano nelle rispettive colonne, senza invadere le firme. I giudici di linea seguono l'ordine prestampato 1/3 a sinistra e 2/4 a destra.
- Durate dei set all'interno delle parentesi stampate; ore e minuti negli spazi prima delle unità di misura.
- Le lettere A/B prestampate nei set 1–4 non vengono riscritte; quelle del set decisivo sono inserite nei cerchi vuoti. Intestazione e distinte restano casa/ospite, con la lettera assegnata al sorteggio.
- Testo con dimensione proporzionata e area di ritaglio per campo: nessuna compressione orizzontale. I valori troppo lunghi sono abbreviati sul foglio principale, conservati integralmente negli allegati insieme a panchine e ufficiali.
- Test automatici sulle aree di scrittura e confronto dei pixel del PDF effettivo con le etichette del modello originale; casi con cinque set, nomi lunghi, punteggi oltre 48, molte registrazioni, cambio campo del quinto e formula su tre set.
- Ispezionate le anteprime generate per una gara completa e per i dati lunghi. Modello e risoluzione originali conservati.

**Verifica ancora da eseguire:** stampa fisica A3 orizzontale, controllo della leggibilità e confronto sul documento stampato. L'ambiente cloud non dispone di una stampante.

## Verifiche sul dispositivo

- Verificare l’OCR 0.5.0 su CAMP3 reali di Como: una foto ravvicinata della lista per squadra e, se utile, una foto separata dello staff. Controllare ritaglio automatico, selezione delle maglie, nomi parziali e numero di atlete riconosciute. Serve un campione reale: i test sintetici non misurano l’accuratezza del riconoscitore sul documento fotografato.
- Passaggio da VolleyAlerts con il suo FileProvider e i permessi Android reali, seguendo [la specifica](INTEGRAZIONE-VOLLEYALERTS.md).

## Rose e interfaccia 0.6.2

- Archivio locale di rose riutilizzabili, aggiornabile senza modificare le gare passate; recupero anche da una partita precedente.
- Rosa completa separata dalle presenti della gara. Solo le presenti entrano in formazioni, cambi e referto; copia JSON completa e compatibilità con i vecchi archivi.
- Righe compatte con dettagli espandibili; colore e panchina richiudibili.
- Ritaglio foto bloccato all'apertura e modifica esplicita, lettura sempre accessibile senza scorrere l'immagine.
- Da verificare sul dispositivo: flusso foto e tastiera, scelta presenze prima di una gara reale.
