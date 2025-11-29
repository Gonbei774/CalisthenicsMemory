<p align="center">
  <img src="../../icon.png" width="150" alt="Calisthenics Memory Icon">
</p>

# Calisthenics Memory

Una aplicación simple y centrada en la privacidad para registrar entrenamientos con peso corporal en Android.

---

<p align="center">
  <a href="https://f-droid.org/packages/io.github.gonbei774.calisthenicsmemory/">
    <img src="https://fdroid.org/badge/get-it-on.png" alt="Disponible en F-Droid" height="80">
  </a>
</p>
<p align="center">
  <a href="https://apt.izzysoft.de/fdroid/index/apk/io.github.gonbei774.calisthenicsmemory">
    <img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButton.png" alt="Disponible en IzzyOnDroid" height="54">
  </a>
</p>

---

🌐 [English](../../README.md) | [日本語](README.ja.md) | [Deutsch](README.de.md) | [Français](README.fr.md) | [Italiano](README.it.md) | [简体中文](README.zh-CN.md)

---

## Acerca de

Calisthenics Memory te ayuda a registrar y gestionar ejercicios con peso corporal como flexiones, dominadas y sentadillas. Crea ejercicios personalizados, organízalos en niveles progresivos y supervisa tu progreso.

La aplicación funciona completamente sin conexión: no requiere internet, sin anuncios, sin rastreo. Tus datos permanecen solo en tu dispositivo.

## Puntos Clave

- **Personalización completa** - Sin funciones bloqueadas para ejercicios personalizados. Repeticiones/tiempo, unilateral/bilateral, objetivos, temporizadores - todo disponible para cada ejercicio
- **Dos modos de registro** - Entrada manual rápida o entrenamiento guiado con temporizadores
- **Solo sin conexión** - Tus datos nunca salen de tu dispositivo

## Características

- **Panel de inicio** - Ve los registros de entrenamiento de hoy de un vistazo, mantén pulsado para copiar
- **Totalmente personalizable** - Crea ejercicios libremente, organiza por grupos, gestiona con 10 niveles, reordena con botones de flecha
- **Favoritos** - Acceso rápido a ejercicios frecuentes
- **Dos modos de registro**
  - Modo registro: Entrada manual rápida con botón "Aplicar configuración del ejercicio"
  - Modo entrenamiento: Entrenamiento guiado automático con ajustes de temporizador por ejercicio (intervalo de descanso, duración de repetición), notificación de flash LED al completar serie
- **Seguimiento del progreso** - Ve registros como listas, gráficos o barras de progreso de desafíos
- **Soporte unilateral/bilateral** - Registra lados izquierdo y derecho por separado para ejercicios de un lado
- **Objetivos de desafío** - Establece series × repeticiones objetivo y rastrea el estado de logro
- **Gestión de datos** - Exporta/importa en formato JSON o CSV (soporte de respaldo completo)
- **Multiidioma** - Inglés, japonés, español, alemán, chino (simplificado), francés, italiano
- **Privacidad primero** - Completamente sin conexión, sin permisos en tiempo de ejecución, sin acceso a Internet

## Capturas de Pantalla

<p align="center">
  <img src="../../screenshots/1.png" width="250"><br>
  <b>Inicio</b> - Entrenamiento de hoy de un vistazo
</p>

<p align="center">
  <img src="../../screenshots/2.png" width="250"><br>
  <b>Ejercicios</b> - Organiza con grupos y favoritos
</p>

<p align="center">
  <img src="../../screenshots/3.png" width="250"><br>
  <b>Registro</b> - Entrada manual rápida
</p>

<p align="center">
  <img src="../../screenshots/4.png" width="250"><br>
  <b>Entrenamiento</b> - Entrenamiento guiado con temporizador
</p>

<p align="center">
  <img src="../../screenshots/5.png" width="250"><br>
  <b>Gráfico</b> - Rastrea tu progreso
</p>

<p align="center">
  <img src="../../screenshots/6.png" width="250"><br>
  <b>Desafío</b> - Estado de logro de objetivos
</p>

## Requisitos

- **Android** 8.0 (API 26) o superior
- **Almacenamiento** ~10MB
- **Internet** No requerido

## Permisos

Esta aplicación utiliza solo **permisos normales (de tiempo de instalación)**, que se otorgan automáticamente durante la instalación sin solicitudes al usuario.

A partir de v1.8.0, se incluyen los siguientes permisos:

| Permiso | Propósito | Añadido por | Fuente |
|---------|-----------|-------------|--------|
| `FLASHLIGHT` | Notificación de flash LED durante el modo entrenamiento | App (v1.8.0) | [FlashController.kt](../../app/src/main/java/io/github/gonbei774/calisthenicsmemory/util/FlashController.kt) |
| `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | Protección de seguridad para componentes internos | Biblioteca AndroidX (automático) | - |

### ¿Qué son los permisos normales?

Android clasifica los permisos en dos tipos:
- **Permisos normales**: Permisos de bajo riesgo otorgados automáticamente durante la instalación. Los usuarios no pueden revocarlos individualmente.
- **Permisos peligrosos**: Permisos de alto riesgo que requieren aprobación explícita del usuario (ej.: cámara, ubicación, contactos).

Esta aplicación no solicita ningún permiso en tiempo de ejecución.

Para más detalles:
- [Resumen de tipos de permisos de Android](https://developer.android.com/guide/topics/permissions/overview)
- [Lista completa de permisos normales](https://developer.android.com/reference/android/Manifest.permission)

### Nota

Los permisos normales se otorgan automáticamente y pueden no aparecer en las listas de tiendas de aplicaciones. Los documentamos aquí por transparencia.

## Compilación

```bash
git clone https://codeberg.org/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

Requiere JDK 17 o superior.

## Licencia

Este proyecto está licenciado bajo la Licencia Pública General de GNU v3.0. Ver [LICENSE](../../LICENSE) para más detalles.