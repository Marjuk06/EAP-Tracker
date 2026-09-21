package com.marjuk.eaptracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SyllabusScreen() {
    val subjects = SyllabusRepository.subjectsState

    var selectedSubjectName by remember { mutableStateOf<String?>(null) }
    var selectedPaperName by remember { mutableStateOf<String?>(null) }
    var expandedSubjectName by remember { mutableStateOf<String?>(null) }

    val isPaperSelected = selectedPaperName != null

    BackHandler(enabled = isPaperSelected) {
        selectedPaperName = null
    }

    // Stable key for AnimatedContent based only on WHETHER a paper is selected
    // This prevents re-animating when the paper content (progress) updates
    AnimatedContent(
        targetState = isPaperSelected,
        transitionSpec = {
            if (targetState) { // Moving TO ChapterList
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut())
            } else { // Moving BACK to SubjectList
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "syllabusContent"
    ) { paperSelected ->
        // Inside here, we use keys to keep the scroll position and state stable
        if (!paperSelected) {
            key("subject_list") {
                SubjectList(
                    subjects = subjects,
                    expandedSubjectName = expandedSubjectName,
                    onToggle = { name ->
                        expandedSubjectName = if (expandedSubjectName == name) null else name
                    },
                    onPaperClick = { subject, p ->
                        selectedSubjectName = subject.name
                        selectedPaperName = p.name
                    }
                )
            }
        } else {
            // Find current paper data from state using stable names
            val currentPaper = subjects.find { it.name == selectedSubjectName }
                ?.papers?.find { it.name == selectedPaperName }
            
            if (currentPaper != null) {
                key("chapter_list_${selectedSubjectName}_${selectedPaperName}") {
                    ChapterList(
                        subjectName = selectedSubjectName ?: "",
                        paper = currentPaper,
                        onBack = { selectedPaperName = null },
                        onSectionToggle = { chapterIndex, sectionIndex ->
                            SyllabusRepository.toggleSection(
                                subjectName = selectedSubjectName ?: "",
                                paperName = selectedPaperName ?: "",
                                chapterIndex = chapterIndex,
                                sectionIndex = sectionIndex
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectList(
    subjects: List<Subject>,
    expandedSubjectName: String?,
    onToggle: (String) -> Unit,
    onPaperClick: (Subject, Paper) -> Unit
) {
    val totalChapters = subjects.sumOf { it.totalChapters }
    val completedChapters = subjects.sumOf { it.completedChapters }
    
    val visibleState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -20 }
        ) {
            Text(
                text = "SYLLABUS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn(tween(500, 100)) + slideInVertically(tween(500, 100)) { 20 }
        ) {
            OverallProgress(
                completedChapters = completedChapters,
                totalChapters = totalChapters,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            itemsIndexed(
                items = subjects,
                key = { _, subject -> subject.name } // Stable key
            ) { index, subject ->
                var itemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(200L + index * 100L)
                    itemVisible = true
                }
                
                AnimatedVisibility(
                    visible = itemVisible,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 30 }
                ) {
                    SyllabusSplitButton(
                        subject = subject,
                        isExpanded = expandedSubjectName == subject.name,
                        onToggle = { onToggle(subject.name) },
                        onPaperClick = { paper -> onPaperClick(subject, paper) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChapterList(
    subjectName: String,
    paper: Paper,
    onBack: () -> Unit,
    onSectionToggle: (Int, Int) -> Unit
) {
    var expandedChapterIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = subjectName.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = paper.name.uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 160.dp)
        ) {
            itemsIndexed(
                items = paper.chapters,
                key = { _, chapter -> chapter.name } // Stable key
            ) { index, chapter ->
                ChapterExpandableItem(
                    chapter = chapter,
                    isExpanded = expandedChapterIndex == index,
                    onToggle = {
                        expandedChapterIndex = if (expandedChapterIndex == index) null else index
                    },
                    onSectionToggle = { sectionIndex ->
                        onSectionToggle(index, sectionIndex)
                    }
                )
            }
        }
    }
}
