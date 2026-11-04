# Proposte di ridisegno · Referto Volley

Concept da confrontare prima di applicare cambiamenti all'interfaccia. Non sono nuove schermate già implementate. La scelta riguarda prima la struttura; ogni direzione potrà poi usare palette chiare e scure.

## Tavole concettuali

- [A · Percorso guidato](proposta-A-percorso-guidato.png)
- [B · Cruscotto gara](proposta-B-cruscotto.png)
- [C · Campo al centro](proposta-C-campo.png)

Le tavole sono mockup generati per discutere organizzazione e stile, non rendering dell'app o file Figma. Figma e Product Design sono stati richiesti ma non espongono strumenti o skill disponibili nella sessione. Dati, disposizioni dei singoli atleti e miniature del PDF sono illustrativi: non costituiscono specifiche regolamentari. L'implementazione manterrà motore e regole esistenti, tagliando valido senza libero nella formazione iniziale, disposizione laterale coerente del campo, firma e modello PDF FIPAV attuali. Conteggi, risultati e numeri saranno derivati dai dati reali.

## Problema da risolvere

Nella preparazione informazioni, spiegazioni e azioni competono per lo stesso spazio. Serve rendere evidente ciò che manca, distinguere i dati obbligatori da quelli facoltativi e permettere correzioni senza perdere il lavoro. Durante la gara contano accesso rapido a punti e operazioni, corretta associazione alla squadra e annullamento affidabile.

## Vincoli comuni

- Pagina iniziale con schede gara riconoscibili e cancellazione con conferma.
- Nomi squadra distinguibili, colori scelti e nomi leggibili: il colore non è l'unico indizio.
- Dettagli secondari aperti solo quando servono; spazio prioritario ai contenuti.
- Rosa riutilizzabile, selezione delle presenti separata dalla modifica dei dati; foto CAMP3 e import restano accessi espliciti.
- Tagliando dal fondo verso la rete: 4–3–2 sopra, 5–6–1 sotto, numeri trascinabili.
- Campo durante la gara visto dal tavolo del refertista, rete verticale, rotazioni coerenti, pallone su chi serve.
- Pulsanti punto e operazioni seguono la disposizione delle squadre in campo; annullamento sempre raggiungibile.
- Firme iniziali e finali distinte. Le correzioni descrittive conservano le firme; le modifiche sportive mostrano un avviso di verifica.
- Firma in orizzontale e anteprima del referto classico prima di esportarlo.
- Tocchi agevoli, etichette leggibili, stati comunicati anche con testo; verificare su schermi stretti e con caratteri ingranditi.

## A · Percorso guidato

Preparazione in tappe: Gara → Rose → Ufficiali → Formazioni. Una domanda principale per schermata, pulsante Continua fisso e accesso al riepilogo per correggere i passaggi precedenti. Le informazioni opzionali sono raccolte in pannelli chiusi.

In gara: punteggi per squadra, campo e una barra compatta per timeout, sostituzioni e libero. L'operazione scelta apre un pannello breve con squadra e conferma esplicite. Annulla rimane visibile.

Conclusione: elenco ordinato delle firme da raccogliere, firme iniziali già raccolte in un riepilogo e referto accessibile prima dell'esportazione.

Controlli: grandi righe selezionabili, checkbox di presenza, schede arrotondate e un'azione principale evidente.

Vantaggio: adatto a chi usa l'app saltuariamente. Compromesso: per attraversare tutta la preparazione servono più passaggi.

## B · Cruscotto gara

Una panoramica mostra quattro sezioni con stato, conteggi e modifica: dati gara, rose, ufficiali e formazioni. Si può compilare in qualsiasi ordine; la prossima azione suggerita resta evidente. Toccare una sezione apre soltanto i dettagli pertinenti.

In gara: punteggi, campo e contatori per ciascuna squadra sono allineati. Breve cronologia dell'ultima azione, Annulla e Ripristina sempre accessibili. Le azioni aprono pannelli focalizzati, senza trasformare la schermata in un lungo modulo.

Conclusione: risultato, stati delle firme e anteprima PDF in un riepilogo unico.

Controlli: righe compatte, etichette di stato, segmenti per le scelte brevi, pulsanti pieni solo per le azioni principali.

Vantaggio: si capisce subito che cosa è pronto e che cosa manca. Compromesso: mostra più informazioni contemporaneamente di A, quindi richiede una gerarchia molto rigorosa.

## C · Campo al centro

Preparazione orientata alla formazione: campo/tagliando e numeri della rosa dominano lo schermo; dati, rose e ufficiali si aprono da un riepilogo compatto con stato. La conferma verifica eventuali elementi mancanti e riporta al punto da correggere.

In gara: campo ampio, punteggio e pulsanti punto allineati ai lati, barra comandi vicina al pollice. Sostituzioni, timeout e libero usano pannelli dal basso che mantengono visibile il contesto della squadra e del punteggio.

Conclusione: anteprima del referto e pannello delle firme ancora mancanti, con accesso alla firma orizzontale.

Controlli: numeri trascinabili, selezione prima della conferma, pannelli brevi e controlli compatti. La presentazione scura è una scelta dimostrativa, non un vincolo di questa struttura.

Vantaggio: ideale per concentrarsi sullo svolgimento della partita. Compromesso: dati e incarichi sono meno visibili finché non si apre il riepilogo.

## Confronto e proposta consigliata

| Aspetto | A | B | C |
| --- | --- | --- | --- |
| Preparazione | Sequenza assistita | Panoramica con stati | Formazione al centro |
| Prossimo passo | Continua | Azione suggerita | Conferma e verifica |
| Correzioni | Riepilogo delle tappe | Apertura diretta della sezione | Riepilogo compatto |
| Gara | Comandi essenziali | Punteggi, campo e contatori | Campo ampio e pannelli |
| Densità | Bassa | Media | Bassa sul campo |

Per le esigenze emerse, la combinazione più promettente è **B per la preparazione e C per la gara**. Lo stato complessivo riduce i dati dimenticati; durante il gioco il campo torna protagonista. A resta la scelta più semplice per un refertista occasionale.

Prima di implementare: scegliere una direzione o una combinazione; poi rifinire in dettaglio rose/presenti, sorteggio, formazione e flusso delle firme. In questa fase il codice dell'app e l'APK non vengono modificati.
