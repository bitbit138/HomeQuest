package dev.tombit.homequest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.tombit.homequest.databinding.ItemFeedBinding
import dev.tombit.homequest.interfaces.FeedCallback
import dev.tombit.homequest.model.FeedItem
import dev.tombit.homequest.utilities.TimeFormatter

/**
 * RecyclerView adapter for the activity feed (dashboard + feed screen).
 * Pattern: Professor's L08 adapter structure with inner ViewHolder and ViewBinding.
 */
class FeedAdapter(private var feedItems: List<FeedItem>) :
    RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    var feedCallback: FeedCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemFeedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                binding.feedLBLMessage.text = "${typeIcon()} $message"
                binding.feedLBLTimestamp.text = timestamp?.let { TimeFormatter.toRelativeTime(it) } ?: ""
                binding.feedLBLActorName.text = actorName
            }
        }
    }

    override fun getItemCount(): Int = feedItems.size

    fun getItem(position: Int): FeedItem = feedItems[position]

    fun updateData(newItems: List<FeedItem>) {
        feedItems = newItems
        notifyDataSetChanged()
    }

    fun prependItems(newItems: List<FeedItem>) {
        val merged = newItems + feedItems
        feedItems = merged
        notifyDataSetChanged()
    }

    inner class FeedViewHolder(val binding: ItemFeedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                feedCallback?.onFeedItemTapped(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
        }
    }
}
