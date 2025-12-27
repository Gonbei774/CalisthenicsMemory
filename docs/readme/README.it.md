<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

<h1 align="center">Calisthenics Memory</h1>

<p align="center">
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/README.md">English</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.ja.md">日本語</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.de.md">Deutsch</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.es.md">Español</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.fr.md">Français</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <a href="https://ci.codeberg.org/repos/15682"><img src="https://ci.codeberg.org/api/badges/15682/status.svg" alt="Build Status"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License: GPL-3.0">
  <img src="https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/yearly/rolling.json&query=$.['io.github.gonbei774.calisthenicsmemory']&label=Downloads" alt="Downloads">
  <a href="https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory"><img src="https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory" alt="Reproducible Builds"></a>
</p>

<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="Disponibile su IzzyOnDroid"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/"><img src="https://fdroid.org/badge/get-it-on.png" height="119" alt="Disponibile su F-Droid"></a>
</p>

## Informazioni

Un tracker per allenamenti a corpo libero. Crea esercizi personalizzati, organizza per gruppi e livelli, traccia i tuoi progressi – completamente offline.

## Funzionalità

- **Dashboard Home** - Visualizza i record di allenamento di oggi a colpo d'occhio, tieni premuto per copiare
- **Completamente personalizzabile** - Crea esercizi liberamente, organizza per gruppi, gestisci con 10 livelli, traccia distanza e peso per esercizio
- **Preferiti** - Accesso rapido agli esercizi usati frequentemente
- **To Do** - Pianifica il tuo allenamento aggiungendo esercizi a un elenco attività giornaliero, tocca per passare direttamente alle schermate Registrazione o Allenamento
- **Due modalità di registrazione**
  - Modalità registrazione: Inserimento manuale rapido con pulsante "Applica impostazioni esercizio"
  - Modalità allenamento: Allenamento guidato automatico con impostazioni timer per esercizio (intervallo di riposo, durata ripetizione), notifica flash LED al completamento della serie
- **Programma** - Crea routine con più esercizi con modalità Timer ON (conto alla rovescia) o Timer OFF (al tuo ritmo), intervalli di riposo configurabili tra le serie
  - Navigazione Jump/Redo/Finish per un controllo flessibile del progresso
  - Salva ed Esci per mettere in pausa e riprendere dopo
  - Valori precedenti mostrati sulle schede esercizio come riferimento
  - Regola serie e obiettivi durante l'esecuzione
- **Monitoraggio progressi** - Visualizza i record come liste, grafici o barre di progresso delle sfide
- **Supporto unilaterale/bilaterale** - Traccia i lati sinistro e destro separatamente per esercizi unilaterali
- **Obiettivi sfida** - Imposta serie × ripetizioni target e traccia lo stato di raggiungimento
- **Gestione dati** - Esporta/importa in formato JSON o CSV (supporto backup completo)
- **Multilingua** - Inglese, giapponese, spagnolo, tedesco, cinese (semplificato), francese, italiano
- **Privacy first** - Completamente offline, nessun permesso pericoloso, nessun accesso a Internet

## Screenshot

<p align="center">
  <img src="../../screenshots/1.png" width="250">
  <img src="../../screenshots/2.png" width="250">
  <img src="../../screenshots/3.png" width="250">
</p>
<p align="center">
  <img src="../../screenshots/4.png" width="250">
  <img src="../../screenshots/5.png" width="250">
  <img src="../../screenshots/6.png" width="250">
</p>

## Requisiti

- **Android** 8.0 (API 26) o superiore
- **Spazio** ~10MB
- **Internet** Non richiesto

## Permessi

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - Eseguire il timer in background
- `FLASHLIGHT` - Notifica flash per intervalli di riposo

## Compilazione

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

Richiede JDK 17 o superiore.

## Licenza

Questo progetto è rilasciato sotto la GNU General Public License v3.0. Vedi [LICENSE](../../LICENSE) per i dettagli.

---

**Ultimo aggiornamento**: 6 dicembre 2025