# NoteKT - Aplicación de Notas en Kotlin

NoteKT es una aplicación moderna de gestión de notas y tareas desarrollada con Kotlin y Jetpack Compose. La aplicación permite a los usuarios crear, editar, completar y eliminar tareas, organizándolas por categorías y estado de finalización.

## Características

- **Gestión de tareas**: Crear, editar, marcar como completadas y eliminar tareas
- **Tareas por voz**: Dictar el título desde el micrófono de Inicio o del formulario, revisarlo y guardar
- **Categorización**: Organizar tareas por categorías (Trabajo, Personal, Compras, Otros)
- **Interfaz moderna**: Diseñada con Jetpack Compose siguiendo los principios de Material Design 3
- **Modo oscuro**: Soporte completo para tema claro y oscuro
- **Arquitectura limpia**: Implementación siguiendo los principios de Clean Architecture y MVVM

## Tecnologías utilizadas

- **Kotlin**: Lenguaje de programación principal
- **Jetpack Compose**: Framework de UI declarativo para Android
- **Room**: Biblioteca de persistencia para almacenamiento local de datos
- **Hilt**: Inyección de dependencias
- **Coroutines & Flow**: Para operaciones asíncronas y flujos de datos reactivos
- **Material 3**: Diseño visual siguiendo las últimas guías de Material Design

## Arquitectura

La aplicación sigue los principios de Clean Architecture y está estructurada en las siguientes capas:

### Presentación
- **UI**: Componentes de Jetpack Compose para la interfaz de usuario
- **ViewModels**: Manejo del estado de la UI y lógica de presentación

### Dominio
- **Modelos**: Entidades de dominio como `Task` y `Category`
- **Interfaces**: Contratos para repositorios y fuentes de datos

### Datos
- **Implementaciones**: Implementaciones concretas de repositorios y fuentes de datos
- **Room**: Entidades, DAOs y base de datos para persistencia local

## Estructura del proyecto

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/manuelduarte077/notyapp/
│   │   │   ├── MainActivity.kt
│   │   │   ├── NoteApplication.kt
│   │   │   ├── features/
│   │   │   │   ├── notes/
│   │   │   │   │   ├── data/
│   │   │   │   │   │   ├── RoomTaskLocalDataSource.kt
│   │   │   │   │   │   ├── TaskDao.kt
│   │   │   │   │   │   ├── TaskEntity.kt
│   │   │   │   │   │   └── TodoDatabase.kt
│   │   │   │   │   ├── domain/
│   │   │   │   │   │   ├── Category.kt
│   │   │   │   │   │   ├── Task.kt
│   │   │   │   │   │   └── TaskLocalDataSource.kt
│   │   │   │   │   └── presentation/
│   │   │   │   │       ├── detail/
│   │   │   │   │       │   ├── TaskScreen.kt
│   │   │   │   │       │   ├── TaskViewModel.kt
│   │   │   │   │       │   └── ...
│   │   │   │   │       └── home/
│   │   │   │   │           ├── HomeScreen.kt
│   │   │   │   │           ├── HomeScreenViewModel.kt
│   │   │   │   │           └── ...
│   │   │   │   ├── auth/
│   │   │   │   ├── onboarding/
│   │   │   │   └── splash/
│   │   │   ├── navigation/
│   │   │   │   └── NavigationRoot.kt
│   │   │   └── ui/
│   │   │       └── theme/
│   │   └── res/
│   └── ...
└── ...
```

## Flujo de la aplicación

1. **Pantalla de inicio (HomeScreen)**:
   - Muestra la lista de tareas pendientes y completadas
   - Permite marcar tareas como completadas/pendientes
   - Permite eliminar tareas individuales o todas las tareas
   - Botón flotante para crear nuevas tareas

2. **Pantalla de detalle (TaskScreen)**:
   - Formulario para crear o editar tareas
   - Campos para título, descripción y categoría
   - Opción para marcar como completada
   - Botón para guardar los cambios

### Crear una tarea por voz

Toca el micrófono junto a «+» en Inicio para abrir una tarea nueva y comenzar el dictado.
El texto reconocido aparece como título: puedes corregirlo, añadir descripción o categoría
en español o inglés. En Android 14 o posterior el reconocimiento puede cambiar de idioma
durante el dictado; en versiones anteriores se usa el idioma de reconocimiento configurado
en el dispositivo.
y tocar **Guardar**. El micrófono junto al título permite repetir el dictado y reemplazarlo.
Cancelar o salir sin guardar no crea una tarea.

NoteKT utiliza `SpeechRecognizer` dentro de la aplicación y solicita el permiso
`RECORD_AUDIO` solo al iniciar un dictado. Prioriza español de Nicaragua, consulta los
modelos disponibles cuando Android lo permite y conserva hasta cinco alternativas con su
confianza. La pantalla muestra resultados parciales y permite cancelar o escoger otra
transcripción antes de guardar.

El audio lo procesa el servicio de reconocimiento instalado y, según el dispositivo, puede
enviarse a sus servidores; NoteKT no almacena el audio. El texto final se interpreta de forma
local para extraer título, descripción explícita, categoría, fecha y hora de vencimiento. El
usuario puede revisar y corregir todos esos campos antes de guardar la tarea.
El reconocedor no recibe una lista fija de palabras: captura dictado libre para no favorecer
un idioma o un conjunto cerrado de frases.

## Modelos de datos

### Task (Tarea)
```kotlin
data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean = false,
    val category: Category? = null,
    val date: LocalDateTime = LocalDateTime.now()
)
```

### Category (Categoría)
```kotlin
enum class Category {
    WORK,
    PERSONAL,
    SHOPPING,
    OTHER
}
```
