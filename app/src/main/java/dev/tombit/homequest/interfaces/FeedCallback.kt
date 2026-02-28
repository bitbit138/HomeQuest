package dev.tombit.homequest.interfaces

import dev.tombit.homequest.model.FeedItem

interface FeedCallback {
    fun onFeedItemTapped(item: FeedItem, position: Int)
}
