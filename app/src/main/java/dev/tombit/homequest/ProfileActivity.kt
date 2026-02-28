package dev.tombit.homequest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.storage.StorageMetadata
import dev.tombit.homequest.R
import dev.tombit.homequest.adapters.LeaderboardAdapter
import dev.tombit.homequest.databinding.ActivityProfileBinding
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.ImageCompressor
import dev.tombit.homequest.utilities.ImageLoader
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager
import android.view.View

/**
 * Profile / Stats screen.
 * Displays level, XP, coin balance for the current user.
 * Leaderboard: fetches all member user docs, sorts client-side by XP desc.
 * Cache: leaderboard result cached for 60 seconds (Risk Register: MED — fine for 2-10 users).
 *
 * Sign-out clears SharedPreferences and navigates to LoginActivity.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private lateinit var profile_LBL_displayName: MaterialTextView
    private lateinit var profile_LBL_level: MaterialTextView
    private lateinit var profile_LBL_xp: MaterialTextView
    private lateinit var profile_LBL_coins: MaterialTextView
    private lateinit var profile_BTN_signOut: MaterialButton
    private lateinit var profile_BTN_back: MaterialButton

    private lateinit var leaderboardAdapter: LeaderboardAdapter
    private var currentUser: User? = null

    // Leaderboard cache (Risk Register: MED — 60s cache for small households)
    private var leaderboardCacheTimestamp: Long = 0L
    private var leaderboardCache: List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViews()
        initViews()
        loadProfile()
    }

    private val pickAvatarImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadAvatar(it) }
    }

    private val requestAvatarPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pickAvatarImage.launch("image/*")
        } else {
            SignalManager.getInstance().toast(getString(R.string.label_permission_denied_storage))
        }
    }

    private fun findViews() {
        profile_LBL_displayName = binding.profileLBLDisplayName
        profile_LBL_level = binding.profileLBLLevel
        profile_LBL_xp = binding.profileLBLXp
        profile_LBL_coins = binding.profileLBLCoins
        profile_BTN_signOut = binding.profileBTNSignOut
        profile_BTN_back = binding.profileBTNBack
    }

    private fun initViews() {
        leaderboardAdapter = LeaderboardAdapter(emptyList())
        binding.profileRVLeaderboard.adapter = leaderboardAdapter

        profile_BTN_signOut.setOnClickListener { signOut() }
        profile_BTN_back.setOnClickListener { finish() }
        binding.profileIMGAvatar.setOnClickListener { launchAvatarPicker() }
    }

    private fun launchAvatarPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ->
                pickAvatarImage.launch("image/*")
            else ->
                requestAvatarPermission.launch(permission)
        }
    }

    private fun uploadAvatar(uri: Uri) {
        val user = currentUser ?: return
        binding.profileIMGAvatar.isClickable = false

        val compressedBytes = ImageCompressor.compress(this, uri)
        if (compressedBytes == null) {
            SignalManager.getInstance().toast("Could not process image. Try another photo.")
            binding.profileIMGAvatar.isClickable = true
            return
        }

        val storagePath = "${Constants.Storage.AVATARS_PATH}/${user.uid}${Constants.Storage.AVATAR_FILE_EXTENSION}"
        val storageRef = FirebaseManager.getInstance().storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        storageRef.putBytes(compressedBytes, metadata)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                FirebaseManager.getInstance().firestore
                    .collection(Constants.Firestore.USERS_COLLECTION)
                    .document(user.uid)
                    .update("avatarUrl", downloadUri.toString())
                    .addOnSuccessListener {
                        val updatedUser = user.copy(avatarUrl = downloadUri.toString())
                        currentUser = updatedUser
                        SharedPreferencesManager.getInstance().putObject(
                            Constants.SP_KEYS.CURRENT_USER_JSON, updatedUser
                        )
                        renderProfile(updatedUser)
                        SignalManager.getInstance().toast(getString(R.string.label_avatar_updated))
                        binding.profileIMGAvatar.isClickable = true
                    }
                    .addOnFailureListener { e ->
                        SignalManager.getInstance().toast("Failed to update: ${e.message}")
                        binding.profileIMGAvatar.isClickable = true
                    }
            }
            .addOnFailureListener { e ->
                SignalManager.getInstance().toast("Upload failed: ${e.message}")
                binding.profileIMGAvatar.isClickable = true
            }
    }

    private fun loadProfile() {
        val uid = FirebaseManager.getInstance().currentUid ?: return

        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.USERS_COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                val user = User.fromMap(doc.data ?: return@addOnSuccessListener)
                currentUser = user
                // Update cache
                SharedPreferencesManager.getInstance().putObject(
                    Constants.SP_KEYS.CURRENT_USER_JSON, user
                )
                renderProfile(user)
                loadLeaderboard(user.householdId)
            }
            .addOnFailureListener { e ->
                SignalManager.getInstance().toast("Failed to load profile: ${e.message}")
                // Fallback to cached user
                currentUser = SharedPreferencesManager.getInstance()
                    .getObject(Constants.SP_KEYS.CURRENT_USER_JSON, User::class.java)
                currentUser?.let {
                    renderProfile(it)
                    loadLeaderboard(it.householdId)
                }
            }
    }

    private fun renderProfile(user: User) {
        profile_LBL_displayName.text = user.displayName
        profile_LBL_level.text = "Level ${user.level}"
        profile_LBL_xp.text = "${user.currentXp} / ${xpForNextLevel(user.level)} XP"
        profile_LBL_coins.text = "${user.coinBalance} 🪙"

        user.avatarUrl?.let {
            ImageLoader.getInstance().loadImage(it, binding.profileIMGAvatar)
        }
    }

    /**
     * Fetches all household member user docs. Fan-out read: N members = N reads.
     * Acceptable for 2-10 users (Risk Register: MED).
     * Result cached for 60 seconds (Constants.Economy.LEADERBOARD_CACHE_MS).
     */
    private fun loadLeaderboard(householdId: String) {
        val now = SystemClock.elapsedRealtime()
        if (leaderboardCache.isNotEmpty() &&
            (now - leaderboardCacheTimestamp) < Constants.Economy.LEADERBOARD_CACHE_MS) {
            leaderboardAdapter.updateData(leaderboardCache)
            return
        }

        // Fetch household to get member UIDs
        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .get()
            .addOnSuccessListener { householdDoc ->
                @Suppress("UNCHECKED_CAST")
                val memberUids = householdDoc.get("members") as? List<String> ?: return@addOnSuccessListener
                fetchMemberUsers(memberUids)
            }
    }

    private fun fetchMemberUsers(uids: List<String>) {
        val db = FirebaseManager.getInstance().firestore
        val members = mutableListOf<User>()
        var remaining = uids.size

        if (remaining == 0) return

        for (uid in uids) {
            db.collection(Constants.Firestore.USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    doc.data?.let { members.add(User.fromMap(it)) }
                    remaining--
                    if (remaining == 0) {
                        val sorted = members.sortedByDescending { it.currentXp }
                        leaderboardCache = sorted
                        leaderboardCacheTimestamp = SystemClock.elapsedRealtime()
                        leaderboardAdapter.updateData(sorted)
                        updateLeaderboardEmptyState(sorted.isEmpty())
                    }
                }
                .addOnFailureListener {
                    remaining--
                    if (remaining == 0 && members.isNotEmpty()) {
                        val sorted = members.sortedByDescending { it.currentXp }
                        leaderboardAdapter.updateData(sorted)
                        updateLeaderboardEmptyState(sorted.isEmpty())
                    }
                }
        }
    }

    private fun updateLeaderboardEmptyState(isEmpty: Boolean) {
        binding.profileLBLLeaderboardEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.profileRVLeaderboard.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun xpForNextLevel(currentLevel: Int): Int {
        val thresholds = Constants.XpThresholds.THRESHOLDS
        val nextLevel = currentLevel // thresholds array is 0-indexed at level 1
        return if (nextLevel < thresholds.size) thresholds[nextLevel] else thresholds.last()
    }

    private fun signOut() {
        FirebaseManager.getInstance().auth.signOut()
        SharedPreferencesManager.getInstance().clearAll()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
