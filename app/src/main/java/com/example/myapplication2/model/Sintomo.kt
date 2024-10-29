package com.example.myapplication2.model

data class Sintomo (
    var id: String = "",
    var nomeSintomo: String = "",
    var gravità: Int = 0, //usiamo int poi cambiamo
    var tempoTrascorsoUltimoPasto: Int =0,
    var dataSegnalazione: String = "",
    var oraSegnalazione: String = "",
    var isPersonalizzato: Boolean = false
)