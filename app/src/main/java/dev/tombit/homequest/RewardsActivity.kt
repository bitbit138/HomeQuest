package dev.tombit.homequest

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ListenerRegistration
import dev.tombit.homequest.adapters.CouponAdapter
import dev.tombit.homequest.databinding.ActivityRewardsBinding
import dev.tombit.homequest.interfaces.CouponCallback
import dev.tombit.homequest.model.Coupon
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager
import android.view.View
import kotlinx.coroutines.launch

/**
 * Rewards Market screen.
 * Two live queries on coupons (Section 2.2):
 *  1. Available: buyerId == null
 *  2. Owned by current user: buyerId == uid
 *
 * Purchase uses Firestore Transaction (Section 7.1) — critical for coin race condition safety.
 * Real-time listener: recommended (registered onResume, removed onPause).
 */
class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding

    private lateinit var rewards_LBL_balance: MaterialTextView
    private lateinit var rewards_BTN_createCoupon: MaterialButton
    private lateinit var rewards_BTN_back: MaterialButton

    private lateinit var availableAdapter: CouponAdapter
    private lateinit var ownedAdapter: CouponAdapter

    private var availableListener: ListenerRegistration? = null
    private var ownedListener: ListenerRegistration? = null
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
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

    override fun onResume() {
        super.onResume()
        currentUser?.let {
            startAvailableListener(it.householdId)
            startOwnedListener(it.householdId, it.uid)
        }
    }

    override fun onPause() {
        super.onPause()
        availableListener?.remove(); availableListener = null
        ownedListener?.remove(); ownedListener = null
    }

    private fun findViews() {
        rewards_LBL_balance = binding.rewardsLBLBalance
        rewards_BTN_createCoupon = binding.rewardsBTNCreateCoupon
        rewards_BTN_back = binding.rewardsBTNBack
    }

    private fun initViews() {
        availableAdapter = CouponAdapter(emptyList())
        availableAdapter.couponCallback = object : CouponCallback {
            override fun onCouponPurchased(coupon: Coupon, position: Int) {
                purchaseCoupon(coupon)
            }
            override fun onCouponRedeemed(coupon: Coupon, position: Int) { /* available coupons can't be redeemed */ }
            override fun onCouponTapped(coupon: Coupon, position: Int) { }
        }
        binding.rewardsRVAvailable.adapter = availableAdapter

        ownedAdapter = CouponAdapter(emptyList())
        ownedAdapter.couponCallback = object : CouponCallback {
            override fun onCouponPurchased(coupon: Coupon, position: Int) { }
            override fun onCouponRedeemed(coupon: Coupon, position: Int) {
                redeemCoupon(coupon)
            }
            override fun onCouponTapped(coupon: Coupon, position: Int) { }
        }
        binding.rewardsRVOwned.adapter = ownedAdapter

        rewards_BTN_createCoupon.setOnClickListener {
            startActivity(android.content.Intent(this, CreateCouponActivity::class.java))
        }
        rewards_BTN_back.setOnClickListener { finish() }
    }

    private fun loadUser() {
        currentUser = SharedPreferencesManager.getInstance()
            .getObject(Constants.SP_KEYS.CURRENT_USER_JSON, User::class.java)
        currentUser?.let {
            rewards_LBL_balance.text = "${it.coinBalance} 🪙"
            availableAdapter.currentUid = it.uid
        }
    }

    private fun startAvailableListener(householdId: String) {
        availableListener?.remove()
        availableListener = FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.COUPONS_SUB_COLLECTION)
            .whereEqualTo("buyerId", null)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val coupons = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Coupon.fromMap(it) }
                } ?: return@addSnapshotListener
                lifecycleScope.launch { 
                    availableAdapter.updateData(coupons)
                    binding.rewardsLBLAvailableEmpty.visibility = if (coupons.isEmpty()) View.VISIBLE else View.GONE
                    binding.rewardsRVAvailable.visibility = if (coupons.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }

    private fun startOwnedListener(householdId: String, uid: String) {
        ownedListener?.remove()
        ownedListener = FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.COUPONS_SUB_COLLECTION)
            .whereEqualTo("buyerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val coupons = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Coupon.fromMap(it) }
                } ?: return@addSnapshotListener
                lifecycleScope.launch { 
                    ownedAdapter.updateData(coupons)
                    binding.rewardsLBLOwnedEmpty.visibility = if (coupons.isEmpty()) View.VISIBLE else View.GONE
                    binding.rewardsRVOwned.visibility = if (coupons.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }

    /**
     * Coupon purchase via Cloud Function (bypasses Firestore rules on coinBalance).
     * Atomically: checks balance >= cost, debits coins, sets buyerId, writes feed.
     */
    private fun purchaseCoupon(coupon: Coupon) {
        val user = currentUser ?: return

        val functions: FirebaseFunctions = Firebase.functions
        val data = hashMapOf(
            "householdId" to user.householdId,
            "couponId" to coupon.couponId
        )

        functions.getHttpsCallable("purchaseCoupon")
            .call(data)
            .addOnSuccessListener {
                SignalManager.getInstance().toast("Coupon purchased! 🎉")
                SignalManager.getInstance().vibrate()
            }
            .addOnFailureListener { e ->
                val msg = e.message ?: "Purchase failed"
                SignalManager.getInstance().toast(msg)
            }
    }

    /**
     * Marks a purchased coupon as redeemed (seller confirms).
     */
    private fun redeemCoupon(coupon: Coupon) {
        val user = currentUser ?: return
        
        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(user.householdId)
            .collection(Constants.Firestore.COUPONS_SUB_COLLECTION)
            .document(coupon.couponId)
            .update("isRedeemed", true)
            .addOnSuccessListener {
                SignalManager.getInstance().toast("Coupon used! ✨")
                SignalManager.getInstance().vibrate()
                
                // Add a feed entry for using the coupon
                val feedRef = FirebaseManager.getInstance().firestore
                    .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
                    .document(user.householdId)
                    .collection(Constants.Firestore.FEED_SUB_COLLECTION)
                    .document()
                
                feedRef.set(mapOf(
                    "entryId" to feedRef.id,
                    "type" to "coupon_redeemed",
                    "actorId" to user.uid,
                    "actorName" to (user.displayName ?: "A member"),
                    "message" to "${user.displayName ?: "A member"} used a reward: \"${coupon.title}\"! 🎁",
                    "relatedEntityId" to coupon.couponId,
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                ))
            }
            .addOnFailureListener { e ->
                SignalManager.getInstance().toast(e.message ?: "Redemption failed")
            }
    }
}
