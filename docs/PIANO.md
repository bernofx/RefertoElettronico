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

- Due fotografie CAMP3 distinte: una per casa e una per ospite, con revisione separata delle rose. Verificare qualità OCR su un campione dei moduli effettivamente usati a Como.
- Passaggio da VolleyAlerts con il suo FileProvider e i permessi Android reali, seguendo [la specifica](INTEGRAZIONE-VOLLEYALERTS.md).
