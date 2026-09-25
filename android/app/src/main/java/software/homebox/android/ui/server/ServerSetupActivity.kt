package software.homebox.android.ui.server

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import software.homebox.android.R
import software.homebox.android.data.api.ApiClient
import software.homebox.android.data.prefs.AppPreferences
import software.homebox.android.databinding.ActivityServerSetupBinding
import software.homebox.android.ui.MainActivity
import software.homebox.android.ui.login.LoginActivity

class ServerSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already logged in, navigate straight to MainActivity
        if (AppPreferences.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityServerSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pre-fill existing server URL if any
        if (AppPreferences.serverUrl.isNotBlank()) {
            binding.etServerUrl.setText(AppPreferences.serverUrl)
        }

        binding.btnTestConnection.setOnClickListener {
            testServerConnection()
        }

        binding.btnContinue.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            if (url.isBlank()) {
                binding.tilServerUrl.error = getString(R.string.server_address_hint)
                return@setOnClickListener
            }
            AppPreferences.serverUrl = url
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun testServerConnection() {
        val inputUrl = binding.etServerUrl.text.toString().trim()
        if (inputUrl.isBlank()) {
            binding.tilServerUrl.error = getString(R.string.server_address_hint)
            return
        }
        binding.tilServerUrl.error = null

        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.server_status_checking)
        binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
        binding.btnTestConnection.isEnabled = false

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService(inputUrl)
                val response = withContext(Dispatchers.IO) {
                    service.getStatus()
                }

                if (response.isSuccessful && response.body()?.ok == true) {
                    val version = response.body()?.build?.version ?: "OK"
                    binding.tvStatus.text = getString(R.string.server_status_ok, version)
                    binding.tvStatus.setTextColor(getColor(R.color.success))
                } else {
                    binding.tvStatus.text = getString(R.string.server_status_failed)
                    binding.tvStatus.setTextColor(getColor(R.color.error))
                }
            } catch (e: Exception) {
                binding.tvStatus.text = getString(R.string.server_status_failed) + "\n(" + e.localizedMessage + ")"
                binding.tvStatus.setTextColor(getColor(R.color.error))
            } finally {
                binding.btnTestConnection.isEnabled = true
            }
        }
    }
}
