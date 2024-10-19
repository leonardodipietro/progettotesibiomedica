package com.example.myapplication2.utility

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView

class UserExperience {




    // Metodo per gestire la visibilità della password
    fun togglePasswordVisibility(editText: EditText, icon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        // Mantenere il cursore alla fine del testo
        editText.setSelection(editText.text.length)
    }

    // Metodo per gestire il TextWatcher e la formattazione del numero di telefono
    fun formatPhoneNumber(editText: EditText, countryPrefix: String = "+39") {
        var isUpdatingPhone = false

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdatingPhone) return

                var currentText = s.toString()

                // Se l'utente cancella tutto, non ripristinare automaticamente il prefisso
                if (currentText.isEmpty()) {
                    return
                }

                // Se l'utente rimuove manualmente il prefisso, non aggiungerlo di nuovo
                if (currentText.length < countryPrefix.length) {
                    return
                }

                // Aggiunge automaticamente il prefisso solo se non è presente
                if (!currentText.startsWith("+") && currentText.length >= 3) {
                    currentText = "$countryPrefix $currentText"
                }

                // Rimuovi eventuali spazi e formatta di nuovo
                val numberPart = currentText.substring(countryPrefix.length).replace(" ", "")
                val formattedText = "$countryPrefix " + numberPart.chunked(3).joinToString(" ")

                // Evita aggiornamenti infiniti
                isUpdatingPhone = true
                editText.setText(formattedText)
                editText.setSelection(formattedText.length)
                isUpdatingPhone = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    fun normalizeInputs(vararg editTexts: EditText) {
        editTexts.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    val normalizedText = s.toString().trim().lowercase()
                    if (normalizedText != s.toString()) {
                        // Evita loop infiniti di aggiornamento
                        editText.setText(normalizedText)
                        editText.setSelection(normalizedText.length)
                    }
                }
            })
        }
    }

    fun validateEmailInput(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    editText.error = "Email non valida"
                }
            }
        })
    }



}