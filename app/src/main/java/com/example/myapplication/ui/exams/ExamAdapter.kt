package com.example.myapplication.ui.exams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.data.entities.ExamEntity

class ExamAdapter(
    private val exams: List<ExamEntity>
) : RecyclerView.Adapter<ExamAdapter.ExamViewHolder>() {

    inner class ExamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textType: TextView = view.findViewById(R.id.textExamType)
        private val textSubject: TextView = view.findViewById(R.id.textSubjectName)
        private val textTeacher: TextView = view.findViewById(R.id.textTeacherName)
        private val textDateTime: TextView = view.findViewById(R.id.textDateTime)
        private val textLocation: TextView = view.findViewById(R.id.textLocation)

        fun bind(exam: ExamEntity) {
            // заглушка
            textType.text = if (exam.subject_id % 2 == 0) "Экзамен" else "Зачёт"
            textSubject.text = "Предмет ${exam.subject_id}"
            textTeacher.text = "Преподаватель ${exam.teacher_id}"
            textDateTime.text = exam.exam_date // пока просто строка
            textLocation.text = exam.location
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exam, parent, false)
        return ExamViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExamViewHolder, position: Int) {
        holder.bind(exams[position])
    }

    override fun getItemCount(): Int = exams.size
}

