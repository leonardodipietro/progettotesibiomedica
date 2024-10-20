package com.example.myapplication2.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.myapplication2.model.Sintomo
import com.example.myapplication2.model.Utente
import com.example.myapplication2.model.UtenteEliminato
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class ExportRepo {

    private val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")
    private val exAccountRef = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app").reference

    fun fetchLastWeekReports(callback: (List<Pair<Sintomo, String>>, List<Sintomo>) -> Unit) {
        fetchGlobalSintomi { globalSintomiList ->
            val sintomiList = mutableListOf<Pair<Sintomo, String>>()
            val currentDate = LocalDate.now()
            val pastWeek = currentDate.minusWeeks(1)
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())

            // Recupero delle segnalazioni dal nodo "users"
            database.reference.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (userSnapshot in dataSnapshot.children) {
                        val username = userSnapshot.child("name").getValue(String::class.java) ?: "Sconosciuto"
                        val sintomiSnapshot = userSnapshot.child("sintomi")
                        for (sintomoIdSnapshot in sintomiSnapshot.children) {
                            val sintomoId = sintomoIdSnapshot.key ?: "" // Qui otteniamo l'ID del sintomo
                            for (yearSnapshot in sintomoIdSnapshot.children) {
                                for (weekSnapshot in yearSnapshot.children) {
                                    for (dataSnapshot in weekSnapshot.children) {
                                        val dataSegnalazione = dataSnapshot.key ?: ""

                                        if (LocalDate.parse(dataSegnalazione, dateFormatter).isAfter(pastWeek)) {
                                            for (oraSnapshot in dataSnapshot.children) {
                                                val sintomoData = oraSnapshot.getValue(Sintomo::class.java)

                                                if (sintomoData != null) {
                                                    sintomoData.id = sintomoId // Assegna l'ID del sintomo recuperato
                                                    sintomoData.dataSegnalazione = dataSegnalazione
                                                    sintomoData.oraSegnalazione = oraSnapshot.key ?: ""

                                                    // Log dell'ID del sintomo per debug
                                                    Log.d("SintomoDebug", "ID Sintomo: ${sintomoData.id}")

                                                    // Associa il nome del sintomo dalla lista globale usando l'ID
                                                    val nomeSintomo = globalSintomiList.find { it.id == sintomoData.id }?.nomeSintomo ?: "Sintomo sconosciuto"
                                                    sintomoData.nomeSintomo = nomeSintomo

                                                    sintomiList.add(Pair(sintomoData, username))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dopo aver recuperato le segnalazioni da "users", procediamo con "exaccount"
                    fetchExAccountReports(sintomiList, pastWeek, dateFormatter, globalSintomiList, callback)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Errore nel recupero dei dati: ${databaseError.message}")
                    callback(emptyList(), emptyList())
                }
            })
        }
    }


    private fun fetchExAccountReports(
        sintomiList: MutableList<Pair<Sintomo, String>>,
        pastWeek: LocalDate,
        dateFormatter: DateTimeFormatter,
        globalSintomiList: List<Sintomo>, // Passiamo la lista globale dei sintomi qui
        callback: (List<Pair<Sintomo, String>>, List<Sintomo>) -> Unit
    ) {
        database.reference.child("exaccount").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(exAccountSnapshot: DataSnapshot) {
                var counter = 1
                for (deletedUserSnapshot in exAccountSnapshot.children) {
                    val usernameEliminato = "Utente eliminato $counter"
                    counter++
                    val sintomiSnapshot = deletedUserSnapshot.child("sintomi")
                    for (sintomoIdSnapshot in sintomiSnapshot.children) {
                        for (yearSnapshot in sintomoIdSnapshot.children) {
                            for (weekSnapshot in yearSnapshot.children) {
                                for (dataSnapshot in weekSnapshot.children) {
                                    val dataSegnalazione = dataSnapshot.key ?: ""

                                    if (LocalDate.parse(dataSegnalazione, dateFormatter).isAfter(pastWeek)) {
                                        for (oraSnapshot in dataSnapshot.children) {
                                            val sintomoData = oraSnapshot.getValue(Sintomo::class.java)

                                            if (sintomoData != null) {
                                                sintomoData.dataSegnalazione = dataSegnalazione
                                                sintomoData.oraSegnalazione = oraSnapshot.key ?: ""

                                                // Verifica se il nome del sintomo è vuoto e recupera dalla lista globale
                                                if (sintomoData.nomeSintomo.isEmpty()) {
                                                    val globalSintomo = globalSintomiList.find { it.id == sintomoData.id }
                                                    sintomoData.nomeSintomo = globalSintomo?.nomeSintomo ?: "Sintomo sconosciuto"
                                                }

                                                sintomiList.add(Pair(sintomoData, usernameEliminato))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                callback(sintomiList, globalSintomiList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Errore nel recupero dei dati exaccount: ${databaseError.message}")
                callback(emptyList(), emptyList()) // Gestione dell'errore
            }
        })
    }

    fun generateExcel(
        context: Context,
        sintomiData: List<Pair<Sintomo, String>>,
        sintomiNomiGlobali: List<Sintomo>,
        fileName: String
    ): Boolean {
        val path = File(context.getExternalFilesDir(null)?.absolutePath + "/Reports")

        if (!path.exists()) {
            path.mkdirs()
        }

        val file = File(path, fileName)

        return try {

            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Sintomi Report")

            // Create the header row in the Excel sheet
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Data")
            headerRow.createCell(1).setCellValue("Nome Utente")
            headerRow.createCell(2).setCellValue("Ora Rilevazione")
            headerRow.createCell(3).setCellValue("Ora Ultimo Pasto")
            headerRow.createCell(4).setCellValue("Gravità")

            sintomiNomiGlobali.map { it.nomeSintomo }.distinct().forEachIndexed { index, nomeSintomo ->
                headerRow.createCell(5 + index).setCellValue(nomeSintomo)
            }

            var rowCount = 1

            for ((sintomo, username) in sintomiData) {
                val row = sheet.createRow(rowCount++)

                // Insert the date and time of the report
                row.createCell(0).setCellValue(sintomo.dataSegnalazione)
                row.createCell(1).setCellValue(username)
                row.createCell(2).setCellValue(sintomo.oraSegnalazione)
                row.createCell(3).setCellValue(sintomo.tempoTrascorsoUltimoPasto.toDouble())
                row.createCell(4).setCellValue(sintomo.gravità.toDouble())

                val sintomoIndex = sintomiNomiGlobali.indexOfFirst { it.id == sintomo.id }

                Log.d("ExcelDebug", "Sintomo segnalato ID: ${sintomo.id}")
                Log.d("ExcelDebug", "Sintomi globali: ${sintomiNomiGlobali.map { it.id }}")

                if (sintomoIndex != -1) {
                    Log.d("ExcelDebug", "Sintomoin colonna index: $sintomoIndex con ID corrispondente: ${sintomiNomiGlobali[sintomoIndex].id}")
                    // Inserics x
                    row.createCell(5 + sintomoIndex).setCellValue("X")
                } else {
                    Log.d("ExcelDebug", "Nessun sintomo per a ID: ${sintomo.id}")
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

                // Recupera i sintomi dagli utenti attivi
                for (userSnapshot in dataSnapshot.children) {
                    val username = userSnapshot.child("name").getValue(String::class.java) ?: "Sconosciuto"

                    // Navigazione tra i nodi di sintomi -> anno -> settimana -> data -> ora
                    val sintomiSnapshot = userSnapshot.child("sintomi")
                    for (sintomoIdSnapshot in sintomiSnapshot.children) {
                        val sintomoId = sintomoIdSnapshot.key ?: ""

                        for (yearSnapshot in sintomoIdSnapshot.children) {
                            for (weekSnapshot in yearSnapshot.children) {
                                for (dataSnapshot in weekSnapshot.children) {
                                    val dataSegnalazione = dataSnapshot.key ?: ""

                                    for (oraSnapshot in dataSnapshot.children) {
                                        val oraSegnalazione = oraSnapshot.key ?: ""
                                        val sintomoData = oraSnapshot.getValue(Sintomo::class.java)

                                        if (sintomoData != null) {
                                            sintomoData.id = sintomoId
                                            sintomoData.dataSegnalazione = dataSegnalazione
                                            sintomoData.oraSegnalazione = oraSegnalazione

                                            Log.d("SintomoData", "Utente: $username, SintomoID: $sintomoId, Data: $dataSegnalazione, Ora: $oraSegnalazione")
                                            sintomiList.add(Pair(sintomoData, username))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Recupero dei sintomi degli utenti eliminati
                database.reference.child("exaccount").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(exAccountSnapshot: DataSnapshot) {
                        var counter = 1

                        for (deletedUserSnapshot in exAccountSnapshot.children) {
                            val deletedUserId = deletedUserSnapshot.key ?: "Sconosciuto"
                            val usernameEliminato = "Utente eliminato $counter"
                            counter++

                            val sintomiSnapshot = deletedUserSnapshot.child("sintomi")
                            for (sintomoIdSnapshot in sintomiSnapshot.children) {
                                val sintomoId = sintomoIdSnapshot.key ?: ""

                                for (yearSnapshot in sintomoIdSnapshot.children) {
                                    for (weekSnapshot in yearSnapshot.children) {
                                        for (dataSnapshot in weekSnapshot.children) {
                                            val dataSegnalazione = dataSnapshot.key ?: ""

                                            for (oraSnapshot in dataSnapshot.children) {
                                                val oraSegnalazione = oraSnapshot.key ?: ""
                                                val sintomoData = oraSnapshot.getValue(Sintomo::class.java)

                                                if (sintomoData != null) {
                                                    sintomoData.id = sintomoId
                                                    sintomoData.dataSegnalazione = dataSegnalazione
                                                    sintomoData.oraSegnalazione = oraSegnalazione

                                                    Log.d("SintomoData", "Utente eliminato: $usernameEliminato, SintomoID: $sintomoId, Data: $dataSegnalazione, Ora: $oraSegnalazione")
                                                    sintomiList.add(Pair(sintomoData, usernameEliminato))
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
                                            Log.d("FileSave", "File salvato su tel")
                                        } else {
                                            Log.e("FileSave", "Errore nel salvataggio sul telef.")
                                        }
                                    }
                                }
                            } else {
                                Log.e("FirebaseStorage", "Errore di creazione file")
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("Firebase", "Errore nel recupero degli utenti eliminati: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Errore nel recupero dei sintomi: ${error.message}")
            }
        })
    }
    fun saveFileToUserHome(context: Context, fileName: String): Boolean {
        val srcFile = File(context.getExternalFilesDir(null)?.absolutePath + "/Reports", fileName)
        val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)

        return try {
            srcFile.copyTo(destFile, overwrite = true)
            Log.d("FileSave", "File copiatoin home ${destFile.absolutePath}")
            true
        } catch (e: Exception) {

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
                Log.e("Firebase", "Errore nel recupero: ${databaseError.message}")
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
                        Log.d("FirebaseStorage", "File caricato  $fileName")
                        callback(true, downloadUri.toString()) // Successo, ritorna l'URL
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseStorage", "Errore nel carvare", e)
                    callback(false, null) // Fallimento
                }
        } else {
            Log.e("FirebaseStorage", "File non trovato: $fileName")
            callback(false, null)
        }
    }






}
