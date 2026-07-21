package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.StudyGuideEntity
import com.example.ui.MedStudyTab
import com.example.ui.MedStudyUiState
import com.example.ui.MedStudyViewModel
import com.example.ui.models.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MedStudyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    // Modals / Dialogs for active selections
    state.selectedBookForDialog?.let { book ->
        AcademicBookReaderDialog(
            book = book,
            onDismiss = { viewModel.closeBookDialog() }
        )
    }

    state.selectedClassPlanForDialog?.let { plan ->
        UcsClassPlanReaderDialog(
            plan = plan,
            onDismiss = { viewModel.closeClassPlanDialog() },
            onStartSocraticCase = {
                viewModel.closeClassPlanDialog()
                viewModel.switchTab(MedStudyTab.SOCRATIC)
            }
        )
    }

    state.selectedCustomGuideForDialog?.let { guide ->
        CustomGuideReaderDialog(
            guide = guide,
            onDismiss = { viewModel.closeCustomGuideDialog() },
            onDelete = { viewModel.deleteCustomGuide(guide) },
            onAskAiAboutGuide = {
                viewModel.closeCustomGuideDialog()
                viewModel.switchTab(MedStudyTab.SOCRATIC)
                viewModel.askCustomQuestionToProfessor("Explícame en detalle y con caso clínico los puntos clave de mi guía: ${guide.title}")
            }
        )
    }

    Scaffold(
        topBar = {
            HeaderGamificationBar(state = state)
        },
        containerColor = VibrantBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Professor Gomez Banner
            ProfessorGomezBanner(state = state)

            // Navigation Tabs
            TabRowSection(
                activeTab = state.activeTab,
                onTabSelected = { viewModel.switchTab(it) }
            )

            // Content Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (state.activeTab) {
                    MedStudyTab.LIBRARY -> LibraryTabContent(viewModel = viewModel, state = state)
                    MedStudyTab.SOCRATIC -> SocraticChatTabContent(viewModel = viewModel, state = state)
                    MedStudyTab.PUZZLE -> PathologyPuzzleTabContent(viewModel = viewModel, state = state)
                    MedStudyTab.EXAM -> ExamSimulatorTabContent(viewModel = viewModel, state = state)
                }
            }
        }
    }
}

