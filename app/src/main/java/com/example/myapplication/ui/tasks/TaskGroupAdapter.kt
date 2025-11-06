package com.example.myapplication.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.entities.TaskEntity


class TaskGroupAdapter(
    private val onTaskCompleted: (task: TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskGroupAdapter.GroupViewHolder>() {

    private var groups = listOf<TaskGroup>()

    fun submitList(list: List<TaskGroup>) {
        groups = list
        notifyDataSetChanged()
    }

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textSubjectName: TextView = view.findViewById(R.id.textSubjectName)
        private val textTaskSummary: TextView = view.findViewById(R.id.textTaskSummary)
        private val containerFullTasks: LinearLayout = view.findViewById(R.id.containerFullTasks)

        fun bind(group: TaskGroup) {
            textSubjectName.text = group.subject
            textTaskSummary.text = group.tasks.firstOrNull()?.title ?: ""

            itemView.setOnClickListener {
                group.isExpanded = !group.isExpanded
                notifyItemChanged(adapterPosition)
            }

            containerFullTasks.removeAllViews()
            if (group.isExpanded) {
                group.tasks.forEach { task ->
                    val taskView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.item_task, containerFullTasks, false)
                    val textTask = taskView.findViewById<TextView>(R.id.textTask)
                    val buttonComplete = taskView.findViewById<ImageButton>(R.id.buttonCompleteTask)
                    textTask.text = task.title
                    buttonComplete.setOnClickListener { onTaskCompleted(task) }
                    containerFullTasks.addView(taskView)
                }
                containerFullTasks.visibility = View.VISIBLE
            } else {
                containerFullTasks.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size
}
