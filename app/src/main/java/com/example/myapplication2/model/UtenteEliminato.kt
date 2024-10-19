package com.example.myapplication2.model

data class UtenteEliminato(
    val id: String,
    val nome: String,
    val sintomi: MutableMap<String, MutableMap<String, MutableMap<String, MutableMap<String, MutableMap<String, Sintomo>>>>>
)