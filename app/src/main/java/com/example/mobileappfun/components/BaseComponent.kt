package com.example.mobileappfun.components

/**
 * Base interface for all components in the application.
 * Implement this interface to create reusable components.
 */
interface BaseComponent {
    val componentId: String
    val componentName: String

    fun initialize()
    fun destroy()
}