@Composable
fun HeaderGamificationBar(state: MedStudyUiState) {
    Surface(
        color = VibrantPrimary,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(VibrantPrimary, Color(0xFF7E57C2), Color(0xFF8E24AA))
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = "Medical Icon",
                                    tint = VibrantHighlight,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "MedStudy Pro UCS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Morfofisiopatología Humana I • 2do Año",
                                style = MaterialTheme.typography.labelSmall,
                                color = VibrantHighlight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Level & Streak Badge
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🔥 ${state.userStreak}d", fontWeight = FontWeight.Bold, color = Color(0xFFFFB74D), fontSize = 12.sp)
                            Text("•", color = Color.White.copy(alpha = 0.6f))
                            Text("Nivel ${state.userLevel}", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // XP Progress Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Rango: ${state.userRankTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${state.userXp} / 100 XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = VibrantHighlight,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (state.userXp % 100) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = VibrantHighlight,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessorGomezBanner(state: MedStudyUiState) {
    Surface(
        color = VibrantSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = VibrantPrimaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👨‍🔬", fontSize = 24.sp)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Prof. Gómez (Docente UCS)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VibrantPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldSuccess)
                    )
                }

                Text(
                    text = state.professorBubbleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateTextPrimary,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TabRowSection(
    activeTab: MedStudyTab,
    onTabSelected: (MedStudyTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = activeTab.ordinal,
        containerColor = VibrantSurfaceVariant,
        contentColor = VibrantPrimary,
        edgePadding = 8.dp
    ) {
        Tab(
            selected = activeTab == MedStudyTab.LIBRARY,
            onClick = { onTabSelected(MedStudyTab.LIBRARY) },
            modifier = Modifier.testTag("tab_library"),
            text = { Text("📚 Biblioteca UCS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (activeTab == MedStudyTab.LIBRARY) VibrantPrimary else SlateTextSecondary) }
        )
        Tab(
            selected = activeTab == MedStudyTab.SOCRATIC,
            onClick = { onTabSelected(MedStudyTab.SOCRATIC) },
            modifier = Modifier.testTag("tab_socratic"),
            text = { Text("💬 Diálogo Socrático", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (activeTab == MedStudyTab.SOCRATIC) VibrantPrimary else SlateTextSecondary) }
        )
        Tab(
            selected = activeTab == MedStudyTab.PUZZLE,
            onClick = { onTabSelected(MedStudyTab.PUZZLE) },
            modifier = Modifier.testTag("tab_puzzle"),
            text = { Text("🧩 Rompecabezas", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (activeTab == MedStudyTab.PUZZLE) VibrantPrimary else SlateTextSecondary) }
        )
        Tab(
            selected = activeTab == MedStudyTab.EXAM,
            onClick = { onTabSelected(MedStudyTab.EXAM) },
            modifier = Modifier.testTag("tab_exam"),
            text = { Text("📝 Examen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (activeTab == MedStudyTab.EXAM) VibrantPrimary else SlateTextSecondary) }
        )
    }
}

// ==================== TAB 1: BIBLIOTECA & PLANES DE CLASE ====================
@Composable
fun LibraryTabContent(
    viewModel: MedStudyViewModel,
    state: MedStudyUiState
) {
    var importTitle by remember { mutableStateOf("") }
    var importContent by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Image Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_med_hero_1784656294183),
                        contentDescription = "Medical Hero Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Portal de Estudio Morfofisiopatología I",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Universidad de las Ciencias de la Salud Hugo Chávez Frías",
                            style = MaterialTheme.typography.bodySmall,
                            color = VibrantHighlight
                        )
                    }
                }
            }
        }

        // Section 1: Academic Bookshelf (Clickable Books!)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "LIBROS DE TEXTO OFICIALES (TOCA CUALQUIERA)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateTextSecondary,
                    letterSpacing = 1.sp
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = state.academicBooks, key = { it.id }) { book ->
                        Card(
                            modifier = Modifier
                                .width(170.dp)
                                .height(200.dp)
                                .testTag("book_card_${book.id}")
                                .clickable(role = Role.Button) { viewModel.openBookDialog(book) },
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(book.colorStartHex),
                                                Color(book.colorEndHex)
                                            )
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.25f)
                                    ) {
                                        Text(
                                            text = book.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = book.author,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "📖 Leer Resumen",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(book.colorEndHex),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Official Class Plans from Uploaded PDFs
        item {
            Text(
                text = "PLANES DE CLASE PNFMIC / UCS (PDFs OFICIALES)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SlateTextSecondary,
                letterSpacing = 1.sp
            )
        }

        items(items = state.classPlans, key = { it.id }) { plan ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("plan_card_${plan.id}")
                    .clickable(role = Role.Button) { viewModel.openClassPlanDialog(plan) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VibrantSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = VibrantPrimaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Semana", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = VibrantPrimary)
                                Text("${plan.week}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = VibrantPrimary)
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plan.topicName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibrantPrimary
                        )
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = plan.summaryText,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(Icons.Default.ChevronRight, contentDescription = "Abrir Plan", tint = VibrantPrimary)
                }
            }
        }

        // Section 3: Custom Uploaded Guides
        if (state.customGuides.isNotEmpty()) {
            item {
                Text(
                    text = "TUS GUÍAS Y DOCUMENTOS GUARDADOS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateTextSecondary,
                    letterSpacing = 1.sp
                )
            }

            items(items = state.customGuides, key = { it.id }) { guide ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_guide_${guide.id}")
                        .clickable(role = Role.Button) { viewModel.openCustomGuideDialog(guide) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantBlueContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC2E7FF))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF001D35))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = guide.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF001D35)
                            )
                            Text(
                                text = guide.content,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF001D35))
                    }
                }
            }
        }

        // Section 4: Uploader Form
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Upload",
                            tint = VibrantPrimary
                        )
                        Text(
                            text = "Cargar Nueva Guía de Estudio / PDF",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }

                    Text(
                        text = "Ingresa el título y notas de tu material o PDF. Se guardarán en tu biblioteca y el Prof. Gómez responderá dudas socráticas sobre ellas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )

                    OutlinedTextField(
                        value = importTitle,
                        onValueChange = { importTitle = it },
                        label = { Text("Título del documento (ej. Resumen de Genética)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = importContent,
                        onValueChange = { importContent = it },
                        label = { Text("Contenido o fragmento del texto...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("import_content_input")
                    )

                    Button(
                        onClick = {
                            if (importTitle.isNotBlank() && importContent.isNotBlank()) {
                                viewModel.addCustomGuide(importTitle, importContent)
                                importTitle = ""
                                importContent = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_submit_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar y Analizar (+30 XP)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== TAB 2: CHAT SOCRÁTICO ====================
@Composable
fun SocraticChatTabContent(
    viewModel: MedStudyViewModel,
    state: MedStudyUiState
) {
    var userQuestionText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Chat Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Tutor Socrático Activo",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateTextPrimary
                )
                state.currentDialogue?.let {
                    Text(
                        text = "Caso: ${it.topicTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibrantPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.resetSocraticChat() },
                modifier = Modifier.testTag("reset_chat_button"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reiniciar", fontSize = 11.sp)
            }
        }

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(VibrantSurface, RoundedCornerShape(24.dp))
                .border(1.dp, SlateCardBorder, RoundedCornerShape(24.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items = state.chatMessages, key = { it.id }) { message ->
                ChatBubbleItem(message = message)
            }

            if (state.isAiThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VibrantPrimary, strokeWidth = 2.dp)
                        Text("Prof. Gómez analizando con el temario...", style = MaterialTheme.typography.bodySmall, color = VibrantPrimary)
                    }
                }
            }
        }

        // Option Arguments for current dialogue
        state.currentDialogue?.let { dialogue ->
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SELECCIONA TU ARGUMENTO CLÍNICO:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateTextSecondary
                )
                for ((index, option) in dialogue.options.withIndex()) {
                    OutlinedButton(
                        onClick = { viewModel.selectSocraticOption(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("socratic_option_$index"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = VibrantSurface)
                    ) {
                        Text(
                            text = option.text,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = SlateTextPrimary
                        )
                    }
                }
            }
        }

        // Custom Question Input to Gemini AI
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userQuestionText,
                onValueChange = { userQuestionText = it },
                placeholder = { Text("Haz una pregunta clínica al Prof. Gómez...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ask_prof_input"),
                singleLine = true
            )

            IconButton(
                onClick = {
                    if (userQuestionText.isNotBlank()) {
                        viewModel.askCustomQuestionToProfessor(userQuestionText)
                        userQuestionText = ""
                    }
                },
                modifier = Modifier
                    .testTag("send_question_button")
                    .background(VibrantPrimary, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val isProf = message.sender == ChatSender.PROFESSOR

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isProf) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (isProf) {
            Surface(
                shape = CircleShape,
                color = VibrantPrimary,
                modifier = Modifier
                    .size(32.dp)
                    .padding(end = 6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("👨‍🔬", fontSize = 16.sp)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isProf) 4.dp else 20.dp,
                bottomEnd = if (isProf) 20.dp else 4.dp
            ),
            color = if (isProf) VibrantSurface else VibrantPrimary,
            border = if (isProf) androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder) else null,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isProf) SlateTextPrimary else Color.White,
                    lineHeight = 18.sp
                )
            }
        }

        if (!isProf) {
            Surface(
                shape = CircleShape,
                color = VibrantPrimaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .padding(start = 6.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🎓", fontSize = 16.sp)
                }
            }
        }
    }
}

// ==================== TAB 3: ROMPECABEZAS ====================
@Composable
fun PathologyPuzzleTabContent(
    viewModel: MedStudyViewModel,
    state: MedStudyUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VibrantPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Rompecabezas Morfo-Histológico",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Relaciona cada alteración molecular u organela con su síndrome o patología correspondiente. ¡Gana +20 XP por cada acierto!",
                        style = MaterialTheme.typography.bodySmall,
                        color = VibrantHighlight
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VibrantBlueContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC2E7FF))
            ) {
                Text(
                    text = state.puzzleFeedback,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D35),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Organelle / Mechanism
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PASO 1: CAUSA / ORGANELA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                    for (item in state.puzzleItems) {
                        val isMatched = state.matchedIds.contains(item.id)
                        val isSelected = state.selectedOrganelleId == item.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("puzzle_organelle_${item.id}")
                                .clickable(enabled = !isMatched, role = Role.Button) { viewModel.selectPuzzleOrganelle(item.id) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isMatched -> EmeraldSuccess.copy(alpha = 0.15f)
                                    isSelected -> VibrantPrimaryContainer
                                    else -> VibrantSurface
                                }
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isMatched -> EmeraldSuccess
                                    isSelected -> VibrantPrimary
                                    else -> SlateCardBorder
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(item.icon, fontSize = 20.sp)
                                Column {
                                    Text(item.organelleTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(item.organelleDesc, style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary, maxLines = 2)
                                }
                            }
                        }
                    }
                }

                // Column 2: Syndrome / Pathology
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("PASO 2: SÍNDROME / ENTIDAD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                    for (item in state.puzzleItems) {
                        val isMatched = state.matchedIds.contains(item.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("puzzle_syndrome_${item.id}")
                                .clickable(enabled = !isMatched, role = Role.Button) { viewModel.selectPuzzleSyndrome(item.id) },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMatched) EmeraldSuccess.copy(alpha = 0.15f) else VibrantSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isMatched) EmeraldSuccess else SlateCardBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.syndromeTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = VibrantPrimary)
                                Text(item.syndromeDesc, style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 4: SIMULADOR DE EXAMEN ====================
@Composable
fun ExamSimulatorTabContent(
    viewModel: MedStudyViewModel,
    state: MedStudyUiState
) {
    val currentQuestion = state.examQuestions.getOrNull(state.activeExamIndex) ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VibrantPrimaryContainer
                ) {
                    Text(
                        text = currentQuestion.tag,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = VibrantOnPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Puntaje: ${state.examScore} / ${state.examAnsweredCount} Resueltas",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateTextPrimary
                )
            }
        }

        // Vignette Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VibrantSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Caso Clínico de Examen Oficial UCS:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = VibrantPrimary
                    )

                    Text(
                        text = currentQuestion.vignette,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        // Options
        items(items = currentQuestion.options, key = { it.key }) { option ->
            val isSelected = state.selectedExamOptionKey == option.key
            val isAnswered = state.isExamAnswered

            val cardBg = when {
                isAnswered && option.isCorrect -> EmeraldSuccess.copy(alpha = 0.15f)
                isAnswered && isSelected && !option.isCorrect -> RoseError.copy(alpha = 0.15f)
                else -> VibrantSurface
            }

            val cardBorder = when {
                isAnswered && option.isCorrect -> EmeraldSuccess
                isAnswered && isSelected && !option.isCorrect -> RoseError
                else -> SlateCardBorder
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exam_option_${option.key}")
                    .clickable(enabled = !isAnswered, role = Role.Button) { viewModel.selectExamOption(option) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = VibrantBlueContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = option.key,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D35)
                            )
                        }
                    }

                    Text(
                        text = option.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Explanation Box
        if (state.isExamAnswered) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantPinkContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3BFEA))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Crítica del Profesor Gómez:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF31111D)
                        )

                        Text(
                            text = currentQuestion.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextPrimary,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = { viewModel.nextExamQuestion() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("next_exam_question_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary)
                        ) {
                            Text("Siguiente Caso Clínico ➔", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==================== INTERACTIVE READER DIALOGS ====================

@Composable
fun UcsClassPlanReaderDialog(
    plan: UcsClassPlan,
    onDismiss: () -> Unit,
    onStartSocraticCase: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = VibrantBackground,
            border = androidx.compose.foundation.BorderStroke(2.dp, VibrantPrimary),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PLAN DE CLASE OFICIAL • SEMANA ${plan.week}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = VibrantPrimary
                        )
                        Text(
                            text = plan.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlateTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_plan_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SlateTextPrimary)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = SlateCardBorder)

                // Scrollable Content Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary Section
                    SectionCard(title = "📌 Sumario del Tema", color = VibrantBlueContainer) {
                        Text(plan.summaryText, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                    }

                    // Objectives Section
                    SectionCard(title = "🎯 Objetivos de la Clase", color = VibrantPinkContainer) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            plan.objectives.forEachIndexed { idx, obj ->
                                Text("${idx + 1}. $obj", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Cellular Mechanisms & Pathology
                    SectionCard(title = "⚙️ Mecanismos Celulares & Patogenia", color = VibrantSurface) {
                        Text(plan.cellularMechanisms, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                    }

                    // Morphological Features
                    SectionCard(title = "🔬 Características Morfológicas", color = VibrantSurface) {
                        Text(plan.morphologicalFeatures, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                    }

                    // Diagnostic Evidence
                    SectionCard(title = "🩺 Evidencias Diagnósticas", color = VibrantSurface) {
                        Text(plan.diagnosticEvidence, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                    }

                    // Conclusions
                    SectionCard(title = "💡 Conclusiones Fundamentales", color = VibrantPrimaryContainer) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            plan.conclusions.forEach { conc ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("•", fontWeight = FontWeight.Bold, color = VibrantPrimary)
                                    Text(conc, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Exam Tip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF3C4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("💡", fontSize = 22.sp)
                            Column {
                                Text("Tip del Examen UCS:", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium, color = Color(0xFF8C4A00))
                                Text(plan.examTips, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5C3100))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action
                Button(
                    onClick = onStartSocraticCase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("socratic_from_plan_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary)
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resolver Casos Socráticos de este Tema ➔", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AcademicBookReaderDialog(
    book: AcademicBook,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape = RoundedCornerShape(28.dp),
            color = VibrantBackground,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(book.colorStartHex)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(book.colorStartHex)
                        )
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlateTextPrimary
                        )
                        Text(
                            text = book.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_book_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = SlateCardBorder)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionCard(title = "📖 Resumen de la Obra", color = VibrantSurface) {
                        Text(book.summary, style = MaterialTheme.typography.bodyMedium, lineHeight = 20.sp)
                    }

                    SectionCard(title = "⭐ Capítulos & Contenidos Clave", color = VibrantBlueContainer) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            book.keyHighlights.forEach { item ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("✓", fontWeight = FontWeight.ExtraBold, color = Color(0xFF001D35))
                                    Text(item, style = MaterialTheme.typography.bodySmall, color = Color(0xFF001D35))
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = VibrantPinkContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3BFEA))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "👨‍🔬 Recomendación del Profesor Gómez:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF31111D)
                            )
                            Text(
                                text = book.profTip,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("finish_book_read_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(book.colorStartHex))
                ) {
                    Text("Cerrar Lectura y Continuar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomGuideReaderDialog(
    guide: StudyGuideEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onAskAiAboutGuide: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f),
            shape = RoundedCornerShape(28.dp),
            color = VibrantBackground,
            border = androidx.compose.foundation.BorderStroke(2.dp, VibrantPrimary),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = guide.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = VibrantPrimary
                        )
                        Text(
                            text = guide.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SlateTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_guide_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = SlateCardBorder)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(VibrantSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, SlateCardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = guide.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateTextPrimary,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_guide_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseError)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp))
                    }

                    Button(
                        onClick = onAskAiAboutGuide,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ask_ai_guide_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VibrantPrimary)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preguntar a Prof. Gómez", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    color: Color,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = SlateTextPrimary
            )
            content()
        }
    }
}
