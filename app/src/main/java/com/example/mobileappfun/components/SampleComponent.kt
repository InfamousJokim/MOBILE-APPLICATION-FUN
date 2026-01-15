package com.example.mobileappfun.components

import java.util.UUID

/**
 * Example component class. Use this as a template for creating your own components.
 */
class SampleComponent(
    override val componentName: String = "SampleComponent"
) : BaseComponent {

    override val componentId: String = UUID.randomUUID().toString()

    private var isInitialized = false

    override fun initialize() {
        if (!isInitialized) {
            // Add initialization logic here
            isInitialized = true
        }
    }

    override fun destroy() {
        if (isInitialized) {
            // Add cleanup logic here
            isInitialized = false
        }
    }

    fun performAction(): String {
        return "Action performed by $componentName"
    }
}
