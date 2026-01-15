package com.example.mobileappfun.data

import com.example.mobileappfun.entities.BaseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Generic repository class for managing entities.
 * Extend or modify this class to add persistence (Room, SharedPreferences, etc.)
 */
class Repository<T : BaseEntity> {

    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: Flow<List<T>> = _items.asStateFlow()

    fun add(item: T) {
        _items.value = _items.value + item
    }

    fun remove(item: T) {
        _items.value = _items.value.filter { it.id != item.id }
    }

    fun update(item: T) {
        _items.value = _items.value.map {
            if (it.id == item.id) item else it
        }
    }

    fun getById(id: String): T? {
        return _items.value.find { it.id == id }
    }

    fun clear() {
        _items.value = emptyList()
    }
}
