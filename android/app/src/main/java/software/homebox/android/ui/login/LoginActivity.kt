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
                    service.login(LoginRequest(username = email, email = email, password = password, stayLoggedIn = true))
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
                    val code = response.code()
                    val errorDetail = if (code == 401) {
                        " (账号或密码错误)"
                    } else if (code == 400) {
                        " (请求错误: 400)"
                    } else {
                        " (HTTP $code)"
                    }
                    Toast.makeText(this@LoginActivity, getString(R.string.login_failed) + errorDetail, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: e.javaClass.simpleName
                Toast.makeText(this@LoginActivity, getString(R.string.login_failed) + ": " + errMsg, Toast.LENGTH_LONG).show()
            } finally {
                binding.btnLogin.isEnabled = true
                binding.pbLoading.visibility = View.GONE
            }
        }
    }
}
