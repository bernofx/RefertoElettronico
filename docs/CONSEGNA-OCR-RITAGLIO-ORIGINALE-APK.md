# Referto Volley — OCR sul ritaglio originale e nuovo APK

## Richiesta da eseguire

Modifica Referto Volley affinché il riconoscimento definitivo della lista giocatrici o dello staff legga il ritaglio dalla foto originale, anziché ritagliare una foto già ridimensionata. Esegui le verifiche, committa e pusha su main, poi genera e pubblica il nuovo APK usando la stessa chiave di firma degli APK già installati.

Repository: https://github.com/bernofx/RefertoElettronico

Questa consegna è stata **implementata nella versione 0.6.1**. La descrizione seguente conserva i requisiti e lo stato iniziale per la verifica. Restano la prova finale del decoder Android sul dispositivo e il confronto dell'accuratezza sulle foto reali; i test in cloud non li sostituiscono. Il codice di VolleyAlerts non è modificato.

## Stato verificato di partenza

Alla verifica del 30 ottobre 2026:

- main era al commit `99eccbeffae958ad08935d118d9497931a198de7`, che pubblica l’APK 0.6.0.
- Il codice della 0.6.0 proviene dal commit `8318006588f6fca88e25df93b8e2fa04437ce256`.
- La 0.6.0 include il collegamento dedicato da VolleyAlerts: conservarlo, insieme a tutte le altre funzioni già presenti.
- Prima di lavorare, leggi le istruzioni del repository e verifica il main corrente. Se sono presenti aggiornamenti successivi, parti da quelli senza sovrascriverli.
- L’ambiente originale di compilazione e firma è disponibile nell’altra chat.

### Comportamento attuale

File verificati:

- `app/src/main/java/it/refertoelettronico/ocr/Camp3Ocr.kt`
- `app/src/main/java/it/refertoelettronico/ocr/PhotoCrop.kt`
- `app/src/test/java/it/refertoelettronico/Camp3PhotoTest.kt`
- `docs/OCR-CAMP3.md`

`decodePhoto(context, uri, quarterTurns)` prepara la foto con lato lungo massimo di 3.600 pixel, correggendo orientamento e rotazione. Su Android 8 usa BitmapFactory con campionamento e un limite di memoria; su Android 9+ usa ImageDecoder. Il percorso ImageDecoder limita anche la dimensione del file codificato a 32 MB.

Successivamente `readPhoto(photo, crop, numberColumn, section)` applica il ritaglio a quel bitmap già ridimensionato e lo passa a ML Kit.

Di conseguenza, una tabella piccola dentro la foto dell’intero foglio può aver già perso dettagli prima di essere ritagliata. Ingrandire quel ritaglio non recupera i pixel persi.

## Comportamento richiesto

### 1. Distinguere anteprima e immagine da riconoscere

- È corretto mantenere un’anteprima ridimensionata per mostrare e ruotare la foto, selezionare il rettangolo e indicare la colonna delle maglie.
- Conserva il riferimento alla sorgente originale per tutta la sessione di acquisizione.
- Il riconoscimento definitivo deve rileggere la zona selezionata dalla sorgente originale.
- Non usare il bitmap dell’anteprima come sorgente dell’OCR definitivo.

### 2. Ritagliare prima di ridimensionare

Ordine richiesto per la lettura definitiva:

1. Determinare dimensioni e orientamento della sorgente.
2. Riportare la selezione dell’utente nelle coordinate dell’originale.
3. Decodificare la regione interessata preservando il dettaglio utile.
4. Correggere orientamento EXIF e rotazione manuale.
5. Ridimensionare soltanto il ritaglio, se supera il budget di elaborazione.
6. Passare il bitmap risultante al riconoscitore ML Kit.

Mantieni inizialmente il limite di 3.600 pixel sul lato lungo **del ritaglio**, senza ingrandire i ritagli più piccoli. Non aumentare semplicemente il limite della foto intera.

Usa una decodifica regionale o una strategia equivalente quando supportata. Evita di caricare inutilmente tutta una foto da 50 o più megapixel in un bitmap ARGB per estrarne una piccola porzione. Verifica le coordinate e l’ordine effettivo delle operazioni delle API scelte: impostare un crop sul decoder non garantisce da solo che il ridimensionamento avvenga dopo il ritaglio.

Gestisci esplicitamente i formati/provider per cui la decodifica regionale non è disponibile: prevedi un fallback con memoria controllata e segnala eventuali limiti reali. Non trasformare un errore di decodifica o una foto troppo grande in un blocco dell’app.

### 3. Coordinate coerenti

- Mantieni una rappresentazione normalizzata del ritaglio, indipendente dalle dimensioni dell’anteprima.
- Considera tutte le trasformazioni EXIF, inclusi ribaltamenti, e le rotazioni manuali di 90°, 180° e 270°.
- Se la decodifica regionale usa coordinate della foto non orientata, trasforma correttamente il rettangolo prima della lettura.
- Mantieni inclusi i bordi e gli ultimi pixel del rettangolo; evita scarti di una riga o colonna dovuti agli arrotondamenti.
- Riporta correttamente anche la colonna maglie indicata manualmente nelle coordinate del bitmap effettivamente passato all’OCR.
- Non applicare due volte la stessa rotazione, il ritaglio o il ridimensionamento.

### 4. Ritaglio automatico e riletture

Il primo passaggio sull’intera immagine può continuare a usare una versione ridotta per individuare la tabella. Dopo averla individuata, la seconda lettura deve estrarre quella zona dall’originale.

