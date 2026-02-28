package dev.tombit.homequest.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.tombit.homequest.databinding.ItemCouponBinding
import dev.tombit.homequest.interfaces.CouponCallback
import dev.tombit.homequest.model.Coupon

/**
 * RecyclerView adapter for the coupon/rewards marketplace.
 * Pattern: Professor's L08 adapter structure with inner ViewHolder and ViewBinding.
 */
class CouponAdapter(private var coupons: List<Coupon>) :
    RecyclerView.Adapter<CouponAdapter.CouponViewHolder>() {

    var couponCallback: CouponCallback? = null
    var currentUid: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val binding = ItemCouponBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CouponViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        with(holder) {
            with(getItem(position)) {
                binding.couponLBLTitle.text = title
                binding.couponLBLCost.text = "$cost coins"
                
                // Button visibility and text
                when {
                    isRedeemed -> {
                        binding.couponBTNPurchase.visibility = android.view.View.GONE
                        binding.couponBTNRedeem.visibility = android.view.View.GONE
                        binding.couponLBLStatus.text = "Redeemed"
                        // Cross out title for redeemed
                        binding.couponLBLTitle.paintFlags = binding.couponLBLTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    }
                    isPurchased -> {
                        binding.couponBTNPurchase.visibility = android.view.View.GONE
                        binding.couponBTNRedeem.visibility = android.view.View.VISIBLE
                        binding.couponBTNRedeem.text = "Use"
                        binding.couponLBLStatus.text = "Owned"
                        binding.couponLBLTitle.paintFlags = binding.couponLBLTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }
                    else -> {
                        binding.couponBTNPurchase.visibility = android.view.View.VISIBLE
                        binding.couponBTNRedeem.visibility = android.view.View.GONE
                        val isOwnCoupon = sellerId == currentUid
                        binding.couponBTNPurchase.isEnabled = !isOwnCoupon
                        binding.couponLBLStatus.text = if (isOwnCoupon) "Your reward" else "Available"
                        binding.couponLBLTitle.paintFlags = binding.couponLBLTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = coupons.size

    fun getItem(position: Int): Coupon = coupons[position]

    fun updateData(newCoupons: List<Coupon>) {
        coupons = newCoupons
        notifyDataSetChanged()
    }

    inner class CouponViewHolder(val binding: ItemCouponBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.couponBTNPurchase.setOnClickListener {
                couponCallback?.onCouponPurchased(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
            binding.couponBTNRedeem.setOnClickListener {
                couponCallback?.onCouponRedeemed(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
            binding.root.setOnClickListener {
                couponCallback?.onCouponTapped(
                    getItem(absoluteAdapterPosition),
                    absoluteAdapterPosition
                )
            }
        }
    }
}
