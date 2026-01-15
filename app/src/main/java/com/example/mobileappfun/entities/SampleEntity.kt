package com.example.mobileappfun.entities

import java.util.UUID

/**
 * Example entity class. Use this as a template for creating your own entities.
 */
data class SampleEntity(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String,
    override val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val customField: String = ""
) : BaseEntity() {

    override fun toMap(): Map<String, Any> {
        return super.toMap() + mapOf(
            "createdAt" to createdAt,
            "customField" to customField
        )
    }

    companion object {
        fun createSample(): SampleEntity {
            return SampleEntity(
                name = "Sample Entity",
                description = "This is a sample entity"
            )
        }
    }
}
