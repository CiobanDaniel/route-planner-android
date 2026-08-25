package com.danielcioban.routeplanner.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcioban.routeplanner.R
import com.danielcioban.routeplanner.data.local.StopTaskEntity
import com.danielcioban.routeplanner.data.local.TaskTemplateEntity
import com.danielcioban.routeplanner.ui.components.IslandListItemColumn
import com.danielcioban.routeplanner.ui.components.SoftOutlinedTextField
import com.danielcioban.routeplanner.ui.theme.IslandColors

/** Task controls kept separate from stop metadata so the dialog remains maintainable. */
@Composable
fun StopTaskChecklist(
    stopId: Long,
    tasks: List<StopTaskEntity>,
    onAddTask: (title: String, required: Boolean) -> Unit,
    onTaskCompletedChange: (taskId: Long, completed: Boolean, note: String) -> Unit,
    onCompletionNoteChange: (taskId: Long, note: String) -> Unit,
    onUpdateTask: (taskId: Long, title: String, required: Boolean) -> Unit,
    onDeleteTask: (taskId: Long) -> Unit,
    templates: List<TaskTemplateEntity> = emptyList(),
    onApplyTemplate: (templateId: Long) -> Unit = {},
    onApplyTemplateToRemaining: (templateId: Long) -> Unit = {},
    readOnly: Boolean = false,
) {
    var newTaskTitle by remember(stopId) { mutableStateOf("") }
    var newTaskRequired by remember(stopId) { mutableStateOf(false) }
    val done = tasks.count { it.isCompleted }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tasks_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = IslandColors.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (tasks.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.tasks_progress, done, tasks.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = IslandColors.onSurfaceMuted,
                )
            }
        }
        if (tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.tasks_empty),
                style = MaterialTheme.typography.bodySmall,
                color = IslandColors.onSurfaceMuted,
            )
        }
        tasks.forEach { task ->
            StopTaskRow(
                task = task,
                readOnly = readOnly,
                onCompletedChange = { completed ->
                    onTaskCompletedChange(task.id, completed, task.completionNote)
                },
                onCompletionNoteChange = { note -> onCompletionNoteChange(task.id, note) },
                onUpdateTask = { title, required -> onUpdateTask(task.id, title, required) },
                onDelete = { onDeleteTask(task.id) },
            )
        }
        if (!readOnly && templates.isNotEmpty()) {
            Text(
                text = stringResource(R.string.task_templates_apply),
                style = MaterialTheme.typography.labelMedium,
                color = IslandColors.onSurfaceMuted,
            )
            templates.forEach { template ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { onApplyTemplate(template.id) },
                        label = {
                            Text(
                                if (template.isRequired) {
                                    stringResource(R.string.task_template_required_chip, template.title)
                                } else {
                                    template.title
                                },
                            )
                        },
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onApplyTemplateToRemaining(template.id) }) {
                        Text(stringResource(R.string.task_template_apply_remaining))
                    }
                }
            }
        }
        if (!readOnly) {
            SoftOutlinedTextField(
                value = newTaskTitle,
                onValueChange = { newTaskTitle = it },
                label = stringResource(R.string.tasks_title_label),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = newTaskRequired,
                    onClick = { newTaskRequired = !newTaskRequired },
                    label = { Text(stringResource(R.string.tasks_required)) },
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        onAddTask(newTaskTitle, newTaskRequired)
                        newTaskTitle = ""
                        newTaskRequired = false
                    },
                    enabled = newTaskTitle.isNotBlank(),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.tasks_add),
                        tint = if (newTaskTitle.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            IslandColors.onSurfaceMuted
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StopTaskRow(
    task: StopTaskEntity,
    readOnly: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    onCompletionNoteChange: (String) -> Unit,
    onUpdateTask: (title: String, required: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember(task.id, task.title) { mutableStateOf(task.title) }
    IslandListItemColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = onCompletedChange)
            if (readOnly) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = IslandColors.onSurface,
                    )
                    if (task.isRequired) {
                        Text(
                            text = stringResource(R.string.tasks_required),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            } else {
                SoftOutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) onUpdateTask(it, task.isRequired)
                    },
                    label = stringResource(R.string.tasks_title_label),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        if (!readOnly) {
            FilterChip(
                selected = task.isRequired,
                onClick = { onUpdateTask(title.ifBlank { task.title }, !task.isRequired) },
                label = { Text(stringResource(R.string.tasks_required)) },
            )
        }
        if (task.isCompleted || task.completionNote.isNotBlank()) {
            SoftOutlinedTextField(
                value = task.completionNote,
                onValueChange = onCompletionNoteChange,
                label = stringResource(R.string.tasks_completion_note),
                singleLine = false,
                minLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
