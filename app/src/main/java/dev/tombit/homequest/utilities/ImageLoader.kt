package dev.tombit.homequest.utilities

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dev.tombit.homequest.R
import java.lang.ref.WeakReference

/**
 * Small Glide facade for loading into ImageViews (thread-safe singleton).
 * Keeps image loading consistent and avoids scattering Glide calls across UI code.
 */
class ImageLoader private constructor(context: Context) {

    private val contextRef = WeakReference(context.applicationContext)

    private val defaultOptions = RequestOptions()
        .placeholder(R.drawable.ic_placeholder)
        .error(R.drawable.ic_placeholder)
        .diskCacheStrategy(DiskCacheStrategy.ALL)

    /**
     * Load an image from a URL string into an ImageView.
     */
    fun loadImage(url: String?, imageView: ImageView) {
        contextRef.get()?.let { ctx ->
            Glide.with(ctx)
                .load(url)
                .apply(defaultOptions)
                .into(imageView)
        }
    }

    /**
     * Load an image from a drawable resource ID into an ImageView.
     */
    fun loadImage(resourceId: Int, imageView: ImageView) {
        contextRef.get()?.let { ctx ->
            Glide.with(ctx)
                .load(resourceId)
                .apply(defaultOptions)
                .into(imageView)
        }
    }

    /**
     * Clear the image binding from an ImageView (stops loading, releases resources).
     */
    fun clear(imageView: ImageView) {
        contextRef.get()?.let { ctx ->
            Glide.with(ctx).clear(imageView)
        }
    }

    companion object {
        @Volatile
        private var instance: ImageLoader? = null

        fun init(context: Context): ImageLoader {
            return instance ?: synchronized(this) {
                instance ?: ImageLoader(context).also { instance = it }
            }
        }

        fun getInstance(): ImageLoader {
            return instance ?: throw IllegalStateException(
                "ImageLoader must be initialized by calling init(context) before use."
            )
        }
    }
}
