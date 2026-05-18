package dev.tombit.homequest

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import dev.tombit.homequest.databinding.ActivityCreateQuestBinding
import dev.tombit.homequest.model.Task
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.FirebaseManager
import dev.tombit.homequest.utilities.SharedPreferencesManager
import dev.tombit.homequest.utilities.SignalManager
import java.util.Calendar
import java.util.Date

/**
 * Create Quest screen.
 * Validates XP/coin rewards against server-side bounds defined in Constants.Economy.
 * Writes to Firestore and creates a feed entry atomically.
 */
class CreateQuestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateQuestBinding

    private lateinit var create_LBL_title: MaterialTextView
    private lateinit var create_BTN_pickDeadline: MaterialButton
    private lateinit var create_BTN_create: MaterialButton
    private lateinit var create_BTN_cancel: MaterialButton
    private lateinit var create_LBL_deadlineValue: MaterialTextView

    private var selectedDeadline: Date? = null
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateQuestBinding.inflate(layoutInflater)
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
        create_LBL_title = binding.createLBLTitle
        create_BTN_pickDeadline = binding.createBTNPickDeadline
        create_BTN_create = binding.createBTNCreate
        create_BTN_cancel = binding.createBTNCancel
        create_LBL_deadlineValue = binding.createLBLDeadlineValue
    }

    private fun initViews() {
        create_BTN_pickDeadline.setOnClickListener { showDatePicker() }
        create_BTN_create.setOnClickListener { attemptCreateQuest() }
        create_BTN_cancel.setOnClickListener { finish() }
    }

    private fun loadUser() {
        currentUser = SharedPreferencesManager.getInstance()
            .getObject(Constants.SP_KEYS.CURRENT_USER_JSON, User::class.java)
    }

    private fun showDatePicker() {
        if (supportFragmentManager.findFragmentByTag(TAG_DEADLINE_PICKER) != null) return

        val todayStartMillis = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

        val constraints = CalendarConstraints.Builder()
            .setStart(todayStartMillis)
            .setValidator(DateValidatorPointForward.from(todayStartMillis))
            .build()

        val initialMillis = selectedDeadline?.time ?: todayStartMillis

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.btn_pick_deadline))
            .setCalendarConstraints(constraints)
            .setSelection(initialMillis)
            .build()

        picker.addOnPositiveButtonClickListener { utcMillis ->
            selectedDeadline = Date(utcMillis)
            val cal = Calendar.getInstance().apply { timeInMillis = utcMillis }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            create_LBL_deadlineValue.text = "$day/$month/$year"
        }

        picker.show(supportFragmentManager, TAG_DEADLINE_PICKER)
    }

    private fun attemptCreateQuest() {
        val user = currentUser ?: run {
            SignalManager.getInstance().toast("User not loaded. Please restart the app.")
            return
        }

        val title = binding.createETTitle.text.toString().trim()
        val description = binding.createETDescription.text.toString().trim()
        val xpText = binding.createETXp.text.toString()
        val coinText = binding.createETCoins.text.toString()

        // Validation
        if (title.isEmpty()) {
            SignalManager.getInstance().toast("Quest title is required.")
            return
        }
        if (title.length > Constants.Task.MAX_TITLE_LENGTH) {
            SignalManager.getInstance().toast("Title too long (max ${Constants.Task.MAX_TITLE_LENGTH} chars).")
            return
        }
        val xp = xpText.toIntOrNull() ?: run {
            SignalManager.getInstance().toast("Enter a valid XP reward.")
            return
        }
        val coins = coinText.toIntOrNull() ?: run {
            SignalManager.getInstance().toast("Enter a valid coin reward.")
            return
        }
        if (xp < Constants.Economy.MIN_XP_REWARD || xp > Constants.Economy.MAX_XP_REWARD) {
            SignalManager.getInstance().toast("XP must be ${Constants.Economy.MIN_XP_REWARD}–${Constants.Economy.MAX_XP_REWARD}.")
            return
        }
        if (coins < Constants.Economy.MIN_COIN_REWARD || coins > Constants.Economy.MAX_COIN_REWARD) {
            SignalManager.getInstance().toast("Coins must be ${Constants.Economy.MIN_COIN_REWARD}–${Constants.Economy.MAX_COIN_REWARD}.")
            return
        }

        create_BTN_create.isEnabled = false

        val db = FirebaseManager.getInstance().firestore
        val taskRef = db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(user.householdId)
            .collection(Constants.Firestore.TASKS_SUB_COLLECTION)
            .document()

        val task = Task.Builder()
            .taskId(taskRef.id)
            .title(title)
            .description(description.ifEmpty { null })
            .xpReward(xp)
            .coinReward(coins)
            .createdBy(user.uid)
            .deadline(selectedDeadline?.let { Timestamp(it) })
            .isRecurring(false) // Removed recurring option (UX spec)
            .build()

        // Feed entry ref
        val feedRef = db.collection(Constants.Firestore.HOUSEHOLDS_COLLECTION)
            .document(user.householdId)
            .collection(Constants.Firestore.FEED_SUB_COLLECTION)
            .document()

        val feedEntry = mapOf(
            "entryId" to feedRef.id,
            "type" to Constants.Feed.TYPE_TASK_CREATED,
            "actorId" to user.uid,
            "actorName" to user.displayName,
            "message" to "${user.displayName} created a new quest: \"$title\" 📋",
            "relatedEntityId" to taskRef.id,
            "timestamp" to FieldValue.serverTimestamp()
        )

        // Batch write task + feed entry atomically
        val batch = db.batch()
        batch.set(taskRef, task.toCreationMap())
        batch.set(feedRef, feedEntry)

        batch.commit()
            .addOnSuccessListener {
                SignalManager.getInstance().toast("Quest created!")
                SignalManager.getInstance().vibrate()
                finish()
            }
            .addOnFailureListener { e ->
                create_BTN_create.isEnabled = true
                SignalManager.getInstance().toast(e.message ?: "Failed to create quest")
            }
    }

    companion object {
        private const val TAG_DEADLINE_PICKER = "deadline_date_picker"
    }
}
