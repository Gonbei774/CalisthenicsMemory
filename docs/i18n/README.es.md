# Calisthenics Memory

[🇬🇧 English](../../README.md) | [🇯🇵 日本語](README.ja.md) | [🇪🇸 Español](README.es.md) | [🇩🇪 Deutsch](README.de.md) | [🇨🇳 简体中文](README.zh-CN.md) | [🇫🇷 Français](README.fr.md)

Una aplicación simple y personalizable para seguimiento de entrenamiento con peso corporal

---

## Acerca de Esta Aplicación

Calisthenics Memory es una aplicación Android para registrar y gestionar ejercicios de peso corporal (calistenia) como flexiones y sentadillas. Crea ejercicios libremente, organízalos en grupos y registra tu progreso a tu manera.

### Características

- **Totalmente Personalizable** - Crea ejercicios libremente, organiza por grupos, gestiona con 10 niveles, registro de favoritos
- **Simple** - Funciones esenciales cuidadosamente seleccionadas con una interfaz intuitiva
- **Dos Modos** - Modo de registro rápido y modo de entrenamiento automático guiado con temporizador
- **Enfocado en la Privacidad** - Operación completamente sin conexión, los datos permanecen solo en tu dispositivo

---

## Capturas de Pantalla

### Pantalla de Inicio
<p align="center">
  <img src="screenshots/01_home.png" width="250" alt="Pantalla de Inicio">
</p>

Pantalla de inicio simple e intuitiva. Acceso rápido a 4 funciones principales.

---

### ⚙️ Gestión de Ejercicios

<p align="center">
  <img src="screenshots/02_create_favorites.png" width="250" alt="Gestión de Ejercicios (Favoritos)">
  <img src="screenshots/03_create_edit.png" width="250" alt="Edición de Ejercicios">
</p>

**Izquierda**: Los ejercicios favoritos se muestran en la parte superior en un grupo dedicado. Fácilmente identificables con marcas ★.
**Derecha**: Pantalla de creación/edición de ejercicios. Configuración flexible para tipo (repeticiones/tiempo), lateralidad, desafíos y niveles.

- Organiza jerárquicamente por grupos
- Gestiona progresivamente con niveles (1-10)
- Acceso rápido a ejercicios frecuentes con favoritos

---

### 📝 Función de Registro

<p align="center">
  <img src="screenshots/04_record_select.png" width="250" alt="Selección de Ejercicio">
  <img src="screenshots/05_record_bilateral.png" width="250" alt="Registro de Ejercicio Bilateral">
  <img src="screenshots/06_record_unilateral.png" width="250" alt="Registro de Ejercicio Unilateral">
</p>

**Izquierda**: Pantalla de selección de ejercicio. Organizada claramente con favoritos y grupos jerárquicos.
**Centro**: Los ejercicios bilaterales (flexiones regulares, sentadillas, etc.) se registran simplemente.
**Derecha**: Los ejercicios unilaterales (sentadillas pistol, flexiones a un brazo, etc.) se registran por separado para izquierda y derecha.

- Número de series libremente ajustable
- Añade fecha, hora y comentarios
- Minimiza el esfuerzo de registro con entrada rápida

---

### 🏋️ Función de Entrenamiento

<p align="center">
  <img src="screenshots/07_workout_select.png" width="250" alt="Selección de Ejercicio">
  <img src="screenshots/08_workout_config.png" width="250" alt="Configuración de Entrenamiento">
  <img src="screenshots/09_workout_progress.png" width="250" alt="Entrenamiento en Progreso">
</p>

Modo de entrenamiento automático guiado:

1. **Seleccionar Ejercicio** - Organizado claramente con favoritos y visualización jerárquica
2. **Ajustar Configuración** - Establece series/repeticiones objetivo, tiempo por repetición, cuenta regresiva e intervalos
3. **Ejecutar** - Progresión automática desde cuenta regresiva hasta ejecución, intervalos

Gestiona tu ritmo solo mirando la pantalla para enfocarte en el entrenamiento. Salta o detén a mitad de camino, y guarda los registros hasta ese punto.

---

### 📊 Función de Consulta - Pestaña de Lista

<p align="center">
  <img src="screenshots/11_view_list.png" width="250" alt="Lista de Registros">
  <img src="screenshots/12_view_list_unilateral.png" width="250" alt="Detalles de Ejercicio Unilateral">
</p>

**Izquierda**: Revisa los registros de entrenamiento pasados cronológicamente.
**Derecha**: Los ejercicios unilaterales muestran valores izquierdo y derecho codificados por colores (verde=derecho, morado=izquierdo).

