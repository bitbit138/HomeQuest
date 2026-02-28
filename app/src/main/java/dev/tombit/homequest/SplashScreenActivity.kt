package dev.tombit.homequest

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.tombit.homequest.databinding.ActivitySplashScreenBinding
import dev.tombit.homequest.utilities.FirebaseManager

/**
 * Entry point activity. Plays a Lottie animation, then routes to:
 *   - MainActivity if user is already authenticated
 *   - LoginActivity if user is not authenticated
 *
 * Pattern: Professor's L08 SplashScreenActivity exactly (Section 3.12).
 */
class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
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
        // All views accessed via binding — no findViewById needed
    }

    private fun initViews() {
        startAnimation()
    }

    private fun startAnimation() {
        binding.splashLOTTIELottie.resumeAnimation()
        binding.splashLOTTIELottie.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator) {
                navigateNext()
            }
            override fun onAnimationStart(p0: Animator) {
                // Pre-fetch could begin here in V2
            }
            override fun onAnimationCancel(p0: Animator) {
                navigateNext()
            }
            override fun onAnimationRepeat(p0: Animator) {}
        })
    }

    private fun navigateNext() {
        val destination = if (FirebaseManager.getInstance().isLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(destination)
        finish()
    }
}
