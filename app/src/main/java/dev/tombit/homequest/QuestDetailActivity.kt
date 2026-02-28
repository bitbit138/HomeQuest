package dev.tombit.homequest

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import dev.tombit.homequest.R
import dev.tombit.homequest.databinding.ActivityQuestDetailBinding
import dev.tombit.homequest.model.Task
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.ImageCompressor
import dev.tombit.homequest.utilities.ImageLoader
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager
import dev.tombit.homequest.utilities.TimeFormatter

/**
 * Quest detail screen.
 * Handles the task state machine: claimed -> [photo upload] -> pending_verification.
 *
 * Photo upload contract (Section 6.2):
 *  - Client-side JPEG compression: max 1280px, 80% quality, retry 60% if > 200KB
 *  - Storage path: proofs/{householdId}/{taskId}.jpg
 *  - proofImageUrl must be non-null before status advances to pending_verification (Section 11.2)
 */
class QuestDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private lateinit var binding: ActivityQuestDetailBinding

    private lateinit var detail_LBL_title: MaterialTextView
    private lateinit var detail_LBL_description: MaterialTextView
    private lateinit var detail_LBL_rewards: MaterialTextView
    private lateinit var detail_LBL_deadline: MaterialTextView
    private lateinit var detail_LBL_status: MaterialTextView
    private lateinit var detail_BTN_uploadProof: MaterialButton
    private lateinit var detail_BTN_submitForReview: MaterialButton
    private lateinit var detail_BTN_approve: MaterialButton

    private var currentTask: Task? = null
    private var currentUser: User? = null
    private var selectedImageUri: Uri? = null

    // Photo picker
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            ImageLoader.getInstance().loadImage(it.toString(), binding.detailIMGProof)
            detail_BTN_submitForReview.isEnabled = true
        }
    }

    // Storage permission for image picker (Android 9–12: READ_EXTERNAL_STORAGE, 13+: READ_MEDIA_IMAGES)
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pickImage.launch("image/*")
        } else {
            SignalManager.getInstance().toast(getString(R.string.label_permission_denied_storage))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestDetailBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViews()
        initViews()
        loadData()
    }

    private fun findViews() {
        detail_LBL_title = binding.detailLBLTitle
        detail_LBL_description = binding.detailLBLDescription
        detail_LBL_rewards = binding.detailLBLRewards
        detail_LBL_deadline = binding.detailLBLDeadline
        detail_LBL_status = binding.detailLBLStatus
        detail_BTN_uploadProof = binding.detailBTNUploadProof
        detail_BTN_submitForReview = binding.detailBTNSubmitForReview
        detail_BTN_approve = binding.detailBTNApprove
    }

    private fun initViews() {
        detail_BTN_uploadProof.setOnClickListener {
            launchImagePicker()
        }
        detail_BTN_submitForReview.setOnClickListener {
            submitProofAndAdvanceStatus()
        }
        detail_BTN_approve.setOnClickListener {
            approveQuest()
        }
        binding.detailBTNBack.setOnClickListener { finish() }
    }

    private fun loadData() {
        currentUser = SharedPreferencesManager.getInstance()
            .getObject(Constants.SP_KEYS.CURRENT_USER_JSON, User::class.java)

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: run {
            SignalManager.getInstance().toast("Invalid quest.")
            finish()
            return
        }

        val householdId = currentUser?.householdId ?: return

        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.TASKS_SUB_COLLECTION)
            .document(taskId)
            .get()
            .addOnSuccessListener { doc ->
                val task = Task.fromMap(doc.data ?: return@addOnSuccessListener)
                currentTask = task
                renderTask(task)
            }
            .addOnFailureListener { e ->
                SignalManager.getInstance().toast("Failed to load quest: ${e.message}")
            }
    }

    private fun renderTask(task: Task) {
        val uid = FirebaseManager.getInstance().currentUid

        detail_LBL_title.text = task.title
        detail_LBL_description.text = task.description ?: "No description provided."
        detail_LBL_rewards.text = "+${task.xpReward} XP  •  +${task.coinReward} coins"
        detail_LBL_deadline.text = TimeFormatter.toDeadlineLabel(task.deadline)
        detail_LBL_status.text = task.status.replace("_", " ").replaceFirstChar { it.uppercase() }

        // Existing proof image
        task.proofImageUrl?.let {
            ImageLoader.getInstance().loadImage(it, binding.detailIMGProof)
        }

        // Show upload controls only if this user can submit proof
        // OR if the quest is OPEN (for the "Claim & Upload" flow)
        val canSubmitProof = uid != null && (task.isProofSubmittableBy(uid) || task.status == Constants.TaskStatus.OPEN)
        
        detail_BTN_uploadProof.visibility = if (canSubmitProof) View.VISIBLE else View.GONE
        detail_BTN_submitForReview.visibility = if (canSubmitProof) View.VISIBLE else View.GONE
        detail_BTN_submitForReview.isEnabled = false // enabled after image picked

        if (task.status == Constants.TaskStatus.OPEN) {
            detail_BTN_submitForReview.text = getString(R.string.btn_claim_and_upload)
        } else {
            detail_BTN_submitForReview.text = getString(R.string.btn_submit_review)
        }

        // Show Approve button when pending_verification and current user is not the claimer
        val canApprove = uid != null &&
            task.status == Constants.TaskStatus.PENDING_VERIFICATION &&
            task.claimedBy != uid
        detail_BTN_approve.visibility = if (canApprove) View.VISIBLE else View.GONE
    }

    private fun launchImagePicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED ->
                pickImage.launch("image/*")
            else ->
                requestPermission.launch(permission)
        }
    }

    /**
     * 1. Compresses selected image (Section 6.2 contract).
     * 2. Uploads to Cloud Storage: proofs/{householdId}/{taskId}.jpg
     * 3. On success, writes proofImageUrl to Firestore and advances status.
     *
     * proofImageUrl MUST be non-null before advancing to pending_verification (Section 11.2).
     */
    private fun submitProofAndAdvanceStatus() {
        val uri = selectedImageUri ?: run {
            SignalManager.getInstance().toast("Please select a photo first.")
            return
        }
        val task = currentTask ?: return
        val user = currentUser ?: return

        detail_BTN_submitForReview.isEnabled = false
        detail_BTN_submitForReview.text = "Uploading…"

        // Step 1: Compress image (Section 6.2)
        val compressedBytes = ImageCompressor.compress(this, uri)
        if (compressedBytes == null) {
            SignalManager.getInstance().toast("Could not process image. Try another photo.")
            detail_BTN_submitForReview.isEnabled = true
            detail_BTN_submitForReview.text = "Submit for Review"
            return
        }

        // Step 2: Upload to Cloud Storage
        val storagePath = "${Constants.Storage.PROOFS_PATH}/${user.householdId}/${task.taskId}${Constants.Storage.PROOF_FILE_EXTENSION}"
        val storageRef: StorageReference = FirebaseManager.getInstance().storage
            .reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        storageRef.putBytes(compressedBytes, metadata)
            .continueWithTask { uploadTask ->
                if (!uploadTask.isSuccessful) throw uploadTask.exception!!
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                // Step 3: Write proofImageUrl + advance status (Section 11.2 — must be non-null)
                advanceStatusWithProof(task, user.householdId, downloadUri.toString())
            }
            .addOnFailureListener { e ->
                detail_BTN_submitForReview.isEnabled = true
                detail_BTN_submitForReview.text = "Submit for Review"
                SignalManager.getInstance().toast("Upload failed: ${e.message}")
            }
    }

    private fun approveQuest() {
        val task = currentTask ?: return
        val user = currentUser ?: return

        detail_BTN_approve.isEnabled = false
        FirebaseManager.getInstance().firestore
            .collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(user.householdId)
            .collection(Constants.Firestore.TASKS_SUB_COLLECTION)
            .document(task.taskId)
            .update("status", Constants.TaskStatus.COMPLETED)
            .addOnSuccessListener {
                SignalManager.getInstance().toast("Quest approved! Rewards will be awarded. ✅")
                SignalManager.getInstance().vibrate()
                finish()
            }
            .addOnFailureListener { e ->
                detail_BTN_approve.isEnabled = true
                SignalManager.getInstance().toast("Approval failed: ${e.message}")
            }
    }

    private fun advanceStatusWithProof(task: Task, householdId: String, proofUrl: String) {
        val uid = FirebaseManager.getInstance().currentUid ?: return
        val user = currentUser ?: return
        
        val updates = mutableMapOf<String, Any>(
            "proofImageUrl" to proofUrl,
            "status" to Constants.TaskStatus.COMPLETED // Grant XP/coins immediately on upload (UX spec)
        )
        
        // If the quest was OPEN, we also need to set claimedBy
        if (task.status == Constants.TaskStatus.OPEN) {
            updates["claimedBy"] = uid
        }

        val db = FirebaseManager.getInstance().firestore
        val batch = db.batch()
        
        val taskRef = db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.TASKS_SUB_COLLECTION)
            .document(task.taskId)
            
        batch.update(taskRef, updates)
        
        // Add a feed entry for claiming/completing the quest
        val feedRef = db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(householdId)
            .collection(Constants.Firestore.FEED_SUB_COLLECTION)
            .document()
            
        val action = if (task.status == Constants.TaskStatus.OPEN) "claimed and completed" else "completed"
        val feedData = mapOf(
            "entryId" to feedRef.id,
            "type" to Constants.Feed.TYPE_TASK_COMPLETED,
            "actorId" to uid,
            "actorName" to (user.displayName ?: "A member"),
            "message" to "${user.displayName ?: "A member"} $action \"${task.title}\"! 🏆",
            "relatedEntityId" to task.taskId,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        batch.set(feedRef, feedData)
        
        batch.commit()
            .addOnSuccessListener {
                val msg = if (task.status == Constants.TaskStatus.OPEN) {
                    "Quest claimed and completed! Rewards awarded. ✅"
                } else {
                    "Quest completed! Rewards awarded. ✅"
                }
                SignalManager.getInstance().toast(msg)
                SignalManager.getInstance().vibrate()
                finish()
            }
            .addOnFailureListener { e ->
                detail_BTN_submitForReview.isEnabled = true
                detail_BTN_submitForReview.text = if (task.status == Constants.TaskStatus.OPEN) {
                    getString(R.string.btn_claim_and_upload)
                } else {
                    getString(R.string.btn_submit_review)
                }
                SignalManager.getInstance().toast("Failed to update status: ${e.message}")
            }
    }
}
