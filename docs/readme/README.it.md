<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

Un'app semplice e attenta alla privacy per tracciare gli allenamenti a corpo libero su Android.

---

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/">
    <img src="https://fdroid.org/badge/get-it-on.png" alt="Disponibile su F-Droid" height="80">
  </a>
</p>
<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" alt="Disponibile su IzzyOnDroid" height="54">
  </a>
</p>

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Deutsch](README.de.md) | [Español](README.es.md) | [Français](README.fr.md) | [简体中文](README.zh-CN.md)

---

## Informazioni

Calisthenics Memory ti aiuta a tracciare e gestire esercizi a corpo libero come flessioni, trazioni e squat. Crea esercizi personalizzati, organizzali in livelli progressivi e monitora i tuoi progressi.

L'app funziona completamente offline — nessuna connessione Internet richiesta, nessuna pubblicità, nessun tracciamento. I tuoi dati rimangono solo sul tuo dispositivo.

## Punti Chiave

- **Personalizzazione completa** - Nessuna funzionalità bloccata per esercizi personalizzati. Ripetizioni/tempo, unilaterale/bilaterale, obiettivi, timer - tutto disponibile per ogni esercizio
- **Due modalità di registrazione** - Inserimento manuale rapido o allenamento guidato con timer
- **Solo offline** - I tuoi dati non lasciano mai il tuo dispositivo

## Funzionalità

- **Dashboard Home** - Visualizza i record di allenamento di oggi a colpo d'occhio, tieni premuto per copiare
- **Completamente personalizzabile** - Crea esercizi liberamente, organizza per gruppi, gestisci con 10 livelli, riordina con i pulsanti freccia, traccia distanza e peso per esercizio
- **Preferiti** - Accesso rapido agli esercizi usati frequentemente
- **Due modalità di registrazione**
  - Modalità registrazione: Inserimento manuale rapido con pulsante "Applica impostazioni esercizio"
  - Modalità allenamento: Allenamento guidato automatico con impostazioni timer per esercizio (intervallo di riposo, durata ripetizione), notifica flash LED al completamento della serie
- **Monitoraggio progressi** - Visualizza i record come liste, grafici o barre di progresso delle sfide
- **Supporto unilaterale/bilaterale** - Traccia i lati sinistro e destro separatamente per esercizi unilaterali
- **Obiettivi sfida** - Imposta serie × ripetizioni target e traccia lo stato di raggiungimento
- **Gestione dati** - Esporta/importa in formato JSON o CSV (supporto backup completo)
- **Multilingua** - Inglese, giapponese, spagnolo, tedesco, cinese (semplificato), francese, italiano
- **Privacy first** - Completamente offline, nessun permesso di runtime, nessun accesso a Internet

## Screenshot

<p align="center">
  <img src="../../screenshots/1.png" width="250"><br>
  <b>Home</b> - L'allenamento di oggi a colpo d'occhio
</p>

<p align="center">
  <img src="../../screenshots/2.png" width="250"><br>
  <b>Esercizi</b> - Organizza con gruppi e preferiti
</p>

<p align="center">
  <img src="../../screenshots/3.png" width="250"><br>
  <b>Registrazione</b> - Inserimento manuale rapido
</p>

<p align="center">
  <img src="../../screenshots/4.png" width="250"><br>
  <b>Allenamento</b> - Allenamento guidato con timer
</p>

<p align="center">
  <img src="../../screenshots/5.png" width="250"><br>
  <b>Grafico</b> - Traccia i tuoi progressi
</p>

<p align="center">
  <img src="../../screenshots/6.png" width="250"><br>
  <b>Sfida</b> - Stato raggiungimento obiettivi
</p>

## Requisiti

- **Android** 8.0 (API 26) o superiore
- **Spazio** ~10MB
- **Internet** Non richiesto

## Permessi

Questa app utilizza solo **permessi normali (al momento dell'installazione)**, che vengono concessi automaticamente durante l'installazione senza richieste all'utente.

A partire dalla v1.9.0, sono inclusi i seguenti permessi:

| Permesso | Scopo | Aggiunto da | Sorgente |
|----------|-------|-------------|----------|
| `FOREGROUND_SERVICE` | Esegue il timer allenamento come servizio in primo piano | App (v1.9.0) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Tipo di servizio in primo piano per timer allenamento | App (v1.9.0) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `WAKE_LOCK` | Mantiene il timer attivo quando lo schermo è spento | App (v1.8.1) | [WorkoutTimerService.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/service/WorkoutTimerService.kt) |
| `FLASHLIGHT` | Notifica flash LED durante la modalità allenamento | App (v1.8.0) | [FlashController.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/FlashController.kt) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Protezione di sicurezza per componenti interni | Libreria AndroidX (automatico) | - |

### Cosa sono i permessi normali?

Android classifica i permessi in due tipi:
- **Permessi normali**: Permessi a basso rischio concessi automaticamente all'installazione. Gli utenti non possono revocarli singolarmente.
- **Permessi pericolosi**: Permessi ad alto rischio che richiedono l'approvazione esplicita dell'utente (es.: fotocamera, posizione, contatti).

Questa app non richiede alcun permesso di runtime.

Per maggiori dettagli:
- [Panoramica dei tipi di permessi Android](https://developer.android.com/guide/topics/permissions/overview)
- [Elenco completo dei permessi normali](https://developer.android.com/reference/android/Manifest.permission)

### Nota

I permessi normali vengono concessi automaticamente e potrebbero non apparire negli elenchi degli app store. Li documentiamo qui per trasparenza.

## Compilazione

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

Richiede JDK 17 o superiore.

## Licenza

Questo progetto è rilasciato sotto la GNU General Public License v3.0. Vedi [LICENSE](../../LICENSE) per i dettagli.