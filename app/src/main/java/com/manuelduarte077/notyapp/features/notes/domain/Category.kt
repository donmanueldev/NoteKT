package com.manuelduarte077.notyapp.features.notes.domain

enum class Category(val storageValue: Int) {
    // These values are stored in Room; keep them stable when changing enum order.
    WORK(storageValue = 0),
    PERSONAL(storageValue = 1),
    SHOPPING(storageValue = 2),
    OTHER(storageValue = 3);

    companion object {
        fun fromStorageValue(value: Int): Category? =
            entries.firstOrNull { it.storageValue == value }
    }
}
