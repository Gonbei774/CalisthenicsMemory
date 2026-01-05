<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

<h1 align="center">Calisthenics Memory</h1>

<p align="center">
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/README.md">English</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.ja.md">日本語</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.de.md">Deutsch</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.fr.md">Français</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.it.md">Italiano</a> |
  <a href="https://codeberg.org/Gonbei774/CalisthenicsMemory/src/branch/master/docs/readme/README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <a href="https://ci.codeberg.org/repos/15682"><img src="https://ci.codeberg.org/api/badges/15682/status.svg" alt="Build Status"></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-green.svg" alt="Android 8.0+">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue.svg" alt="License: GPL-3.0">
  <img src="https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/yearly/rolling.json&query=$.['io.github.gonbei774.calisthenicsmemory']&label=Downloads" alt="Downloads">
  <a href="https://shields.rbtlog.dev/io.github.gonbei774.calisthenicsmemory"><img src="https://shields.rbtlog.dev/simple/io.github.gonbei774.calisthenicsmemory" alt="Reproducible Builds"></a>
  <a href="https://translate.codeberg.org/engage/calisthenics-memory/"><img src="https://translate.codeberg.org/widget/calisthenics-memory/svg-badge.svg" alt="translated"></a>
</p>

<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="80" alt="Disponible en IzzyOnDroid"></a>
</p>

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/"><img src="https://fdroid.org/badge/get-it-on.png" height="119" alt="Disponible en F-Droid"></a>
</p>

## Acerca de

Un rastreador de entrenamientos con peso corporal. Crea ejercicios personalizados, organiza por grupos y niveles, rastrea tu progreso – completamente sin conexión.

## Características

- **Panel de inicio** - Ve los registros de entrenamiento de hoy de un vistazo, mantén pulsado para copiar
- **Totalmente personalizable** - Crea ejercicios libremente, organiza por grupos, gestiona con 10 niveles, registra distancia y peso por ejercicio
- **Favoritos** - Acceso rápido a ejercicios frecuentes
- **To Do** - Planifica tu entrenamiento añadiendo ejercicios a una lista de tareas diaria, toca para ir directamente a las pantallas de Registro o Entrenamiento
- **Dos modos de registro**
  - Modo registro: Entrada manual rápida con botón "Aplicar configuración del ejercicio"
  - Modo entrenamiento: Entrenamiento guiado automático con ajustes de temporizador por ejercicio (intervalo de descanso, duración de repetición), notificación de flash LED al completar serie
- **Programa** - Crea rutinas de múltiples ejercicios con modos Temporizador ON (cuenta regresiva) o Temporizador OFF (a tu ritmo), intervalos de descanso configurables entre series
  - Navegación Jump/Redo/Finish para control flexible del progreso
  - Guardar y Salir para pausar y continuar después
  - Valores anteriores mostrados en tarjetas de ejercicio como referencia
  - Ajustar series y objetivos durante la ejecución
- **Seguimiento del progreso** - Ve registros como listas, gráficos o barras de progreso de desafíos
- **Soporte unilateral/bilateral** - Registra lados izquierdo y derecho por separado para ejercicios de un lado
- **Objetivos de desafío** - Establece series × repeticiones objetivo y rastrea el estado de logro
- **Gestión de datos** - Exporta/importa en formato JSON o CSV (soporte de respaldo completo)
- **Multiidioma** - Inglés, japonés, español, alemán, chino (simplificado), francés, italiano
- **Privacidad primero** - Completamente sin conexión, sin permisos peligrosos, sin acceso a Internet

## Capturas de Pantalla

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

## Requisitos

- **Android** 8.0 (API 26) o superior
- **Almacenamiento** ~10MB
- **Internet** No requerido

## Permisos

- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `WAKE_LOCK` - Ejecutar temporizador en segundo plano
- `FLASHLIGHT` - Notificación de flash para intervalos de descanso

## Compilación

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

Requiere JDK 17 o superior.

## Licencia

Este proyecto está licenciado bajo la Licencia Pública General de GNU v3.0. Ver [LICENSE](../../LICENSE) para más detalles.

---

**Última actualización**: 6 de diciembre de 2025