package com.catat.app.domain.model

data class Template(
    val id: Long = 0,
    val name: String,
    val content: String,
    val category: String,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
