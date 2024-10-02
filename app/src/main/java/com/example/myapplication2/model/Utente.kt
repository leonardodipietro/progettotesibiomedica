package com.example.myapplication2.model

data class Utente (
    val id: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val admin: Boolean=false,
    val username: String = "",
    val password: String="",
)
