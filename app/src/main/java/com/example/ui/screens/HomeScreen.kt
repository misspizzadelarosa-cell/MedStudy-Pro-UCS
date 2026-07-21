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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
                            Text(
                                text = "NIVEL ${state.userLevel}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = VibrantHighlight
                            )
                            Text(
                                text = "🔥 ${state.userStreak}d",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldStreak
                            )
                        }
                    }
                }

                // XP Progress Bar & Rank
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RANGO: ${state.userRankTitle}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = VibrantHighlight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${state.userXp}/100 XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                LinearProgressIndicator(
                    progress = { (state.userXp / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = VibrantHighlight,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun ProfessorGomezBanner(state: MedStudyUiState) {
    Surface(
        color = VibrantSurfaceVariant,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Professor Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(2.dp, VibrantPrimary, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_professor_gomez_1784656282351),
                    contentDescription = "Prof. Gómez",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Speech Bubble
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                color = VibrantSurface,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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

// ==================== TAB 1: BIBLIOTECA & SÍLABO ====================
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
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Portal de Estudio Morfofisiopatología",
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

        // Section: Uploader
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
                            text = "Cargar Guías de Estudio PNFMIC / UCS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextPrimary
                        )
                    }

                    Text(
                        text = "Ingresa el título y contenido de tu guía o resumen. El Prof. Gómez las analizará y adaptará las preguntas socráticas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondary
                    )

                    OutlinedTextField(
                        value = importTitle,
                        onValueChange = { importTitle = it },
                        label = { Text("Título de la guía (ej. Resumen de Lesión Celular)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = importContent,
                        onValueChange = { importContent = it },
                        label = { Text("Contenido o notas de la guía...") },
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
                        Text("Analizar con Prof. Gómez (+30 XP)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Loaded Materials
        item {
            Text(
                text = "MATERIALES OFICIALES DE MORFOFISIOPATOLOGÍA I",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SlateTextSecondary,
                letterSpacing = 1.sp
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VibrantBlueContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC2E7FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🧬", fontSize = 24.sp)
                        Column {
                            Text(
                                text = "Manual Introductorio de Morfofisiopatología I",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D35)
                            )
                            Text(
                                text = "Sílaba Básica PNFMIC / UCS • Lesión y Muerte Celular",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextSecondary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VibrantPrimaryContainer
                    ) {
                        Text(
                            text = "Básico",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = VibrantOnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        items(items = state.customGuides, key = { it.id }) { guide ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = VibrantPinkContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3BFEA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📄", fontSize = 24.sp)
                        Column {
                            Text(
                                text = guide.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF31111D)
                            )
                            Text(
                                text = guide.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateTextSecondary
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = VibrantPrimaryContainer
                    ) {
                        Text(
                            text = "Estudiante",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = VibrantOnPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Section: Academic Bookshelf
        item {
            Text(
                text = "ESTANTE ACADÉMICO (TOCA UN LIBRO)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SlateTextSecondary,
                letterSpacing = 1.sp
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = state.academicBooks, key = { it.id }) { book ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(180.dp)
                            .testTag("book_card_${book.id}")
                            .clickable { viewModel.onBookClicked(book) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = book.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Column {
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
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Core Modules
        item {
            Text(
                text = "NÚCLEOS TEMÁTICOS DE 2DO AÑO (UCS)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SlateTextSecondary,
                letterSpacing = 1.sp
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("module_lesion_celular")
                        .clickable { viewModel.loadUcsModule("lesion_celular") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("💀", fontSize = 28.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema 1: Lesión y Muerte Celular", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Mecanismos de tumefacción, esteatosis, necrosis (coagulativa, licuefactiva, caseosa) y apoptosis.", style = MaterialTheme.typography.bodySmall, color = SlateTextSecondary)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VibrantPrimary)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("module_genetica")
                        .clickable { viewModel.loadUcsModule("genetica") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🧬", fontSize = 28.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema 2: Genética Médica y Alteraciones", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Cromosomopatías, aberraciones numéricas y estructurales según la Dra. Araceli Lantigua.", style = MaterialTheme.typography.bodySmall, color = SlateTextSecondary)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VibrantPrimary)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("module_hemodinamia")
                        .clickable { viewModel.loadUcsModule("hemodinamia") },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = VibrantSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🩸", fontSize = 28.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema 3: Trastornos Hemodinámicos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("Procesos de congestión pasiva, edema, trombosis, embolia e infarto tisular.", style = MaterialTheme.typography.bodySmall, color = SlateTextSecondary)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VibrantPrimary)
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
            Text(
                text = "Tutor Socrático Activo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SlateTextPrimary
            )
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
                        Text("Prof. Gómez analizando el caso...", style = MaterialTheme.typography.bodySmall, color = VibrantPrimary)
                    }
                }
            }
        }

        // Option Arguments for current dialogue
        state.currentDialogue?.let { dialogue ->
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            modifier = Modifier.fillMaxWidth()
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
                    Text("PASO 1: ORGANELA / CAUSA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                    for (item in state.puzzleItems) {
                        val isMatched = state.matchedIds.contains(item.id)
                        val isSelected = state.selectedOrganelleId == item.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("puzzle_organelle_${item.id}")
                                .clickable(enabled = !isMatched) { viewModel.selectPuzzleOrganelle(item.id) },
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
                    Text("PASO 2: SÍNDROME / PATOLOGÍA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SlateTextSecondary)
                    for (item in state.puzzleItems) {
                        val isMatched = state.matchedIds.contains(item.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("puzzle_syndrome_${item.id}")
                                .clickable(enabled = !isMatched) { viewModel.selectPuzzleSyndrome(item.id) },
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
                    .clickable(enabled = !isAnswered) { viewModel.selectExamOption(option) },
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
