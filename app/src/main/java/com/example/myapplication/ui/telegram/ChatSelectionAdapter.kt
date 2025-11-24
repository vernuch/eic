package com.example.myapplication.ui.telegram

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.drinkless.tdlib.TdApi
import com.example.myapplication.R

class ChatSelectionAdapter(
    private val onSelectionChanged: (Set<Long>) -> Unit
) : RecyclerView.Adapter<ChatSelectionAdapter.Holder>() {

    private val items = mutableListOf<TdApi.Chat>()
    private val selected = mutableSetOf<Long>()

    fun setItems(list: List<TdApi.Chat>, preselected: Set<Long> = emptySet()) {
        items.clear()
        items.addAll(list)
        selected.clear()
        selected.addAll(preselected)
        notifyDataSetChanged()
    }

    fun getSelected(): Set<Long> = selected.toSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_select, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chat = items[position]
        val title = when (val t = chat.type) {
            is TdApi.ChatTypePrivate -> chat.title ?: "user"
            else -> chat.title ?: "chat ${chat.id}"
        }
        holder.title.text = title
        holder.type.text = chat.type.javaClass.simpleName
        holder.checkbox.isChecked = selected.contains(chat.id)
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) selected.add(chat.id) else selected.remove(chat.id)
            onSelectionChanged(getSelected())
        }
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val title: TextView = view.findViewById(R.id.chatTitle)
        val type: TextView = view.findViewById(R.id.chatType)
    }
}
