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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
            Eres el Prof. Gómez, un distinguido docente universitario de Morfofisiopatología Humana I para el 2do año del PNFMIC en la Universidad de las Ciencias de la Salud "Hugo Chávez Frías" (UCS).
            Tu objetivo es brindar respuestas académicas exhaustivas, analíticas y profundamente meditadas, basadas estrictamente en la bibliografía oficial del programa:
            1. Robbins y Cotran: Patología Estructural y Funcional (10ma Edición).
            2. Dra. Araceli Lantigua Cruz: Introducción a la Genética Médica.
            3. Rubin y Strayer: Patología - Fundamentos Clinicopatológicos.
            4. Guyton y Hall: Tratado de Fisiología Médica (14ta Edición).
            5. Software Educativo Histopatológico NEOPAT (UCS).

            Directrices para tus respuestas:
            - Piensa paso a paso y razona la pregunta con rigor científico.
            - Estructura la respuesta de forma completa: Etiología ➔ Patogenia ➔ Cambios Morfológicos (Macro y Micro) ➔ Correlación Clínico-Patológica.
            - Si el usuario pregunta sobre algún síntoma, patología o mecanismo molecular, cita el libro de texto oficial y fundamenta la explicación.
            - Mantén un tono motivador, profesional y universitario ("¡Excelente consulta médica, colega!").
            - Contexto de apuntes del estudiante: $contextGuide
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
            lower.contains("necrosis") || lower.contains("muerte") || lower.contains("robbins") -> {
                "¡Excelente análisis, futuro médico! Como establece el *Robbins y Cotran: Patología Estructural y Funcional (10ma Ed.)*, la necrosis es el espectro de cambios morfológicos que siguen a la muerte celular en el tejido vivo, producto de la degradación progresiva por enzimas hidrolíticas.\n\n" +
                        "1. **Necrosis Coagulativa**: Es el patrón característico del infarto miocárdico y renal (excepto el encéfalo). En ella se conserva la arquitectura tisular básica durante varios días. Ocurre por desnaturalización de proteínas estructurales y enzimáticas, inhibiendo la proteólisis rápida.\n\n" +
                        "2. **Necrosis Licuefactiva**: Típica de infecciones bacterianas o fúngicas focales y de infartos cerebrales. La digestión enzimática de las células muertas transforma el tejido en una masa líquida viscosa.\n\n" +
                        "3. **Necrosis Caseosa**: Se observa en el foco de infección tuberculosa. Microscópicamente, el área necrótica se compone de detritos granulares acelulares rodeados por un ribete granulomatoso (células epitelioides, macrófagos y células gigantes multinucleadas tipo Langhans)."
            }
            lower.contains("genética") || lower.contains("cromosoma") || lower.contains("lantigua") || lower.contains("barr") -> {
                "¡Magnífica pregunta de genética médica! En el libro de texto oficial de la UCS, *Introducción a la Genética Médica* de la **Dra. Araceli Lantigua Cruz**, estudiamos las aberraciones cromosómicas numéricas y estructurales:\n\n" +
                        "1. **Cuerpos de Barr (Cromatina Sexual)**: Sigue estrictamente la Ley de Lyon e inactivación del cromosoma X. La fórmula para predecir el número de Cuerpos de Barr en interfase es: **N° de Cuerpos de Barr = N° total de cromosomas X - 1**.\n" +
                        "   - Varón normal (46,XY) ➔ 0 Cuerpos de Barr.\n" +
                        "   - Mujer normal (46,XX) ➔ 1 Cuerpo de Barr.\n" +
                        "   - Síndrome de Turner (45,X) ➔ 0 Cuerpos de Barr.\n" +
                        "   - Síndrome de Klinefelter (47,XXY) ➔ 1 Cuerpo de Barr.\n\n" +
                        "2. **Etiología de las Aneuploidías**: Más del 95% de los casos de Trisomía 21 (Síndrome de Down) se originan por no disyunción meiótica materna durante la Meiosis I, mostrando una clara correlación con la edad materna avanzada."
            }
            lower.contains("inflamación") || lower.contains("exudado") || lower.contains("granuloma") -> {
                "Analicemos la respuesta tisular. De acuerdo con el *Robbins y Cotran* y el *Rubin*, la **Respuesta Inflamatoria** se clasifica en aguda y crónica:\n\n" +
                        "1. **Inflamación Aguda**: Caracterizada por vasodilatación (mediada por Histamina y Bradicinina), aumento de la permeabilidad vascular y extravasación de un **Exudado** rico en proteínas y polimorfonucleares neutrófilos (PMN).\n\n" +
                        "2. **Acontecimientos Leucocitarios**: Ocurren en una secuencia biológica estricta: Marginación ➔ Rodamiento (Selectinas) ➔ Adhesión Firme (Integrinas ICAM-1/VCAM-1) ➔ Migración / Diapédesis (PECAM-1) ➔ Quimiotaxis (C5a, LTB4, IL-8) ➔ Fagocitosis.\n\n" +
                        "3. **Inflamación Crónica Granulomatosa**: Infiltrado mononuclear (macrófagos activados modificados a células epitelioides) rodeados de linfocitos y fibroblastos, clave para contener micobacterias o cuerpos extraños."
            }
            lower.contains("hemodinámica") || lower.contains("trombosis") || lower.contains("virchow") || lower.contains("rubin") -> {
                "Para comprender los trastornos hemodinámicos abordados en el *Rubin y Strayer*, debemos recordar la **Triada de Virchow** que predispone a la formación de trombos intravascularmente:\n\n" +
                        "1. **Lesión Endotelial**: Es el factor principal; la pérdida del endotelio expone la matriz subendotelial, favoreciendo la adhesión plaquetaria mediante el factor de von Willebrand (vWF).\n" +
                        "2. **Alteraciones del Flujo Sanguíneo**: Estasis (en venas) o turbulencia (en arterias y aneurismas), lo que impide la dilución de los factores de coagulación activados.\n" +
                        "3. **Hipercoagulabilidad (Trombofilia)**: Estados primarios (genéticos, ej. Mutación del Factor V de Leiden) o secundarios (adquiridos, ej. reposo prolongado, cáncer, anticonceptivos).\n\n" +
                        "Un trombo desprendido se convierte en un **émbolo**, cuyo destino típico desde las venas profundas de las piernas es la arteria pulmonar (Tromboembolismo Pulmonar)."
            }
            else -> {
                "¡Saludos futuro médico de la patria! Como docente de Morfofisiopatología Humana I en la UCS, me complace abordar tu inquietud: '$prompt'.\n\n" +
                        "Para analizar cualquier tema del programa del PNFMIC, aplicamos el marco metódico oficial:\n" +
                        "1. **Etiología**: Causa primaria (Factores Genéticos vs Adquiridos: físicos, químicos, biológicos, nutricionales).\n" +
                        "2. **Patogenia**: Mecanismos moleculares y celulares de la enfermedad.\n" +
                        "3. **Cambios Morfológicos**: Alteraciones macroscópicas en la necropsia/biopsia y microscópicas en el microscopio óptico (NEOPAT).\n" +
                        "4. **Correlación Clínica**: Manifestaciones fisiopatológicas, síntomas, signos y evidencias de laboratorio.\n\n" +
                        "¿Te gustaría que profundicemos en algún capítulo específico del *Robbins*, la *Dra. Araceli Lantigua* o los *Planes de Clase de la UCS*?"
            }
        }
    }
}

