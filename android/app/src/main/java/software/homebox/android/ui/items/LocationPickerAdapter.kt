package software.homebox.android.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import software.homebox.android.R
import software.homebox.android.data.api.FlatLocationItem
import software.homebox.android.databinding.ItemLocationPickerRowBinding

class LocationPickerAdapter(
    private val onLocationSelected: (FlatLocationItem?) -> Unit
) : RecyclerView.Adapter<LocationPickerAdapter.ViewHolder>() {

    private val allItems = mutableListOf<FlatLocationItem>()
    private val displayItems = mutableListOf<FlatLocationItem>()
    private var selectedId: String? = null

    fun setData(items: List<FlatLocationItem>, currentSelectedId: String?) {
        allItems.clear()
        allItems.addAll(items)
        selectedId = currentSelectedId
        filter("")
    }

    fun filter(query: String) {
        displayItems.clear()
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            displayItems.addAll(allItems)
        } else {
            displayItems.addAll(allItems.filter {
                it.name.lowercase().contains(q) || it.treeString.lowercase().contains(q)
            })
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLocationPickerRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = displayItems.size + 1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {
            // Option 0: No Location (Clear option)
            holder.binding.ivLocationIcon.setImageResource(R.drawable.ic_close)
            holder.binding.tvLocationName.text = holder.itemView.context.getString(R.string.location_selector_none)
            holder.binding.tvLocationPath.visibility = View.GONE
            val isNoneSelected = selectedId.isNullOrBlank()
            holder.binding.ivSelectedCheck.visibility = if (isNoneSelected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                onLocationSelected(null)
            }
        } else {
            val item = displayItems[position - 1]
            holder.binding.ivLocationIcon.setImageResource(R.drawable.ic_nav_locations)
            holder.binding.tvLocationName.text = item.name
            if (item.treeString != item.name && item.treeString.contains(">")) {
                holder.binding.tvLocationPath.visibility = View.VISIBLE
                holder.binding.tvLocationPath.text = item.treeString
            } else {
                holder.binding.tvLocationPath.visibility = View.GONE
            }
            val isSelected = (item.id == selectedId)
            holder.binding.ivSelectedCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                onLocationSelected(item)
            }
        }
    }

    class ViewHolder(val binding: ItemLocationPickerRowBinding) : RecyclerView.ViewHolder(binding.root)
}
