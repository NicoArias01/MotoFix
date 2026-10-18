package com.motofix.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Perfil y preferencias de la cuenta abierta (RF13). */
class UserRepository(private val store: AppStore) {

    val user: Flow<UserProfile?> = store.data.map { it.user }.distinctUntilChanged()

    val settings: Flow<AppSettings> = store.data.map { it.settings }.distinctUntilChanged()

    fun updateName(name: String) {
        store.update { data -> data.copy(user = data.user?.copy(name = name.trim())) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        store.update { data -> data.copy(settings = transform(data.settings)) }
    }
}
