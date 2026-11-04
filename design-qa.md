# Design QA · stili B e C · 0.9.0

final result: passed

## Ambito

Adattamento dei due concept scelti all'app **Android nativa esistente**, con comportamento di produzione. La richiesta non è un clone web: nessun server, sito o controllo in browser viene dichiarato verificato. Le differenze fra mockup illustrativi e layout nativo sono deliberate; regole, motore, campo e PDF esistenti prevalgono sui dati inventati nelle tavole.

## Riferimenti aperti e confrontati

- `docs/design/proposta-B-cruscotto.png`: riepilogo della preparazione, stato sezioni, accesso diretto alle modifiche.
- `docs/design/proposta-C-campo.png`: campo centrale, squadra scelta e operazioni dal basso.
- `docs/ui/stile-B-preparazione-chiaro.png`: viewport nativa del riepilogo con dati fittizi, stesso contesto preparazione del riferimento B.
- `docs/ui/stile-B-gara-scuro.png` e `docs/ui/stile-C-gara-chiaro.png`: Activity Android reale, punto dalla ricezione e impostazioni di ciascuno stile.
- `docs/ui/stile-C-pannello.png`: dettaglio nativo del pannello timeout aperto dalla gara; ritaglio dell'area non trasparente della finestra. Non è uno screenshot con sfondo ricostruito.
- Matrice `app/build/outputs/verification/style-{DASHBOARD,COURT}-{LIGHT,DARK}-{0,1,2}.png`: preparazione, gara, firme; schermate di contenuto in un contenitore di test, distinte dalle catture dell'Activity.

## Esito

- Rimosso il titolo duplicato dal riepilogo B; la prossima azione occupa la larghezza disponibile.
- B: sezioni complete e da completare leggibili, modifiche e ritorno al riepilogo senza perdita della bozza.
- C: campo più ampio, comandi fuori dallo scorrimento, selezione della squadra e conferma in un pannello dal basso. Punteggio e Annulla restano accessibili; contenuto secondario scorrevole.
- Profili B/C indipendenti; luminosità e palette non sono vincolate al layout. Personalizzazione di principale e accento con contrasto automatico del testo.
- Campi delle firme e modello PDF FIPAV effettivi conservati, senza riprodurre i facsimili delle tavole. Nessun libero inserito nel tagliando iniziale; campo laterale e rotazioni del motore esistente.
- 205 test di regressione e successivi 9 controlli mirati superati. Il cambio di stile nell'Activity conserva il Journal completo.

Nessun P0/P1/P2 aperto nell'ambito verificato. Le trasparenze e gli effetti 3D dei concept sono sostituiti da componenti Material nativi; il collaudo su telefono fisico resta da effettuare.
