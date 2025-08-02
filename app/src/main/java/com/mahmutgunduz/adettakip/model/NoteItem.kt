package com.mahmutgunduz.adettakip.model

import java.util.Date

data class NoteItem(
    val title: String,
    val content: String,
    val timestamp: Date
)