- Detalles de sesión (fecha/hora, contenido de series, comentarios) de un vistazo
- Toca para editar, botón de eliminar para borrar
- Filtrar por período (1 semana/1 mes/3 meses/todo el tiempo)

---

### 📈 Función de Consulta - Pestaña de Gráfica

<p align="center">
  <img src="screenshots/13_view_graph.png" width="250" alt="Gráfica (Ejercicio Unilateral)">
  <img src="screenshots/14_view_graph_max.png" width="250" alt="Gráfica (Ejercicio Isométrico)">
</p>

**Izquierda**: Los ejercicios unilaterales muestran izquierda y derecha como líneas separadas (verde=derecho, morado=izquierdo).
**Derecha**: Los ejercicios isométricos (Plancha, etc.) también se grafican. Verifica el tiempo total de entrenamiento con visualización de suma.

- Cambia el tipo de estadística (promedio/máximo/total) para análisis multifacético
- Filtro de período (1 semana/1 mes/3 meses/todo el tiempo)
- El resumen de estadísticas muestra series totales, promedio, mejor y valores más bajos

---

### 🎯 Función de Consulta - Pestaña de Desafío

<p align="center">
  <img src="screenshots/15_view_challenge_complete.png" width="250" alt="Pestaña de Desafío (Completo)">
  <img src="screenshots/16_view_challenge_progress.png" width="250" alt="Pestaña de Desafío (En Progreso)">
</p>

Verifica visualmente el estado de logro de objetivos. Las barras de progreso muestran el progreso de un vistazo:

- **100% o más**: Perfectamente completado (✓ marca de logro mostrada)
- **75-99%**: Buena condición
- **50-74%**: Casi ahí
- **0-49%**: Sigue adelante

Visualización jerárquica de todos los grupos incluyendo favoritos. Filtra por ejercicio para enfocarte en el progreso de entrenamiento específico.

---

### ⚙️ Pantalla de Configuración

<p align="center">
  <img src="screenshots/17_settings.png" width="250" alt="Pantalla de Configuración">
</p>

Funciones de gestión de datos:

**Copia de Seguridad Completa (JSON)**
- Exportar/importar todos los datos (ejercicios, grupos, registros)
- Soporta migración de datos al cambiar de dispositivo
- ⚠️ Los datos existentes se eliminan al importar

**Añadir Registros (CSV)**
- Descarga plantilla para añadir registros en masa
- Conveniente para migrar desde registros analógicos o aplicaciones de notas
- Añadido a datos existentes (no eliminados)

---

## Funciones Principales

### Registros de Entrenamiento
- Registra fecha, hora, series, repeticiones (o segundos)
- Soporta ejercicios bilaterales (flexiones, etc.) y ejercicios unilaterales (sentadillas pistol, etc.)
- Función de comentarios para notas sobre forma y percepciones

### Consulta de Registros
Revisa registros en 3 pestañas:

1. **Pestaña de Lista** - Muestra registros pasados en lista, edición y eliminación posible
2. **Pestaña de Gráfica** - Visualiza progreso por período con gráficas (promedio/máximo/total)
3. **Pestaña de Desafío** - Verifica estado de logro de objetivos, evaluado en 4 etapas

### Configuración de Desafíos
- Establece series objetivo × valor objetivo (ejemplo: 3 series × 50 repeticiones)
- Evaluación flexible de logro juzgando con suma de las N series superiores
- El color de la barra de progreso cambia según la tasa de logro

### Gestión de Grupos y Favoritos
- Agrupa ejercicios (ej. Flexiones, Sentadillas, Dominadas, etc.)
- Organiza claramente con visualización jerárquica
- Gestiona progreso progresivo con niveles (1-10)
- Muestra ejercicios frecuentes en un grupo dedicado con registro de favoritos

### Copia de Seguridad
- Exporta datos en formato JSON
- Soporta migración de datos a otro dispositivo o respaldo
- Importa registros en formato CSV (conveniente para migrar desde registros analógicos o aplicaciones de notas)

### Soporte Multilingüe
- Soporta español, inglés, japonés, alemán, chino (simplificado) y francés
- Cambia automáticamente según la configuración de idioma del dispositivo

---

## Cómo Usar

### 1. Crear Ejercicios
Añade nuevos ejercicios desde la pantalla "Crear":

