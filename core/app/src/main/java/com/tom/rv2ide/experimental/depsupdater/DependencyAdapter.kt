package com.tom.rv2ide.experimental.depsupdater

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.color.MaterialColors
import com.tom.rv2ide.R

class DependencyAdapter(
    private val onItemClick: (Dependency) -> Unit
) : ListAdapter<Dependency, DependencyAdapter.ViewHolder>(DependencyDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dependency, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: MaterialCardView = itemView.findViewById(R.id.cardDependency)
        private val tvTitle: MaterialTextView = itemView.findViewById(R.id.tvDependencyTitle)
        private val tvCurrentVersion: MaterialTextView = itemView.findViewById(R.id.tvCurrentVersion)
        private val tvUpdateInfo: MaterialTextView = itemView.findViewById(R.id.tvUpdateInfo)
        
        fun bind(dependency: Dependency, onItemClick: (Dependency) -> Unit) {
            if (dependency.group.isEmpty() && dependency.name.isEmpty()) {
                itemView.visibility = View.GONE
                return
            }
            
            itemView.visibility = View.VISIBLE
            tvTitle.text = "${dependency.group}:${dependency.name}"
            
            tvCurrentVersion.text = if (dependency.currentVersion.isNotEmpty()) {
                "Current: ${dependency.currentVersion}"
            } else {
                "Current: Unknown"
            }
            
            val context = itemView.context
            val colorPrimary = MaterialColors.getColor(context, android.R.attr.colorPrimary, 0)
            val colorError = MaterialColors.getColor(context, android.R.attr.colorError, 0)
            val colorTertiary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiary, 0)
            val colorSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, 0)
            val colorOnSurfaceVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
            val colorOutlineVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, 0)
            val colorPrimaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, 0)
            val colorErrorContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorErrorContainer, 0)
            val colorTertiaryContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiaryContainer, 0)
            
            when {
                dependency.hasUpdate && dependency.latestVersion != null -> {
                    tvUpdateInfo.text = context.getString(R.string.dep_update_available, dependency.latestVersion)
                    tvUpdateInfo.setTextColor(colorPrimary)
                    tvUpdateInfo.visibility = View.VISIBLE
                    card.setCardBackgroundColor(colorPrimaryContainer)
                    card.strokeColor = colorPrimary
                    card.setOnClickListener { onItemClick(dependency) }
                }
                dependency.latestVersion != null && !dependency.latestVersion.startsWith("Error") -> {
                    tvUpdateInfo.text = context.getString(R.string.dep_up_to_date)
                    tvUpdateInfo.setTextColor(colorOnSurfaceVariant)
                    tvUpdateInfo.visibility = View.VISIBLE
                    card.setCardBackgroundColor(colorSurface)
                    card.strokeColor = colorOutlineVariant
                    card.setOnClickListener(null)
                }
                dependency.latestVersion != null && dependency.latestVersion.startsWith("Error") -> {
                    tvUpdateInfo.text = dependency.latestVersion
                    tvUpdateInfo.setTextColor(colorError)
                    tvUpdateInfo.visibility = View.VISIBLE
                    card.setCardBackgroundColor(colorErrorContainer)
                    card.strokeColor = colorError
                    card.setOnClickListener(null)
                }
                else -> {
                    tvUpdateInfo.text = context.getString(R.string.dep_not_found)
                    tvUpdateInfo.setTextColor(colorTertiary)
                    tvUpdateInfo.visibility = View.VISIBLE
                    card.setCardBackgroundColor(colorTertiaryContainer)
                    card.strokeColor = colorTertiary
                    card.setOnClickListener(null)
                }
            }
        }
    }
}

class DependencyDiffCallback : DiffUtil.ItemCallback<Dependency>() {
    override fun areItemsTheSame(oldItem: Dependency, newItem: Dependency): Boolean {
        return oldItem.group == newItem.group && oldItem.name == newItem.name
    }
    
    override fun areContentsTheSame(oldItem: Dependency, newItem: Dependency): Boolean {
        return oldItem == newItem
    }
}