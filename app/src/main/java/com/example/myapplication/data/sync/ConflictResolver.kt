package com.example.myapplication.data.sync

import com.example.myapplication.data.dao.TaskDao
import com.example.myapplication.data.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ConflictResolver(
    private val taskDao: TaskDao
) {

    private val _conflicts = MutableStateFlow<List<DataConflict>>(emptyList())
    val conflicts: Flow<List<DataConflict>> = _conflicts

    fun detectTaskConflicts(local: TaskEntity, remote: TaskEntity): DataConflict? {
        val changes = mutableListOf<FieldChange>()

        if (local.title != remote.title && local.title.isNotEmpty() && remote.title.isNotEmpty()) {
            changes.add(FieldChange("title", local.title, remote.title))
        }

        if (local.description != remote.description && local.description.isNotEmpty() && remote.description.isNotEmpty()) {
            changes.add(FieldChange("description", local.description, remote.description))
        }

        if (local.deadline != remote.deadline && local.deadline.isNotEmpty() && remote.deadline.isNotEmpty()) {
            changes.add(FieldChange("deadline", local.deadline, remote.deadline))
        }

        if (local.status != remote.status) {
            changes.add(FieldChange("status", local.status, remote.status))
        }

        return if (changes.isNotEmpty()) {
            DataConflict(
                entityType = "Task",
                entityId = local.task_id,
                localVersion = local,
                remoteVersion = remote,
                fieldChanges = changes
            )
        } else {
            null
        }
    }

    suspend fun resolveConflict(conflict: DataConflict, resolution: ConflictResolution) {
        when (resolution) {
            is ConflictResolution.UseLocal -> {
                when (conflict.entityType) {
                    "Task" -> {
                        val localTask = conflict.localVersion as TaskEntity
                        taskDao.insertTask(localTask)
                    }
                }
            }
            is ConflictResolution.UseRemote -> {
                when (conflict.entityType) {
                    "Task" -> {
                        val remoteTask = conflict.remoteVersion as TaskEntity
                        taskDao.insertTask(remoteTask)
                    }
                }
            }
            is ConflictResolution.Merge -> {
                when (conflict.entityType) {
                    "Task" -> {
                        val mergedTask = mergeTaskEntities(conflict, resolution.customValues)
                        taskDao.insertTask(mergedTask)
                    }
                }
            }
        }

        _conflicts.value = _conflicts.value - conflict
    }

    private fun mergeTaskEntities(conflict: DataConflict, customValues: Map<String, Any>): TaskEntity {
        val localTask = conflict.localVersion as TaskEntity
        val remoteTask = conflict.remoteVersion as TaskEntity

        return localTask.copy(
            title = customValues["title"] as? String ?: chooseValue(conflict, "title", remoteTask.title),
            description = customValues["description"] as? String ?: chooseValue(conflict, "description", remoteTask.description),
            deadline = customValues["deadline"] as? String ?: chooseValue(conflict, "deadline", remoteTask.deadline),
            status = customValues["status"] as? String ?: chooseValue(conflict, "status", remoteTask.status)
        )
    }

    private fun chooseValue(conflict: DataConflict, fieldName: String, remoteValue: String): String {
        return remoteValue
    }

    fun addConflict(conflict: DataConflict) {
        _conflicts.value = _conflicts.value + conflict
    }

    fun clearConflicts() {
        _conflicts.value = emptyList()
    }

    suspend fun autoResolveSimpleConflicts() {
        val currentConflicts = _conflicts.value.toMutableList()

        for (conflict in currentConflicts) {
            if (isSimpleConflict(conflict)) {
                resolveConflict(conflict, ConflictResolution.UseRemote)
            }
        }
    }

    private fun isSimpleConflict(conflict: DataConflict): Boolean {
        return conflict.fieldChanges.all { change ->
            when (change.fieldName) {
                "description" -> change.localValue.length < 50 && change.remoteValue.length < 50
                else -> false
            }
        }
    }

    fun getConflictsCount(): Int {
        return _conflicts.value.size
    }
}

data class DataConflict(
    val entityType: String,
    val entityId: Int,
    val localVersion: Any,
    val remoteVersion: Any,
    val fieldChanges: List<FieldChange>
)

data class FieldChange(
    val fieldName: String,
    val localValue: String,
    val remoteValue: String
)

sealed class ConflictResolution {
    object UseLocal : ConflictResolution()
    object UseRemote : ConflictResolution()
    data class Merge(val customValues: Map<String, Any> = emptyMap()) : ConflictResolution()
}