1. Ingresa nombre del ejercicio (ejemplo: "Wall Push-up")
2. Selecciona tipo (Dinámico: basado en repeticiones / Isométrico: basado en tiempo)
3. Selecciona lateralidad (Bilateral: ambos lados / Unilateral: un lado)
4. Selecciona grupo (opcional)
5. Establece nivel (1-10, opcional)
6. Establece desafío (series objetivo × valor objetivo, opcional)
7. Registro de favoritos con marca ★ (opcional)

### 2. Registrar Entrenamiento
Añade registros desde la pantalla "Registrar":

1. Selecciona ejercicio (desde favoritos o visualización jerárquica)
2. Establece número de series
3. Ingresa valores para cada serie
4. Añade comentario (opcional)
5. Verifica fecha y hora (cambia si es necesario)
6. Toca "Registrar"

### 3. Usar Modo de Entrenamiento
Entrenamiento automático guiado desde la pantalla "Entrenar":

1. Selecciona ejercicio
2. Establece series y repeticiones objetivo
3. Configuración de temporizador (tiempo por repetición, cuenta regresiva, intervalo)
4. Toca "Iniciar"
5. Cuenta regresiva automática → ejecución → intervalo → siguiente serie
6. Guarda registro después de completar

### 4. Consultar Registros
Revisa registros pasados en la pantalla "Ver Registros":

- **Pestaña de Lista**: Muestra por sesión, edición y eliminación posible
- **Pestaña de Gráfica**: Visualiza progreso con gráficas de líneas
- **Pestaña de Desafío**: Muestra estado de logro de objetivos con barras de progreso

Filtra por ejercicio y período.

### 5. Respaldar Datos
Exporta e importa datos desde la pantalla "Configuración":

**Copia de Seguridad Completa (JSON)**
- **Exportar**: "Exportar Datos" → Selecciona destino de guardado
- **Importar**: "Importar Datos" → Selecciona archivo JSON
  - ⚠️ Los datos existentes se eliminan al importar

**Añadir Registros (CSV)**
- **Descargar Plantilla**: Obtén plantilla CSV de registro con "Exportar Plantilla de Entrada"
- **Importar Registros**: Añade registros desde archivo CSV con "Importar Registros"
  - Añadido a datos existentes (no eliminados)
  - Conveniente para migrar desde registros analógicos o aplicaciones de notas

---

## Mecanismo de Evaluación de Desafíos

### Reglas Básicas
El logro de objetivos se juzga por la **suma de las N series superiores**.

**Ejemplo: Cuando el objetivo es "2 series × 20 repeticiones"**

**Patrón Completado**:
- 20 reps + 20 reps + 5 reps → Suma de las 2 superiores = 40 reps (100%)
- 25 reps + 16 reps + 10 reps → Suma de las 2 superiores = 41 reps (102%)

**Patrón No Completado**:
- 15 reps + 15 reps + 15 reps → Suma de las 2 superiores = 30 reps (75%)

### Para Ejercicios Unilaterales
Calcula las N superiores para izquierda y derecha respectivamente, y evalúa por promedio.

**Ejemplo: Cuando el objetivo es "2 series × 20 repeticiones (por lado)"**

**Ambos Lados Completados**:
- Derecho: 20 reps + 20 reps = 40 reps (100%)
- Izquierdo: 19 reps + 21 reps = 40 reps (100%)
- **Promedio: 100%** → Completado

**Solo Un Lado Completado**:
- Derecho: 20 reps + 20 reps = 40 reps (100%)
- Izquierdo: 15 reps + 15 reps = 30 reps (75%)
- **Promedio: 87.5%** → No Completado

### Criterios de Evaluación

El color de la barra de progreso cambia según la tasa de logro:
- **100% o más**: Perfectamente completado (✓ marca de logro mostrada)
- **75-99%**: Buena condición
- **50-74%**: Casi ahí
- **0-49%**: Sigue adelante

---

## Estructura de Pantallas

### Pantalla de Inicio
Accede a cada función desde 4 botones:
- **Crear** - Gestiona ejercicios y grupos
- **Registrar** - Ingresa registros de entrenamiento
- **Entrenar** - Entrenamiento automático guiado
- **Ver** - Revisa registros pasados

Accede a la pantalla de configuración desde el botón ⚙️ en la esquina inferior derecha.

### Pantalla de Gestión de Ejercicios (Crear)
Gestiona ejercicios y grupos, establece desafíos. Los ejercicios favoritos se muestran con marcas ★ y se colocan en un grupo dedicado.

### Pantalla de Registro (Registrar)
Selecciona ejercicio → Ingresa series y valores → Registra

### Pantalla de Entrenamiento (Entrenar)
Selecciona ejercicio → Configuración → Preparación → Ejecución → Intervalo → Completar → Registrar

