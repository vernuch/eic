package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.entities.TaskEntity
import com.example.myapplication.databinding.FragmentTasksBinding
import com.example.myapplication.ui.tasks.TaskGroup
import com.example.myapplication.ui.tasks.TaskGroupAdapterStub
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy { TaskGroupAdapterStub(::onTaskCompleted) }

    private var activeTasks = mutableListOf<TaskEntity>()
    private var archivedTasks = mutableListOf<TaskEntity>()
    private var showingActive = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tasksRecyclerView.adapter = adapter

        binding.buttonActive.setOnClickListener {
            showingActive = true
            updateButtons()
            showTasks()
        }

        binding.buttonArchive.setOnClickListener {
            showingActive = false
            updateButtons()
            showTasks()
        }

        // заглушка
        loadTasks()
    }

    private fun loadTasks() {
        activeTasks = mutableListOf(
            TaskEntity(1, 101, "Задание 1", "Описание задания 1", "2025-10-25 09:00", "active", null),
            TaskEntity(2, 101, "Задание 2", "Описание задания 2", "2025-10-25 10:30", "active", null),
            TaskEntity(3, 102, "Задание 3", "Описание задания 3", "2025-10-26 09:00", "active", null)
        )

        archivedTasks = mutableListOf(
            TaskEntity(4, 101, "Прошлое задание", "Описание архивного задания", "2025-10-20 09:00", "archived", null)
        )

        updateButtons()
        showTasks()
    }

    private fun updateButtons() {
        if (showingActive) {
            binding.buttonActive.setTextColor(0xFFFFFFFF.toInt()) // белый
            binding.buttonArchive.setTextColor(0xFF000000.toInt()) // черный
        } else {
            binding.buttonActive.setTextColor(0xFF000000.toInt())
            binding.buttonArchive.setTextColor(0xFFFFFFFF.toInt())
        }
    }

    private fun showTasks() {
        val list = if (showingActive) activeTasks else archivedTasks
        val groups = list.groupBy { it.subject_id }
            .map { (subjectId, tasks) -> TaskGroup("Предмет $subjectId", tasks) }
        adapter.submitList(groups)
    }

    private fun onTaskCompleted(task: TaskEntity) {
        lifecycleScope.launch {
            activeTasks.remove(task)
            archivedTasks.add(task.copy(status = "archived"))
            showTasks()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
