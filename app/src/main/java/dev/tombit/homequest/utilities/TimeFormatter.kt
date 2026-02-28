package dev.tombit.homequest.utilities

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility object for all date/time formatting in HomeQuest.
 * Adapted from professor's L04/L08 time helpers.
 * Use these consistently; never format timestamps inline in an Activity or Adapter.
 */
object TimeFormatter {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    /**
     * Returns a human-readable relative time string (e.g. "2 hours ago", "just now").
     */
    fun toRelativeTime(timestamp: Timestamp): String {
        val now = System.currentTimeMillis()
        val then = timestamp.toDate().time
        val diffMs = now - then

        return when {
            diffMs < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diffMs < TimeUnit.HOURS.toMillis(1) -> {
                val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
                "$mins min${if (mins == 1L) "" else "s"} ago"
            }
            diffMs < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
                "$hours hour${if (hours == 1L) "" else "s"} ago"
            }
            diffMs < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                "$days day${if (days == 1L) "" else "s"} ago"
            }
            else -> dateFormat.format(timestamp.toDate())
        }
    }

    /**
     * Returns a deadline display string: "Today", "Tomorrow", "In X days", or a date.
     * Returns "No deadline" if timestamp is null.
     */
    fun toDeadlineLabel(timestamp: Timestamp?): String {
        if (timestamp == null) return "No deadline"
        val now = System.currentTimeMillis()
        val then = timestamp.toDate().time
        val diffMs = then - now
        
        val oneDayMs = TimeUnit.DAYS.toMillis(1)

        return when {
            diffMs < 0 -> "Overdue"
            diffMs < oneDayMs -> "Today"
            diffMs < oneDayMs * 2 -> "Tomorrow"
            diffMs < oneDayMs * 7 -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                "In $days days"
            }
            else -> dateFormat.format(timestamp.toDate())
        }
    }

    /**
     * Returns true if the deadline is tomorrow (within 24-48 hours from now).
     */
    fun isDeadlineTomorrow(timestamp: Timestamp?): Boolean {
        if (timestamp == null) return false
        val now = System.currentTimeMillis()
        val then = timestamp.toDate().time
        val diffMs = then - now
        val oneDayMs = TimeUnit.DAYS.toMillis(1)
        return diffMs in oneDayMs until (oneDayMs * 2)
    }

    /**
     * Returns true if the deadline has passed.
     */
    fun isOverdue(timestamp: Timestamp?): Boolean {
        if (timestamp == null) return false
        return timestamp.toDate().time < System.currentTimeMillis()
    }

    fun toDateString(date: Date): String = dateFormat.format(date)

    fun toDateTimeString(timestamp: Timestamp): String = dateTimeFormat.format(timestamp.toDate())
}
