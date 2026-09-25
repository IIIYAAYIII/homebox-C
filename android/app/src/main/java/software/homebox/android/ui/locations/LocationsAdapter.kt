package software.homebox.android.ui.locations

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import software.homebox.android.data.api.LocationTreeItem
import software.homebox.android.databinding.LocationRowBinding

class LocationsAdapter(
    private val onLocationClick: (LocationTreeItem) -> Unit
) : RecyclerView.Adapter<LocationsAdapter.LocationViewHolder>() {

    private val locations = mutableListOf<LocationTreeItem>()

    fun submitList(newLocations: List<LocationTreeItem>) {
        locations.clear()
        locations.addAll(newLocations)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = LocationRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(locations[position])
    }

    override fun getItemCount(): Int = locations.size

    inner class LocationViewHolder(private val binding: LocationRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(loc: LocationTreeItem) {
            binding.tvLocationName.text = loc.name
            if (!loc.description.isNullOrBlank()) {
                binding.tvLocationDesc.visibility = View.VISIBLE
                binding.tvLocationDesc.text = loc.description
            } else {
                binding.tvLocationDesc.visibility = View.GONE
            }

            val childCount = (loc.children?.size ?: 0)
            val itemCount = loc.itemCount ?: 0
            val parts = mutableListOf<String>()
            if (childCount > 0) parts.add("$childCount 子位置")
            if (itemCount > 0) parts.add("$itemCount 物品")
            binding.tvLocationItemCount.text = parts.joinToString(" · ")

            itemView.setOnClickListener {
                onLocationClick(loc)
            }
        }
    }
}
