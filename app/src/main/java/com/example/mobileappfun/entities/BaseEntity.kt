package com.example.mobileappfun.entities

/**
 * Base class for all entities in the application.
 * Extend this class to create your own entities.
 */
abstract class BaseEntity {
    abstract val id: String
    abstract val name: String
    abstract val description: String

    open fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "description" to description
        )
    }
}