### Pantalla de Consulta (Ver Registros)
Revisa registros en 3 pestañas: Lista / Gráfica / Desafío

---

## Consejos

### Uso Efectivo
- **Utiliza Favoritos**: Registra ejercicios frecuentes para acceso más rápido
- **Utiliza Niveles**: Gestiona progreso progresivo con niveles 1-10
- **Establece Desafíos**: Los objetivos claros son efectivos para mantener la motivación
- **Función de Comentarios**: Anota percepciones sobre forma y condición física
- **Respaldos Regulares**: Exporta y guarda datos

### Cómo Leer Gráficas
- **Promedio**: Verifica estabilidad del entrenamiento
- **Máximo**: Verifica progreso del récord personal
- **Total**: Verifica volumen de entrenamiento
- Los ejercicios unilaterales muestran izquierda y derecha como líneas separadas (verde=derecho, morado=izquierdo)

---

## Requisitos del Sistema

- **SO Soportado**: Android 8.0 (API 26) o superior
- **Almacenamiento**: Aproximadamente 10MB
- **Internet**: No requerido (operación completamente sin conexión)

---

## Licencia

Esta aplicación se publica bajo la Licencia Pública General GNU v3.0. Consulta el archivo [LICENSE](LICENSE) para más detalles.

---

## Instalación

### 📥 Descarga de APK

La última versión se puede descargar desde [Releases](https://github.com/Gonbei774/CalisthenicsMemory/releases).

**[📦 Descargar v1.3.0](https://github.com/Gonbei774/CalisthenicsMemory/releases/download/v1.3.0/app-release.apk)**

Si necesitas una versión anterior, puedes descargarla desde la página de [Releases](https://github.com/Gonbei774/CalisthenicsMemory/releases).

### ⚠️ Descargo de Responsabilidad

Esta aplicación se proporciona sin garantía. Distribuida bajo la licencia GPL-3.0 en una base "TAL CUAL", sin garantía de comerciabilidad o idoneidad para un propósito particular. No somos responsables de ningún daño derivado del uso.

### Pasos de Instalación

1. Descarga el archivo APK desde el enlace anterior
2. Toca el archivo descargado
3. Permite "Instalar desde fuentes desconocidas" si se solicita
4. Instalación completa

### Verificación de Seguridad

Si deseas verificar que el APK no ha sido alterado, verifica el checksum SHA256:

```bash
# Calcula SHA256 del APK descargado
sha256sum app-release.apk

# Compara con el SHA256 oficial
# https://github.com/Gonbei774/CalisthenicsMemory/releases/download/v1.3.0/app-release.apk.sha256
```

---

## Preguntas Frecuentes

### P: ¿Qué métodos de entrenamiento son soportados?
R: Se soportan cualquier método de entrenamiento con peso corporal, incluyendo Convict Conditioning, StartBodyweight, o tus programas personalizados. Puedes crear ejercicios libremente, por lo que puedes usarlo según tu filosofía de entrenamiento.

### P: ¿Dónde se almacenan los datos?
R: Se almacenan en una base de datos local (SQLite) dentro de la aplicación. Nunca se envían a internet y operan completamente sin conexión.

### P: ¿Se realizan copias de seguridad automáticamente?
R: No, necesitas exportar manualmente. Recomendamos copias de seguridad regulares.

### P: ¿Se eliminarán los datos si desinstalo la aplicación?
R: Sí, se eliminarán. Asegúrate de exportar antes de desinstalar.

### P: ¿Puedo usarlo sin establecer desafíos?
R: Sí, la función de registro se puede usar sin configuración de desafíos. No se mostrará en la pestaña de desafío, pero la lista y las gráficas funcionan bien.

### P: ¿Es compatible con múltiples idiomas?
R: Se soportan español, inglés, japonés, alemán, chino (simplificado) y francés. Cambia automáticamente según la configuración de idioma del dispositivo.

### P: ¿Dónde se muestran los ejercicios favoritos?
R: Los ejercicios favoritos se muestran automáticamente en el grupo "Favoritos". Continúan mostrándose en su grupo original, por lo que puedes acceder desde cualquiera.

---

## Desarrollo

### Instrucciones de Compilación

```bash
git clone https://github.com/Gonbei774/CalisthenicsMemory.git
cd CalisthenicsMemory
./gradlew assembleDebug
```

### Requisitos
- JDK 17 o superior
- Android SDK (API 26 o superior)
- Gradle (incluido en el proyecto)

---

**Última Actualización**: 13 de noviembre de 2025