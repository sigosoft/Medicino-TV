package com.medicinoclinic.model

data class CounterListingModel(
    val counter_id: String,
    val token: String,
    val call_status: String,
    val counter_name: String,
    val booking_id: String,
    /** 1 = scroll counter name on TV, 2 = disable scroll and show full name */
    val scroll_up: String = "1"
)