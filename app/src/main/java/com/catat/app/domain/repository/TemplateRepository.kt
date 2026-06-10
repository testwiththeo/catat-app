package com.catat.app.domain.repository

import com.catat.app.domain.model.Template
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<Template>>
    fun getTemplatesByCategory(category: String): Flow<List<Template>>
    suspend fun getTemplateById(id: Long): Template?
    suspend fun saveTemplate(template: Template): Long
    suspend fun deleteTemplate(id: Long)
    suspend fun incrementUsage(id: Long)
}
