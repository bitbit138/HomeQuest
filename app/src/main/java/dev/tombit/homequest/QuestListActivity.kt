package dev.tombit.homequest

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ListenerRegistration
import dev.tombit.homequest.R
import dev.tombit.homequest.adapters.QuestAdapter
import dev.tombit.homequest.databinding.ActivityQuestListBinding
import dev.tombit.homequest.interfaces.QuestCallback
import dev.tombit.homequest.model.Task
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager
import kotlinx.coroutines.launch

/**
 * Quest list screen.
 * Shows OPEN and CLAIMED quests so users can find their claimed quests and submit proof.
 * Uses two separate listeners (one per status) to avoid a composite index on whereIn+orderBy.
 * Results are merged and sorted client-side by deadline.
 */
class QuestListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuestListBinding
    private lateinit var questAdapter: QuestAdapter
    private var openListener: ListenerRegistration? = null
    private var claimedListener: ListenerRegistration? = null
    private var pendingListener: ListenerRegistration? = null
    private var currentUser: User? = null

    // Merge buffer — updated by whichever listener fires first
    private val openTasks = mutableListOf<Task>()
    private val claimedTasks = mutableListOf<Task>()
    private val pendingTasks = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestListBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        loadUser()
    }

    override fun onResume() {
        super.onResume()
        currentUser?.let { startTaskListeners(it.householdId) }
    }

    override fun onPause() {
        super.onPause()
        openListener?.remove(); openListener = null
        claimedListener?.remove(); claimedListener = null
        pendingListener?.remove(); pendingListener = null
    }

    private fun initViews() {
        questAdapter = QuestAdapter(emptyList())
        questAdapter.questCallback = object : QuestCallback {
            override fun onQuestClaimed(task: Task, position: Int) { openQuestDetail(task) }
            override fun onQuestCompleted(task: Task, position: Int) { openQuestDetail(task) }
            override fun onQuestTapped(task: Task, position: Int) { openQuestDetail(task) }
        }
        binding.questListRVQuests.adapter = questAdapter
        binding.questListBTNBack.setOnClickListener { finish() }
        binding.questListFABNewQuest.setOnClickListener {
            startActivity(Intent(this, CreateQuestActivity::class.java))
        }
    }

    private fun loadUser() {
        currentUser = SharedPreferencesManager.getInstance()
            .getObject(Constants.SP_KEYS.CURRENT_USER_JSON, User::class.java)
        currentUser?.let {
            questAdapter.currentUid = it.uid
            startTaskListeners(it.householdId)
        }
    }

    /**
     * Two separate real-time listeners — one for OPEN, one for CLAIMED.
     * Each single-field query works without a composite index.
     * Results are merged and sorted by deadline client-side.
     */
    private fun startTaskListeners(householdId: String) {
        openListener?.remove()
        claimedListener?.remove()
        pendingListener?.remove()

        val tasksRef = FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.TASKS_SUB_COLLECTION)

        openListener = tasksRef
            .whereEqualTo("status", Constants.TaskStatus.OPEN)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    SignalManager.getInstance().toast("Error loading quests")
                    return@addSnapshotListener
                }
                openTasks.clear()
                openTasks.addAll(snapshot?.documents?.mapNotNull { it.data?.let { d -> Task.fromMap(d) } } ?: emptyList())
                publishMergedList()
            }

        claimedListener = tasksRef
            .whereEqualTo("status", Constants.TaskStatus.CLAIMED)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                claimedTasks.clear()
                claimedTasks.addAll(snapshot?.documents?.mapNotNull { it.data?.let { d -> Task.fromMap(d) } } ?: emptyList())
                publishMergedList()
            }

        pendingListener = tasksRef
            .whereEqualTo("status", Constants.TaskStatus.PENDING_VERIFICATION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                pendingTasks.clear()
                pendingTasks.addAll(snapshot?.documents?.mapNotNull { it.data?.let { d -> Task.fromMap(d) } } ?: emptyList())
                publishMergedList()
            }
    }

    private fun publishMergedList() {
        val merged = (openTasks + claimedTasks + pendingTasks)
            .sortedWith(compareBy(nullsLast()) { it.deadline })
        lifecycleScope.launch {
            questAdapter.updateData(merged)
            binding.questListLBLEmpty.visibility =
                if (merged.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun claimTask(task: Task) {
        val uid = FirebaseManager.getInstance().currentUid ?: return
        val user = currentUser ?: return

        if (!task.isClaimableBy(uid)) {
            val msg = if (task.createdBy == uid) {
                getString(R.string.label_cannot_claim_own_quest)
            } else {
                getString(R.string.label_quest_cannot_claim)
            }
            SignalManager.getInstance().toast(msg)
            return
        }

        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(user.householdId)
            .collection(Constants.Firestore.TASKS_SUB_COLLECTION)
            .document(task.taskId)
            .update(mapOf(
                "status" to Constants.TaskStatus.CLAIMED,
                "claimedBy" to uid
            ))
            .addOnSuccessListener {
                SignalManager.getInstance().toast("Quest claimed! Tap it to submit proof 📸")
                SignalManager.getInstance().vibrate()
            }
            .addOnFailureListener { e ->
                SignalManager.getInstance().toast(e.message ?: "Could not claim quest")
            }
    }

    private fun openQuestDetail(task: Task) {
        val uid = FirebaseManager.getInstance().currentUid
        if (uid != null && task.status == Constants.TaskStatus.OPEN && task.createdBy == uid) {
            SignalManager.getInstance().toast(getString(R.string.label_cannot_claim_own_quest))
            return
        }
        startActivity(Intent(this, QuestDetailActivity::class.java).apply {
            putExtra(QuestDetailActivity.EXTRA_TASK_ID, task.taskId)
        })
    }
}
