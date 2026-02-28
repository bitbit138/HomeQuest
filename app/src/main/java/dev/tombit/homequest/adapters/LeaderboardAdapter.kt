package dev.tombit.homequest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.tombit.homequest.databinding.ItemLeaderboardBinding
import dev.tombit.homequest.model.User
import dev.tombit.homequest.utilities.ImageLoader

/**
 * RecyclerView adapter for the leaderboard on ProfileActivity.
 * Data is pre-sorted by XP descending before being passed in.
 * Pattern: Professor's L08 adapter structure with inner ViewHolder and ViewBinding.
 */
class LeaderboardAdapter(private var users: List<User>) :
    RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardViewHolder {
        val binding = ItemLeaderboardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LeaderboardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeaderboardViewHolder, position: Int) {
        with(holder) {
            val user = getItem(position)
            val rank = position + 1
            binding.leaderboardLBLRank.text = when (rank) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "#$rank"
            }
            binding.leaderboardLBLName.text = user.displayName
            binding.leaderboardLBLXp.text = "${user.currentXp} XP"
            binding.leaderboardLBLLevel.text = "Lvl ${user.level}"
            ImageLoader.getInstance().loadImage(user.avatarUrl, binding.leaderboardIMGAvatar)
        }
    }

    override fun getItemCount(): Int = users.size

    fun getItem(position: Int): User = users[position]

    fun updateData(newUsers: List<User>) {
        users = newUsers.sortedByDescending { it.currentXp }
        notifyDataSetChanged()
    }

    inner class LeaderboardViewHolder(val binding: ItemLeaderboardBinding) :
        RecyclerView.ViewHolder(binding.root)
}
