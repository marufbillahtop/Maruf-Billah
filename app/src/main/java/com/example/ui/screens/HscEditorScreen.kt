package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OfficeScreen
import com.example.ui.OfficeViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HscEditorScreen(
    viewModel: OfficeViewModel,
    docId: String,
    modifier: Modifier = Modifier
) {
    val activeDoc = viewModel.activeDoc
    var title by remember { mutableStateOf("") }
    
    // HSC Exam JSON States
    var boardHeader by remember { mutableStateOf("ঢাকা শিক্ষা বোর্ড") }
    var examSubject by remember { mutableStateOf("") }
    var subjectCode by remember { mutableStateOf("২৭৫") }
    var examTime by remember { mutableStateOf("৩ ঘণ্টা") }
    var fullMarks by remember { mutableStateOf("১০০") }
    
    val cqsList = remember { mutableStateListOf<JSONObject>() }
    val mcqsList = remember { mutableStateListOf<JSONObject>() }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabsList = listOf("📋 বিবরণ", "📝 সৃজনশীল (CQ)", "🎯 MCQ", "🖨️ প্রিন্ট প্রিভিউ")

    // AI Generation Fields
    var aiSubject by remember { mutableStateOf("তথ্য ও যোগাযোগ প্রযুক্তি") }
    var aiTopic by remember { mutableStateOf("এইচটিএমএল (HTML)") }
    var numMcqToGen by remember { mutableStateOf(5) }

    // Sync state from DB
    LaunchedEffect(activeDoc) {
        if (activeDoc != null && activeDoc.id == docId) {
            title = activeDoc.title
            try {
                val json = JSONObject(activeDoc.content)
                val header = json.optJSONObject("header")
                if (header != null) {
                    boardHeader = header.optString("board", "ঢাকা শিক্ষা বোর্ড")
                    examSubject = header.optString("subject", activeDoc.title)
                    subjectCode = header.optString("subjectCode", "২৭৫")
                    examTime = header.optString("time", "৩ ঘণ্টা")
                    fullMarks = header.optString("fullMarks", "১০০")
                } else {
                    examSubject = activeDoc.title
                }
                
                // CQs
                val cqArray = json.optJSONArray("cqs")
                cqsList.clear()
                if (cqArray != null) {
                    for (i in 0 until cqArray.length()) {
                        cqsList.add(cqArray.getJSONObject(i))
                    }
                }
                
                // MCQs
                val mcqArray = json.optJSONArray("mcqs")
                mcqsList.clear()
                if (mcqArray != null) {
                    for (i in 0 until mcqArray.length()) {
                        mcqsList.add(mcqArray.getJSONObject(i))
                    }
                }
            } catch (e: Exception) {
                examSubject = activeDoc.title
            }
        }
    }

    // Save JSON to DB helper
    val saveExam = {
        if (activeDoc != null) {
            val json = JSONObject().apply {
                put("header", JSONObject().apply {
                    put("board", boardHeader)
                    put("subject", examSubject)
                    put("subjectCode", subjectCode)
                    put("time", examTime)
                    put("fullMarks", fullMarks)
                })
                put("cqs", JSONArray().apply {
                    cqsList.forEach { put(it) }
                })
                put("mcqs", JSONArray().apply {
                    mcqsList.forEach { put(it) }
                })
            }
            viewModel.saveDocumentContent(json.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "এইচএসসি প্রশ্ন নির্মাতা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(OfficeScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudQueue,
                            "Saved",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
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
            // Tab row selector
            TabRow(selectedTabIndex = selectedTab) {
                tabsList.forEachIndexed { index, text ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6))
                    .padding(12.dp)
            ) {
                when (selectedTab) {
                    0 -> { // DETAILS TAB
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(2.dp, RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("পরীক্ষার সাধারণ বিবরণ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            
                            OutlinedTextField(
                                value = boardHeader,
                                onValueChange = {
                                    boardHeader = it
                                    saveExam()
                                },
                                label = { Text("শিক্ষা বোর্ড / কলেজের নাম") },
                                modifier = Modifier.fillMaxWidth().testTag("board_header_input")
                            )

                            OutlinedTextField(
                                value = examSubject,
                                onValueChange = {
                                    examSubject = it
                                    saveExam()
                                    if (it.isNotEmpty()) {
                                        viewModel.renameDocument(docId, it)
                                    }
                                },
                                label = { Text("পরীক্ষার বিষয়") },
                                modifier = Modifier.fillMaxWidth().testTag("subject_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = subjectCode,
                                    onValueChange = {
                                        subjectCode = it
                                        saveExam()
                                    },
                                    label = { Text("বিষয় কোড") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = examTime,
                                    onValueChange = {
                                        examTime = it
                                        saveExam()
                                    },
                                    label = { Text("সময়") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = fullMarks,
                                    onValueChange = {
                                        fullMarks = it
                                        saveExam()
                                    },
                                    label = { Text("পূর্ণমান") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Tips", tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = "তথ্য পরিবর্তনের পর স্বয়ংক্রিয়ভাবে ডাটা সেভ হয়ে যায়। পরবর্তী ট্যাবে চলে যান সৃজনশীল বা MCQ যোগ করার জন্য।",
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    1 -> { // CREATIVE QUESTION (CQ) TAB
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Gemini AI CQ Generator Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.tertiary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("এআই সৃজনশীল প্রশ্ন জেনারেটর (Gemini)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = aiSubject,
                                                onValueChange = { aiSubject = it },
                                                label = { Text("বিষয়") },
                                                modifier = Modifier.weight(1.5f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = aiTopic,
                                                onValueChange = { aiTopic = it },
                                                label = { Text("অধ্যায়/টপিক") },
                                                modifier = Modifier.weight(2f),
                                                singleLine = true
                                            )
                                        }

                                        if (viewModel.isAiGenerating) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.generateHscCQAi(aiSubject, aiTopic) { stem, qa, qb, qc, qd ->
                                                        val newCq = JSONObject().apply {
                                                            put("stem", stem)
                                                            put("qA", qa)
                                                            put("qB", qb)
                                                            put("qC", qc)
                                                            put("qD", qd)
                                                        }
                                                        cqsList.add(newCq)
                                                        saveExam()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("generate_cq_ai_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, "AI icon")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("বোর্ড স্ট্যান্ডার্ড উদ্দীপক ও প্রশ্ন তৈরি করুন")
                                            }
                                        }
                                        
                                        if (viewModel.aiError != null) {
                                            Text(viewModel.aiError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // Added CQs List
                            itemsIndexed(cqsList) { idx, item ->
                                CqEditorCard(
                                    cqIndex = idx + 1,
                                    cqJson = item,
                                    onUpdate = { saveExam() },
                                    onDelete = {
                                        cqsList.removeAt(idx)
                                        saveExam()
                                    }
                                )
                            }

                            // Manual Add CQ Button
                            item {
                                Button(
                                    onClick = {
                                        val newCq = JSONObject().apply {
                                            put("stem", "একটি নতুন উদ্দীপকের বিবরণ এখানে লিখুন।")
                                            put("qA", "ক-এর প্রশ্ন?")
                                            put("qB", "খ-এর প্রশ্ন?")
                                            put("qC", "গ-এর প্রশ্ন?")
                                            put("qD", "ঘ-এর প্রশ্ন?")
                                        }
                                        cqsList.add(newCq)
                                        saveExam()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_cq_manual_button")
                                ) {
                                    Icon(Icons.Default.Add, "Add manual")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("নিজে সৃজনশীল প্রশ্ন (CQ) যোগ করুন")
                                }
                            }
                        }
                    }
                    2 -> { // MULTIPLE CHOICE (MCQ) TAB
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Gemini AI MCQ Generator Card
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("এআই বহুনির্বাচনী প্রশ্ন জেনারেটর (Gemini)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = aiSubject,
                                                onValueChange = { aiSubject = it },
                                                label = { Text("বিষয়") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = aiTopic,
                                                onValueChange = { aiTopic = it },
                                                label = { Text("টপিক/অধ্যায়") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                        }

                                        if (viewModel.isAiGenerating) {
                                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator()
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    viewModel.generateHscMcqAi(aiSubject, aiTopic, numMcqToGen) { array ->
                                                        for (i in 0 until array.length()) {
                                                            mcqsList.add(array.getJSONObject(i))
                                                        }
                                                        saveExam()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("generate_mcq_ai_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, "AI icon")
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("৫টি বোর্ড স্ট্যান্ডার্ড MCQ প্রশ্ন জেনারেট করুন")
                                            }
                                        }
                                    }
                                }
                            }

                            // MCQs List
                            itemsIndexed(mcqsList) { idx, item ->
                                McqEditorCard(
                                    mcqIndex = idx + 1,
                                    mcqJson = item,
                                    onUpdate = { saveExam() },
                                    onDelete = {
                                        mcqsList.removeAt(idx)
                                        saveExam()
                                    }
                                )
                            }

                            // Manual Add MCQ Button
                            item {
                                Button(
                                    onClick = {
                                        val newMcq = JSONObject().apply {
                                            put("question", "নতুন বহুনির্বাচনী প্রশ্ন?")
                                            put("options", JSONArray().apply {
                                                put("ক অপশন")
                                                put("খ অপশন")
                                                put("গ অপশন")
                                                put("ঘ অপশন")
                                            })
                                            put("correct", 0)
                                        }
                                        mcqsList.add(newMcq)
                                        saveExam()
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("add_mcq_manual_button")
                                ) {
                                    Icon(Icons.Default.Add, "Add MCQ")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("নিজে MCQ প্রশ্ন যোগ করুন")
                                }
                            }
                        }
                    }
                    3 -> { // PRINT PREVIEW TAB (Crowning Board layout preview)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(4.dp, RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Board Header Title
                            Text(
                                text = boardHeader,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "উচ্চ মাধ্যমিক পরীক্ষা - ২০২৬",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "বিষয়: $examSubject",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                                    .border(width = 0.5.dp, color = Color.Gray)
                                    .padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Text("বিষয় কোড: $subjectCode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("সময়: $examTime", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("পূর্ণমান: $fullMarks", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Section A: Creative Questions (CQ)
                            if (cqsList.isNotEmpty()) {
                                Text(
                                    text = "ক-বিভাগ: সৃজনশীল প্রশ্ন (CQ)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 6.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "[যেকোনো ৫টি প্রশ্নের উত্তর দাও। প্রতিটি প্রশ্নের মান ১০]",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                cqsList.forEachIndexed { idx, item ->
                                    val stem = item.optString("stem", "")
                                    val qA = item.optString("qA", "")
                                    val qB = item.optString("qB", "")
                                    val qC = item.optString("qC", "")
                                    val qD = item.optString("qD", "")

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "প্রশ্ন ${toBengaliNumber(idx + 1)}:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stem,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp,
                                            textAlign = TextAlign.Justify,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "(ক) $qA", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text(text = "১", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "(খ) $qB", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text(text = "২", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "(গ) $qC", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text(text = "৩", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(text = "(ঘ) $qD", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                            Text(text = "৪", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        HorizontalDivider(modifier = Modifier.padding(top = 10.dp), thickness = 0.5.dp)
                                    }
                                }
                            }

                            // Section B: Multiple Choice Questions (MCQ)
                            if (mcqsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "খ-বিভাগ: বহুনির্বাচনী প্রশ্ন (MCQ)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "[সব প্রশ্নের উত্তর আবশ্যক। প্রতিটি প্রশ্নের মান ১]",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                mcqsList.forEachIndexed { idx, item ->
                                    val question = item.optString("question", "")
                                    val optionsArr = item.optJSONArray("options")
                                    val options = mutableListOf<String>()
                                    if (optionsArr != null) {
                                        for (j in 0 until optionsArr.length()) {
                                            options.add(optionsArr.getString(j))
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = "${toBengaliNumber(idx + 1)}. $question",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        // Standard Bengali options layout
                                        if (options.size == 4) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = "(ক) ${options[0]}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                                Text(text = "(খ) ${options[1]}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(text = "(গ) ${options[2]}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                                                Text(text = "(ঘ) ${options[3]}", fontSize = 11.sp, modifier = Modifier.weight(1f))
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
}

@Composable
fun CqEditorCard(
    cqIndex: Int,
    cqJson: JSONObject,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    var stem by remember { mutableStateOf(cqJson.optString("stem", "")) }
    var qA by remember { mutableStateOf(cqJson.optString("qA", "")) }
    var qB by remember { mutableStateOf(cqJson.optString("qB", "")) }
    var qC by remember { mutableStateOf(cqJson.optString("qC", "")) }
    var qD by remember { mutableStateOf(cqJson.optString("qD", "")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সৃজনশীল প্রশ্ন - ${toBengaliNumber(cqIndex)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = stem,
                onValueChange = {
                    stem = it
                    cqJson.put("stem", it)
                    onUpdate()
                },
                label = { Text("উদ্দীপক (Stimulus text)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qA,
                onValueChange = {
                    qA = it
                    cqJson.put("qA", it)
                    onUpdate()
                },
                label = { Text("(ক) জ্ঞানমূলক প্রশ্ন (১ নম্বর)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qB,
                onValueChange = {
                    qB = it
                    cqJson.put("qB", it)
                    onUpdate()
                },
                label = { Text("(খ) অনুধাবনমূলক প্রশ্ন (২ নম্বর)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qC,
                onValueChange = {
                    qC = it
                    cqJson.put("qC", it)
                    onUpdate()
                },
                label = { Text("(গ) প্রয়োগমূলক প্রশ্ন (৩ নম্বর)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = qD,
                onValueChange = {
                    qD = it
                    cqJson.put("qD", it)
                    onUpdate()
                },
                label = { Text("(ঘ) উচ্চতর দক্ষতা প্রশ্ন (৪ নম্বর)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun McqEditorCard(
    mcqIndex: Int,
    mcqJson: JSONObject,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    var question by remember { mutableStateOf(mcqJson.optString("question", "")) }
    val optionsArr = mcqJson.optJSONArray("options") ?: JSONArray()
    
    var op1 by remember { mutableStateOf(optionsArr.optString(0, "")) }
    var op2 by remember { mutableStateOf(optionsArr.optString(1, "")) }
    var op3 by remember { mutableStateOf(optionsArr.optString(2, "")) }
    var op4 by remember { mutableStateOf(optionsArr.optString(3, "")) }
    
    var correctIdx by remember { mutableStateOf(mcqJson.optInt("correct", 0)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MCQ প্রশ্ন - ${toBengaliNumber(mcqIndex)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }

            OutlinedTextField(
                value = question,
                onValueChange = {
                    question = it
                    mcqJson.put("question", it)
                    onUpdate()
                },
                label = { Text("বহুনির্বাচনী প্রশ্নটি লিখুন") },
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("অপশনসমূহ ও সঠিক উত্তর নির্বাচন করুন:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = correctIdx == 0, onClick = { correctIdx = 0; mcqJson.put("correct", 0); onUpdate() })
                    OutlinedTextField(
                        value = op1,
                        onValueChange = {
                            op1 = it
                            optionsArr.put(0, it)
                            mcqJson.put("options", optionsArr)
                            onUpdate()
                        },
                        label = { Text("অপশন (ক)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = correctIdx == 1, onClick = { correctIdx = 1; mcqJson.put("correct", 1); onUpdate() })
                    OutlinedTextField(
                        value = op2,
                        onValueChange = {
                            op2 = it
                            optionsArr.put(1, it)
                            mcqJson.put("options", optionsArr)
                            onUpdate()
                        },
                        label = { Text("অপশন (খ)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = correctIdx == 2, onClick = { correctIdx = 2; mcqJson.put("correct", 2); onUpdate() })
                    OutlinedTextField(
                        value = op3,
                        onValueChange = {
                            op3 = it
                            optionsArr.put(2, it)
                            mcqJson.put("options", optionsArr)
                            onUpdate()
                        },
                        label = { Text("অপশন (গ)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = correctIdx == 3, onClick = { correctIdx = 3; mcqJson.put("correct", 3); onUpdate() })
                    OutlinedTextField(
                        value = op4,
                        onValueChange = {
                            op4 = it
                            optionsArr.put(3, it)
                            mcqJson.put("options", optionsArr)
                            onUpdate()
                        },
                        label = { Text("অপশন (ঘ)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Convert integers to Bengali characters (১, ২, ৩...) for authentic board style
fun toBengaliNumber(number: Int): String {
    val english = number.toString()
    val bengali = StringBuilder()
    for (char in english) {
        val converted = when (char) {
            '0' -> '০'
            '1' -> '১'
            '2' -> '২'
            '3' -> '৩'
            '4' -> '৪'
            '5' -> '৫'
            '6' -> '৬'
            '7' -> '৭'
            '8' -> '৮'
            '9' -> '৯'
            else -> char
        }
        bengali.append(converted)
    }
    return bengali.toString()
}
