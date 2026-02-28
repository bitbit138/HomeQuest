package dev.tombit.homequest.interfaces

import dev.tombit.homequest.model.Coupon

interface CouponCallback {
    fun onCouponPurchased(coupon: Coupon, position: Int)
    fun onCouponRedeemed(coupon: Coupon, position: Int)
    fun onCouponTapped(coupon: Coupon, position: Int)
}
