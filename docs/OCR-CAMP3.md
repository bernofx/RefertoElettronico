# Lettura dei due CAMP3 da foto

Disponibile dalla versione 0.3.0. Ogni squadra viene acquisita separatamente: **due documenti, due rose da revisionare**.

## Uso

1. Aprire **Dati → Modifica i dati della gara → Squadra di casa** (oppure ospite).
2. Prima dell'inizio della gara, premere **Leggi CAMP3 da foto**.
3. **Fotografa** apre la fotocamera Android; **Carica foto** seleziona un'immagine già disponibile. Inquadrare la tabella completa delle atlete, con il foglio disteso e luce uniforme. Sono accettate fotografie/immagini, non file PDF.
4. Se necessario, usare **Ruota di 90° e rileggi**. Se la lettura è scadente, scegliere un'altra foto o ritagliarla con l'editor del telefono sulla sola tabella delle atlete.
5. Controllare le righe suggerite. Correggere nome e numero, escludere le righe estranee, aggiungere eventuali atlete non riconosciute. Selezionare **K** per la capitana e **L** per i liberi. I ruoli non vengono dedotti automaticamente.
6. Confermare **Sostituisci rosa** solo dopo i controlli. L'operazione riguarda esclusivamente la squadra indicata nel titolo, non cambia i dati dell'altra squadra e non importa dati gara o ufficiali. Non viene dedotto automaticamente da quale squadra provenga la foto.
7. Ripetere nella scheda dell'altra squadra con il suo CAMP3. Salvare le modifiche della gara per conservarle sul dispositivo.

Chiudere senza importare mantiene la rosa precedente. Dopo l'inizio del primo set la funzione è disabilitata come le altre modifiche a numeri e ruoli: occorre annullare fino all'inizio per cambiarli. Le modifiche al modulo si salvano con **Salva modifiche**, non premendo il solo comando OCR.

## Riconoscimento e limiti

- Il riconoscitore è Google ML Kit Text Recognition per caratteri latini, con modello incluso nell'APK; la lettura e l'analisi avvengono sul dispositivo. L'app non ha il permesso Internet.
- L'app gestisce l'orientamento EXIF e riduce le immagini molto grandi per contenere la memoria. Foto poco nitide, prospettiva, righe inclinate o testo piccolo possono ridurre il riconoscimento.
- Le parole vengono raggruppate per riga e, quando leggibili, vengono usate le intestazioni COGNOME/NOMINATIVO e MAGLIA per proporre nomi e numeri. Numeri di tessera, progressivi e date di nascita non devono essere considerati numeri di maglia.
- Senza un'intestazione MAGLIA identificabile, i numeri restano vuoti e vanno inseriti. È intenzionale: un progressivo o una parte della data non è un numero di maglia verificato.
- Il parser è una prima implementazione con euristiche, non un riconoscimento certificato di tutti i modelli CAMP3. Può perdere righe, includere testi estranei o associare erroneamente un valore. La revisione è sempre necessaria, anche per numeri proposti automaticamente.
- Per confermare servono 6–14 atlete, numeri unici tra 1 e 99, una capitana, almeno sei non liberi e al massimo due liberi. Con oltre 12 atlete serve almeno un libero. Si importano soltanto le righe selezionate.
- I campioni di test sono sintetici. L'accuratezza sul CAMP3 effettivamente utilizzato a Como e la fotocamera devono ancora essere verificate su un telefono reale.

## Dati e foto

La foto acquisita viene conservata temporaneamente nella cache privata dell'app e rimossa alla chiusura dell'importazione. Se Android termina il processo durante l'acquisizione può restare un file di cache, eliminabile dal sistema. Le immagini già presenti sul dispositivo restano nella posizione originale. Nessuna foto viene allegata alla gara o al PDF. Il testo OCR completo serve solo durante la revisione e non viene salvato; il salvataggio della gara conserva soltanto i nominativi, i numeri e i ruoli confermati.
