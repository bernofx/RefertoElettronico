# Stile, temi e colori · 0.9.2

Apri **Stile · temi e colori** dalla pagina iniziale. Nei layout B e C, durante la gara usa **⋮ → Stile**; nel layout Classico resta disponibile anche **Dati → Stile**.

## Disposizione

- **B · Cruscotto gara**: panoramica della preparazione con stato dei dati, rose, ufficiali e passaggio alle formazioni. Tocca una scheda: i dettagli si aprono in un pannello dal basso. **Fatto · riepilogo** conserva la bozza e torna alla panoramica; il pulsante principale applica i dati alla gara. In gara rimangono visibili punteggi, campo e contatori per squadra.
- **C · Campo al centro**: campo più ampio e barra comandi fissa sotto la zona scorrevole. Tocca il nome della squadra sopra il punteggio, poi Timeout, Sostituzione o Libero. I moduli si aprono dal basso; i vincoli e la conferma Registra restano gli stessi.
- **Classico**: disposizione precedente, conservata inizialmente per gli aggiornamenti.

Il tagliando continua a mostrare 4–3–2 sopra e 5–6–1 sotto, con numeri trascinabili. La formazione iniziale esclude il libero. Il campo della gara è visto dal tavolo del segnapunti; punti e selettori seguono il lato effettivo delle squadre.

## Tema e colori separati per stile

Ogni disposizione conserva **le proprie preferenze**. Per esempio puoi scegliere B scuro verde/arancio e C chiaro blu/oro: tornando a B ritrovi la prima combinazione.

1. Scegli la disposizione.
2. Seleziona **Chiaro**, **Scuro** o **Come il telefono**.
3. Scegli una palette di partenza: Salvia, Blu sport o Lavanda.
4. Apri **Personalizza i colori principali** per scegliere separatamente colore principale e accento: blu, verde, viola, petrolio, rosa, arancio, oro o grafite.
5. **Fatto** chiude il pannello; le scelte sono già salvate.

Scegliere una nuova palette ripristina i suoi colori; puoi personalizzarli nuovamente. Sono disponibili anche il ripristino dei soli colori e la modalità automatica che segue Android. Il testo sui colori personalizzati adotta un contrasto leggibile.

Le preferenze sono del dispositivo. Il cambio di stile non modifica gare, cronologia, formazioni, colori delle squadre, firme o PDF.

## Preparazione e navigazione

In B e C la barra superiore è compatta. **⋮** apre Gara, Dati, Registro, Referto, Stile, Guida, comandi di annullamento/ripristino e ritorno alle gare. La precedente barra inferiore non occupa lo spazio del campo.

Nella preparazione del set, Dati/Rose/Ufficiali aprono le sezioni corrispondenti. La riga di servizio e lato apre il sorteggio o il riepilogo delle scelte alternate. Le sei maglie seguono il foglietto dell'allenatore: **4–3–2 sopra, 5–6–1 sotto**. I numeri disponibili escludono quelli già sul campo; si possono trascinare anche le maglie fra le posizioni. C indica la capitana. Il libero resta fuori dalla formazione iniziale.

**Conferma formazione** passa all'altra squadra se manca ancora la sua formazione. Con entrambe complete il comando permette di iniziare il set, dopo il sorteggio e il controllo delle firme preliminari. **Copia precedente** riprende il tagliando del set prima. Fra i set il risultato precedente è una riga compatta, non un lungo avviso.

Il referto raggruppa firme prima/dopo la gara, evidenzia quelle mancanti o da verificare e mostra la pagina PDF effettiva. Nei layout B/C l'anteprima viene preparata all'apertura della schermata Referto; la consegna avviene dai comandi espliciti di salvataggio e condivisione.

## Leggibilità nella 0.9.2

La barra Stato di preparazione è verde e alta 12 dp, indipendentemente dalla palette. B e C usano testi più grandi e titoli in grassetto; le sezioni completate hanno check verdi circolari. Il tagliando mostra la legenda **C giallo = Capitano**, **L azzurro = Libero**. La rete ha maglie, bande e pali più evidenti nei temi chiaro e scuro.

## Confronti e schermate native

- [Proposta B e implementazione](ui/confronto-B-0.9.2.png)
- [Proposta C e implementazione](ui/confronto-C-0.9.2.png)
- B: [preparazione](ui/B-preparazione-0.9.2.png), [dettagli nel pannello](ui/B-modifica-dati-0.9.2.png), [gara](ui/B-gara-0.9.2.png), [firme/referto](ui/B-referto-0.9.2.png).
- C: [terzo set con nomi lunghi](ui/C-preparazione-set3-0.9.2.png), [primo set chiaro](ui/C-preparazione-chiaro-0.9.2.png), [gara](ui/C-gara-0.9.2.png), [firme/referto](ui/C-referto-0.9.2.png).

Catture delle schermate Android effettive con dati fittizi e grafica nativa Robolectric. Dove è aperto un pannello, il compositore di test unisce la finestra Android del pannello a quella dell'Activity, con la sua trasparenza. I confronti ridimensionano le immagini alla stessa larghezza: il concept e il dispositivo di prova hanno proporzioni differenti. Le [proposte originali](design/PROPOSTE-UI.md) restano come riferimento; numeri e miniature PDF illustrativi non sostituiscono regolamento e modello FIPAV reali.

## Staff e incarichi · 0.9.3

Aggiungi persona / incarico apre un modulo con ruolo e nome: Conferma inserisce la persona già selezionata per questa gara. In Ufficiali, gli incarichi di servizio si possono aggiungere anche senza proposte importate. Le proposte ricevute continuano a richiedere la spunta; ambito, descrizione e codice sorgente si trovano in Dettagli importazione. La panchina manuale mostra righe compatte con Aggiungi/Modifica e permette di rimuovere il nominativo. [Modulo effettivo con dati fittizi](ui/staff-entry-0.9.3.png).
