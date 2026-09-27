package software.homebox.android.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.api.EntityItem
import software.homebox.android.databinding.ItemRowBinding

class ItemsAdapter(
    private val onItemClick: (EntityItem) -> Unit
) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    private val items = mutableListOf<EntityItem>()

    fun submitList(newItems: List<EntityItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ItemViewHolder(private val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EntityItem) {
            binding.tvItemName.text = item.name
            val qtyInt = item.quantity?.toInt() ?: 1
            binding.tvItemQuantity.text = itemView.context.getString(R.string.item_quantity, qtyInt)

            val location = item.locationName ?: item.parentName
            if (!location.isNullOrBlank()) {
                binding.tvItemLocation.visibility = View.VISIBLE
                binding.tvItemLocation.text = itemView.context.getString(R.string.item_location, location)
            } else {
                binding.tvItemLocation.visibility = View.GONE
            }

            val serialOrModel = listOfNotNull(item.modelNumber, item.serialNumber).joinToString(" · ")
            if (serialOrModel.isNotBlank()) {
                binding.tvItemSerial.visibility = View.VISIBLE
                binding.tvItemSerial.text = serialOrModel
            } else {
                binding.tvItemSerial.visibility = View.GONE
            }

            // Load primary photo thumbnail if present (checks thumbnailId, imageId, or attachments)
            val targetImageId = item.thumbnailId
                ?: item.imageId
                ?: item.attachments?.firstOrNull { it.type?.startsWith("image", ignoreCase = true) == true || it.type.equals("photo", ignoreCase = true) || it.id.isNotBlank() }?.id

            if (!targetImageId.isNullOrBlank()) {
                val photoUrl = ApiClient.getAttachmentUrl(item.id, targetImageId)
                Glide.with(itemView.context)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_nav_items)
                    .error(R.drawable.ic_nav_items)
                    .centerCrop()
                    .into(binding.ivItemThumb)
            } else {
                binding.ivItemThumb.setImageResource(R.drawable.ic_nav_items)
            }

            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
