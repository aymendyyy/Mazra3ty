package com.mazra3ty.app.ui.worker

data class Job(
    val title: String,
    val price: String,
    val duration: String,
    val location: String,
    val category: String = "All"
)