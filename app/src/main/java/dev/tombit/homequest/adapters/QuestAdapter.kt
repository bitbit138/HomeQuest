package dev.tombit.homequest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.tombit.homequest.R
import dev.tombit.homequest.databinding.ItemQuestBinding
import dev.tombit.homequest.interfaces.QuestCallback
import dev.tombit.homequest.model.Task
import dev.tombit.homequest.utilities.Constants
import dev.tombit.homequest.utilities.TimeFormatter

/**
 * RecyclerView adapter for the task/quest list.
 * Structure: inner ViewHolder with ViewBinding, callback wired in ViewHolder init block.
 * Pattern: Professor's L08 MovieAdapter structure exactly.
 *
 * RULE: callback is nullable and always safe-called (?.)
 * RULE: Callback assigned as anonymous object in the hosting Activity — never a lambda.
 */
class QuestAdapter(private var quests: List<Task>) :
    RecyclerView.Adapter<QuestAdapter.QuestViewHolder>() {

    var questCallback: QuestCallback? = null
    /** Current user UID — used to disable claim on own quests. Set by hosting Activity. */
    var currentUid: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val binding = ItemQuestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QuestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                val ctx = binding.root.context
                binding.questLBLTitle.text = title
                binding.questLBLDescription.text = description ?: ""
                binding.questLBLXp.text = ctx.getString(R.string.quest_xp_format, xpReward)
                binding.questLBLCoins.text = ctx.getString(R.string.quest_coins_format, coinReward)
                binding.questLBLDeadline.text = TimeFormatter.toDeadlineLabel(deadline)

                // Late/Tomorrow Banner (UX spec)
                val banner = binding.questLBLBanner
                val isOverdue = TimeFormatter.isOverdue(deadline)
                val isTomorrow = TimeFormatter.isDeadlineTomorrow(deadline)
                
                when {
                    isOverdue -> {
                        banner.visibility = android.view.View.VISIBLE
                        banner.text = "LATE"
                        banner.setBackgroundColor(ContextCompat.getColor(ctx, R.color.hq_error))
                        binding.questLBLTitle.setTextColor(ContextCompat.getColor(ctx, R.color.hq_error))
                        // Half coins if overdue
                        binding.questLBLCoins.text = ctx.getString(R.string.quest_coins_format, coinReward / 2)
                    }
                    isTomorrow -> {
                        banner.visibility = android.view.View.VISIBLE
                        banner.text = "TOMORROW 50%"
                        banner.setBackgroundColor(ContextCompat.getColor(ctx, R.color.hq_purple_primary))
                        binding.questLBLTitle.setTextColor(ContextCompat.getColor(ctx, R.color.hq_on_surface))
                    }
                    else -> {
                        banner.visibility = android.view.View.GONE
                        binding.questLBLTitle.setTextColor(ContextCompat.getColor(ctx, R.color.hq_on_surface))
                    }
                }

                // Button text and state based on status (UX spec)
                val btn = binding.questBTNClaim
                when (status) {
                    Constants.TaskStatus.OPEN -> {
                        btn.text = ctx.getString(R.string.btn_claim)
                        btn.isEnabled = true
                    }
                    Constants.TaskStatus.CLAIMED -> {
                        if (claimedBy == currentUid) {
                            btn.text = ctx.getString(R.string.btn_upload_proof_action)
                            btn.isEnabled = true
                        } else {
                            btn.text = ctx.getString(R.string.status_claimed)
                            btn.isEnabled = false
                        }
                    }
                    Constants.TaskStatus.PENDING_VERIFICATION -> {
                        btn.text = ctx.getString(R.string.status_pending_verification)
                        btn.isEnabled = false
                    }
                    Constants.TaskStatus.COMPLETED -> {
                        btn.text = ctx.getString(R.string.status_completed)
                        btn.isEnabled = false
                    }
                }

                // Sage border for open quests (UX spec: cozy farm house styling)
                val card = binding.root
                if (status == Constants.TaskStatus.OPEN) {
                    card.setStrokeColor(ContextCompat.getColor(ctx, R.color.hq_teal_secondary))
                    card.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.quest_card_stroke_open)
                } else {
                    card.setStrokeColor(ContextCompat.getColor(ctx, R.color.hq_outline))
                    card.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.quest_card_stroke_default)
                }
            }
        }
    }

    private fun statusStringRes(status: String): Int = when (status) {
        Constants.TaskStatus.OPEN -> R.string.status_open
        Constants.TaskStatus.CLAIMED -> R.string.status_claimed
        Constants.TaskStatus.PENDING_VERIFICATION -> R.string.status_pending_verification
        Constants.TaskStatus.COMPLETED -> R.string.status_completed
        else -> R.string.status_open
    }

    override fun getItemCount(): Int = quests.size

    fun getItem(position: Int): Task = quests[position]

    fun updateData(newQuests: List<Task>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = quests.size
            override fun getNewListSize(): Int = newQuests.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
                quests[oldPos].taskId == newQuests[newPos].taskId
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
                quests[oldPos] == newQuests[newPos]
        })
        quests = newQuests
        diffResult.dispatchUpdatesTo(this)
    }

    inner class QuestViewHolder(val binding: ItemQuestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.questBTNClaim.setOnClickListener {
                questCallback?.onQuestClaimed(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
            binding.root.setOnClickListener {
                questCallback?.onQuestTapped(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
        }
    }
}
