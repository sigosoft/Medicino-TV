package com.medicinoclinic.model

data class DoctorListingModel(
    val doctor_id: String,
    val name: String,
    val room: String,
    val token: String,
    val date: String,
    val token_call_status: String,
    /** 1 = scroll doctor name, 2 = disable scroll and show full name on 2 lines */
    val scroll_up: String = "1"
)
