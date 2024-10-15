package com.example.myapplication2.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportRepo {

    fun generateExcel(
        context: Context,
        sintomiData: List<Pair<Sintomo, String>>,
        sintomiNomiGlobali: List<Sintomo>,  // The global list of symptom names
        fileName: String
    ): Boolean {
        val path = File(context.getExternalFilesDir(null)?.absolutePath + "/Reports")

        if (!path.exists()) {
            path.mkdirs() // Create the directory if it does not exist
        }

        val file = File(path, fileName)

        return try {
            // Create a new workbook and sheet for Excel
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Sintomi Report")

            // Create the header row in the Excel sheet
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Data")
            headerRow.createCell(1).setCellValue("Nome Utente") // You may add the user name if needed
            headerRow.createCell(2).setCellValue("Ora Rilevazione")
            headerRow.createCell(3).setCellValue("Ora Ultimo Pasto")
            headerRow.createCell(4).setCellValue("Gravità")

            // Add the global symptom names as headers in Excel
            sintomiNomiGlobali.map { it.nomeSintomo }.distinct().forEachIndexed { index, nomeSintomo ->
                headerRow.createCell(5 + index).setCellValue(nomeSintomo)  // Start from column 4 for symptoms
            }

            // Keep track of the row number to avoid overwriting
            var rowCount = 1

            // Add symptom data for each report
            for ((sintomo, username) in sintomiData) {
                val row = sheet.createRow(rowCount++)

                // Insert the date and time of the report
                row.createCell(0).setCellValue(sintomo.dataSegnalazione)  // Symptom date
                row.createCell(1).setCellValue(username)  // Placeholder for user name (replace if you have user data)
                row.createCell(2).setCellValue(sintomo.oraSegnalazione)    // Reporting time
                row.createCell(3).setCellValue(sintomo.tempoTrascorsoUltimoPasto.toDouble())  // Time since last meal
                row.createCell(4).setCellValue(sintomo.gravità.toDouble())

                val sintomoIndex = sintomiNomiGlobali.indexOfFirst { it.id == sintomo.id }

                Log.d("ExcelDebug", "Sintomo segnalato ID: ${sintomo.id}")
                Log.d("ExcelDebug", "Sintomi globali: ${sintomiNomiGlobali.map { it.id }}")

                if (sintomoIndex != -1) {
                    Log.d("ExcelDebug", "Sintomo trovato in colonna index: $sintomoIndex con ID corrispondente: ${sintomiNomiGlobali[sintomoIndex].id}")
                    // Inserisci una "X" nella colonna corretta che rappresenta il sintomo segnalato
                    row.createCell(5 + sintomoIndex).setCellValue("X")
                } else {
                    Log.d("ExcelDebug", "Nessun sintomo trovato corrispondente a ID: ${sintomo.id}")
                }
            }
            // Write the Excel file to disk
            FileOutputStream(file).use { fileOut ->
                workbook.write(fileOut)
                workbook.close()
            }

            Log.d("Excel", "File saved at: ${file.absolutePath}")
            true // Success
        } catch (e: Exception) {
            Log.e("Excel", "Error generating Excel file", e)
            false // Error
        }
    }
    fun fetchDataAndGenerateExcel(context: Context) {
        val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")

        // Recupero del nodo "users" con i relativi sintomi
        database.reference.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val sintomiList = mutableListOf<Pair<Sintomo, String>>()  // Sintomi con nome utente associato

  /*              for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(Utente::class.java)
                    val username = user?.username ?: "Sconosciuto"  // Recupero del nome utente

                    if (user != null) {
                        // Recupera i sintomi per ogni utente
                        val sintomiSnapshot = userSnapshot.child("sintomi")

                        for (sintomoSnapshot in sintomiSnapshot.children) {
                            val sintomoId = sintomoSnapshot.key ?: ""
                            for (dataSnapshot in sintomoSnapshot.children) {
                                val data = dataSnapshot.key ?: ""

                                // Ciclo attraverso tutte le ore per quella data
                                for (oraSnapshot in dataSnapshot.children) {
                                    val ora = oraSnapshot.key ?: ""
                                    var sintomoData = oraSnapshot.getValue(Sintomo::class.java)

                                    if (sintomoData != null) {
                                        Log.d("SintomoData", "Utente: $username, SintomoID: $sintomoId, Data: $data, Ora: $ora, Gravità: ${sintomoData.gravità}, Ultimo Pasto ${sintomoData.tempoTrascorsoUltimoPasto}")
                                        sintomoData.id = sintomoId
                                        // Aggiungi il sintomo e il nome utente alla lista
                                        sintomiList.add(Pair(sintomoData, username))
                                    }
                                }
                            }
                        }
                    }
                }*/
                for (userSnapshot in dataSnapshot.children) {
                    val username = userSnapshot.child("name").getValue(String::class.java) ?: "Sconosciuto"

                    // Navigazione tra i nodi di sintomi -> anno -> settimana -> data -> ora
                    val sintomiSnapshot = userSnapshot.child("sintomi")
                    for (sintomoIdSnapshot in sintomiSnapshot.children) {
                        val sintomoId = sintomoIdSnapshot.key ?: ""

                        for (yearSnapshot in sintomoIdSnapshot.children) {
                            val year = yearSnapshot.key ?: ""

                            for (weekSnapshot in yearSnapshot.children) {
                                val week = weekSnapshot.key ?: ""

                                for (dataSnapshot in weekSnapshot.children) {
                                    val dataSegnalazione = dataSnapshot.key ?: ""

                                    for (oraSnapshot in dataSnapshot.children) {
                                        val oraSegnalazione = oraSnapshot.key ?: ""
                                        val sintomoData = oraSnapshot.getValue(Sintomo::class.java)

                                        if (sintomoData != null) {
                                            sintomoData.id = sintomoId
                                            sintomoData.dataSegnalazione = dataSegnalazione
                                            sintomoData.oraSegnalazione = oraSegnalazione

                                            Log.d("SintomoData", "Utente: $username, SintomoID: $sintomoId, Anno: $year, Settimana: $week, Data: $dataSegnalazione, Ora: $oraSegnalazione, Gravità: ${sintomoData.gravità}, Ultimo Pasto: ${sintomoData.tempoTrascorsoUltimoPasto}")
                                            sintomiList.add(Pair(sintomoData, username))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Recupera i sintomi globali (nomi) separatamente
                fetchGlobalSintomi { sintomiListaNomi ->
                    // Ora che hai sia i dati dei sintomi utente che i nomi globali dei sintomi
                    val fileName = "Report_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.xlsx"
                    val success = generateExcel(context, sintomiList, sintomiListaNomi, fileName)

                    // Genera il file Excel e restituisce un booleano
                    if (success) {
                        uploadFileToFirebaseStorage(context, fileName) { uploadSuccess, url ->
                            if (uploadSuccess) {
                                // Copia il file nella directory Home
                                if (saveFileToUserHome(context, fileName)) {
                                    Log.d("FileSave", "File salvato anche nella directory Home dell'utente.")
                                } else {
                                    Log.e("FileSave", "Errore nel salvataggio del file nella directory Home dell'utente.")
                                }
                            }
                        }
                    } else {
                        Log.e("FirebaseStorage", "Errore durante la generazione del file.")
                    }//QUI C è ELSE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
    fun saveFileToUserHome(context: Context, fileName: String): Boolean {
        val srcFile = File(context.getExternalFilesDir(null)?.absolutePath + "/Reports", fileName)
        val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            srcFile.copyTo(destFile, overwrite = true)
            Log.d("FileSave", "File copiato correttamente nella directory Home: ${destFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("FileSave", "Errore nel copiare il file nella directory Home", e)
            false
        }
    }


    fun fetchGlobalSintomi(callback: (List<Sintomo>) -> Unit) {
        val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
        val sintomiList = mutableListOf<Sintomo>()

        database.reference.child("sintomi").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(sintomiSnapshot: DataSnapshot) {
                for (sintomoSnapshot in sintomiSnapshot.children) {
                    val sintomo = sintomoSnapshot.getValue(Sintomo::class.java)
                    if (sintomo != null) {
                        sintomiList.add(sintomo)
                    }
                }
                Log.d("Firebase", "Sintomi globali recuperati: $sintomiList")
                callback(sintomiList)  // Restituisce la lista solo dopo aver terminato
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Errore nel recupero dei sintomi globali: ${databaseError.message}")
                callback(emptyList())  // Restituisce una lista vuota in caso di errore
            }
        })
    }

    fun uploadFileToFirebaseStorage(context: Context, fileName: String, callback: (Boolean, String?) -> Unit) {
        // Ottieni il riferimento allo storage di Firebase
        val storage = FirebaseStorage.getInstance().reference
        val file = File(context.getExternalFilesDir(null)?.absolutePath + "/Reports", fileName)

        if (file.exists()) {
            val uri = Uri.fromFile(file)
            val storageRef = storage.child("reports/$fileName") // Cartella 'reports' in Firebase Storage

            // Carica il file
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    // Ottieni l'URL di download
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        Log.d("FirebaseStorage", "File caricato con successo: $fileName")
                        callback(true, downloadUri.toString()) // Successo, ritorna l'URL
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Errore durante il caricamento del file", e)
                    callback(false, null) // Fallimento
                }
        } else {
            Log.e("FirebaseStorage", "File non trovato: $fileName")
            callback(false, null) // File non esistente
        }
    }






}
