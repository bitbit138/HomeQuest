package dev.tombit.homequest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dev.tombit.homequest.adapters.FeedAdapter
import dev.tombit.homequest.databinding.ActivityMainBinding
import dev.tombit.homequest.interfaces.FeedCallback
import dev.tombit.homequest.model.FeedItem
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.ImageLoader
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager
import android.view.View
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var main_LBL_greeting: MaterialTextView
    private lateinit var main_LBL_coins: MaterialTextView
    private lateinit var main_LBL_xp: MaterialTextView
    private lateinit var main_LBL_level: MaterialTextView
    private lateinit var main_BTN_quests: MaterialButton
    private lateinit var main_BTN_rewards: MaterialButton
    private lateinit var main_BTN_profile: MaterialButton
    private lateinit var main_FAB_newQuest: ExtendedFloatingActionButton
    private lateinit var main_SRL_feed: SwipeRefreshLayout

    private var feedListener: ListenerRegistration? = null
    private lateinit var feedAdapter: FeedAdapter
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViews()
        initViews()
        loadCurrentUser()
    }

    override fun onResume() {
        super.onResume()
        currentUser?.let { startFeedListener(it.householdId) }
    }

    override fun onPause() {
        super.onPause()
        feedListener?.remove()
        feedListener = null
    }

    private fun findViews() {
        main_LBL_greeting = binding.mainLBLGreeting
        main_LBL_coins    = binding.mainLBLCoins
        main_LBL_xp       = binding.mainLBLXp
        main_LBL_level    = binding.mainLBLLevel
        main_BTN_quests   = binding.mainBTNQuests
        main_BTN_rewards  = binding.mainBTNRewards
        main_BTN_profile  = binding.mainBTNProfile
        main_FAB_newQuest = binding.mainFABNewQuest
        main_SRL_feed     = binding.mainSRLFeed
    }

    private fun initViews() {
        feedAdapter = FeedAdapter(emptyList())
        feedAdapter.feedCallback = object : FeedCallback {
            override fun onFeedItemTapped(item: FeedItem, position: Int) { }
        }
        binding.mainRVFeed.adapter = feedAdapter

        main_BTN_quests.setOnClickListener   { startActivity(Intent(this, QuestListActivity::class.java)) }
        main_BTN_rewards.setOnClickListener  { startActivity(Intent(this, RewardsActivity::class.java)) }
        main_BTN_profile.setOnClickListener  { startActivity(Intent(this, ProfileActivity::class.java)) }
        main_FAB_newQuest.setOnClickListener { startActivity(Intent(this, CreateQuestActivity::class.java)) }
        main_SRL_feed.setOnRefreshListener   { loadCurrentUser() }
        main_SRL_feed.setColorSchemeColors(ContextCompat.getColor(this, R.color.hq_purple_primary))
    }

    private fun loadCurrentUser() {
        val uid = FirebaseManager.getInstance().currentUid ?: run { navigateToLogin(); return }

        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.USERS_COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                main_SRL_feed.isRefreshing = false
                val user = User.fromMap(doc.data ?: return@addOnSuccessListener)
                currentUser = user
                SharedPreferencesManager.getInstance().putObject(Constants.SP_KEYS.CURRENT_USER_JSON, user)
                updateHeaderUI(user)
                loadHouseholdInfo(user.householdId)
                startFeedListener(user.householdId)
            }
            .addOnFailureListener { e ->
                main_SRL_feed.isRefreshing = false
                SignalManager.getInstance().toast("Failed to load user: ${e.message}")
            }
    }

    /** Load household name + invite code to display in header (Bugs 4 & 6). */
    private fun loadHouseholdInfo(householdId: String) {
        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .get()
            .addOnSuccessListener { doc ->
                val name       = doc.getString("name") ?: ""
                val inviteCode = doc.getString("inviteCode") ?: ""

                binding.mainLBLHouseholdName.text = "🏠 $name"
                binding.mainLBLInviteCode.text = inviteCode

                // Tap invite code strip to copy
                binding.mainCVInviteCode.setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Invite Code", inviteCode))
                    SignalManager.getInstance().toast("Invite code copied! 📋")
                }
            }
    }

    private fun updateHeaderUI(user: User) {
        main_LBL_greeting.text = "Hey, ${user.displayName}! 👋"
        main_LBL_coins.text    = "${user.coinBalance} 🪙"
        main_LBL_xp.text       = "${user.currentXp} XP"
        main_LBL_level.text    = "Lvl ${user.level}"
        
        // Refresh SharedPreferences as well
        SharedPreferencesManager.getInstance().putObject(Constants.SP_KEYS.CURRENT_USER_JSON, user)
        
        user.avatarUrl?.let { ImageLoader.getInstance().loadImage(it, binding.mainIMGAvatar) }
    }

    private fun startFeedListener(householdId: String) {
        feedListener?.remove()
        feedListener = FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.FEED_SUB_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(Constants.Feed.PAGE_SIZE.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    SignalManager.getInstance().toast("Feed error: ${error.message}")
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { FeedItem.fromMap(it) }
                } ?: return@addSnapshotListener

                lifecycleScope.launch { 
                    feedAdapter.updateData(items)
                    binding.mainLBLFeedEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    binding.mainRVFeed.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
