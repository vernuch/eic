package com.example.myapplication.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import org.threeten.bp.LocalDate

class MonthCalendarAdapter(
    private val onDateSelected: (LocalDate) -> Unit
) : RecyclerView.Adapter<MonthCalendarAdapter.DayViewHolder>() {

    private var days: List<LocalDate> = emptyList()
    private var selectedDate: LocalDate? = null

    fun submitList(newDays: List<LocalDate>) {
        days = newDays
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate = date
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDay: TextView = itemView.findViewById(R.id.text_day_number)

        fun bind(date: LocalDate) {
            textDay.text = date.dayOfMonth.toString()
            val context = itemView.context
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now()

            when {
                isSelected -> {
                    textDay.setBackgroundResource(R.drawable.week_day_selector)
                    textDay.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                isToday -> {
                    textDay.setBackgroundResource(R.drawable.week_day_today_bg)
                    textDay.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
                else -> {
                    textDay.setBackgroundResource(R.drawable.week_day_normal_bg)
                    textDay.setTextColor(ContextCompat.getColor(context, R.color.black))
                }
            }

            itemView.setOnClickListener {
                onDateSelected(date)
            }
        }
    }
}