Anche queste azioni devono usare la sorgente originale:

- Ritaglio manuale e «Ritaglia e rileggi».
- Nuova lettura dopo aver indicato la colonna delle maglie.
- Lettura della sezione staff.
- «Foto intera», mantenendo in quel caso il normale budget per l’immagine completa.

Controlla tutti i chiamanti effettivi di `decodePhoto` e `readPhoto`, inclusi i composable/dialoghi di acquisizione: non limitarti a introdurre un helper che l’interfaccia poi non usa.

### 5. Memoria e conservazione

- Esegui decodifica e OCR fuori dal thread principale.
- Rilascia bitmap e riconoscitore nei percorsi di successo, errore, annullamento e chiusura.
- Non riciclare un bitmap ancora usato dall’anteprima.
- Mantieni i limiti di memoria e di dimensione file, documentando eventuali adattamenti necessari.
- Gestisci provider che espongono soltanto stream.
- Non modificare la foto scelta dall’archivio.
- Mantieni analisi locale e assenza del permesso Internet nell’APK.
- Non allegare foto o testo OCR integrale alla gara. Conserva solo le informazioni confermate dall’utente.
- L’importazione di una rosa o dello staff deve continuare a richiedere revisione e conferma.

## Verifiche richieste

Aggiungi test mirati e riutilizza quelli esistenti:

1. Una foto grande con un ritaglio piccolo conserva la risoluzione della regione originale, anziché quella della stessa zona nell’anteprima ridotta.
2. Un ritaglio più piccolo di 3.600 pixel non viene ingrandito; uno più grande viene ridotto mantenendo le proporzioni.
3. Le coordinate restano corrette dopo EXIF, ribaltamenti e rotazioni manuali.
4. Bordo destro e inferiore inclusi, con selezioni anche vicino ai margini.
5. La colonna maglie selezionata manualmente rimane allineata al ritaglio riconosciuto.
6. La rilettura automatica e manuale usa davvero la sorgente originale.
7. Immagine non leggibile, file troppo grande e provider solo-stream producono errori gestiti.
8. Nessuna regressione nella revisione/importazione di giocatrici e staff, nelle gare esistenti e nel collegamento da VolleyAlerts.

Esegui i test del progetto, `assembleDebug` e `lintDebug`, seguendo l’ambiente originale. Il README precedente indicava:

```sh
source /workspace/android-setup/activate.sh
./gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
```

Verifica i percorsi correnti: non assumere che siano ancora quelli indicati. Riporta eventuali controlli non eseguiti e non dichiarare un’accuratezza OCR misurata sui CAMP3 reali sulla base dei soli test sintetici.

## Versione, firma e pubblicazione APK

- Se main è ancora alla 0.6.0, usa **0.6.1**, con `versionCode 7`. Se nel frattempo è avanzato, incrementa coerentemente la versione corrente.
- Mantieni `applicationId = it.refertoelettronico`.
- Usa **la stessa chiave di firma degli APK precedenti**. Non generare una nuova chiave per l’APK da distribuire.
- Confronta il certificato dell’APK generato con quello della 0.6.0 pubblicata. Il manifest `downloads/RefertoVolley-0.6.0.build.json` riporta questo SHA-256 del certificato:

```text
6cfd99c9ea08cd62f89dc16541984acfb007916f90a86918aa89179f82ebe15f
```

- Se la chiave originale non è disponibile, completa codice e verifiche ma segnala il blocco prima di distribuire un APK con firma diversa. Non proporre la disinstallazione come passaggio automatico.
- Pubblica l’APK aggiornato in `downloads/`, con checksum SHA-256 e metadati di build analoghi a quelli della 0.6.0.
- Aggiorna README e guida OCR spiegando che l’anteprima può essere ridotta, mentre il riconoscimento definitivo rilegge il ritaglio dall’originale.
- Committa e pusha le modifiche su main senza chiedere una nuova conferma per le normali operazioni autorizzate.

## Risultato da restituire

Indica:

- Versione e commit del codice effettivamente compilato.
- Test, build e lint eseguiti con esito.
- Conferma che la firma corrisponde a quella precedente.
- Link diretto all’APK aggiornato e relativo checksum.
- Eventuali limitazioni di formato/dispositivo ancora presenti.
- Una breve prova da fare sul telefono: fotografare il foglio intero, selezionare la tabella, rileggere e confrontare numeri e cognomi con la lettura precedente.

La verifica finale sul telefono riguarda sia la qualità OCR sia l’aggiornamento dell’app conservando le gare già salvate.

## Implementazione consegnata

- `OriginalPhoto.kt`: copia originale privata e limitata, decodifica regionale, fallback controllato, proprietà e rilascio delle risorse.
- `PhotoGeometry.kt`: coordinate raw/orientate, EXIF/ribaltamenti e rotazioni manuali.
- `Camp3Ocr.kt`: riconoscimento sulla regione originale, con colonna maglie riportata sul bitmap effettivo.
- `Camp3ImportDialog.kt`: tutti i percorsi automatici e manuali usano la sorgente originale; anteprima indipendente.
- `OriginalPhotoTest.kt`: test mirati su risoluzione, geometria, memoria, stream, errori e proprietà. Per il limite di Robolectric sul codec regionale e la verifica sul telefono consultare [VERIFICA.md](VERIFICA.md).
- APK e provenienza: `downloads/RefertoVolley-0.6.1.apk`, `.sha256` e `.build.json`.
