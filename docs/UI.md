# Interfaccia 0.6.4

Tema Material 3 con palette verde/avorio, accento ambra per la nuova gara, superfici chiare e gerarchia tipografica coerente. I colori assegnati alle squadre restano indipendenti dal tema.

- **Inizio:** azione principale ben distinta, dimostrazione e gare salvate separate. Il cestino elimina una gara dopo la conferma con squadre e data; rose riutilizzabili e JSON esportati restano disponibili.
- **Gara:** barra e punteggi compatti, colori squadra sui pulsanti, icone vettoriali nella navigazione. Ultima azione e avvisi scorrono con il campo. Il test a 411×891 dp verifica oltre il 60% dello schermo disponibile per il contenuto scorrevole. I controlli di ogni squadra occupano due righe: timeout, poi cambi/libero.
- **Distinta:** righe compatte, presenze e icona matita per i dettagli; caricamento, salvataggio, colore e foto hanno comandi distinti.
- **Compilazione:** campi arrotondati e selettori tonali con indicazione di apertura.
- **Foto:** controllo a segmenti per Blocca/Ritaglia/Maglie, con pulsante di lettura sempre accessibile.

Le icone decorative sono accompagnate da testo; matita e colore hanno un nome accessibile. I comandi conservano le aree touch minime Material. Non vengono aggiunti font remoti o dipendenze di rete.

Queste immagini provengono dal rendering delle schermate Compose nei test Android, con dati dimostrativi fittizi.

| Gara compatta | Tagliando formazioni |
| --- | --- |
| ![Gara](ui/gara-0.6.4.png) | ![Tagliando](ui/formazioni-0.6.4.png) |

Il tagliando mostra la rete in alto e le zone **4–3–2 / 5–6–1**, identiche per entrambe le squadre. I numeri si trascinano dopo una pressione prolungata, oppure si assegnano toccando prima il numero e poi la zona. Un numero già assegnato scambia posto; un numero dalla panchina sostituisce quello nella zona. **Svuota** libera tutte le zone. Le zone vuote e le scelte mancanti del sorteggio impediscono l’avvio.

Il sorteggio è all’inizio della preparazione del primo e del quinto set: **Chi serve per primo?** e **Chi è a sinistra del tavolo?**. Il secondo riferimento è sempre la vista del segnapunti. Negli altri set servizio e lati si alternano automaticamente. Le formazioni partono vuote; dopo il primo set è disponibile **Copia formazione del set precedente**, da confrontare con il nuovo tagliando. Durante la gara rimane il campo visto dal tavolo del segnapunti.


Verifiche: suite di 128 test, build APK e lint Android. Restano da verificare sul telefono tastiera, dimensione caratteri personalizzata e comportamento dei dialoghi di sistema.
