package com.example.myapplication2.Presenter

import com.example.myapplication2.interfacepackage.SuperAdminView
import com.example.myapplication2.model.Utente
import com.example.myapplication2.repository.FaqRepo
import com.example.myapplication2.repository.UserRepo
import com.google.firebase.auth.FirebaseAuth
import org.mindrot.jbcrypt.BCrypt

class SuperAdminPresenter (private val view: SuperAdminView){

    private val auth = FirebaseAuth.getInstance()
    private val userRepo = UserRepo()
    private val faqRepo= FaqRepo()


    fun registerAdmin(email: String, username: String, name: String, address: String, password: String, confermaPassword: String) {
        if (email.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && confermaPassword.isNotEmpty()
           ) {

            if (password == confermaPassword) {
                userRepo.checkUsernameExists(username) { exists ->
                    if (exists) {
                        view.showError("Username già in uso, selezionare un altro")
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid ?: ""
                                    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12))
                                    val user = Utente(
                                        id = userId,
                                        email = email,
                                        name = name,
                                        address = address,
                                        username = username,
                                        password = hashedPassword,
                                        ruolo = "admin",
                                        phoneNumber=""
                                    )
                                    view.clearInputFields()
                                    userRepo.saveUserToFirebase(username, name, address, hashedPassword,"admin")
                                    view.showSuccess("Admin registrato con successo")
                                } else {
                                    view.showError("Registrazione fallita:")
                                }
                            }
                    }
                }
            } else {
                view.showError("Le password non corrispondono")
            }
        } else {
            view.showError("Tutti i campi devono essere compilati")
        }
    }


    fun logout() {
        view.showLogoutConfirmation()
    }

    fun addFaq(question: String, answer: String) {
        if (question.isNotEmpty() && answer.isNotEmpty()) {
            faqRepo.addFaq(question, answer, {
                view.showSuccess("FAQ aggiunta con successo")
            }, { error ->
                view.showError("Errore durante l'aggiunta della FAQ:")
            })
        } else {
            view.showError("Domanda e risposta non possono essere vuoti")
        }
    }

    fun updateFaq(faqId: String, question: String, answer: String) {
        faqRepo.updateFaq(faqId, question, answer, {
            view.showSuccess("FAQ aggiornata con successo")
        }, { error ->
            view.showError("Errore update")
        })
    }

    fun loadFaqData() {
        val context = view.getContext() // Assicurati che la tua view possa restituire il Context
        faqRepo.fetchFaqList(context) { faqList ->
            view.showFaqList(faqList)
        }
    }

    fun deleteFaq(faqId: String) {
        faqRepo.deleteFaq(faqId, {
            view.showSuccess("FAQ eliminata con successo")
        }, { error ->
            view.showError("Errore durante l'eliminazione ")
        })
    }
    fun confirmLogout() {
        view.returnToMain()
    }


}