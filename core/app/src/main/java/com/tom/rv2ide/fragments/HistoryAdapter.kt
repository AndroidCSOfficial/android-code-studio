package com.tom.rv2ide.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tom.rv2ide.R
import com.tom.rv2ide.artificial.agents.UnifiedModificationAttempt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<UnifiedModificationAttempt>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())

    fun setItems(newItems: List<UnifiedModificationAttempt>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val historyIcon: ImageView = itemView.findViewById(R.id.historyIcon)
        private val historyTitle: TextView = itemView.findViewById(R.id.historyTitle)
        private val historyTime: TextView = itemView.findViewById(R.id.historyTime)
        private val historyDetails: TextView = itemView.findViewById(R.id.historyDetails)

        fun bind(item: UnifiedModificationAttempt) {
            val fileName = File(item.filePath).name
            val status = if (item.success) "Modified" else "Failed to modify"
            
            historyTitle.text = "$status $fileName"
            historyTime.text = dateFormat.format(Date(item.timestamp))
            historyDetails.text = itemView.context.getString(R.string.history_attempt, item.attemptNumber, if (item.success) itemView.context.getString(R.string.dep_success) else itemView.context.getString(R.string.dep_error))
            
            // Set icon tint based on success/failure
            val colorRes = if (item.success) {
                android.R.color.holo_green_dark
            } else {
                android.R.color.holo_red_dark
            }
            historyIcon.setColorFilter(itemView.context.getColor(colorRes))
        }
    }
}