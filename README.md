# NoteKT - Aplicación de Notas en Kotlin

NoteKT es una aplicación moderna de gestión de notas y tareas desarrollada con Kotlin y Jetpack Compose. La aplicación permite a los usuarios crear, editar, completar y eliminar tareas, organizándolas por categorías y estado de finalización.

## Características

- **Gestión de tareas**: Crear, editar, marcar como completadas y eliminar tareas
- **Tareas por voz**: Dictar texto libre desde Inicio o el formulario, editarlo y guardar
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
El texto reconocido aparece completo como título. Puedes corregirlo, elegir otra transcripción
o repetir el dictado. La descripción, categoría, fecha y hora se agregan en el formulario;
el dictado no interpreta comandos ni rellena esos campos. Toca **Guardar** para crear la tarea.
Cancelar o salir sin guardar no crea nada.

NoteKT utiliza `SpeechRecognizer` dentro de la aplicación y solicita el permiso
`RECORD_AUDIO` solo al iniciar un dictado. No envía una lista de frases ni limita los idiomas
a una lista fija. En Android 14 o posterior solicita cambio automático de idioma; en versiones
anteriores usa el idioma configurado en el dispositivo. La disponibilidad de idiomas depende
del servicio de reconocimiento y los modelos instalados. La pantalla muestra resultados
parciales y permite escoger entre hasta cinco transcripciones antes de guardar.

El audio lo procesa el servicio de reconocimiento instalado y, según el dispositivo, puede
enviarse a sus servidores; NoteKT no almacena el audio.

## Modelos de datos

### Task (Tarea)
```kotlin
data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val isCompleted: Boolean = false,
    val category: Category? = null,
    val date: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null
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
