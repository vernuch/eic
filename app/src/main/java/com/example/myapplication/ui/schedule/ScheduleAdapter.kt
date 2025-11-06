package com.example.myapplication.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class ScheduleAdapter : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    private var scheduleList: List<ScheduleItem> = emptyList()

    fun submitList(list: List<ScheduleItem>) {
        scheduleList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = scheduleList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = scheduleList.size

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTime: TextView = itemView.findViewById(R.id.text_time)
        private val textSubject: TextView = itemView.findViewById(R.id.text_subject)
        private val textRoom: TextView = itemView.findViewById(R.id.text_room)

        private val replacementContainer: LinearLayout = itemView.findViewById(R.id.replacement_container)
        private val textReplacementSubject: TextView = itemView.findViewById(R.id.text_replacement_subject)
        private val textReplacementRoom: TextView = itemView.findViewById(R.id.text_replacement_room)

        fun bind(item: ScheduleItem) {
            // основная
            textTime.text = item.time
            textSubject.text = item.subject
            textRoom.text = item.room

            // замена
            if (item.isReplacement) {
                replacementContainer.visibility = View.VISIBLE
                textReplacementSubject.text = item.replacementSubject
                textReplacementRoom.text = item.replacementRoom

                textSubject.text = " ${item.subject}"
            } else {
                replacementContainer.visibility = View.GONE
                textSubject.text = item.subject
            }
        }
    }
}