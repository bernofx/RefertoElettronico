# Fonti e comportamento del referto

Fonti consultate il **30 ottobre 2026**. Ambito: U14 femminile, Comitato territoriale di Como, stagione 2026/27. I documenti sono collegati dalle pagine ufficiali del Comitato e del Settore nazionale Ufficiali di gara.

| Fonte | Riferimento verificato | Applicazione |
| --- | --- | --- |
| [Regolamento U14F Como 2026/27](https://www.fipav.como.it/data-files/fipavcomo/dropbox/Regolamento%20U14F%20Stagione%202026-2027.pdf) | pag. 1: rete 2,15 m, libero e secondo libero ammessi, tre set su cinque; pag. 3: eccezione al referto elettronico per assenza totale di rete o problemi del portale | Profilo predefinito e motivo del cartaceo |
| [Commissione gare Como 2026/27](https://www.fipav.como.it/article.php?article_id=1288) | Indizioni e regolamenti territoriali, inclusa l'indizione revisione 05 | Provenienza del regolamento |
| [Nuove regole, Como](https://www.fipav.como.it/article.php?article_id=1320) | Lettera federale del 30/09/2026: applicazione dal 28/09 in ogni serie e categoria, con le eccezioni tecnologiche indicate | Profilo 2026/27 |
| [Sperimentazioni regolamentari 2026/27](https://www.fipav.como.it/data-files/fipavcomo/article_attachments/NuoveregoleperCRCTDEF.pdf) | Sezione “Il limite sale a 8 sostituzioni per set”: 8 sostituzioni, 2 timeout; invariati i vincoli della regola 15.6 | Contatori e abbinamenti |
| [Documenti nazionali FIPAV](https://www.federvolley.it/documenti-UG) | Manuale del Segnapunti addetto al referto cartaceo 2025/26, autore Settore Nazionale Ufficiali di Gara | Compilazione e controllo |
| Manuale cartaceo, §2.2.6 | Un libero: numero della rimpiazzata, barrato al rientro; due liberi: libero-rimpiazzata, con eventuale ulteriore libero nella medesima annotazione | Registro libero |
| Manuale cartaceo, §2.2.9 | Sottolineare l'ultimo punto, annullare i punti non usati, cerchiare l'ultimo parziale di entrambe le squadre; turno non spuntato se il punto finale è vinto in ricezione | Chiusura del set |
| Manuale cartaceo, §2.2.10 | Quinto set: nuovo sorteggio; a 8 cambio campo; passaggio dalla sezione sinistra a quella destra senza ricopiare i turni già conclusi | Riquadro decisivo |
| Manuale cartaceo, §2.3.1 | Riepilogo, somme, tempi, vincente; conteggiare anche le sostituzioni eccezionali nel riepilogo | Risultato finale |
| [Avviso Como del 28/10/2026](https://www.fipav.como.it/article.php?article_id=1341) | Invio del referto e degli altri documenti gara secondo le disposizioni del Comitato | Nessun invio automatico |

## Provenienza del modello incorporato

`app/src/main/assets/referto-fipav.pdf` incorpora l'immagine del referto della **figura 82, ultima pagina PDF (44)** del Manuale nazionale 2025/26, con orientamento orizzontale. L'immagine del PDF è conservata alla risoluzione originale; `referto-preview.png` è soltanto la versione ridotta per l'anteprima sul telefono, disegnata con le medesime registrazioni per evitare la decodifica della grande immagine di stampa; la geometria logica del modello è 1190 × 730 punti. L'output è su foglio A3 orizzontale (1190 × 842 punti), con titolo di stato, modello, indicazione delle firme e allegati. Non è il precedente schema sperimentale preparato durante l'onboarding, che è stato scartato.

URL del manuale collegato dalla pagina nazionale al momento della consultazione:

https://overjoyed-wilson.s3.eu-south-mil.io.cloud.ovh.net/s3fs-public/2026-06/Manuale_Segnapunti_2025_26_v1.pdf?VersionId=1782761693.836059

Il modulo e il marchio appartengono ai rispettivi titolari. Il riferimento al documento FIPAV non indica approvazione o certificazione di questa app.

Il font DejaVu Sans è incorporato per rendere i testi del PDF ricercabili e preservare i caratteri accentati; la licenza e gli avvisi sono in `DejaVu-LICENSE.txt`. I caratteri non presenti nel font causano un errore di esportazione esplicito anziché la perdita silenziosa del nome.

## Decisioni di implementazione

- Punteggio sempre associato all'identità della squadra. Il cambio campo modifica la posizione visualizzata, non le squadre.
- Il campo è proiettato dal tavolo del segnapunti: zona 1 in basso a sinistra nel campo sinistro e in alto a destra in quello destro. La formazione opposta ruota di 180 gradi, mantenendo i numeri leggibili. Gli spostamenti sono animati per identità della giocatrice (650 ms), anche dopo Annulla. Il cambio dei lati non anima un attraversamento della rete.
- Le posizioni del campo sono 1..6; una rotazione porta la giocatrice di zona 2 in zona 1. I turni di servizio rimangono associati al posto iniziale I..VI.
- Il libero resta distinto dalla giocatrice rimpiazzata. Una rotazione può richiederne l'uscita: l'app la segnala e impedisce la successiva azione finché non viene registrata. Rimpiazzi forzati senza azione richiedono una motivazione arbitrale.
- Una penalizzazione assegna punto e servizio all'avversaria, ma non conta come azione completata per il libero.
- La sostituzione normale verifica coppia e rientro della titolare, nonché il limite di set. Espulsione, squalifica e inabilità impediscono di continuare con la giocatrice indisponibile.
- I controlli dell'app non determinano l'avvenuta autorizzazione arbitrale. Tutti gli inserimenti avvengono a palla fuori gioco, dopo la decisione degli ufficiali.
- Gli annullamenti sono correzioni del segnapunti. Non simulano la decisione arbitrale di annullare punti per un fallo di rotazione scoperto successivamente: tale ricostruzione richiede le disposizioni dell'arbitro e le relative annotazioni.
- Il registro allegato include soltanto gli eventi accettati nel ramo corrente; gli annullati restano ripristinabili nella copia JSON finché non si registra un nuovo evento. Eccedenze di nomi, turni o libero sono riportate nell'allegato, con richiamo nelle osservazioni.
- Le firme restano cartacee. Il documento prodotto non include firme inventate, risultati a tavolino automatici né attestazioni di omologazione.
