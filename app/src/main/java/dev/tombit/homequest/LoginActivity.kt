package dev.tombit.homequest

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import dev.tombit.homequest.databinding.ActivityLoginBinding
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.SignalManager
import kotlinx.coroutines.launch

/**
 * Firebase Email/Password sign-in screen.
 * On success: fetches user document, reads householdId, routes to MainActivity.
 * Pattern: findViews() + initViews() from onCreate() (Section 3.2).
 * ViewBinding used throughout (Section 3.3).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // Named view references (optional aliases when binding property access is verbose)
    private lateinit var login_LBL_title: MaterialTextView
    private lateinit var login_BTN_signIn: MaterialButton
    private lateinit var login_BTN_signUp: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViews()
        initViews()
    }

    private fun findViews() {
        login_LBL_title = binding.loginLBLTitle
        login_BTN_signIn = binding.loginBTNSignIn
        login_BTN_signUp = binding.loginBTNSignUp
    }

    private fun initViews() {
        login_BTN_signIn.setOnClickListener { attemptSignIn() }
        login_BTN_signUp.setOnClickListener { navigateToSignUp() }
    }

    private fun attemptSignIn() {
        val email = binding.loginETEmail.text.toString().trim()
        val password = binding.loginETPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            SignalManager.getInstance().toast("Please enter your email and password")
            return
        }

        login_BTN_signIn.isEnabled = false

        FirebaseManager.getInstance().auth
            .signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                navigateToMain()
            }
            .addOnFailureListener { e ->
                login_BTN_signIn.isEnabled = true
                SignalManager.getInstance().toast(e.message ?: "Sign-in failed")
            }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }
}
