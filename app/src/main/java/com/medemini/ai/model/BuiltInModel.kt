package com.medemini.ai.model

data class BuiltInModel(
    val name: String,
    val endpoint: String,
    val apiKey: String,
    val dailyLimit: Int,
    val enabled: Boolean,
    val description: String
)
