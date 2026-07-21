package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiService
import com.example.data.local.ExamStatsEntity
import com.example.data.local.MedStudyDatabase
import com.example.data.local.StudyGuideEntity
import com.example.data.local.UserStatsEntity
import com.example.ui.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MedStudyTab {
    LIBRARY,
    SOCRATIC,
    PUZZLE,
    EXAM
}

data class MedStudyUiState(
    val activeTab: MedStudyTab = MedStudyTab.LIBRARY,
    val userXp: Int = 45,
    val userLevel: Int = 1,
    val userStreak: Int = 1,
    val userRankTitle: String = "Iniciando Histopatología",
    val professorBubbleText: String = "¡Saludos futuro médico de la patria! He adaptado nuestro temario de Morfofisiopatología Humana I a una simulación de alto rendimiento. Explora la bibliografía básica, desafía mi chat socrático o mide tu intelecto con exámenes oficiales.",
    
    // Bookshelf
    val academicBooks: List<AcademicBook> = emptyList(),
    
    // Custom uploaded study guides
    val customGuides: List<StudyGuideEntity> = emptyList(),
    
    // Socratic Chat
    val activeSocraticIndex: Int = 0,
    val chatMessages: List<ChatMessage> = emptyList(),
    val currentDialogue: SocraticDialogue? = null,
    val isAiThinking: Boolean = false,
    
    // Puzzle Game
    val puzzleItems: List<PuzzleItem> = emptyList(),
    val selectedOrganelleId: String? = null,
    val matchedIds: Set<String> = emptySet(),
    val puzzleFeedback: String = "¡Selecciona un par para evaluarlo!",
    
    // Exam Simulator
    val examQuestions: List<ExamQuestion> = emptyList(),
    val activeExamIndex: Int = 0,
    val examScore: Int = 0,
    val examAnsweredCount: Int = 0,
    val selectedExamOptionKey: String? = null,
    val isExamAnswered: Boolean = false
)

class MedStudyViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = MedStudyDatabase.getDatabase(application).medStudyDao()
    private val geminiApi = GeminiApiService()

    private val _uiState = MutableStateFlow(MedStudyUiState())
    val uiState: StateFlow<MedStudyUiState> = _uiState.asStateFlow()

    private val rankTitles = listOf(
        "Iniciando Histopatología",
        "Inspector de Necrosis",
        "Maestro de Robbins",
        "Genetista Comunitario",
        "Experto en Morfofisiopatología"
    )

    init {
        loadInitialData()
        observeDatabase()
    }

    private fun loadInitialData() {
        val books = listOf(
            AcademicBook(
                id = "robbins",
                title = "Robbins y Cotran",
                author = "Patología Estructural y Funcional",
                category = "Robbins 10ma Ed.",
                tag = "Pilar Fundamental",
                colorStartHex = 0xFFBE123C,
                colorEndHex = 0xFF881337,
                profTip = "Del Robbins y Cotran, recuerden de memoria la diferencia morfológica entre Necrosis Licuefactiva (típica en cerebro por digestión enzimática) y Necrosis Coagulativa (infartos, excepto cerebro). ¡Es pregunta fija de examen!"
            ),
            AcademicBook(
                id = "lantigua",
                title = "Dra. Araceli Lantigua Cruz",
                author = "Introducción a la Genética Médica",
                category = "Genética UCS",
                tag = "Genética Patológica",
                colorStartHex = 0xFF4338CA,
                colorEndHex = 0xFF312E81,
                profTip = "De la Dra. Lantigua, no olviden repasar la Herencia Multifactorial. Explica las malformaciones congénitas comunes y enfermedades complejas del adulto en medicina comunitaria."
            ),
            AcademicBook(
                id = "rubin",
                title = "Rubin y Strayer",
                author = "Fundamentos Clinicopatológicos",
                category = "Rubin Patología",
                tag = "Clínico-Morfológico",
                colorStartHex = 0xFFD97706,
                colorEndHex = 0xFF92400E,
                profTip = "Del Rubin y Strayer les recomiendo estudiar a fondo los diagramas de shock endotóxico, la cascada de citoquinas IL-1 e IL-6, y la respuesta tisular al Factor de Necrosis Tumoral (TNF)."
            ),
            AcademicBook(
                id = "neopat",
                title = "Software NEOPAT",
                author = "Diagnóstico Histopatológico UCS",
                category = "Software UCS",
                tag = "Láminas Prácticas",
                colorStartHex = 0xFF059669,
                colorEndHex = 0xFF065F46,
                profTip = "Las guías prácticas del NEOPAT les exigen identificar muestras de inflamación granulomatosa crónico-caseosa. Busquen siempre las células gigantes multinucleadas de tipo Langhans."
            )
        )

        val dialogues = getPresetDialogues()
        val puzzles = getPresetPuzzles()
        val exams = getPresetExamQuestions()

        _uiState.update { state ->
            state.copy(
                academicBooks = books,
                puzzleItems = puzzles,
                examQuestions = exams,
                currentDialogue = dialogues.firstOrNull(),
                chatMessages = listOf(
                    ChatMessage(
                        id = "1",
                        sender = ChatSender.PROFESSOR,
                        text = dialogues.first().profPrompt
                    )
                )
            )
        }
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            dao.getUserStats().collect { stats ->
                if (stats != null) {
                    _uiState.update {
                        it.copy(
                            userXp = stats.xp,
                            userLevel = stats.level,
                            userStreak = stats.streakDays,
                            userRankTitle = stats.rankTitle
                        )
                    }
                } else {
                    dao.updateUserStats(UserStatsEntity())
                }
            }
        }

        viewModelScope.launch {
            dao.getAllGuides().collect { guides ->
                _uiState.update { it.copy(customGuides = guides) }
            }
        }

        viewModelScope.launch {
            dao.getExamStats().collect { examStats ->
                if (examStats != null) {
                    _uiState.update {
                        it.copy(
                            examScore = examStats.correctAnswers,
                            examAnsweredCount = examStats.totalQuestionsAnswered
                        )
                    }
                } else {
                    dao.updateExamStats(ExamStatsEntity())
                }
            }
        }
    }

    fun switchTab(tab: MedStudyTab) {
        val profBubble = when (tab) {
            MedStudyTab.LIBRARY -> "Has entrado a la Biblioteca de la UCS. Revisa la bibliografía complementaria o carga tus guías del PNFMIC para iniciar el estudio interactivo."
            MedStudyTab.SOCRATIC -> "El método socrático es indispensable para construir pensamiento clínico de calidad. Responde mis interrogantes basándote en la etiología y patogenia de los temas oficiales."
            MedStudyTab.PUZZLE -> "¡Asociación histológica dinámica! Conecta de forma interactiva las organelas y alteraciones celulares con sus síndromes patológicos correspondientes. ¡Adelante!"
            MedStudyTab.EXAM -> "Pon a prueba tus conocimientos con viñetas reales de exámenes pasados de Morfofisiopatología Humana I. ¡Lee cada palabra del caso clínico!"
        }
        _uiState.update { it.copy(activeTab = tab, professorBubbleText = profBubble) }
    }

    fun onBookClicked(book: AcademicBook) {
        _uiState.update { it.copy(professorBubbleText = book.profTip) }
        addXp(10)
    }

    fun addXp(amount: Int) {
        val currentXp = _uiState.value.userXp + amount
        var level = _uiState.value.userLevel
        var remainingXp = currentXp

        if (remainingXp >= 100) {
            remainingXp -= 100
            level++
            setProfessorBubble("🎉 ¡Excelente colega! Has subido al Nivel $level. Sigamos consolidando la Morfofisiopatología Humana.")
        }

        val rankIndex = (level - 1).coerceAtMost(rankTitles.size - 1)
        val rankTitle = rankTitles[rankIndex]

        viewModelScope.launch {
            dao.updateUserStats(
                UserStatsEntity(
                    id = 1,
                    xp = remainingXp,
                    level = level,
                    streakDays = _uiState.value.userStreak,
                    rankTitle = rankTitle
                )
            )
        }
    }

    fun setProfessorBubble(text: String) {
        _uiState.update { it.copy(professorBubbleText = text) }
    }

    // --- Socratic Chat ---
    fun selectSocraticOption(option: SocraticOption) {
        val dialogue = _uiState.value.currentDialogue ?: return
        val currentMessages = _uiState.value.chatMessages.toMutableList()

        // Student Message
        currentMessages.add(
            ChatMessage(
                id = System.currentTimeMillis().toString(),
                sender = ChatSender.STUDENT,
                text = option.text
            )
        )

        // Professor Reaction
        currentMessages.add(
            ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                sender = ChatSender.PROFESSOR,
                text = "Retroalimentación del Prof. Gómez: ${option.response}"
            )
        )

        _uiState.update { it.copy(chatMessages = currentMessages) }
        addXp(option.xpReward)

        // Advance to next preset dialogue if correct
        if (option.isCorrect) {
            viewModelScope.launch {
                val dialogues = getPresetDialogues()
                val nextIndex = (_uiState.value.activeSocraticIndex + 1) % dialogues.size
                val nextDialogue = dialogues[nextIndex]

                currentMessages.add(
                    ChatMessage(
                        id = (System.currentTimeMillis() + 2).toString(),
                        sender = ChatSender.PROFESSOR,
                        text = "Siguiente reto socrático: ${nextDialogue.profPrompt}"
                    )
                )

                _uiState.update {
                    it.copy(
                        activeSocraticIndex = nextIndex,
                        currentDialogue = nextDialogue,
                        chatMessages = currentMessages
                    )
                }
            }
        }
    }

    fun askCustomQuestionToProfessor(prompt: String) {
        if (prompt.isBlank()) return
        val currentMessages = _uiState.value.chatMessages.toMutableList()

        currentMessages.add(
            ChatMessage(
                id = System.currentTimeMillis().toString(),
                sender = ChatSender.STUDENT,
                text = prompt
            )
        )

        _uiState.update { it.copy(chatMessages = currentMessages, isAiThinking = true) }

        viewModelScope.launch {
            val guideContext = _uiState.value.customGuides.joinToString("\n") { it.title + ": " + it.content.take(200) }
            val aiResponse = geminiApi.askProfessorGomez(prompt, guideContext)

            val updatedMessages = _uiState.value.chatMessages.toMutableList()
            updatedMessages.add(
                ChatMessage(
                    id = System.currentTimeMillis().toString(),
                    sender = ChatSender.PROFESSOR,
                    text = aiResponse,
                    isAiGenerated = true
                )
            )

            _uiState.update {
                it.copy(chatMessages = updatedMessages, isAiThinking = false)
            }
            addXp(15)
        }
    }

    fun resetSocraticChat() {
        val dialogues = getPresetDialogues()
        _uiState.update {
            it.copy(
                activeSocraticIndex = 0,
                currentDialogue = dialogues.first(),
                chatMessages = listOf(
                    ChatMessage(
                        id = "1",
                        sender = ChatSender.PROFESSOR,
                        text = dialogues.first().profPrompt
                    )
                )
            )
        }
        setProfessorBubble("He restablecido nuestro diálogo de estudio. ¡Comencemos de nuevo!")
    }

    // --- Pathological Puzzle ---
    fun selectPuzzleOrganelle(id: String) {
        if (_uiState.value.matchedIds.contains(id)) return
        _uiState.update {
            it.copy(
                selectedOrganelleId = id,
                puzzleFeedback = "Organela/Mecanismo seleccionado. Ahora haz clic en su patología o síndrome correspondiente."
            )
        }
    }

    fun selectPuzzleSyndrome(id: String) {
        val selectedOrganelle = _uiState.value.selectedOrganelleId
        if (selectedOrganelle == null) {
            _uiState.update { it.copy(puzzleFeedback = "¡Primero selecciona un mecanismo u organela a la izquierda!") }
            return
        }

        if (selectedOrganelle == id) {
            // Match!
            val updatedMatched = _uiState.value.matchedIds + id
            val matchesCount = updatedMatched.size
            _uiState.update {
                it.copy(
                    selectedOrganelleId = null,
                    matchedIds = updatedMatched,
                    puzzleFeedback = "¡Excelente pareja correcta! Has ganado +20 XP de la UCS."
                )
            }
            addXp(20)

            if (matchesCount == _uiState.value.puzzleItems.size) {
                setProfessorBubble("¡Espectacular! Has completado el Rompecabezas Morfofisiopatológico con un puntaje perfecto. Dominas la histopatología molecular.")
                addXp(50)
            }
        } else {
            // Wrong
            _uiState.update {
                it.copy(
                    puzzleFeedback = "Pareja incorrecta. Analiza de nuevo la causa molecular y su expresión clínica."
                )
            }
        }
    }

    // --- Exam Simulator ---
    fun selectExamOption(option: ExamOption) {
        if (_uiState.value.isExamAnswered) return

        val currentQuestion = _uiState.value.examQuestions.getOrNull(_uiState.value.activeExamIndex) ?: return
        val isCorrect = option.isCorrect
        val totalAns = _uiState.value.examAnsweredCount + 1
        val correctAns = if (isCorrect) _uiState.value.examScore + 1 else _uiState.value.examScore

        _uiState.update {
            it.copy(
                selectedExamOptionKey = option.key,
                isExamAnswered = true,
                examAnsweredCount = totalAns,
                examScore = correctAns
            )
        }

        viewModelScope.launch {
            dao.updateExamStats(
                ExamStatsEntity(
                    id = 1,
                    totalQuestionsAnswered = totalAns,
                    correctAnswers = correctAns
                )
            )
        }

        if (isCorrect) {
            addXp(25)
        }
    }

    fun nextExamQuestion() {
        val questions = _uiState.value.examQuestions
        val nextIdx = (_uiState.value.activeExamIndex + 1) % questions.size
        _uiState.update {
            it.copy(
                activeExamIndex = nextIdx,
                selectedExamOptionKey = null,
                isExamAnswered = false
            )
        }
    }

    // --- Guide Import ---
    fun addCustomGuide(title: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        val entity = StudyGuideEntity(
            title = title,
            content = content,
            category = "Guía PNFMIC / UCS",
            isCustom = true
        )
        viewModelScope.launch {
            dao.insertGuide(entity)
        }
        setProfessorBubble("¡Excelente! He analizado tu documento local '$title'. He guardado tu material y adaptado el tutor AI para responder sobre él.")
        addXp(30)
    }

    fun loadUcsModule(moduleId: String) {
        val text = when (moduleId) {
            "lesion_celular" -> "Has seleccionado Tema 1: Lesión y Muerte Celular. Recuerda que los cambios morfológicos de la lesión reversible son la tumefacción celular y el cambio graso hepático. Te invito al Diálogo Socrático."
            "genetica" -> "Has seleccionado Tema 2: Genética Médica. El texto de la Dra. Lantigua es primordial. Enfócate en las aberraciones de tipo translocaciones robertsonianas."
            else -> "Has seleccionado Tema 3: Trastornos Hemodinámicos. Concéntrate en la triada de Virchow para la formación de trombos: lesión endotelial, estasis e hipercoagulabilidad."
        }
        setProfessorBubble(text)
        addXp(15)
        switchTab(MedStudyTab.SOCRATIC)
    }

    // Preset Data Helpers
    private fun getPresetDialogues(): List<SocraticDialogue> {
        return listOf(
            SocraticDialogue(
                id = "d1",
                profPrompt = "Colega del segundo año de medicina de la UCS: Analicemos un caso de necropsia. Un paciente fallece por infarto agudo de miocardio. Observas bajo el microscopio cardiomiocitos con núcleos ausentes (cariólisis), pero que conservan el contorno estructural de la célula sin digestión lisosómica inmediata. ¿Qué tipo de necrosis es y cómo la explicas según el Robbins?",
                options = listOf(
                    SocraticOption(
                        text = "Necrosis Coagulativa, por desnaturalización de proteínas estructurales y enzimáticas.",
                        response = "¡Excelente razonamiento! La necrosis coagulativa preserva la arquitectura tisular básica durante unos días porque no solo se denaturan las proteínas estructurales, sino también las enzimas lisosómicas inhibiendo la proteólisis. +20 XP",
                        isCorrect = true,
                        xpReward = 20
                    ),
                    SocraticOption(
                        text = "Necrosis Licuefactiva, producida por liberación masiva de hidrolasas neutrófilas.",
                        response = "Atención: la necrosis licuefactiva se caracteriza por autodigestión rápida que transforma el tejido en una masa líquida viscosa (típica de abscesos o infartos cerebrales). Revisa el Robbins. +10 XP",
                        isCorrect = false,
                        xpReward = 10
                    ),
                    SocraticOption(
                        text = "Necrosis Caseosa, típica de granulomas tuberculosos.",
                        response = "La necrosis caseosa muestra un aspecto blanquecino fraccionado tipo 'queso' con granulomas epitelioides típicos de Tuberculosis, no de infarto coronario. +10 XP",
                        isCorrect = false,
                        xpReward = 10
                    )
                )
            ),
            SocraticDialogue(
                id = "d2",
                profPrompt = "Imaginen a un lactante de 6 meses con retraso psicomotor evidente, organomegalia difusa, rigidez esquelética extrema y opacidad corneal progresiva. NEOPAT demuestra vacuolas vacías citoplasmáticas y cariotipo normal. ¿Qué defecto en el etiquetado golgiano de enzimas lisosomales es responsable de esta patología?",
                options = listOf(
                    SocraticOption(
                        text = "Falta de N-acetilglucosamina-1-fosfotransferasa en el aparato de Golgi para sintetizar Manosa-6-Fosfato (Enfermedad de Células I).",
                        response = "¡Extraordinario análisis! Al fallar la Manosa-6-Fosfato, las enzimas lisosomales son secretadas al espacio extracelular en lugar de ser dirigidas a los lisosomas. +25 XP",
                        isCorrect = true,
                        xpReward = 25
                    ),
                    SocraticOption(
                        text = "Déficit en la degradación de lípidos por falta de la enzima de Tay-Sachs.",
                        response = "Buen intento, pero en Tay-Sachs no hay visceromegalia severa ni opacidad corneal difusa con rigidez esquelética. ¡Sigue analizando el sistema golgiano!",
                        isCorrect = false,
                        xpReward = 10
                    )
                )
            )
        )
    }

    private fun getPresetPuzzles(): List<PuzzleItem> {
        return listOf(
            PuzzleItem(
                id = "lesion_reversible",
                organelleTitle = "Bomba Na+/K+ ATPasa alterada",
                organelleDesc = "Falta de ATP por hipoxia aguda celular",
                syndromeTitle = "Tumefacción Celular (Edema)",
                syndromeDesc = "Mecanismo básico de lesión celular reversible en NEOPAT",
                icon = "💧"
            ),
            PuzzleItem(
                id = "lisosoma",
                organelleTitle = "Lisosoma Defectuoso",
                organelleDesc = "Acumulación de lípidos por enzimas ausentes",
                syndromeTitle = "Enfermedad de Tay-Sachs",
                syndromeDesc = "Acumulación de gangliósidos GM2 por falta de Hexosaminidasa A",
                icon = "🗑️"
            ),
            PuzzleItem(
                id = "peroxisoma",
                organelleTitle = "Peroxisoma Ausente",
                organelleDesc = "Fallo en la beta-oxidación de ácidos grasos de cadena muy larga",
                syndromeTitle = "Síndrome de Zellweger",
                syndromeDesc = "Muerte neonatal precoz con hepatopatía y malformaciones",
                icon = "⚡"
            ),
            PuzzleItem(
                id = "reticulo_er",
                organelleTitle = "Retículo Endoplásmico Rugoso",
                organelleDesc = "Plegamiento proteico CFTR ΔF508 anómalo",
                syndromeTitle = "Fibrosis Quística",
                syndromeDesc = "Retención de la proteína CFTR misfoldeada en RER y degradación",
                icon = "🧱"
            ),
            PuzzleItem(
                id = "cromosomopatia",
                organelleTitle = "No Disyunción Meiótica",
                organelleDesc = "Error en la separación cromosómica meiótica materna",
                syndromeTitle = "Síndrome de Down (Trisomía 21)",
                syndromeDesc = "Aberración cromosómica numérica típica de Araceli Lantigua",
                icon = "🧬"
            )
        )
    }

    private fun getPresetExamQuestions(): List<ExamQuestion> {
        return listOf(
            ExamQuestion(
                id = "q1",
                tag = "Lesión Celular - Robbins",
                vignette = "Durante una clase práctica de Morfofisiopatología Humana I con el Software Educativo NEOPAT, se analiza una lámina histológica de tejido hepático que presenta vacuolas citoplasmáticas claras y bien delimitadas que desplazan el núcleo hacia la periferia en un paciente alcohólico crónico. ¿A qué alteración morfo-patológica celular corresponde y cuál es su etiología fundamental?",
                options = listOf(
                    ExamOption("A", "Necrosis coagulativa, por isquemia directa del parénquima."),
                    ExamOption("B", "Cambio Graso (Esteatosis), debido al incremento de la síntesis y reducción de la oxidación de triglicéridos.", isCorrect = true),
                    ExamOption("C", "Tumefacción celular, originada por la acumulación masiva de glucógeno citoplasmático."),
                    ExamOption("D", "Apoptosis inducida por estimulación del factor de necrosis tumoral (TNF).")
                ),
                explanation = "¡Brillante! El cambio graso hepático (esteatosis) se caracteriza por vacuolas de lípidos que desplazan el núcleo celular. Es un tipo de lesión reversible clásica provocada por el metabolismo de etanol que estimula la síntesis de triglicéridos e inhibe la beta-oxidación mitocondrial."
            ),
            ExamQuestion(
                id = "q2",
                tag = "Genética Médica - Lantigua",
                vignette = "Una lactante de 3 meses presenta llanto débil, hipotonía generalizada severa, cara redonda y aplanada, fisuras palpebrales oblicuas y un pliegue palmar transversal único. Basado en la literatura de la Dra. Araceli Lantigua Cruz, ¿cuál es el mecanismo genético patológico más frecuente detrás de este cuadro clínico y qué examen citogenético confirma el diagnóstico?",
                options = listOf(
                    ExamOption("A", "Mutación monogénica en el gen de la elastina; confirmado con PCR cuantitativo."),
                    ExamOption("B", "No disyunción meiótica materna (Trisomía 21 libre), confirmado mediante Cariotipo.", isCorrect = true),
                    ExamOption("C", "Translocación recíproca balanceada 14/21; confirmado por microarreglos."),
                    ExamOption("D", "Deleción cromosómica parcial del brazo corto del cromosoma 5; mediante Western Blot.")
                ),
                explanation = "¡Correcto! El Síndrome de Down es una cromosomopatía numérica clásica estudiada en el programa de la UCS. Más del 95% de los casos corresponden a trisomía libre originada por una no disyunción meiótica durante la ovogénesis, confirmándose de forma absoluta mediante cariotipo."
            ),
            ExamQuestion(
                id = "q3",
                tag = "Trastornos Hemodinámicos - Rubin",
                vignette = "Un paciente de 62 años con antecedentes de insuficiencia cardíaca congestiva descompensada fallece. En la autopsia, se analizan muestras del pulmón en NEOPAT revelando alvéolos ocupados por macrófagos cargados de hemosiderina ('células de la falla cardíaca'). ¿Cuál es el proceso patógeno hemodinámico subyacente según el Rubin?",
                options = listOf(
                    ExamOption("A", "Congestión pasiva pulmonar crónica, con hipertensión venosa y microhemorragias alveolares.", isCorrect = true),
                    ExamOption("B", "Embolia gaseosa masiva, originando infarto pulmonar rojo."),
                    ExamOption("C", "Trombosis arterial local in situ de origen infeccioso."),
                    ExamOption("D", "Edema no inflamatorio de causa hipoalbuminémica severa.")
                ),
                explanation = "¡Extraordinario análisis! La insuficiencia cardíaca izquierda genera hipertensión venosa retrógrada en los vasos pulmonares. La congestión pasiva crónica fuerza la salida de eritrocitos al espacio alveolar, donde son fagocitados por macrófagos, degradándose la hemoglobina en hemosiderina."
            )
        )
    }
}
