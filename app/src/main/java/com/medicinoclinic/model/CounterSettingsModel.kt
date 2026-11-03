package com.medicinoclinic.model

data class CounterSettingsModel(
    val id: String,
    val counterName: String,
    var isSelected: Boolean
)
