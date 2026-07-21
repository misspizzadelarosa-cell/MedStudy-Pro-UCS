package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class GeminiPart(val text: String)
data class GeminiContent(val parts: List<GeminiPart>, val role: String? = "user")
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

class GeminiApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun askProfessorGomez(userPrompt: String, contextGuide: String = ""): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineProfResponse(userPrompt)
        }

        val systemPrompt = """
            Eres el Prof. Gómez, un docente universitario de Biología, Patología y Morfofisiopatología Humana I en la Universidad de las Ciencias de la Salud Hugo Chávez Frías (UCS).
            Tu tono es socrático, académico pero sumamente motivador y cercano ("¡Saludos futuro médico!").
            Responde brevemente (2-4 párrafos), citando la literatura médica relevante de la UCS cuando aplique: Robbins y Cotran (Patología Estructural), Genética Médica de la Dra. Araceli Lantigua Cruz, Rubin y Strayer, o el software educativo NEOPAT.
            Guía al estudiante para que razone la etiología, patogenia, morfología y clínica de los casos.
            Contexto de estudio adicional provisto por el estudiante: $contextGuide
        """.trimIndent()

        val requestData = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(userPrompt)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt)))
        )

        val adapter = moshi.adapter(GeminiRequest::class.java)
        val jsonBody = adapter.toJson(requestData)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseStr = response.body?.string() ?: ""
                if (response.isSuccessful && responseStr.contains("\"text\":")) {
                    extractTextFromResponse(responseStr)
                } else {
                    getOfflineProfResponse(userPrompt)
                }
            }
        } catch (e: Exception) {
            getOfflineProfResponse(userPrompt)
        }
    }

    private fun extractTextFromResponse(jsonResponse: String): String {
        return try {
            val regex = """"text"\s*:\s*"([^"]*(?:\\.[^"]*)*)"""".toRegex()
            val matches = regex.findAll(jsonResponse)
            val textBuilder = StringBuilder()
            for (match in matches) {
                val extracted = match.groupValues[1]
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                textBuilder.append(extracted)
            }
            if (textBuilder.isNotEmpty()) textBuilder.toString() else getOfflineProfResponse("")
        } catch (e: Exception) {
            getOfflineProfResponse("")
        }
    }

    private fun getOfflineProfResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("necrosis") || lower.contains("muerte") -> {
                "¡Excelente inquietud colega! En Morfofisiopatología Humana I diferenciamos la necrosis según la morfología y el tejido afectado. " +
                        "Según el Robbins, la necrosis coagulativa preserva la arquitectura celular por desnaturalización de proteínas (ej. infarto miocárdico), " +
                        "mientras la licuefactiva se caracteriza por autodigestión enzimática (típica en infecciones bacterianas y del Sistema Nervioso Central)."
            }
            lower.contains("genética") || lower.contains("cromosoma") || lower.contains("lantigua") -> {
                "Como enseña la Dra. Araceli Lantigua Cruz en 'Introducción a la Genética Médica', las cromosomopatías numéricas " +
                        "como la Trisomía 21 se originan por una no disyunción meiótica materna en más del 95% de los casos. " +
                        "El diagnóstico citogenético definitivo exige siempre la realización de un cariotipo."
            }
            lower.contains("hemodinámica") || lower.contains("trombosis") || lower.contains("edema") -> {
                "Para comprender los trastornos hemodinámicos en el Rubin, recuerda la Triada de Virchow: lesión endotelial, " +
                        "estasis circulatoria e hipercoagulabilidad. Una tromboembolia pulmonar se origina típicamente en el sistema venoso profundo de miembros inferiores."
            }
            else -> {
                "¡Saludos futuro médico! Como docente de la UCS, mi objetivo es ayudarte a interrelacionar la alteración estructural con la manifestación clínica. " +
                        "Recuerda seguir nuestro método de abordaje: Etiología ➔ Patogenia ➔ Cambios Estructurales ➔ Clínica."
            }
        }
    }
}
