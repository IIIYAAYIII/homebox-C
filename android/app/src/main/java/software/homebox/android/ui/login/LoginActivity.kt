package software.homebox.android.ui.login

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
import software.homebox.android.data.api.LoginRequest
import software.homebox.android.data.prefs.AppPreferences
import software.homebox.android.databinding.ActivityLoginBinding
import software.homebox.android.ui.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvServerDisplay.text = getString(R.string.settings_current_server) + ": " + AppPreferences.serverUrl

        if (AppPreferences.userEmail.isNotBlank()) {
            binding.etEmail.setText(AppPreferences.userEmail)
        }

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.btnChangeServer.setOnClickListener {
            finish()
        }
    }

    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isBlank()) {
            binding.etEmail.error = getString(R.string.email_hint)
            return
        }
        if (password.isBlank()) {
            binding.etPassword.error = getString(R.string.password_hint)
            return
        }

        binding.btnLogin.isEnabled = false
        binding.pbLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val service = ApiClient.getService()
                val response = withContext(Dispatchers.IO) {
                    service.login(LoginRequest(email, password))
                }

                if (response.isSuccessful && response.body() != null) {
                    val tokenResp = response.body()!!
                    AppPreferences.authToken = tokenResp.token
                    AppPreferences.userEmail = email

                    Toast.makeText(this@LoginActivity, getString(R.string.app_name) + " - OK", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, getString(R.string.login_failed), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, getString(R.string.login_failed) + ": " + e.localizedMessage, Toast.LENGTH_LONG).show()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.pbLoading.visibility = View.GONE
            }
        }
    }
}
