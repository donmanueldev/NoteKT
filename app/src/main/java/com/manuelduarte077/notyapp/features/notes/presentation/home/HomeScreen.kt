package com.manuelduarte077.notyapp.features.notes.presentation.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.manuelduarte077.notyapp.R
import com.manuelduarte077.notyapp.features.notes.presentation.home.components.SectionTitle
import com.manuelduarte077.notyapp.features.notes.presentation.home.components.SummaryInfo
import com.manuelduarte077.notyapp.features.notes.presentation.home.components.TaskItem
import com.manuelduarte077.notyapp.features.notes.presentation.home.providers.HomeScreenPreviewProvider
import com.manuelduarte077.notyapp.ui.theme.NoteTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun HomeScreenRoot(
    navigateToTaskScreen: (String?, Boolean) -> Unit,
    viewModel: HomeScreenViewModel
) {
    val state = viewModel.state
    val event = viewModel.events
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        var visible = false
        val observer = LifecycleEventObserver { _, lifecycleEvent ->
            when (lifecycleEvent) {
                Lifecycle.Event.ON_RESUME -> {
                    if (!visible) {
                        visible = true
                        viewModel.onScreenVisible()
                    }
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> visible = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
            !visible
        ) {
            visible = true
            viewModel.onScreenVisible()
        }
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        true
    ) {
        event.collect { event ->
            when (event) {
                HomeScreenEvent.DeletedTask -> {
                    Toast.makeText(
                        context,
                        R.string.task_deleted,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                HomeScreenEvent.AllTaskDeleted -> {
                    Toast.makeText(
                        context,
                        R.string.all_task_deleted,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                HomeScreenEvent.UpdatedTask -> {
                    Toast.makeText(
                        context,
                        R.string.task_updated,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    HomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is HomeScreenAction.OnAddTask -> {
                    navigateToTaskScreen(null, false)
                }

                is HomeScreenAction.OnAddTaskByVoice -> {
                    navigateToTaskScreen(null, true)
                }

                is HomeScreenAction.OnClickTask -> {
                    navigateToTaskScreen(action.taskId, false)
                }

                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeDataState,
    onAction: (HomeScreenAction) -> Unit
) {
    var isMenuExtended by remember { mutableStateOf(false) }
    var showDeleteAllConfirmation by remember { mutableStateOf(false) }
    var isTaskMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val taskMenuIconRotation by animateFloatAsState(
        targetValue = if (isTaskMenuExpanded) 45f else 0f,
        animationSpec = tween(
            durationMillis = 160,
            easing = FastOutSlowInEasing,
        ),
        label = "task menu icon rotation",
    )

    BackHandler(enabled = isTaskMenuExpanded) {
        isTaskMenuExpanded = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .clickable {
                                isMenuExtended = true
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Add Task",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                        DropdownMenu(
                            expanded = isMenuExtended,
                            modifier = Modifier.background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ),
                            onDismissRequest = { isMenuExtended = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.delete_all),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    isMenuExtended = false
                                    showDeleteAllConfirmation = true
                                }
                            )
                        }
                    }
                }

            )
        },
        content = { paddingValues ->

            if (state.completedTask.isEmpty() && state.pendingTask.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_tasks),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues = paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(
                        8.dp
                    )
                ) {
                    item {
                        SummaryInfo(
                            date = state.date,
                            tasksSummary = stringResource(R.string.summary, state.summary),
                            completedTasks = state.completedTask.size,
                            totalTask = state.completedTask.size + state.pendingTask.size
                        )
                    }

                    stickyHeader {
                        SectionTitle(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.surface
                                )
                                .fillParentMaxWidth(),
                            title = stringResource(R.string.pending_tasks)
                        )
                    }

                    items(
                        items = state.pendingTask,
                        key = { task -> task.id }
                    ) { task ->
                        TaskItem(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(8.dp)
                                )
                                .animateItem(),
                            task = task,
                            onClickItem = {
                                onAction(HomeScreenAction.OnClickTask(task.id))
                            },
                            onDeleteItem = {
                                onAction(HomeScreenAction.OnDeleteTask(task))
                            },
                            onToggleCompletion = {
                                onAction(HomeScreenAction.OnToggleTask(it))
                            }
                        )
                    }

                    stickyHeader {
                        SectionTitle(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surface
                                ),
                            title = stringResource(R.string.completed_tasks)
                        )
                    }

                    items(
                        items = state.completedTask,
                        key = { task -> task.id }
                    ) { task ->
                        TaskItem(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(8.dp)
                                )
                                .animateItem(),
                            task = task,
                            onClickItem = {
                                onAction(HomeScreenAction.OnClickTask(task.id))
                            },
                            onDeleteItem = {
                                onAction(HomeScreenAction.OnDeleteTask(task))
                            },
                            onToggleCompletion = {
                                onAction(HomeScreenAction.OnToggleTask(it))
                            }
                        )
                    }

                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedVisibility(
                    visible = isTaskMenuExpanded,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 100),
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 75),
                    ),
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ExtendedFloatingActionButton(
                            text = { Text(stringResource(R.string.add_task_by_voice)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                isTaskMenuExpanded = false
                                onAction(HomeScreenAction.OnAddTaskByVoice)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            elevation = taskMenuItemElevation(),
                        )
                        ExtendedFloatingActionButton(
                            text = { Text(stringResource(R.string.add_task)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                isTaskMenuExpanded = false
                                onAction(HomeScreenAction.OnAddTask)
                            },
                            elevation = taskMenuItemElevation(),
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { isTaskMenuExpanded = !isTaskMenuExpanded },
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = taskMenuIconRotation
                        },
                        contentDescription = stringResource(
                            if (isTaskMenuExpanded) {
                                R.string.hide_task_creation_options
                            } else {
                                R.string.show_task_creation_options
                            },
                        ),
                    )
                }
            }
        }
    )

    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllConfirmation = false },
            title = { Text(stringResource(R.string.delete_all_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_all_confirmation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllConfirmation = false
                        onAction(HomeScreenAction.OnDeleteAllTasks)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun taskMenuItemElevation() = FloatingActionButtonDefaults.elevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
)

@Preview
@Composable
fun HomeScreenPreviewLight(
    @PreviewParameter(HomeScreenPreviewProvider::class) state: HomeDataState
) {
    NoteTheme {
        HomeScreen(
            state = state,
            onAction = {}
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun HomeScreenPreviewDark(
    @PreviewParameter(HomeScreenPreviewProvider::class) state: HomeDataState
) {
    NoteTheme {
        HomeScreen(
            state = state,
            onAction = {}
        )
    }
}
