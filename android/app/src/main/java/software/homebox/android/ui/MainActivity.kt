package software.homebox.android.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.api.EntityItem
import software.homebox.android.data.api.LocationTreeItem
import software.homebox.android.data.prefs.AppPreferences
import software.homebox.android.databinding.ActivityMainBinding
import software.homebox.android.ui.items.ItemCreateActivity
import software.homebox.android.ui.items.ItemDetailActivity
import software.homebox.android.ui.items.ItemsAdapter
import software.homebox.android.ui.locations.LocationsAdapter
import software.homebox.android.ui.scanner.ScannerActivity
import software.homebox.android.ui.server.ServerSetupActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var itemsAdapter: ItemsAdapter
    private lateinit var locationsAdapter: LocationsAdapter

    private var currentTab = R.id.nav_items
    private var allItems = listOf<EntityItem>()
    private var allLocations = listOf<LocationTreeItem>()

    private val createItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadItems()
        }
    }

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val code = result.data?.getStringExtra("scanned_code")
            if (!code.isNullOrBlank()) {
                binding.searchView.setQuery(code, true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupBottomNavigation()
        setupSearch()

        binding.swipeRefresh.setOnRefreshListener {
            if (currentTab == R.id.nav_items) {
                loadItems()
            } else if (currentTab == R.id.nav_locations) {
                loadLocations()
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, ItemCreateActivity::class.java)
            createItemLauncher.launch(intent)
        }

        loadItems()
    }

    private fun setupRecyclerView() {
        itemsAdapter = ItemsAdapter { item ->
            val intent = Intent(this, ItemDetailActivity::class.java).apply {
                putExtra("extra_entity", item)
            }
            createItemLauncher.launch(intent)
        }

        locationsAdapter = LocationsAdapter { loc ->
            // Filter items by this location
            binding.bottomNavigation.selectedItemId = R.id.nav_items
            binding.searchView.setQuery(loc.name, true)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = itemsAdapter
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_items -> {
                    currentTab = R.id.nav_items
                    binding.searchContainer.visibility = View.VISIBLE
                    binding.fabAdd.visibility = View.VISIBLE
                    binding.recyclerView.adapter = itemsAdapter
                    filterItems(binding.searchView.query?.toString().orEmpty())
                    if (allItems.isEmpty()) loadItems()
                    true
                }
                R.id.nav_locations -> {
                    currentTab = R.id.nav_locations
                    binding.searchContainer.visibility = View.GONE
                    binding.fabAdd.visibility = View.GONE
                    binding.recyclerView.adapter = locationsAdapter
                    loadLocations()
                    true
                }
                R.id.nav_scanner -> {
                    val intent = Intent(this, ScannerActivity::class.java)
                    scannerLauncher.launch(intent)
                    false
                }
                R.id.nav_settings -> {
                    showSettingsDialog()
                    false
                }
                else -> false
            }
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterItems(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterItems(newText.orEmpty())
                return true
            }
        })
    }

    private fun loadItems() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val response = withContext(Dispatchers.IO) {
                    service.getEntities(page = 1, pageSize = 100)
                }

                if (response.isSuccessful && response.body() != null) {
                    allItems = response.body()!!.items ?: emptyList()
                    filterItems(binding.searchView.query?.toString().orEmpty())
                } else {
                    Toast.makeText(this@MainActivity, "加载物品失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "网络错误: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun loadLocations() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val response = withContext(Dispatchers.IO) {
                    service.getLocationTree()
                }

                if (response.isSuccessful && response.body() != null) {
                    allLocations = response.body()!!
                    locationsAdapter.submitList(allLocations)
                    binding.emptyState.visibility = if (allLocations.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    Toast.makeText(this@MainActivity, "加载库位失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "网络错误: " + e.localizedMessage, Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun filterItems(query: String) {
        if (currentTab != R.id.nav_items) return
        val filtered = if (query.isBlank()) {
            allItems
        } else {
            val q = query.lowercase().trim()
            allItems.filter {
                it.name.lowercase().contains(q) ||
                (it.description?.lowercase()?.contains(q) == true) ||
                (it.serialNumber?.lowercase()?.contains(q) == true) ||
                (it.modelNumber?.lowercase()?.contains(q) == true) ||
                (it.locationName?.lowercase()?.contains(q) == true)
            }
        }

        itemsAdapter.submitList(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showSettingsDialog() {
        val server = AppPreferences.serverUrl
        val email = AppPreferences.userEmail

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setMessage("服务器: $server\n当前用户: $email")
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                confirmLogout()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(R.string.btn_logout)
            .setMessage(R.string.logout_confirm)
            .setPositiveButton(R.string.btn_logout) { _, _ ->
                AppPreferences.clearAuth()
                val intent = Intent(this, ServerSetupActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
