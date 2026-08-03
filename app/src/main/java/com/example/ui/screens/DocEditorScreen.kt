package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OfficeScreen
import com.example.ui.OfficeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditorScreen(
    viewModel: OfficeViewModel,
    docId: String,
    modifier: Modifier = Modifier
) {
    val activeDoc = viewModel.activeDoc
    var textContent by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    // Synchronize initial state
    LaunchedEffect(activeDoc) {
        if (activeDoc != null && activeDoc.id == docId) {
            textContent = activeDoc.content
            title = activeDoc.title
        }
    }

    // Auto-save on change
    var isSaving by remember { mutableStateOf(false) }
    LaunchedEffect(textContent, title) {
        if (activeDoc != null && (textContent != activeDoc.content || title != activeDoc.title)) {
            isSaving = true
            viewModel.saveDocumentContent(textContent)
            if (title.isNotEmpty() && title != activeDoc.title) {
                viewModel.renameDocument(docId, title)
            }
            kotlinx.coroutines.delay(800)
            isSaving = false
        }
    }

    // AI Helper panel state
    var showAiPanel by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }

    // Local styling states
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var textAlignment by remember { mutableStateOf(TextAlign.Left) }
    var fontSize by remember { mutableStateOf(16) }

    // Counts
    val charCount = textContent.length
    val wordCount = if (textContent.isBlank()) 0 else textContent.trim().split(Regex("\\s+")).size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("doc_title_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(OfficeScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSaving) {
                            Icon(
                                Icons.Default.CloudSync,
                                contentDescription = "Saving",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = "Saved",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Button(
                            onClick = { showAiPanel = !showAiPanel },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showAiPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Assistant",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("এআই রাইটার", fontSize = 12.sp)
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Text Formatting Ribbon
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isBold = !isBold },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                    }
                    IconButton(
                        onClick = { isItalic = !isItalic },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                    }
                    
                    VerticalDivider(modifier = Modifier.height(24.dp))

                    IconButton(
                        onClick = { textAlignment = TextAlign.Left },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (textAlignment == TextAlign.Left) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.FormatAlignLeft, contentDescription = "Align Left")
                    }
                    IconButton(
                        onClick = { textAlignment = TextAlign.Center },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (textAlignment == TextAlign.Center) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.FormatAlignCenter, contentDescription = "Align Center")
                    }
                    IconButton(
                        onClick = { textAlignment = TextAlign.Right },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (textAlignment == TextAlign.Right) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.FormatAlignRight, contentDescription = "Align Right")
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { if (fontSize > 10) fontSize -= 1 }) {
                            Icon(Icons.Default.Remove, contentDescription = "Font size down", modifier = Modifier.size(16.dp))
                        }
                        Text(text = "$fontSize pt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        IconButton(onClick = { if (fontSize < 40) fontSize += 1 }) {
                            Icon(Icons.Default.Add, contentDescription = "Font size up", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Word processing A4 Page workspace
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6)) // PC office desktop gray background
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Realistic A4 Paper sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .shadow(4.dp, RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    TextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("doc_text_input"),
                        placeholder = { Text("এখানে আপনার তথ্য লিখতে থাকুন...", fontSize = fontSize.sp) },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = fontSize.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                            textAlign = textAlignment
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    
                    // Live counters at page bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "শব্দ: $wordCount  |  অক্ষর: $charCount",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "পৃষ্ঠা ১",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Expandable AI Writer Assistant Box
            if (showAiPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("এআই অ্যাসিস্ট্যান্ট রাইটার", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            IconButton(onClick = { showAiPanel = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        
                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            placeholder = { Text("এআই কে নির্দেশ দিন। যেমন: 'ডিজিটাল বাংলাদেশ নিয়ে প্যারাগ্রাফ লেখ'", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (viewModel.isAiGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    IconButton(
                                        onClick = {
                                            if (aiPrompt.trim().isNotEmpty()) {
                                                viewModel.askGemini(aiPrompt) { response ->
                                                    aiResponse = response
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        )

                        if (viewModel.aiError != null) {
                            Text(
                                text = viewModel.aiError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (aiResponse.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .heightIn(max = 140.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = aiResponse,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = {
                                                textContent = if (textContent.isEmpty()) aiResponse else "$textContent\n\n$aiResponse"
                                                aiResponse = ""
                                                aiPrompt = ""
                                                showAiPanel = false
                                            }
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Insert")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ডকুমেন্টে যুক্ত করুন")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
