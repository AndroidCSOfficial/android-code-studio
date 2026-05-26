package com.tom.rv2ide.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.tom.rv2ide.R

class FileModificationAdapter : RecyclerView.Adapter<FileModificationAdapter.ViewHolder>() {

    private val items = mutableListOf<FileModificationItem>()
    private var onItemClickListener: ((String) -> Unit)? = null

    data class FileModificationItem(
        val fileName: String,
        val status: Status
    )

    enum class Status {
        MODIFYING, SUCCESS, FAILED
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileName)
        val fileStatus: TextView = view.findViewById(R.id.fileStatus)
        val statusIcon: ImageView = view.findViewById(R.id.fileStatusIcon)
        val progressIndicator: CircularProgressIndicator = view.findViewById(R.id.fileProgressIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_modification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.fileName.text = item.fileName
        
        // Add click listener
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item.fileName)
        }
        
        when (item.status) {
            Status.MODIFYING -> {
                holder.fileStatus.text = holder.itemView.context.getString(R.string.file_mod_modifying)
                holder.progressIndicator.visibility = View.VISIBLE
                holder.statusIcon.visibility = View.GONE
            }
            Status.SUCCESS -> {
                holder.fileStatus.text = holder.itemView.context.getString(R.string.file_mod_success)
                holder.progressIndicator.visibility = View.GONE
                holder.statusIcon.visibility = View.VISIBLE
                holder.statusIcon.setImageResource(android.R.drawable.ic_menu_save)
                holder.statusIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_green_dark)
                )
            }
            Status.FAILED -> {
                holder.fileStatus.text = holder.itemView.context.getString(R.string.file_mod_failed)
                holder.progressIndicator.visibility = View.GONE
                holder.statusIcon.visibility = View.VISIBLE
                holder.statusIcon.setImageResource(android.R.drawable.ic_delete)
                holder.statusIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.context, android.R.color.holo_red_dark)
                )
            }
        }
    }

    override fun getItemCount() = items.size

    fun setOnItemClickListener(listener: (String) -> Unit) {
        this.onItemClickListener = listener
    }
    
    fun addItem(fileName: String) {
        items.add(FileModificationItem(fileName, Status.MODIFYING))
        notifyItemInserted(items.size - 1)
    }

    fun updateItemStatus(fileName: String, success: Boolean) {
        val index = items.indexOfFirst { it.fileName == fileName }
        if (index != -1) {
            items[index] = items[index].copy(
                status = if (success) Status.SUCCESS else Status.FAILED
            )
            notifyItemChanged(index)
        }
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
}