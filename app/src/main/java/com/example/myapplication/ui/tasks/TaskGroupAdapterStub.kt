package com.example.myapplication.ui.tasks

import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entities.TaskEntity

class TaskGroupAdapterStub(
    private val onTaskCompleted: (task: TaskEntity) -> Unit
) : RecyclerView.Adapter<TaskGroupAdapterStub.GroupViewHolder>() {

    private var groups = listOf<TaskGroup>()

    fun submitList(list: List<TaskGroup>) {
        groups = list
        notifyDataSetChanged()
    }

    inner class GroupViewHolder(val container: LinearLayout) : RecyclerView.ViewHolder(container) {

        fun bind(group: TaskGroup) {
            container.removeAllViews()

            val header = TextView(container.context).apply {
                text = "${group.subject} (${group.tasks.size})"
                textSize = 18f
                setPadding(16, 16, 16, 16)
                setBackgroundColor(0xFFE0E0E0.toInt())
                setTextColor(0xFF000000.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(header)

            // заглушка
            group.isExpanded = true

            header.setOnClickListener {
                group.isExpanded = !group.isExpanded
                notifyItemChanged(adapterPosition)
            }

            if (group.isExpanded) {
                group.tasks.forEach { task ->
                    val taskLayout = LinearLayout(container.context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(32, 16, 16, 16)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val taskText = TextView(container.context).apply {
                        text = task.title
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                        textSize = 16f
                    }
                    taskLayout.addView(taskText)

                    val button = Button(container.context).apply {
                        text = "✓"
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        setOnClickListener { onTaskCompleted(task) }
                    }
                    taskLayout.addView(button)

                    container.addView(taskLayout)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val layout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }
        return GroupViewHolder(layout)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount(): Int = groups.size
}

