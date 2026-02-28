package dev.tombit.homequest

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.FieldValue
import dev.tombit.homequest.databinding.ActivityCreateCouponBinding
import dev.tombit.homequest.model.Coupon
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager

/**
 * Create Coupon screen — lists a new reward in the household marketplace.
 */
class CreateCouponActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCouponBinding

    private lateinit var createCoupon_LBL_title: MaterialTextView
    private lateinit var createCoupon_BTN_create: MaterialButton
    private lateinit var createCoupon_BTN_cancel: MaterialButton

    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCouponBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViews()
        initViews()
        loadUser()
    }

    private fun findViews() {
        createCoupon_LBL_title = binding.createCouponLBLTitle
        createCoupon_BTN_create = binding.createCouponBTNCreate
        createCoupon_BTN_cancel = binding.createCouponBTNCancel
    }

    private fun initViews() {
        createCoupon_BTN_create.setOnClickListener { attemptCreateCoupon() }
        createCoupon_BTN_cancel.setOnClickListener { finish() }
    }

    private fun loadUser() {
        currentUser = SharedPreferencesManager.getInstance()
            .getObject(Constants.SP_KEYS.CURRENT_USER_JSON, User::class.java)
    }

    private fun attemptCreateCoupon() {
        val user = currentUser ?: run {
            SignalManager.getInstance().toast("User not loaded.")
            return
        }

        val title = binding.createCouponETTitle.text.toString().trim()
        val costText = binding.createCouponETCost.text.toString()

        if (title.isEmpty()) {
            SignalManager.getInstance().toast("Coupon title is required.")
            return
        }
        if (title.length > Constants.Coupon.MAX_TITLE_LENGTH) {
            SignalManager.getInstance().toast("Title too long (max ${Constants.Coupon.MAX_TITLE_LENGTH} chars).")
            return
        }
        val cost = costText.toIntOrNull() ?: run {
            SignalManager.getInstance().toast("Enter a valid coin cost.")
            return
        }
        if (cost <= 0) {
            SignalManager.getInstance().toast("Cost must be greater than 0.")
            return
        }

        createCoupon_BTN_create.isEnabled = false

        val db = FirebaseManager.getInstance().firestore
        val couponRef = db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(user.householdId)
            .collection(Constants.Firestore.COUPONS_SUB_COLLECTION)
            .document()

        val coupon = Coupon.Builder()
            .couponId(couponRef.id)
            .title(title)
            .cost(cost)
            .sellerId(user.uid)
            .build()

        couponRef.set(coupon.toCreationMap())
            .addOnSuccessListener {
                SignalManager.getInstance().toast("Coupon listed!")
                finish()
            }
            .addOnFailureListener { e ->
                createCoupon_BTN_create.isEnabled = true
                SignalManager.getInstance().toast(e.message ?: "Failed to create coupon")
            }
    }
}
