package com.catat.app.data.repository

import com.catat.app.data.db.dao.TemplateDao
import com.catat.app.data.db.entity.TemplateEntity
import com.catat.app.domain.model.Template
import com.catat.app.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val dao: TemplateDao
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<Template>> {
        return dao.getAll().map { list -> list.map { it.toDomain() } }
    }

    override fun getTemplatesByCategory(category: String): Flow<List<Template>> {
        return dao.getByCategory(category).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTemplateById(id: Long): Template? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun saveTemplate(template: Template): Long {
        return dao.insert(template.toEntity())
    }

    override suspend fun deleteTemplate(id: Long) {
        dao.getById(id)?.let { dao.delete(it) }
    }

    override suspend fun incrementUsage(id: Long) {
        dao.incrementUsage(id)
    }

    private fun TemplateEntity.toDomain() = Template(
        id = id,
        name = name,
        content = content,
        category = category,
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Template.toEntity() = TemplateEntity(
        id = id,
        name = name,
        content = content,
        category = category,
        usageCount = usageCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
