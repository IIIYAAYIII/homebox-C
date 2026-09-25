package software.homebox.android.ui.items

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.api.EntityItem
import software.homebox.android.databinding.ActivityItemDetailBinding

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private var entity: EntityItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        entity = intent.getSerializableExtra("extra_entity") as? EntityItem

        if (entity != null) {
            bindEntity(entity!!)
        } else {
            val entityId = intent.getStringExtra("extra_entity_id")
            if (!entityId.isNullOrBlank()) {
                loadEntityById(entityId)
            } else {
                finish()
            }
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun bindEntity(item: EntityItem) {
        this.entity = item
        binding.tvDetailName.text = item.name

        val loc = item.locationName ?: item.parentName
        if (!loc.isNullOrBlank()) {
            binding.tvDetailLocation.visibility = View.VISIBLE
            binding.tvDetailLocation.text = getString(R.string.item_location, loc)
        } else {
            binding.tvDetailLocation.visibility = View.GONE
        }

        if (!item.description.isNullOrBlank()) {
            binding.tvDetailDesc.visibility = View.VISIBLE
            binding.tvDetailDesc.text = item.description
        } else {
            binding.tvDetailDesc.visibility = View.GONE
        }

        binding.tvDetailQuantity.text = getString(R.string.item_quantity, item.quantity ?: 1)
        binding.tvDetailModel.text = "型号: " + (item.modelNumber ?: "-")
        binding.tvDetailSerial.text = "序列号/SN: " + (item.serialNumber ?: "-")
        binding.tvDetailPrice.text = "购入价格: " + if (item.purchasePrice != null) "¥ ${item.purchasePrice}" else "-"

        val photo = item.attachments?.firstOrNull()
        if (photo != null) {
            val url = ApiClient.getAttachmentUrl(photo.id)
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_nav_items)
                .error(R.drawable.ic_nav_items)
                .centerCrop()
                .into(binding.ivDetailPhoto)
        } else {
            binding.ivDetailPhoto.setImageResource(R.drawable.ic_nav_items)
        }
    }

    private fun loadEntityById(id: String) {
        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val resp = withContext(Dispatchers.IO) { service.getEntity(id) }
                if (resp.isSuccessful && resp.body() != null) {
                    bindEntity(resp.body()!!)
                } else {
                    Toast.makeText(this@ItemDetailActivity, "未找到该资产信息", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ItemDetailActivity, "加载失败: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun confirmDelete() {
        val current = entity ?: return
        AlertDialog.Builder(this)
            .setTitle(R.string.btn_delete)
            .setMessage(R.string.item_delete_confirm)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                performDelete(current.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun performDelete(id: String) {
        binding.btnDelete.isEnabled = false
        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val resp = withContext(Dispatchers.IO) { service.deleteEntity(id) }
                if (resp.isSuccessful) {
                    Toast.makeText(this@ItemDetailActivity, R.string.item_deleted_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Toast.makeText(this@ItemDetailActivity, "删除失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ItemDetailActivity, "删除出错: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            } finally {
                binding.btnDelete.isEnabled = true
            }
        }
    }
}
