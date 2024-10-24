package com.example.myapplication2.repository
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class TranslationRepo {

    private val client = OkHttpClient()

    fun translate(text: String, targetLang: String, onResult: (String?) -> Unit) {
        val apiKey = "ec2f97e4c87c4598a25f6f99f8377e8b"
        val region = "westeurope"
        val url = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=$targetLang"

        val jsonBody = JSONArray().apply {
            put(JSONObject().apply {
                put("Text", text)
            })
        }


        val request = Request.Builder()
            .url(url)
            .header("Ocp-Apim-Subscription-Key", apiKey)
            .header("Ocp-Apim-Subscription-Region", region)
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString()))
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Gestisce l'errore nella chiamata
                e.printStackTrace()
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    var responseBody = response.body?.string()
                    if (responseBody != null) {
                        // Elabora la risposta JSON
                        val jsonResponse = JSONArray(responseBody)
                        val translations = jsonResponse.getJSONObject(0)
                            .getJSONArray("translations")
                        val translatedText = translations.getJSONObject(0).getString("text")

                   
                        onResult(translatedText)
                    } else {
                        onResult(null)
                    }
                } else {
                    onResult(null)
                }
            }
        })
    }
}


