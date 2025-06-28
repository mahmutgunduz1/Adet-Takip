package com.osmanyildiz.scs.model

import java.util.Date

data class PeriodDate(
    // Başlangıç ve bitiş tarihleri
    val date: Date,
    val cycleLength: Int = 28,
    val hour: Int? = null,
    val minute: Int? = null
) 