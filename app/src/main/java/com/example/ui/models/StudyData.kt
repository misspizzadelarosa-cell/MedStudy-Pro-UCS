package com.example.ui.models

data class SocraticOption(
    val text: String,
    val response: String,
    val isCorrect: Boolean = false,
    val xpReward: Int = 10
)

data class SocraticDialogue(
    val id: String,
    val topicTitle: String,
    val profPrompt: String,
    val options: List<SocraticOption>
)

data class ExamOption(
    val key: String,
    val text: String,
    val isCorrect: Boolean = false
)

data class ExamQuestion(
    val id: String,
    val tag: String,
    val vignette: String,
    val options: List<ExamOption>,
    val explanation: String
)

data class ConceptDetail(
    val term: String,
    val definition: String,
    val clinicalRelevance: String,
    val ucsExamImportance: String
)

data class VideoExplanation(
    val videoTitle: String,
    val youtubeId: String,
    val channelOrAuthor: String,
    val duration: String,
    val keyTopicsInVideo: List<String>,
    val aiVideoSummary: String
)

data class AcademicBook(
    val id: String,
    val title: String,
    val author: String,
    val category: String,
    val tag: String,
    val colorStartHex: Long,
    val colorEndHex: Long,
    val summary: String,
    val keyHighlights: List<String>,
    val profTip: String,
    val fullChapterContent: String = "",
    val allKeyConcepts: List<ConceptDetail> = emptyList(),
    val videoExplanation: VideoExplanation? = null
)

data class UcsClassPlan(
    val id: String,
    val week: Int,
    val themeNumber: Int,
    val title: String,
    val topicName: String,
    val method: String = "Expositivo, Ilustrativo",
    val summaryText: String,
    val objectives: List<String>,
    val cellularMechanisms: String,
    val morphologicalFeatures: String,
    val diagnosticEvidence: String,
    val conclusions: List<String>,
    val examTips: String,
    val keywords: List<String> = emptyList(),
    val allKeyConcepts: List<ConceptDetail> = emptyList(),
    val videoExplanation: VideoExplanation? = null
)

data class PuzzleItem(
    val id: String,
    val organelleTitle: String,
    val organelleDesc: String,
    val syndromeTitle: String,
    val syndromeDesc: String,
    val icon: String
)

enum class ChatSender {
    PROFESSOR,
    STUDENT
}

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val isAiGenerated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

