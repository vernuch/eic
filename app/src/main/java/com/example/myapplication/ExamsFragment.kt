package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entities.ExamEntity
import com.example.myapplication.ui.exams.ExamAdapter

class ExamsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exams, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.examsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // заглушки
        val exams = listOf(
            ExamEntity(1, 101, "25.10 09:00", "ауд. 101", 201, null),
            ExamEntity(2, 102, "27.10 14:00", "ауд. 203", 202, null),
            ExamEntity(3, 103, "30.10 11:00", "ауд. 105", 203, null)
        )

        recyclerView.adapter = ExamAdapter(exams)
    }
}
