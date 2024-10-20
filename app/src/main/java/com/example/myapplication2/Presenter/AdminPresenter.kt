package com.example.myapplication2.Presenter

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.myapplication2.interfacepackage.AdminView
import com.example.myapplication2.interfacepackage.PasswordType
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.ExportRepo
import com.example.myapplication2.repository.SintomoRepo
import com.example.myapplication2.repository.UserRepo
import com.google.gson.Gson
class AdminPresenter(
    private val view: AdminView,
    private val userRepo:UserRepo,
    private val sintomoRepo: SintomoRepo,
    private val exportRepo: ExportRepo,
    private val context: Context
) {

    fun addSintomo(sintomo: String) {
        if (sintomo.isNotEmpty()) {
            sintomoRepo.aggiungiSintomo(sintomo) { success ->
                if (success) view.showAddSintomoSuccess()
                else view.showAddSintomoError()
            }
        }
    }

    fun fetchSintomiUltimaSettimana() {
        exportRepo.fetchLastWeekReports { sintomiList, _ ->
            if (sintomiList.isNotEmpty()) {
                view.showSintomiListUser(sintomiList)
            } else {
                view.showUpdateError("Nessuna segnalazione trovata nell'ultima settimana")
            }
        }
    }

    fun loadSintomi() {
        val nomiSintomi = mutableListOf<String>()
        val idSintomi = mutableListOf<String>()
        sintomoRepo.caricaSintomi(nomiSintomi, idSintomi) {
            view.showSintomiList(nomiSintomi, idSintomi)
        }
    }
    fun loadUserData(userId: String) {
        userRepo.getUserData(userId) { user ->
            if (user != null) {
                Log.d("dati trovati","dati trovati")
            } else {
                view.showError("Dati utente non trovati")
            }
        }
    }
    fun removeSintomo(sintomoId: String) {
        sintomoRepo.rimuoviSintomo(sintomoId) { success ->
            if (success) {
                view.showRemoveSintomoSuccess()
                loadSintomi()
            } else {
                view.showRemoveSintomoError()
            }
        }
    }

    fun logout() {
        view.showLogoutConfirmation()
    }

    fun confirmLogout() {
        view.returnToMain()
    }

    fun exportToExcel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view.hasWritePermission()) {
                exportRepo.fetchDataAndGenerateExcel(context)
            } else {
                view.requestWritePermission()
            }
        } else {
            exportRepo.fetchDataAndGenerateExcel(context)
        }
    }

    fun onRequestPermissionsResult(granted: Boolean, context: Context) {
        if (granted) {
            exportRepo.fetchDataAndGenerateExcel(context)
        } else {
            view.showPermissionDeniedError()
        }
    }
}
