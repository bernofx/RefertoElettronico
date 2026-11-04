# Design QA · fedeltà B e C · 0.9.1

final result: passed

## Correzione del problema segnalato

La 0.9.0 conservava strutture precedenti, in particolare il tagliando, i selettori squadra e gli spazi fissi: non riproduceva adeguatamente i concept scelti. La 0.9.1 usa viste dedicate per preparazione, gara e referto nei layout B/C. Non si tratta di un clone web: l'ambito richiesto è l'app Android esistente.

## Evidenza esaminata

Riferimenti: `docs/design/proposta-B-cruscotto.png`, `docs/design/proposta-C-campo.png` e immagini fornite dall'utente della preparazione del terzo set.

Risultati: `docs/ui/B-{preparazione,modifica-dati,gara,referto}-0.9.1.png`, `docs/ui/C-{preparazione-set3,preparazione-chiaro,gara,referto}-0.9.1.png`. Sono aperte e confrontate le schermate native e i riferimenti, negli stessi contesti di preparazione, gara e referto, con stati reali equivalenti. I confronti `confronto-{B,C}-0.9.1.png` riportano la viewport più alta del telefono di prova e i dati dimostrativi differenti.

## Verifiche

- B: panoramica di quattro sezioni, colori di stato, riepilogo con squadre e dati gara, callout del prossimo passo, modifica in pannelli dedicati, applicazione finale dei dati.
- C: intestazione del prossimo set corretta, controlli Dati/Rose/Ufficiali compatti, maglie numerate con capitana, rete a maglie e griglia 4–3–2/5–6–1, numeri disponibili e conferma fissa. Sei posizioni e conferma restano visibili nel caso del terzo set con nomi lunghi.
- Gara B: tabellone colorato, punti distinti per lato, contatori affiancati, ultime azioni e accesso al registro. Gara C: punteggio compatto con +1 ai lati, campo centrale e barra operazioni. Annullamento e selezione squadra verificati.
- Firme/referto: gruppi prima/fine gara, ruoli mancanti e stati conservati, anteprima PDF reale, salvataggio/condivisione espliciti. Le immagini illustrative del referto e gli errori di numerazione/libero presenti nei concept non vengono trasferiti nel prodotto.
- Navigazione: menu ⋮ per tutte le sezioni, impostazioni di stile indipendenti, opzioni di colore conservate. I controlli al di fuori della schermata Gara restano raggiungibili.
- 212 test completi superati anche nel collaudo conclusivo della versione da pubblicare. Assembled APK e lint finali superati.

I layout riprendono struttura, gerarchia e controlli dei riferimenti, con componenti nativi e contenuti regolamentari. Non sono dichiarati identici pixel per pixel: il concept non ha la viewport del telefono dell'utente e alcuni dati illustrati non sono regolamentari. Nessun P0/P1/P2 aperto nell'ambito verificato; prova sul telefono fisico ancora da effettuare.

## Ritocchi 0.9.2

Controllate le catture native B-preparazione, C-preparazione-set3 e C-preparazione-chiaro: avanzamento verde 12 dp, tipografia e grassetti aumentati, check verdi sulle intestazioni, legenda C oro/L azzurro, rete con maglie e bande più marcate e pali colorati. Sei slot e conferma visibili nei casi esistenti. Build/lint e 15 test stili finali superati; confronti aggiornati in docs/ui/confronto-B-0.9.2.png e confronto-C-0.9.2.png. Limite: rendering Android Robolectric, nessun telefono fisico.
