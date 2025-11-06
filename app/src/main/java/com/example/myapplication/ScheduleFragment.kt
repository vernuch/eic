package com.example.myapplication.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentScheduleBinding
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private val weekAdapter by lazy { WeekAdapter(::onDaySelected) }
    private val monthAdapter by lazy { MonthCalendarAdapter(::onDaySelected) }
    private val scheduleAdapter by lazy { ScheduleAdapter() }

    private var currentDate = LocalDate.now()
    private var currentMonth = YearMonth.now()
    private var isCalendarVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWeekRecycler()
        setupMonthRecycler()
        setupScheduleRecycler()
        setupNavigation()
        updateUI()
    }

    private fun setupWeekRecycler() {
        binding.weekRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.weekRecyclerView.adapter = weekAdapter
    }

    private fun setupMonthRecycler() {
        binding.monthRecyclerView.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.monthRecyclerView.adapter = monthAdapter
    }

    private fun setupScheduleRecycler() {
        binding.scheduleRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.scheduleRecyclerView.adapter = scheduleAdapter
    }

    private fun setupNavigation() {
        binding.buttonPrevDay.setOnClickListener {
            currentDate = currentDate.minusDays(1)
            updateUI()
            scrollWeekToCurrent()
        }
        binding.buttonNextDay.setOnClickListener {
            currentDate = currentDate.plusDays(1)
            updateUI()
            scrollWeekToCurrent()
        }

        binding.buttonToggleCalendar.setOnClickListener {
            isCalendarVisible = !isCalendarVisible
            if (isCalendarVisible) {

                binding.monthContainer.visibility = View.VISIBLE
                binding.weekRecyclerView.visibility = View.GONE
                binding.buttonToggleCalendar.setImageResource(R.drawable.ic_arrow_up)
                updateMonthCalendar()
            } else {

                binding.monthContainer.visibility = View.GONE
                binding.weekRecyclerView.visibility = View.VISIBLE
                binding.buttonToggleCalendar.setImageResource(R.drawable.ic_arrow_down)
                updateWeekStrip()
            }
        }

        binding.buttonPrevMonth.setOnClickListener {
            currentMonth = currentMonth.minusMonths(1)
            updateMonthCalendar()
        }

        binding.buttonNextMonth.setOnClickListener {
            currentMonth = currentMonth.plusMonths(1)
            updateMonthCalendar()
        }
    }

    private fun updateUI() {
        updateDateHeader()
        if (isCalendarVisible) updateMonthCalendar() else updateWeekStrip()
        updateSchedule()
    }

    private fun updateDateHeader() {
        val formatter = DateTimeFormatter.ofPattern("dd.MM")
        binding.textCurrentDate.text = currentDate.format(formatter)
    }

    private fun updateWeekStrip() {
        val today = LocalDate.now()
        val startOfWeek = currentDate.with(DayOfWeek.MONDAY)
        val weekDays = (0..6).map {
            val date = startOfWeek.plusDays(it.toLong())
            WeekDayItem(date, date == currentDate, date == today)
        }
        weekAdapter.submitList(weekDays)
        weekAdapter.setSelectedDate(currentDate)
    }

    private fun scrollWeekToCurrent() {
        val position = when (currentDate.dayOfWeek) {
            DayOfWeek.MONDAY -> 0
            DayOfWeek.TUESDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.THURSDAY -> 3
            DayOfWeek.FRIDAY -> 4
            DayOfWeek.SATURDAY -> 5
            else -> 6
        }
        binding.weekRecyclerView.scrollToPosition(position)
    }

    private fun updateMonthCalendar() {
        val days = mutableListOf<LocalDate>()
        val firstOfMonth = currentMonth.atDay(1)
        val firstDayOfWeek = (firstOfMonth.dayOfWeek.value + 6) % 7
        val totalDays = currentMonth.lengthOfMonth()

        for (i in 1..firstDayOfWeek) {
            days.add(firstOfMonth.minusDays((firstDayOfWeek - i + 1).toLong()))
        }

        for (i in 1..totalDays) {
            days.add(firstOfMonth.plusDays((i - 1).toLong()))
        }

        while (days.size % 7 != 0) {
            days.add(days.last().plusDays(1))
        }

        monthAdapter.submitList(days)
        monthAdapter.setSelectedDate(currentDate)

        val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
        val monthName = currentMonth.format(monthFormatter)
        val correctMonth = monthName
            .replace("января", "январь")
            .replace("февраля", "февраль")
            .replace("марта", "март")
            .replace("апреля", "апрель")
            .replace("мая", "май")
            .replace("июня", "июнь")
            .replace("июля", "июль")
            .replace("августа", "август")
            .replace("сентября", "сентябрь")
            .replace("октября", "октябрь")
            .replace("ноября", "ноябрь")
            .replace("декабря", "декабрь")

        binding.textCurrentMonth.text =
            correctMonth.replaceFirstChar { it.uppercase() }
    }

    private fun updateSchedule() {
        val lessons = getMockScheduleForDate(currentDate)
        scheduleAdapter.submitList(lessons)
    }

    private fun getMockScheduleForDate(date: LocalDate): List<ScheduleItem> {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                ScheduleItem("09:00 - 10:30", "Математика", "101", true, "Физика", "203"),
                ScheduleItem("10:40 - 12:10", "Программирование", "305")
            )
            DayOfWeek.TUESDAY -> listOf(ScheduleItem("09:00 - 10:30", "Физика", "204"))
            else -> emptyList()
        }
    }

    private fun onDaySelected(date: LocalDate) {
        currentDate = date
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


