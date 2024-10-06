package com.example.myapplication2.repository

import android.content.Context
import android.net.Uri
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

    // Funzione semplificata per generare il file Excel con i dati dei sintomi
    fun generateExcel(
        context: Context,
        sintomiData: List<Sintomo>,
        sintomiNomiGlobali: List<Sintomo>,  // Aggiungi la lista dei nomi globali qui
        fileName: String
    ): Boolean {
        val path = File(context.getExternalFilesDir(null)?.absolutePath + "/Reports")

        if (!path.exists()) {
            path.mkdirs() // Crea la cartella se non esiste
        }

        val file = File(path, fileName)

        return try {
            // Crea un nuovo workbook e foglio Excel
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Sintomi Report")

            // Aggiungi l'intestazione (header) al foglio Excel
            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue("Data")
            headerRow.createCell(1).setCellValue("Nome")
            headerRow.createCell(2).setCellValue("Ora Rilevazione")
            headerRow.createCell(3).setCellValue("Ora Ultimo Pasto")

            // Aggiungi i nomi dei sintomi globali come intestazione
            sintomiNomiGlobali.map { it.nomeSintomo }.distinct().forEachIndexed { index, nomeSintomo ->
                headerRow.createCell(4 + index).setCellValue(nomeSintomo)  // Colonne per i sintomi
            }

            // Mantieni traccia del numero di righe per non sovrascrivere
            var rowCount = 1

            // Aggiungi i dati dei sintomi per ogni segnalazione
            for (sintomo in sintomiData) {
                Log.d("Excel", "Scrittura sintomo: ${sintomo.nomeSintomo} con gravità ${sintomo.gravità}")
                val row = sheet.createRow(rowCount++)

                // Inserisci data e ora della segnalazione
                row.createCell(0).setCellValue(sintomo.dataSegnalazione)  // Data del sintomo
                row.createCell(1).setCellValue("Utente")  // Placeholder per nome utente (da aggiornare se hai i dati utente)
                row.createCell(2).setCellValue(sintomo.oraSegnalazione)    // Ora di rilevazione
                row.createCell(3).setCellValue(sintomo.tempoTrascorsoUltimoPasto.toDouble())  // Tempo trascorso dall'ultimo pasto

                // Inserisci la gravità del sintomo nella colonna corrispondente
                val sintomoIndex = sintomiNomiGlobali.indexOfFirst { it.nomeSintomo == sintomo.nomeSintomo }
                if (sintomoIndex != -1) {
                    row.createCell(4 + sintomoIndex).setCellValue(sintomo.gravità.toDouble()) // Gravità del sintomo
                }
            }

            // Scrivi il file Excel
            FileOutputStream(file).use { fileOut ->
                workbook.write(fileOut)
                workbook.close()
            }

            Log.d("Excel", "File salvato in: ${file.absolutePath}")
            true // Successo
        } catch (e: Exception) {
            Log.e("Excel", "Errore durante la generazione del file Excel", e)
            false // Errore
        }
    }



    fun fetchDataAndGenerateExcel(context: Context) {
        val database = FirebaseDatabase.getInstance("https://myapplication2-7be0f-default-rtdb.europe-west1.firebasedatabase.app")

        // Recupero del nodo "users" con i relativi sintomi
        database.reference.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userList = mutableListOf<Utente>()
                val sintomiList = mutableListOf<Sintomo>()
                val userSintomi = mutableMapOf<String, MutableMap<String, MutableMap<String, Sintomo>>>()

                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(Utente::class.java)
                    if (user != null) {
                        userList.add(user)

                        // Recupera i sintomi per ogni utente
                        val sintomiSnapshot = userSnapshot.child("sintomi")
                        val sintomiMap = mutableMapOf<String, MutableMap<String, Sintomo>>()

                        for (sintomoSnapshot in sintomiSnapshot.children) {
                            val sintomoId = sintomoSnapshot.key ?: ""
                            for (dataSnapshot in sintomoSnapshot.children) {
                                val data = dataSnapshot.key ?: ""

                                // Ciclo attraverso tutte le ore per quella data
                                for (oraSnapshot in dataSnapshot.children) {
                                    val ora = oraSnapshot.key ?: ""
                                    val sintomoData = oraSnapshot.getValue(Sintomo::class.java)

                                    if (sintomoData != null) {
                                        Log.d("SintomoData", "Utente: ${user.id}, SintomoID: $sintomoId, Data: $data, Ora: $ora, Gravità: ${sintomoData.gravità}, Ultimo Pasto ${sintomoData.tempoTrascorsoUltimoPasto}")
                                        if (!sintomiMap.containsKey(data)) {
                                            sintomiMap[data] = mutableMapOf()
                                        }
                                        sintomiMap[data]!![ora] = sintomoData

                                        // Aggiungi il sintomo alla lista
                                        sintomiList.add(sintomoData)
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
                        Log.d("FirebaseStorage", "File generato correttamente.")

                        // Chiama la funzione per caricare il file su Firebase Storage
                        uploadFileToFirebaseStorage(context, fileName) { uploadSuccess, url ->
                            if (uploadSuccess) {
                                Log.d("FirebaseStorage", "File caricato correttamente su Firebase Storage. URL: $url")
                            } else {
                                Log.e("FirebaseStorage", "Errore durante il caricamento del file su Firebase Storage.")
                            }
                        }
                    } else {
                        Log.e("FirebaseStorage", "Errore durante la generazione del file.")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
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
