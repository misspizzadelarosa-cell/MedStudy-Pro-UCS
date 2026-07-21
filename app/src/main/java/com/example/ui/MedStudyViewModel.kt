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
    val professorBubbleText: String = "¡Saludos futuro médico de la patria! He adaptado nuestro temario de Morfofisiopatología Humana I a una simulación de alto rendimiento. Explora los planes de clase oficiales, lee los libros de texto o mide tu intelecto con exámenes oficiales.",
    
    // Bookshelf
    val academicBooks: List<AcademicBook> = emptyList(),
    
    // Official UCS Class Plans (From uploaded PDFs)
    val classPlans: List<UcsClassPlan> = emptyList(),
    
    // Custom uploaded study guides
    val customGuides: List<StudyGuideEntity> = emptyList(),
    
    // Dialog Selection State (To open full-screen readers on tap)
    val selectedBookForDialog: AcademicBook? = null,
    val selectedClassPlanForDialog: UcsClassPlan? = null,
    val selectedCustomGuideForDialog: StudyGuideEntity? = null,
    
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
        val books = getPresetBooks()
        val plans = getPresetClassPlans()
        val dialogues = getPresetDialogues()
        val puzzles = getPresetPuzzles()
        val exams = getPresetExamQuestions()

        _uiState.update { state ->
            state.copy(
                academicBooks = books,
                classPlans = plans,
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
            MedStudyTab.LIBRARY -> "Has entrado a la Biblioteca UCS. Toca cualquier libro o plan de clase oficial para abrir el lector interactivo."
            MedStudyTab.SOCRATIC -> "El método socrático desarrolla juicio clínico. Elige la mejor respuesta ante mis planteamientos."
            MedStudyTab.PUZZLE -> "¡Asociación histopatológica! Vincula cada causa u organela con su patología correspondiente."
            MedStudyTab.EXAM -> "Casos clínicos reales de los exámenes oficiales de Morfofisiopatología Humana I de la UCS. ¡Demuestra tu dominio!"
        }
        _uiState.update { it.copy(activeTab = tab, professorBubbleText = profBubble) }
    }

    // --- Dialog Triggers (Interactive Readers) ---
    fun openBookDialog(book: AcademicBook) {
        _uiState.update {
            it.copy(
                selectedBookForDialog = book,
                professorBubbleText = "Examinando: ${book.title}. Leamos sus fundamentos clave."
            )
        }
        addXp(10)
    }

    fun closeBookDialog() {
        _uiState.update { it.copy(selectedBookForDialog = null) }
    }

    fun openClassPlanDialog(plan: UcsClassPlan) {
        _uiState.update {
            it.copy(
                selectedClassPlanForDialog = plan,
                professorBubbleText = "Abriendo Plan de Clase: ${plan.title}. ¡Contenido evaluable de examen!"
            )
        }
        addXp(15)
    }

    fun closeClassPlanDialog() {
        _uiState.update { it.copy(selectedClassPlanForDialog = null) }
    }

    fun openCustomGuideDialog(guide: StudyGuideEntity) {
        _uiState.update {
            it.copy(
                selectedCustomGuideForDialog = guide,
                professorBubbleText = "Visualizando tu documento guardado: '${guide.title}'."
            )
        }
    }

    fun closeCustomGuideDialog() {
        _uiState.update { it.copy(selectedCustomGuideForDialog = null) }
    }

    fun deleteCustomGuide(guide: StudyGuideEntity) {
        viewModelScope.launch {
            dao.deleteGuide(guide)
            closeCustomGuideDialog()
            setProfessorBubble("Documento '${guide.title}' eliminado de tu biblioteca local.")
        }
    }

    fun addXp(amount: Int) {
        val currentXp = _uiState.value.userXp + amount
        var level = _uiState.value.userLevel
        var remainingXp = currentXp

        if (remainingXp >= 100) {
            remainingXp -= 100
            level++
            setProfessorBubble("🎉 ¡Felicidades colega! Has subido al Nivel $level. Tu rango es ahora '${rankTitles.getOrElse(level-1) { rankTitles.last() }}'.")
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

        currentMessages.add(
            ChatMessage(
                id = System.currentTimeMillis().toString(),
                sender = ChatSender.STUDENT,
                text = option.text
            )
        )

        currentMessages.add(
            ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                sender = ChatSender.PROFESSOR,
                text = "Retroalimentación del Prof. Gómez: ${option.response}"
            )
        )

        _uiState.update { it.copy(chatMessages = currentMessages) }
        addXp(option.xpReward)

        if (option.isCorrect) {
            viewModelScope.launch {
                val dialogues = getPresetDialogues()
                val nextIndex = (_uiState.value.activeSocraticIndex + 1) % dialogues.size
                val nextDialogue = dialogues[nextIndex]

                currentMessages.add(
                    ChatMessage(
                        id = (System.currentTimeMillis() + 2).toString(),
                        sender = ChatSender.PROFESSOR,
                        text = "Siguiente reto socrático [${nextDialogue.topicTitle}]: ${nextDialogue.profPrompt}"
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
                puzzleFeedback = "Organela/Mecanismo seleccionado. Ahora toca su patología o síndrome correspondiente a la derecha."
            )
        }
    }

    fun selectPuzzleSyndrome(id: String) {
        val selectedOrganelle = _uiState.value.selectedOrganelleId
        if (selectedOrganelle == null) {
            _uiState.update { it.copy(puzzleFeedback = "¡Primero selecciona una organela/causa a la izquierda!") }
            return
        }

        if (selectedOrganelle == id) {
            val updatedMatched = _uiState.value.matchedIds + id
            val matchesCount = updatedMatched.size
            _uiState.update {
                it.copy(
                    selectedOrganelleId = null,
                    matchedIds = updatedMatched,
                    puzzleFeedback = "¡Excelente asociación correcta! Has ganado +20 XP de Morfofisiopatología."
                )
            }
            addXp(20)

            if (matchesCount == _uiState.value.puzzleItems.size) {
                setProfessorBubble("¡Espectacular! Has completado el Rompecabezas Morfofisiopatológico con puntaje perfecto. Dominas la histopatología molecular.")
                addXp(50)
            }
        } else {
            _uiState.update {
                it.copy(
                    puzzleFeedback = "Asociación incorrecta. Revisa de nuevo la causa molecular y su cuadro clínico."
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
        setProfessorBubble("¡Excelente! He analizado tu documento local '$title'. Lo he guardado en tu biblioteca y adaptado el tutor AI para responder sobre él.")
        addXp(30)
    }

    // Preset Data extracted directly from PDF OCR content
    private fun getPresetClassPlans(): List<UcsClassPlan> {
        return listOf(
            UcsClassPlan(
                id = "sem1_tema1",
                week = 1,
                themeNumber = 1,
                title = "Procesos Patológicos y Métodos de Estudio",
                topicName = "Tema 1: Caracterización de los Procesos Patológicos",
                summaryText = "Definición de proceso patológico, clasificación por etiología (Genéticos vs Adquiridos). Núcleo básico: etiología, patogenia, cambios morfológicos, fisiopatología, semiología y patocronía.",
                objectives = listOf(
                    "Clasificar los procesos patológicos según su origen.",
                    "Explicar la génesis de los procesos genéticos y adquiridos.",
                    "Reconocer las influencias ambientales y factores de riesgo."
                ),
                cellularMechanisms = "Génesis por interacción de factores endógenos y exógenos. Alteraciones de mecanismos homeostáticos a nivel molecular y celular.",
                morphologicalFeatures = "Cambios moleculares, subcelulares, tisulares y de órganos que conducen a síntomas y signos clínicos.",
                diagnosticEvidence = "Integración de Laboratorio Clínico, Imagenología, Anatomía Patológica, Genética y Microbiología.",
                conclusions = listOf(
                    "Los procesos patológicos se dividen en genéticos (monogénicos, cromosómicos, multifactoriales) y adquiridos.",
                    "Los agentes adquiridos incluyen físicos, químicos, biológicos, hemodinámicos, inmunológicos y nutricionales.",
                    "El diagnóstico se apoya en evidencias clínicas y paraclínicas multidiciplinarias."
                ),
                examTips = "Pregunta de examen: Identificar la diferencia entre etiología (causa) y patogenia (mecanismo)."
            ),
            UcsClassPlan(
                id = "sem2_tema1",
                week = 2,
                themeNumber = 1,
                title = "Métodos de Estudio Generales y Muestras Biológicas",
                topicName = "Tema 1.4: Muestras y Diagnóstico de Laboratorio",
                summaryText = "Concepto de muestra biológica (sangre, orina, LCR, esputo, líquido amniótico, fragmentos de tejido). Requisitos de toma de muestra y conservación. Principios bioéticos y consentimiento informado.",
                objectives = listOf(
                    "Definir tipos de muestras biológicas y sus laboratorios de destino.",
                    "Explicar requisitos para una correcta toma de muestra (ayuno, medicamentos, conservación).",
                    "Clasificar métodos de anatomía patológica, imagenología y laboratorio clínico."
                ),
                cellularMechanisms = "Procesamiento tisular y celular para examen macroscópico y microscópico.",
                morphologicalFeatures = "Evaluación de atipias celulares, exudados, necrosis en cortes de biopsia, citología exfoliativa y piezas de autopsia.",
                diagnosticEvidence = "Biopsias (incisional, excisional, PAAF, congelación, ponche, legrado), Necropsia (clínica y médico-legal), Rx, Eco, TAC, RMN, Gammagrafía.",
                conclusions = listOf(
                    "De la correcta toma y conservación de muestra depende la confiabilidad diagnóstica.",
                    "El consentimiento informado y confidencialidad son exigencias bioéticas obligatorias.",
                    "La biopsia por congelación determina la conducta intraoperatoria rápida."
                ),
                examTips = "Diferenciar biopsia incisional (muestra parcial) de excisional (extirpación total con margen >1cm)."
            ),
            UcsClassPlan(
                id = "sem3_tema2",
                week = 3,
                themeNumber = 2,
                title = "Adaptación Celular y Lesión Hipóxica",
                topicName = "Tema 2.1 - 2.2: Adaptación y Daño Celular",
                summaryText = "Tipos de adaptación celular: Atrofia, Hipertrofia, Hiperplasia y Metaplasia. Etiopatogenia de la lesión celular reversible e irreversible por hipoxia e isquemia.",
                objectives = listOf(
                    "Identificar transformaciones celulares frente a estímulos nocivos.",
                    "Diferenciar adaptaciones fisiológicas y patológicas.",
                    "Explicar los mecanismos bioquímicos del daño hipóxico (depleción de ATP, fallo de bomba Na+/K+)."
                ),
                cellularMechanisms = "1) Caída de la fosforilación oxidativa. 2) Fallo de bomba Na+/K+ ATPasa con entrada de Na+ y agua. 3) Glucólisis anaeróbica y caída de pH. 4) Aumento de Ca2+ citosólico.",
                morphologicalFeatures = "Tumefacción celular, dilatación del RER, pérdida de microvellosidades, figuras de mielina.",
                diagnosticEvidence = "Rx simple para cardiomegalia, TAC para atrofia cerebral, Ecosonografía para hiperplasia prostática, Citología para metaplasia de cérvix.",
                conclusions = listOf(
                    "Las adaptaciones celulares son respuestas reversibles que mantienen la viabilidad.",
                    "La hipoxia es la causa más frecuente de daño celular en la práctica clínica.",
                    "Si la isquemia persiste, la lesión progresa de reversible a irreversible (necrosis)."
                ),
                examTips = "Falla de la bomba Na+/K+ causa tumefacción celular. Metaplasia más común: epitelio cilíndrico a escamoso en fumadores."
            ),
            UcsClassPlan(
                id = "sem4_tema2",
                week = 4,
                themeNumber = 2,
                title = "Alteraciones Morfofuncionales: Reversible, Necrosis y Apoptosis",
                topicName = "Tema 2.3 - 2.5: Lesión Reversible, Necrosis y Apoptosis",
                summaryText = "Lesión reversible: Tumefacción celular y Cambio Graso (Esteatosis). Muerte celular: Necrosis (Coagulativa, Licuefactiva, Caseosa, Enzimática de grasas) y Apoptosis (caspasas, Bcl-2, p53).",
                objectives = listOf(
                    "Identificar cambios macro y microscópicos de lesión reversible e irreversible.",
                    "Diferenciar los 4 patrones principales de necrosis tisular.",
                    "Describir la morfología y vías de la apoptosis."
                ),
                cellularMechanisms = "Desnaturalización de proteínas vs digestión enzimática (autólisis y heterólisis). En apoptosis: activación de caspasas 8, 9 y 3, liberación de citocromo c.",
                morphologicalFeatures = "Necrosis: picnosis, cariólisis, cariorrexis, eosinofilia. Apoptosis: condensación de cromatina, vesiculación y cuerpos apoptóticos sin inflamación.",
                diagnosticEvidence = "Transaminasas AST/ALT elevadas en esteatosis; CPK-MB y troponinas en IAM; Amilasa/Lipasa en pancreatitis; Biopsia e imágenes.",
                conclusions = listOf(
                    "La necrosis siempre es patológica y se acompaña de inflamación local.",
                    "La apoptosis elimina células no deseadas sin romper la membrana ni causar inflamación.",
                    "El tejido de amputación en pancreatitis presenta saponificación de las grasas (jabones de calcio)."
                ),
                examTips = "Infarto de miocardio = Necrosis coagulativa (imagen en lápida sepulcral). Infarto cerebral = Necrosis licuefactiva."
            ),
            UcsClassPlan(
                id = "sem5_tema2",
                week = 5,
                themeNumber = 2,
                title = "Acumulaciones Intracelulares, Aterosclerosis y Diabetes Mellitus",
                topicName = "Tema 2.6 - 2.9: Acumulaciones, Aterosclerosis, Diabetes y Calcificaciones",
                summaryText = "Acumulaciones de agua, grasa, glucógeno, pigmentos (antracosis, lipofuscina, hemosiderina). Aterosclerosis (placa de ateroma G1, G2, G3). Diabetes Mellitus (macroangiopatía, microangiopatía, Kimmelstiel-Wilson). Calcificación Distrófica vs Metastásica.",
                objectives = listOf(
                    "Clasificar acumulaciones intracelulares según origen.",
                    "Describir la etiopatogenia y complicaciones de la placa de ateroma.",
                    "Diferenciar calcificación distrófica (en tejido muerto, calcio normal) de metastásica (en tejido sano, hipercalcemia)."
                ),
                cellularMechanisms = "Acumulación de colesterol en macrófagos (células espumosas) en la íntima arterial. Microangiopatía por engrosamiento de membrana basal capilar en diabetes.",
                morphologicalFeatures = "Placa de ateroma con núcleo lipídico y cubierta fibrosa. Glomeruloesclerosis nodular de Kimmelstiel-Wilson en riñón diabético.",
                diagnosticEvidence = "Lipidograma completo, Glicemia, HbA1c, Arteriografía, AngioTAC, Ecosonografía Doppler.",
                conclusions = listOf(
                    "La aterosclerosis afecta arterias elásticas y musculares de gran y mediano calibre.",
                    "La calcificación distrófica ocurre en tejidos necróticos con calcio sérico normal.",
                    "La calcificación metastásica ocurre en tejidos sanos por hipercalcemia (ej. hiperparatiroidismo)."
                ),
                examTips = "Kimmelstiel-Wilson = patognomónico de Diabetes Mellitus. Lipofuscina = pigmento de desgaste por envejecimiento."
            ),
            UcsClassPlan(
                id = "sem6_tema3",
                week = 6,
                themeNumber = 3,
                title = "Respuesta Inflamatoria Aguda y Crónica",
                topicName = "Tema 3: Inflamación, Mediadores y Patrones Morfológicos",
                summaryText = "Respuesta inflamatoria vascular y celular. Acontecimientos leucocitarios: marginación, rodamiento, adhesión/pavimentación, migración, quimiotaxis, fagocitosis. Signos cardinales (calor, rubor, edema, dolor, impotencia funcional). Patrones morfológicos.",
                objectives = listOf(
                    "Enumerar causas de inflamación aguda y crónica.",
                    "Explicar las etapas de la extravasación leucocitaria y fagocitosis.",
                    "Describir patrones morfológicos (seroso, fibrinoso, supurativo, granulomatoso)."
                ),
                cellularMechanisms = "Mediadores químicos: Histamina (vasodilatación y permeabilidad), Prostaglandinas, Citoquinas (IL-1, TNF), Sistema del Complemento.",
                morphologicalFeatures = "Exudado rico en PMN neutrófilos en inflamación aguda. Linfocitos, macrófagos, células epitelioides y células gigantes en crónica.",
                diagnosticEvidence = "Leucograma (neutrofilia en bacterias, linfocitosis en virus, eosinofilia en parásitos), VSG elevada, Proteína C Reactiva (PCR).",
                conclusions = listOf(
                    "La histamina es el primer mediador de la fase inmediata de inflamación aguda.",
                    "La inflamación granulomatosa se caracteriza por células epitelioides y células gigantes (ej. Tuberculosis).",
                    "Los reactantes de fase aguda como la PCR se incrementan significativamente en suero."
                ),
                examTips = "Pasos leucocitarios en orden: Marginación -> Rodamiento -> Adhesión -> Migración -> Quimiotaxis -> Fagocitosis."
            ),
            UcsClassPlan(
                id = "sem7_tema4",
                week = 7,
                themeNumber = 4,
                title = "Reparación Tisular: Regeneración y Cicatrización",
                topicName = "Tema 4: Reparación Tisular y Cicatrización",
                summaryText = "Regeneración parenquimatosa vs Cicatrización. Clasificación celular: Lábiles (continuas), Estables (quiescentes), Permanentes (no se dividen). Tejido de granulación, angiogénesis (VEGF), matriz extracelular (colágeno, TGF-β, MMPs). Cicatrización por 1ra y 2da intención.",
                objectives = listOf(
                    "Diferenciar regeneración de cicatrización.",
                    "Describir los 4 pasos del proceso de cicatrización.",
                    "Comparar la cicatrización por primera y segunda intención.",
                    "Enumerar complicaciones (dehiscencia, queloides, contracturas)."
                ),
                cellularMechanisms = "Proliferación de fibroblastos estimulada por TGF-β. Angiogénesis mediada por VEGF y angiopoyetinas. Remodelado por Metaloproteinasas de Matriz (MMPs).",
                morphologicalFeatures = "Tejido de granulación: vasos de nueva formación, fibroblastos, edema y macrófagos. Contracción de la herida por miofibroblastos.",
                diagnosticEvidence = "Evaluación clínica de bordes de herida, ecografía y estudios anatomopatológicos.",
                conclusions = listOf(
                    "Las células permanentes (neuronas, cardiomiocitos) se reparan exclusivamente por cicatrización/fibrosis.",
                    "La cicatrización por 2da intención presenta mayor pérdida tisular, más tejido de granulación y contracción.",
                    "La deficiencia de Vitamina C y proteínas inhibe la síntesis de colágeno y retrasa la curación."
                ),
                examTips = "Células lábiles = epitelio/piel. Estables = hepatocitos/túbulos renales. Permanentes = neuronas/corazón."
            ),
            UcsClassPlan(
                id = "sem8_tema5",
                week = 8,
                themeNumber = 5,
                title = "Transmisión de Simples Mutaciones (Enfermedades Monogénicas)",
                topicName = "Tema 5.1: Herencia Mendelianasy Fenómenos Biológicos",
                summaryText = "Patrones de herencia mendeliana: Autosómica Dominante (Polidactilia), Autosómica Recesiva (Albinismo, Sicklemia, Fibrosis Quística), Dominante ligada al X (Raquitismo hipofosfatémico), Recesiva ligada al X (Hemofilia A). Confección de genealogías.",
                objectives = listOf(
                    "Interpretar los 4 patrones de herencia mendeliana en árboles genealógicos.",
                    "Identificar fenómenos que dificultan el análisis (penetrancia reducida, expresividad variable, pleiotropía).",
                    "Explicar el concepto de ligamiento genético y frecuencia de recombinación."
                ),
                cellularMechanisms = "Mutaciones en genes únicos. Inactivación del X (Hipótesis de Lyon). Pleiotropía: un gen mutado causa múltiples efectos en órganos variados (Síndrome de Marfan).",
                morphologicalFeatures = "Facies y alteraciones en múltiples sistemas corporales.",
                diagnosticEvidence = "Electroforesis de Hemoglobina (HbSS en sicklemia), PCR, Southern Blot, Secuenciación de ADN.",
                conclusions = listOf(
                    "En herencia recesiva ligada al X, los hombres enfermos tienen 100% de hijas portadoras y 100% de hijos sanos.",
                    "La consanguinidad aumenta la probabilidad de afecciones autosómicas recesivas.",
                    "La hemofilia A se debe a deficiencia del Factor VIII de coagulación."
                ),
                examTips = "Padres sanos con hijo enfermo = herencia autosómica recesiva (ambos padres son heterocigotos Aa)."
            ),
            UcsClassPlan(
                id = "sem9_tema5",
                week = 9,
                themeNumber = 5,
                title = "Interferencias Biológicas y Bases Bioquímicas",
                topicName = "Tema 5.1.4 - 5.1.5: Herencia No Clásica y Proteínas Afectadas",
                summaryText = "Herencia mitocondrial (sólo transmisión materna, Neuropatía de Leber). Mutaciones dinámicas (expansión de tripletes CGG, Síndrome Frágil X). Impronta genómica (Angelman vs Prader-Willi en 15q11-13). Disomía uniparental y Mosaicismo. Defectos según clase proteica.",
                objectives = listOf(
                    "Explicar patrones de herencia no mendeliana.",
                    "Comprender la diferencia entre homoplasmia y heteroplasmia mitocondrial.",
                    "Relacionar mutaciones proteicas con sus patologías (PKU, Duchenne, Hemofilia, FGFR3)."
                ),
                cellularMechanisms = "Impronta: inactivación epigenética selectiva del alelo materno o paterno. Mutaciones dinámicas: fenómeno de anticipación por expansión del triplete.",
                morphologicalFeatures = "Frágil X: fenotipo Martin-Bell (orejas grandes, mandíbula prominente, macroorquidismo). Duchenne: hipertrofia de pantorrillas y atrofia muscular.",
                diagnosticEvidence = "Estudios de genética molecular directa e indirecta, cuantificación de enzimas séricas, inmunofluorescencia.",
                conclusions = listOf(
                    "En la herencia mitocondrial, los varones enfermos NUNCA transmiten la enfermedad a sus hijos.",
                    "Angelman se expresa si la deleción 15q11-13 ocurre en el cromosoma materno; Prader-Willi si ocurre en el paterno.",
                    "La Fenilcetonuria (PKU) se debe a deficiencia de la enzima fenilalanina hidroxilasa (PAH)."
                ),
                examTips = "Frágil X = expansión de triplete CGG > 200. Transmisión mitocondrial = estricta vía materna."
            ),
            UcsClassPlan(
                id = "sem10_tema5",
                week = 10,
                themeNumber = 5,
                title = "Aberraciones Cromosómicas Numéricas y Estructurales",
                topicName = "Tema 5.2: Citogenética, Cariotipo y Cromatina Sexual",
                summaryText = "Aberraciones numéricas: Aneuploidías (Trisomía 21 Down, Trisomía 13 Patau, Trisomía 18 Edwards, Monosomía X Turner 45,X, Klinefelter 47,XXY) por no disyunción meiótica. Aberraciones estructurales balanceadas vs no balanceadas. Cariotipo y Cuerpos de Barr.",
                objectives = listOf(
                    "Explicar la etiología de las aberraciones cromosómicas.",
                    "Identificar las fórmulas cromosómicas internacionales.",
                    "Interpretar el estudio de la cromatina sexual (Cuerpos de Barr = Número de cromosomas X - 1)."
                ),
                cellularMechanisms = "Fallo en la separación cromosómica durante la anafase I o II meiótica (no disyunción). Anafase retardada.",
                morphologicalFeatures = "Down: fisuras palpebrales mongoloides, pliegue palmar único, hipotonía. Turner: cuello palmeado, baja talla, amenorrea primaria.",
                diagnosticEvidence = "Cariotipo con bandeado G, Cromatina sexual (raspado de mucosa oral), Ecosonografía prenatal (hidrops fetal).",
                conclusions = listOf(
                    "El número de Cuerpos de Barr en una célula equivale a: (Total de cromosomas X - 1).",
                    "Mujer normal (46,XX) = 1 Cuerpo de Barr; Síndrome de Turner (45,X) = 0 Cuerpos de Barr; Klinefelter (47,XXY) = 1 Cuerpo de Barr.",
                    "La translocación robertsoniana 14/21 entre acrocéntricos puede causar Down hereditario."
                ),
                examTips = "Cuerpos de Barr en Klinefelter (47,XXY) = 1. Cuerpos de Barr en Turner (45,X) = 0."
            ),
            UcsClassPlan(
                id = "sem11_tema5",
                week = 11,
                themeNumber = 5,
                title = "Marcadores Genéticos y Genética Poblacional",
                topicName = "Tema 5.3 - 5.4: Grupos Sanguíneos, HLA y Ley de Hardy-Weinberg",
                summaryText = "Marcadores genéticos: Sistemas ABO, Rh, MN, Complejo Mayor de Histocompatibilidad (MHC/HLA) y RFLP. Genética Poblacional y Ley de Hardy-Weinberg: p + q = 1, p² + 2pq + q² = 1.",
                objectives = listOf(
                    "Identificar características de los marcadores sanguíneos ABO y Rh.",
                    "Explicar la importancia del HLA en trasplantes de órganos.",
                    "Calcular frecuencias genotípicas y génicas en poblaciones en equilibrio."
                ),
                cellularMechanisms = "Codominancia de alelos A y B sobre el alelo recesivo O. Expresión de antígenos en membrana eritrocitaria.",
                morphologicalFeatures = "Hemólisis intravascular por conflicto ABO o Rh (enfermedad hemolítica del recién nacido).",
                diagnosticEvidence = "Titulaciones de anticuerpos, pruebas de hemoaglutinación, tipaje HLA, electroforesis RFLP.",
                conclusions = listOf(
                    "Padres de grupos A (genotipo AO) y B (genotipo BO) pueden tener un hijo de grupo O (genotipo OO).",
                    "La Ley de Hardy-Weinberg presupone matrimonios al azar, población grande y ausencia de mutación/selección.",
                    "p² representa homocigotos dominantes (AA), 2pq heterocigotos (Aa), y q² homocigotos recesivos (aa)."
                ),
                examTips = "Si en una población 700 de 1000 son Rh positivos, la frecuencia fenotípica es del 70% (0.70)."
            ),
            UcsClassPlan(
                id = "sem12_tema5",
                week = 12,
                themeNumber = 5,
                title = "Herencia Multifactorial y Asesoramiento Genético",
                topicName = "Tema 5.5 - 5.6: Defectos Congénitos y Asesoramiento Genético",
                summaryText = "Herencia multifactorial en enfermedades comunes del adulto (HTA, Diabetes, Asma, Cáncer) y defectos congénitos (Malformaciones, Deformaciones, Disrupciones, Displasias). Diagnóstico prenatal e implicaciones éticas.",
                objectives = listOf(
                    "Diferenciar malformación, deformación, disrupción y displasia.",
                    "Explicar el concepto de heredabilidad y predisposición genética.",
                    "Clasificar el riesgo genético (Mendeliano vs Empírico; Alto, Moderado, Bajo).",
                    "Conocer métodos prenatales invasivos y no invasivos."
                ),
                cellularMechanisms = "Efecto aditivo de poligenes interactuando con factores ambientales (dieta, estrés, toxinas).",
                morphologicalFeatures = "Labio leporino/paladar hendido, defectos del tubo neural (anencefalia, mielomeningocele), amputaciones por bridas amnióticas.",
                diagnosticEvidence = "Ecosonografía prenatal de alta resolución, Amniocentesis, Biopsia de vellosidades coriónicas, Cordocentesis.",
                conclusions = listOf(
                    "Malformación = defecto genético estructural primario (ej. labio leporino).",
                    "Disrupción = ruptura secundaria de tejidos bien formados (ej. bridas amnióticas).",
                    "Deformación = fuerza mecánica anómala sobre tejido normal (ej. pie zambo por oligohidramnios).",
                    "El asesoramiento genético debe ser no directivo, respetando la autonomía del paciente."
                ),
                examTips = "Riesgo mendeliano = predicción teórica (50% en autosómica dominante). Riesgo empírico = basado en datos estadísticos."
            )
        )
    }

    private fun getPresetBooks(): List<AcademicBook> {
        return listOf(
            AcademicBook(
                id = "robbins",
                title = "Robbins y Cotran",
                author = "Patología Estructural y Funcional (10ma Ed.)",
                category = "Robbins 10ma Ed.",
                tag = "Pilar Fundamental",
                colorStartHex = 0xFFBE123C,
                colorEndHex = 0xFF881337,
                summary = "El texto de referencia mundial para la anatomía patológica. Aborda en profundidad la lesión celular, respuestas inflamatorias, reparación tisular, inmunopatología, neoplasias y enfermedades genéticas.",
                keyHighlights = listOf(
                    "Capítulo 1: Lesión celular, necrosis (coagulativa, licuefactiva, caseosa, grasa) y apoptosis.",
                    "Capítulo 2: Inflamación aguda y crónica, fenómenos leucocitarios y mediadores químicos.",
                    "Capítulo 3: Reparación tisular, regeneración y cicatrización por 1ra y 2da intención.",
                    "Capítulo 5: Trastornos genéticos, enfermedades monogénicas y cromosómicas."
                ),
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
                summary = "Obra insigne de la formación médica en el programa PNFMIC / UCS. Detalla de forma metódica las leyes mendelianas, el análisis de pedigríes, citogenética humana, cromosomopatías, herencia no clásica y genética poblacional.",
                keyHighlights = listOf(
                    "Capítulo 5: Transmisión de simples mutaciones y criterios de árboles genealógicos.",
                    "Capítulo 8: Aberraciones cromosómicas numéricas y estructurales.",
                    "Capítulo 10: Interferencias biológicas, herencia mitocondrial, impronta genómica y mutaciones dinámicas.",
                    "Capítulo 12: Genética poblacional y equilibrio de Hardy-Weinberg."
                ),
                profTip = "De la Dra. Lantigua, no olviden repasar la Herencia Multifactorial y las Aberraciones Cromosómicas. Los ejercicios de Cuerpos de Barr = (X - 1) son evaluados sistemáticamente."
            ),
            AcademicBook(
                id = "rubin",
                title = "Rubin y Strayer",
                author = "Fundamentos Clinicopatológicos",
                category = "Rubin Patología",
                tag = "Clínico-Morfológico",
                colorStartHex = 0xFFD97706,
                colorEndHex = 0xFF92400E,
                summary = "Texto enfocado en la correlación anatomo-clínica. Explica con claridad la fisiopatología del shock, trastornos hemodinámicos, congestión pasiva y la cascada de citoquinas en la respuesta sistémica de fase aguda.",
                keyHighlights = listOf(
                    "Fisiopatología de la congestión pasiva hepática y pulmonar.",
                    "Mecanismos de edema inflamatorio vs no inflamatorio (trasudado vs exudado).",
                    "Triada de Virchow en la patogenia de los trombos vasculares.",
                    "Shock endotóxico y fallo multiorgánico."
                ),
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
                summary = "Colección oficial de láminas virtuales e imágenes histopatológicas utilizadas en las clases prácticas de Morfofisiopatología Humana I en la UCS. Incluye cortes de necropsias y biopsias patológicas.",
                keyHighlights = listOf(
                    "Lámina de Necrosis Caseosa Tuberculosa con células de Langhans.",
                    "Lámina de Esteatosis Hepática con vacuolas lipídicas periféricas.",
                    "Lámina de Glomeruloesclerosis Nodular de Kimmelstiel-Wilson.",
                    "Lámina de Pericarditis Fibrinosa 'en pan con mantequilla'."
                ),
                profTip = "Las guías prácticas del NEOPAT les exigen identificar muestras de inflamación granulomatosa crónico-caseosa. Busquen siempre las células gigantes multinucleadas de tipo Langhans."
            )
        )
    }

    private fun getPresetDialogues(): List<SocraticDialogue> {
        return listOf(
            SocraticDialogue(
                id = "d1",
                topicTitle = "Tema 2: Lesión Celular e Infarto",
                profPrompt = "Colega del segundo año de medicina de la UCS: Analicemos un caso de necropsia. Un paciente fallece por infarto agudo de miocardio. Observas bajo el microscopio cardiomiocitos con núcleos ausentes (cariólisis), pero que conservan el contorno estructural de la célula sin digestión lisosómica inmediata. ¿Qué tipo de necrosis es y cómo la explicas según el Robbins?",
                options = listOf(
                    SocraticOption(
                        text = "Necrosis Coagulativa, por desnaturalización de proteínas estructurales y enzimáticas.",
                        response = "¡Excelente razonamiento! La necrosis coagulativa preserva la arquitectura tisular básica durante unos días porque no solo se denaturan las proteínas estructurales, sino también las enzimas lisosómicas inhibiendo la proteólisis rápida. +20 XP",
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
                topicTitle = "Tema 5: Aberraciones Cromosómicas",
                profPrompt = "Analicemos un informe de citogenética de un recién nacido masculino con baja talla, genitales ambiguos e hipotonía. El recuento de Cuerpos de Barr en células de mucosa oral es CERO (0). Según la regla de Cuerpos de Barr = (X - 1) explicada por la Dra. Lantigua, ¿cuál de las siguientes fórmulas cromosómicas corresponde al diagnóstico?",
                options = listOf(
                    SocraticOption(
                        text = "Fórmula 45,X (Síndrome de Turner).",
                        response = "¡Correcto! En el Síndrome de Turner (45,X), al haber un solo cromosoma X, el cálculo de Cuerpos de Barr es (1 - 1) = 0. +25 XP",
                        isCorrect = true,
                        xpReward = 25
                    ),
                    SocraticOption(
                        text = "Fórmula 47,XXY (Síndrome de Klinefelter).",
                        response = "En Klinefelter (47,XXY) hay 2 cromosomas X, por lo que se observa (2 - 1) = 1 Cuerpo de Barr. Revisa la lección de citogenética. +10 XP",
                        isCorrect = false,
                        xpReward = 10
                    )
                )
            ),
            SocraticDialogue(
                id = "d3",
                topicTitle = "Tema 3: Inflamación y Eventos Leucocitarios",
                profPrompt = "Durante un proceso de inflamación aguda por apendicitis purulenta, los leucocitos deben salir de los vasos sanguíneos para llegar al foco infeccioso. ¿Cuál es la secuencia correcta de acontecimientos leucocitarios?",
                options = listOf(
                    SocraticOption(
                        text = "Marginación -> Rodamiento -> Adhesión -> Migración (Diapédesis) -> Quimiotaxis -> Fagocitosis.",
                        response = "¡Impecable! Esta es la secuencia cronológica biológica exacta demostrada en el Plan de Clase de la Semana 6. +25 XP",
                        isCorrect = true,
                        xpReward = 25
                    ),
                    SocraticOption(
                        text = "Quimiotaxis -> Marginación -> Fagocitosis -> Rodamiento -> Adhesión.",
                        response = "Incorrecto. Los leucocitos primero se desplazan a la periferia vascular (marginación) antes de realizar quimiotaxis hacia el gradiente químico. +10 XP",
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
                organelleTitle = "Fallo de Bomba Na+/K+ ATPasa",
                organelleDesc = "Caída de ATP por hipoxia aguda en parénquima",
                syndromeTitle = "Tumefacción Celular (Edema)",
                syndromeDesc = "Mecanismo básico de lesión celular reversible",
                icon = "💧"
            ),
            PuzzleItem(
                id = "lisosoma",
                organelleTitle = "Lisosoma Defectuoso",
                organelleDesc = "Acumulación de gangliósidos por falta de Hexosaminidasa A",
                syndromeTitle = "Enfermedad de Tay-Sachs",
                syndromeDesc = "Enfermedad por almacenamiento lisosómico",
                icon = "🗑️"
            ),
            PuzzleItem(
                id = "pancreas_grasa",
                organelleTitle = "Liberación de Lipasas Pancreáticas",
                organelleDesc = "Acción de enzimas líticas sobre tejido adiposo",
                syndromeTitle = "Saponificación Grasa (Jabones de Ca)",
                syndromeDesc = "Necrosis enzimática de las grasas en pancreatitis",
                icon = "🧀"
            ),
            PuzzleItem(
                id = "reticulo_er",
                organelleTitle = "Proteína CFTR Misfoldeada en RER",
                organelleDesc = "Mutación en gen de canal de cloro",
                syndromeTitle = "Fibrosis Quística",
                syndromeDesc = "Moco espeso en páncreas, pulmón e intestinos",
                icon = "🧱"
            ),
            PuzzleItem(
                id = "cromosomopatia",
                organelleTitle = "No Disyunción Meiótica Materna",
                organelleDesc = "Fallo de separación cromosómica en ovogénesis I",
                syndromeTitle = "Síndrome de Down (Trisomía 21)",
                syndromeDesc = "Aneuploidía numérica clásica (47,XX,+21 o 47,XY,+21)",
                icon = "🧬"
            )
        )
    }

    private fun getPresetExamQuestions(): List<ExamQuestion> {
        return listOf(
            ExamQuestion(
                id = "bateria6_q1",
                tag = "Examen Oficial 2024 - Batería 6",
                vignette = "El aumento de tamaño de la próstata por incremento del número de sus células producto a una disfunción hormonal androgénica es una adaptación celular patológica conocida como:",
                options = listOf(
                    ExamOption("A", "Hipertrofia prostática."),
                    ExamOption("B", "Hiperplasia prostática.", isCorrect = true),
                    ExamOption("C", "Metaplasia prostática."),
                    ExamOption("D", "Atrofia prostática.")
                ),
                explanation = "¡Correcto! El aumento en el NÚMERO de células debido a estimulación hormonal es una Hiperplasia. La hipertrofia se refiere al aumento en el TAMAÑO de las células."
            ),
            ExamQuestion(
                id = "bateria6_q2",
                tag = "Examen Oficial 2024 - Batería 6",
                vignette = "En la cervicitis crónica se produce una sustitución del epitelio cilíndrico por epitelio escamoso, más resistente al ambiente adverso. Esta adaptación celular se conoce como:",
                options = listOf(
                    ExamOption("A", "Hiperplasia escamosa."),
                    ExamOption("B", "Atrofia cervical."),
                    ExamOption("C", "Metaplasia escamosa.", isCorrect = true),
                    ExamOption("D", "Dysplasia escamosa.")
                ),
                explanation = "¡Exacto! La sustitución de un tipo de epitelio adulto (cilíndrico) por otro tipo de epitelio adulto más resistente (escamoso) se denomina Metaplasia."
            ),
            ExamQuestion(
                id = "bateriaA_q1",
                tag = "Examen Oficial 2016 - Batería A",
                vignette = "Paciente que ingresa con diagnóstico clínico de infarto agudo de miocardio de cara anterior. Fallece y el patólogo reporta: corazón aumentado de tamaño a expensas de incremento del grosor de la pared del ventrículo izquierdo, y oclusión total de la arteria coronaria derecha. ¿Qué tipo de necrosis sufrió la cara anterior del miocardio y cuál fue su causa?",
                options = listOf(
                    ExamOption("A", "Necrosis licuefactiva por infección bacteriana."),
                    ExamOption("B", "Necrosis coagulativa por isquemia secundaria a oclusión coronaria.", isCorrect = true),
                    ExamOption("C", "Necrosis caseosa por Mycobacterium tuberculosis."),
                    ExamOption("D", "Necrosis enzimática por activación de lipasas.")
                ),
                explanation = "¡Perfecto! En el infarto agudo de miocardio, la oclusión coronaria provoca isquemia severa que desencadena Necrosis Coagulativa con conservación temporal de los contornos celulares (imágenes en lápida sepulcral)."
            ),
            ExamQuestion(
                id = "bateria3_q1",
                tag = "Examen Oficial 2020 - Batería 3",
                vignette = "Paciente masculino de 27 años con dolor en fosa ilíaca derecha, fiebre de 38°C y leucocitosis con neutrofilia. Operado de urgencia con diagnóstico anatomopatológico de apendicitis aguda flemonosa. ¿Cuál es el patrón morfológico de la inflamación y la célula inflamatoria predominante?",
                options = listOf(
                    ExamOption("A", "Inflamación granulomatosa con abundantes linfocitos."),
                    ExamOption("B", "Inflamación supurativa o purulenta con predominio de polimorfonucleares neutrófilos.", isCorrect = true),
                    ExamOption("C", "Inflamación serosa con abundantes eosinófilos."),
                    ExamOption("D", "Inflamación mononuclear con necrosis caseosa.")
                ),
                explanation = "¡Excelente! La apendicitis aguda bacteriana presenta exudado purulento rico en polimorfonucleares neutrófilos y detritus celulares."
            ),
            ExamQuestion(
                id = "bateria2_q1",
                tag = "Examen Oficial 2010 - Batería 2",
                vignette = "Paciente de 48 años con dolor abdominal y vómitos. Exámenes revelan elevación de lipasas pancreáticas e imágenes de ecosonografía con áreas focales blanquecinas y firmes en tejido adiposo pancreático. ¿A qué tipo de necrosis corresponde?",
                options = listOf(
                    ExamOption("A", "Necrosis coagulativa."),
                    ExamOption("B", "Necrosis colicuativa."),
                    ExamOption("C", "Necrosis enzimática de las grasas.", isCorrect = true),
                    ExamOption("D", "Necrosis caseosa.")
                ),
                explanation = "¡Muy bien! En la pancreatitis aguda, las lipasas digestivas digieren los adipocitos, liberando ácidos grasos que se combinan con calcio formando focos blanquecinos calcáreos de saponificación (necrosis enzimática de las grasas)."
            )
        )
    }
}
