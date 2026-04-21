package com.mazra3ty.app.ui.worker

data class Job(
    val id: String = "",
    val title: String,
    val price: String,
    val duration: String,
    val location: String,
    val category: String = "All",
    val description: String = "",
    val farmerName: String = "",
    val farmerEmail: String = "",
    val requirements: String = "",
    val postedDate: String = ""
)