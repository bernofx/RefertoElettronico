# Tutorial e guida nell’app

Dalla **0.6.9** il tutorial usa le schermate reali; la **0.8.0** introduce preparazione guidata e conservazione delle firme: un velo scuro lascia scoperto il controllo spiegato e il riquadro in sovraimpressione descrive l’operazione. Il bersaglio segue il layout effettivo e viene portato in vista scorrendo la pagina, senza coordinate fisse.

Il percorso comprende preparazione e presenti, sorteggio, tagliando 4–3–2 / 5–6–1, punti e rotazioni, timeout, cambi, ingressi e uscite del libero, Annulla/Ripristina, Registro, osservazioni, sanzioni e altre decisioni arbitrali, firme preliminari e approvazione finale. Avanti mostra gli effetti su una gara dimostrativa. Alla fine il generatore reale produce il PDF cartaceo classico FIPAV di una gara completata 3–0 con dati e firme fittizi e ne mostra l’anteprima effettiva.

I comandi Avanti, Indietro ed Esci restano disponibili nel riquadro; il testo scorre se necessario. I controlli sottostanti sono illustrati, non azionabili durante la visita. Le azioni vengono simulate dal motore di gara, senza registrarle nell’archivio. Le finestre di timeout, cambio, libero, annotazione e sanzione usano lo stesso modulo della gara reale, incorporato nella finestra del tutorial.

Il tutorial vive in una finestra separata: non naviga nella gara reale, non salva configurazioni, rose o bozze, non crea schede nella home. Il PDF dimostrativo è temporaneo nella cache e viene eliminato alla chiusura. Le schermate e la gara aperta rimangono composte dietro la guida e si ritrovano come prima uscendo. Eventuali dati arrivati da VolleyAlerts attendono la chiusura della guida prima della revisione.

Si apre al primo avvio o quando cambia la revisione dei contenuti. Si può riaprire da **Guida** nella pagina iniziale, dal pulsante dell’editor e dalla scheda **Guida** durante la gara. La scheda raccoglie gli stessi testi per consultazione rapida. Esci/Fine segna la revisione come vista; la visita non si ripete a ogni apertura.

## Mantenere la guida aggiornata

1. Aggiornare `AppGuide.steps` con titolo, spiegazione, consiglio, schermata, identificativo del controllo e fase dell’esercitazione.
2. Se si sposta o sostituisce un controllo, aggiornare il suo `Modifier.tourTarget`: deve identificare il controllo reale, non un duplicato disegnato sopra. I modificatori non hanno effetto fuori dal tutorial.
3. Aggiornare `GuideExample` quando cambia la sequenza dimostrata: ogni evento deve passare attraverso il normale motore e rispettarne i vincoli. Non usare `MatchStore`, `DraftStore` o la libreria delle rose nell’esercitazione.
4. Aggiornare `AppGuide.updatedFor` e incrementare `AppGuide.revision` per cambiamenti sostanziali. Non incrementare la revisione per sole rifiniture estetiche.
5. Verificare tutti i bersagli, scorrimento, Avanti/Indietro/Esci/Fine, generazione e anteprima PDF, ripristino della schermata e integrità degli archivi. Controllare anche uno schermo piccolo e l’orientamento orizzontale su dispositivo.

## Icona adottata

Rimane l’icona C6 approvata: sfondo giallo, referto blu e pallone con pannelli blu/gialli/bianchi. Icona adattiva con margine centrale per le maschere dei launcher.

Le immagini in `docs/ui` sono rendering Android con Robolectric native graphics, non fotografie di un telefono. Installazione, gesti e rotazioni su dispositivo restano da verificare.

## Anteprime verificate

- [Campo e rotazione](ui/tutorial-0.8.0-campo.png)
- [Modulo del libero](ui/tutorial-0.8.0-libero.png)
- [Anteprima PDF nel tutorial](ui/tutorial-0.8.0-pdf.png)
- [PDF completo dell’esercitazione](../downloads/RefertoVolley-tutorial-0.8.0.pdf)

La revisione 4 spiega i quattro passi di preparazione, il prossimo dato da completare e la conservazione delle firme dopo le correzioni. Le variazioni sportive vengono segnalate senza cancellare i tratti; le correzioni descrittive non richiedono nuove firme.

Nella **0.8.1** la voce **Aspetto e temi** si trova nella pagina iniziale e nella scheda Dati: tre palette con modalità chiara, scura o automatica. [Anteprime e istruzioni](TEMI.md). Le quattro schede di preparazione ora riportano Gara, Casa, Ospiti e Ufficiali.

La **0.9.0**, revisione guida **5**, introduce B Cruscotto e C Campo al centro: la guida spiega accesso agli stili, riepilogo di preparazione e selezione della squadra nella barra di C. I bersagli del tutorial seguono i moduli effettivi nei due layout. [Scelta e personalizzazione](TEMI.md).
