package com.example.myapplication.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import org.threeten.bp.LocalDate

class WeekAdapter(
    private val onDayClick: (LocalDate) -> Unit
) : RecyclerView.Adapter<WeekAdapter.WeekViewHolder>() {

    private var items: List<WeekDayItem> = emptyList()
    private var selectedDate: LocalDate? = null

    fun submitList(newItems: List<WeekDayItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate = date
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_week_day, parent, false)
        return WeekViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class WeekViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDayNumber: TextView = itemView.findViewById(R.id.text_day_number)

        fun bind(item: WeekDayItem) {
            textDayNumber.text = item.date.dayOfMonth.toString()

            val isSelected = item.date == selectedDate
            val isToday = item.isToday

            when {
                isSelected -> {
                    textDayNumber.setBackgroundResource(R.drawable.week_day_selector)
                    textDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                }
                isToday -> {
                    textDayNumber.setBackgroundResource(R.drawable.week_day_today_bg)
                    textDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                }
                else -> {
                    textDayNumber.setBackgroundResource(R.drawable.week_day_normal_bg)
                    textDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                }
            }

            itemView.setOnClickListener {
                onDayClick(item.date)
            }
        }

    }

}
