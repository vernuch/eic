package com.example.myapplication.ui.messages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages = listOf<MessageItem>()

    fun submitList(list: List<MessageItem>) {
        messages = list
        notifyDataSetChanged()
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val textType: TextView = view.findViewById(R.id.textType)
        private val textPreview: TextView = view.findViewById(R.id.textPreview)
        private val textFull: TextView = view.findViewById(R.id.textFull)

        fun bind(message: MessageItem) {
            textType.text = message.type
            textPreview.text = message.content.take(50) + if (message.content.length > 50) "..." else ""
            textFull.text = message.content

            textFull.visibility = if (message.isExpanded) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                message.isExpanded = !message.isExpanded
                notifyItemChanged(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size
}

