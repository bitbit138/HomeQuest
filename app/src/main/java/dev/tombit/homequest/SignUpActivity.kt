package dev.tombit.homequest

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FieldValue
import dev.tombit.homequest.databinding.ActivitySignUpBinding
import dev.tombit.homequest.model.Household
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.SignalManager

/**
 * Sign-up screen: two-step flow.
 * Step 1: User picks CREATE or JOIN by tapping a card.
 * Step 2: Relevant fields appear; user fills in and submits.
 *
 * Critical rules (Section 5.1, Risk Register):
 *  - users/{uid} document ID must equal Firebase Auth UID.
 *  - Invite code must be globally unique — checked before write; retry on collision.
 */
class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding

    private enum class Mode { NONE, CREATE, JOIN }
    private var selectedMode = Mode.NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
    }

    private fun initViews() {
        // Step 1: mode selection cards
        binding.signupCARDCreate.setOnClickListener { selectMode(Mode.CREATE) }
        binding.signupCARDJoin.setOnClickListener { selectMode(Mode.JOIN) }

        // Step 2: submit buttons
        binding.signupBTNCreateHousehold.setOnClickListener { attemptSignUpAndCreateHousehold() }
        binding.signupBTNJoinHousehold.setOnClickListener { attemptSignUpAndJoinHousehold() }

        // Back: if mode selected → go back to mode selection; else finish
        binding.signupBTNBack.setOnClickListener {
            if (selectedMode != Mode.NONE) resetToModeSelection()
            else finish()
        }
    }

    private fun selectMode(mode: Mode) {
        selectedMode = mode
        // Highlight selected card
        binding.signupCARDCreate.strokeWidth = if (mode == Mode.CREATE) 3 else 0
        binding.signupCARDJoin.strokeWidth = if (mode == Mode.JOIN) 3 else 0

        // Show fields container
        binding.signupLLFields.visibility = View.VISIBLE

        // Show mode-specific fields and submit button
        when (mode) {
            Mode.CREATE -> {
                binding.signupTILHouseholdName.visibility = View.VISIBLE
                binding.signupTILInviteCode.visibility = View.GONE
                binding.signupBTNCreateHousehold.visibility = View.VISIBLE
                binding.signupBTNJoinHousehold.visibility = View.GONE
                binding.signupLBLTitle.text = "Create your household"
            }
            Mode.JOIN -> {
                binding.signupTILHouseholdName.visibility = View.GONE
                binding.signupTILInviteCode.visibility = View.VISIBLE
                binding.signupBTNCreateHousehold.visibility = View.GONE
                binding.signupBTNJoinHousehold.visibility = View.VISIBLE
                binding.signupLBLTitle.text = "Join a household"
            }
            Mode.NONE -> {}
        }
    }

    private fun resetToModeSelection() {
        selectedMode = Mode.NONE
        binding.signupLLFields.visibility = View.GONE
        binding.signupCARDCreate.strokeWidth = 0
        binding.signupCARDJoin.strokeWidth = 0
        binding.signupLBLTitle.text = "Join HomeQuest"
    }

    // ── Create flow ────────────────────────────────────────────────────────

    private fun attemptSignUpAndCreateHousehold() {
        val email = binding.signupETEmail.text.toString().trim()
        val password = binding.signupETPassword.text.toString()
        val displayName = binding.signupETDisplayName.text.toString().trim()
        val householdName = binding.signupETHouseholdName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || displayName.isEmpty() || householdName.isEmpty()) {
            SignalManager.getInstance().toast("Please fill in all fields")
            return
        }
        if (password.length < 6) {
            SignalManager.getInstance().toast("Password must be at least 6 characters")
            return
        }

        setLoading(true)
        FirebaseManager.getInstance().auth
            .createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    setLoading(false)
                    SignalManager.getInstance().toast("Account creation failed.")
                    return@addOnSuccessListener
                }
                createHouseholdAndUser(uid, email, displayName, householdName)
            }
            .addOnFailureListener { e ->
                setLoading(false)
                SignalManager.getInstance().toast(e.message ?: "Sign-up failed")
            }
    }

    private fun createHouseholdAndUser(
        uid: String, email: String, displayName: String,
        householdName: String, retryCount: Int = 0
    ) {
        if (retryCount > 5) {
            setLoading(false)
            SignalManager.getInstance().toast("Failed to generate unique invite code. Try again.")
            return
        }

        val inviteCode = generateInviteCode()
        val db = FirebaseManager.getInstance().firestore

        db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .whereEqualTo("inviteCode", inviteCode)
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) {
                    createHouseholdAndUser(uid, email, displayName, householdName, retryCount + 1)
                    return@addOnSuccessListener
                }

                val householdRef = db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION).document()
                val householdId = householdRef.id

                val household = Household.Builder()
                    .householdId(householdId)
                    .name(householdName)
                    .members(listOf(uid))
                    .inviteCode(inviteCode)
                    .createdBy(uid)
                    .build()

                val user = User.Builder()
                    .uid(uid)
                    .displayName(displayName)
                    .email(email)
                    .householdId(householdId)
                    .build()

                val batch = db.batch()
                batch.set(householdRef, household.toCreationMap())
                batch.set(
                    db.collection(Constants.Firestore.USERS_COLLECTION).document(uid),
                    user.toCreationMap()
                )

                batch.commit()
                    .addOnSuccessListener {
                        writeMemberJoinedFeedEntry(uid, displayName, householdId)
                        navigateToMain()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        SignalManager.getInstance().toast(e.message ?: "Setup failed")
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                SignalManager.getInstance().toast(e.message ?: "Could not verify invite code")
            }
    }

    // ── Join flow ──────────────────────────────────────────────────────────

    private fun attemptSignUpAndJoinHousehold() {
        val email = binding.signupETEmail.text.toString().trim()
        val password = binding.signupETPassword.text.toString()
        val displayName = binding.signupETDisplayName.text.toString().trim()
        val inviteCode = binding.signupETInviteCode.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || displayName.isEmpty() || inviteCode.isEmpty()) {
            SignalManager.getInstance().toast("Please fill in all fields")
            return
        }

        setLoading(true)
        FirebaseManager.getInstance().auth
            .createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    setLoading(false)
                    SignalManager.getInstance().toast("Account creation failed.")
                    return@addOnSuccessListener
                }
                joinHouseholdByCode(uid, email, displayName, inviteCode.uppercase())
            }
            .addOnFailureListener { e ->
                setLoading(false)
                SignalManager.getInstance().toast(e.message ?: "Sign-up failed")
            }
    }

    private fun joinHouseholdByCode(uid: String, email: String, displayName: String, inviteCode: String) {
        val db = FirebaseManager.getInstance().firestore

        db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .whereEqualTo("inviteCode", inviteCode)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    setLoading(false)
                    SignalManager.getInstance().toast("Invite code not found. Check and try again.")
                    return@addOnSuccessListener
                }

                val householdDoc = result.documents[0]
                val householdId = householdDoc.id

                val user = User.Builder()
                    .uid(uid)
                    .displayName(displayName)
                    .email(email)
                    .householdId(householdId)
                    .build()

                val batch = db.batch()
                batch.set(
                    db.collection(Constants.Firestore.USERS_COLLECTION).document(uid),
                    user.toCreationMap()
                )
                batch.update(
                    db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION).document(householdId),
                    "members", FieldValue.arrayUnion(uid)
                )

                batch.commit()
                    .addOnSuccessListener {
                        writeMemberJoinedFeedEntry(uid, displayName, householdId)
                        navigateToMain()
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        SignalManager.getInstance().toast(e.message ?: "Join failed")
                    }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                SignalManager.getInstance().toast(e.message ?: "Could not look up invite code")
            }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun writeMemberJoinedFeedEntry(uid: String, displayName: String, householdId: String) {
        val db = FirebaseManager.getInstance().firestore
        val entryRef = db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.FEED_SUB_COLLECTION)
            .document()

        entryRef.set(mapOf(
            "entryId" to entryRef.id,
            "type" to Constants.Feed.TYPE_MEMBER_JOINED,
            "actorId" to uid,
            "actorName" to displayName,
            "message" to "$displayName joined the household! 👋",
            "relatedEntityId" to uid,
            "timestamp" to FieldValue.serverTimestamp()
        ))
    }

    private fun generateInviteCode(): String {
        val chars = Constants.Household.INVITE_CODE_CHARS
        return (1..Constants.Household.INVITE_CODE_LENGTH).map { chars.random() }.joinToString("")
    }

    private fun setLoading(loading: Boolean) {
        binding.signupBTNCreateHousehold.isEnabled = !loading
        binding.signupBTNJoinHousehold.isEnabled = !loading
        binding.signupCARDCreate.isEnabled = !loading
        binding.signupCARDJoin.isEnabled = !loading
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
