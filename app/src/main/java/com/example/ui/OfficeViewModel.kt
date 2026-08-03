package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiService
import com.example.data.OfficeDocument
import com.example.data.OfficeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class OfficeScreen {
    object Dashboard : OfficeScreen()
    data class DocEditor(val docId: String) : OfficeScreen()
    data class SheetEditor(val docId: String) : OfficeScreen()
    data class SlideEditor(val docId: String) : OfficeScreen()
    data class HscEditor(val docId: String) : OfficeScreen()
}

class OfficeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: OfficeRepository
    
    // Screens navigation state
    var currentScreen by mutableStateOf<OfficeScreen>(OfficeScreen.Dashboard)
        private set

    // DB Documents flow
    private val _documents = MutableStateFlow<List<OfficeDocument>>(emptyList())
    val documents: StateFlow<List<OfficeDocument>> = _documents.asStateFlow()

    // Active document being edited
    var activeDoc by mutableStateOf<OfficeDocument?>(null)
        private set

    // AI Status
    var isAiGenerating by mutableStateOf(false)
        private set
    var aiError by mutableStateOf<String?>(null)
        private set

    init {
        val database = AppDatabase.getDatabase(application)
        repository = OfficeRepository(database.officeDao())
        
        // Observe documents
        viewModelScope.launch {
            repository.allDocuments.collect { docs ->
                if (docs.isEmpty()) {
                    preloadTemplates()
                } else {
                    _documents.value = docs
                }
            }
        }
    }

    fun navigateTo(screen: OfficeScreen) {
        currentScreen = screen
        if (screen is OfficeScreen.Dashboard) {
            activeDoc = null
        } else {
            // Load specific document
            val id = when (screen) {
                is OfficeScreen.DocEditor -> screen.docId
                is OfficeScreen.SheetEditor -> screen.docId
                is OfficeScreen.SlideEditor -> screen.docId
                is OfficeScreen.HscEditor -> screen.docId
                else -> ""
            }
            if (id.isNotEmpty()) {
                viewModelScope.launch {
                    val doc = repository.getDocumentById(id)
                    activeDoc = doc
                }
            }
        }
    }

    // Preload HSC Question Templates
    private fun preloadTemplates() {
        viewModelScope.launch {
            val templates = listOf(
                OfficeDocument(
                    id = "template-hsc-ict",
                    title = "এইচএসসি আইসিটি নির্বাচনী পরীক্ষা - ২০২৬",
                    type = "HSC",
                    content = getIctTemplateJson(),
                    lastModified = System.currentTimeMillis() - 1000
                ),
                OfficeDocument(
                    id = "template-doc-summary",
                    title = "উচ্চ মাধ্যমিক পরীক্ষার প্রস্তুতি গাইড",
                    type = "DOC",
                    content = "এইচএসসি পরীক্ষার প্রস্তুতির জন্য একটি সুনির্দিষ্ট গাইডলাইন নিচে তুলে ধরা হলো:\n\n১. পদার্থবিজ্ঞান: গাণিতিক সমস্যাগুলোর প্রতি জোর দিন। বিশেষ করে ভেক্টর, গতিবিদ্যা ও চলতড়িৎ অধ্যায়ের সূত্রগুলো খাতায় লিখে অনুশীলন করুন।\n২. আইসিটি: সংখ্যা পদ্ধতি ও এইচটিএমএল (HTML) কোডিং সরাসরি লিখে প্র্যাকটিস করুন। সি প্রোগ্রামিং এর ক্ষেত্রে লজিকগুলো মাথায় রাখুন।\n৩. বাংলা: সৃজনশীল প্রশ্নের (ক) ও (খ) অংশের উত্তর সংক্ষেপ ও স্পষ্ট হওয়া উচিত। কবি পরিচিতি ও মূলভাব বেশি বেশি পড়ুন।\n\nপরীক্ষায় ভালো করার মূল চাবিকাঠি হলো সময়ের সঠিক ব্যবহার এবং পরিষ্কার পরিচ্ছন্ন লেখা উপস্থাপন।",
                    lastModified = System.currentTimeMillis() - 2000
                ),
                OfficeDocument(
                    id = "template-sheet-marks",
                    title = "এইচএসসি আইসিটি মার্কশিট ক্যালকুলেটর",
                    type = "SHEET",
                    content = getSheetTemplateJson(),
                    lastModified = System.currentTimeMillis() - 3000
                ),
                OfficeDocument(
                    id = "template-slide-intro",
                    title = "আইসিটি ১ম অধ্যায়: বায়োমেট্রিক্স",
                    type = "SLIDE",
                    content = getSlideTemplateJson(),
                    lastModified = System.currentTimeMillis() - 4000
                )
            )
            templates.forEach { repository.insertDocument(it) }
        }
    }

    // --- CRUD Operations ---
    
    fun createDocument(type: String, customTitle: String? = null) {
        viewModelScope.launch {
            val uuid = UUID.randomUUID().toString()
            val title = customTitle ?: when (type) {
                "DOC" -> "নতুন ডকুমেন্ট ${documents.value.filter { it.type == "DOC" }.size + 1}"
                "SHEET" -> "নতুন স্প্রেডশিট ${documents.value.filter { it.type == "SHEET" }.size + 1}"
                "SLIDE" -> "নতুন প্রেজেন্টেশন ${documents.value.filter { it.type == "SLIDE" }.size + 1}"
                "HSC" -> "নতুন এইচএসসি প্রশ্নপত্র ${documents.value.filter { it.type == "HSC" }.size + 1}"
                else -> "নতুন ফাইল"
            }

            val defaultContent = when (type) {
                "DOC" -> ""
                "SHEET" -> "{\"cells\":{},\"rowCount\":30,\"colCount\":10}"
                "SLIDE" -> "{\"slides\":[{\"title\":\"স্লাইডের শিরোনাম\",\"body\":\"• এখানে আপনার বিষয়বস্তু বুলেট আকারে লিখুন।\\n• পিসির মতো চমৎকার প্রেজেন্টেশন তৈরি করুন।\",\"bg\":\"#1E3A8A\"}]}"
                "HSC" -> getEmptyHscJson(title)
                else -> ""
            }

            val newDoc = OfficeDocument(
                id = uuid,
                title = title,
                type = type,
                content = defaultContent,
                lastModified = System.currentTimeMillis()
            )
            repository.insertDocument(newDoc)
            navigateTo(
                when (type) {
                    "DOC" -> OfficeScreen.DocEditor(uuid)
                    "SHEET" -> OfficeScreen.SheetEditor(uuid)
                    "SLIDE" -> OfficeScreen.SlideEditor(uuid)
                    "HSC" -> OfficeScreen.HscEditor(uuid)
                    else -> OfficeScreen.Dashboard
                }
            )
        }
    }

    fun saveDocumentContent(contentStr: String) {
        val current = activeDoc ?: return
        viewModelScope.launch {
            val updated = current.copy(
                content = contentStr,
                lastModified = System.currentTimeMillis()
            )
            repository.insertDocument(updated)
            activeDoc = updated
        }
    }

    fun renameDocument(id: String, newTitle: String) {
        viewModelScope.launch {
            val doc = repository.getDocumentById(id) ?: return@launch
            val updated = doc.copy(
                title = newTitle,
                lastModified = System.currentTimeMillis()
            )
            repository.insertDocument(updated)
            if (activeDoc?.id == id) {
                activeDoc = updated
            }
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            repository.deleteDocumentById(id)
            if (activeDoc?.id == id) {
                navigateTo(OfficeScreen.Dashboard)
            }
        }
    }

    // --- AI Features (Gemini) ---

    fun askGemini(prompt: String, onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            isAiGenerating = true
            aiError = null
            val result = GeminiService.generate(prompt)
            isAiGenerating = false
            result.onSuccess {
                onCompleted(it)
            }.onFailure {
                aiError = it.message ?: "কিছু একটা ভুল হয়েছে। দয়া করে আবার চেষ্টা করুন।"
            }
        }
    }

    // Auto-generate HSC Creative Question using Gemini
    fun generateHscCQAi(subject: String, topic: String, onGenerated: (String, String, String, String, String) -> Unit) {
        val prompt = """
            You are an expert Bangladesh HSC Board examiner.
            Please generate a highly standard HSC Creative Question (সৃজনশীল প্রশ্ন - CQ) in Bengali for Subject: $subject, Chapter/Topic: $topic.
            The question must contain:
            1. An interesting real-life Stimulus/Scenario (উদ্দীপক).
            2. Question (ক) - Knowledge (জ্ঞানমূলক, 1 mark).
            3. Question (খ) - Comprehension (অনুধাবনমূলক, 2 marks).
            4. Question (গ) - Application (প্রয়োগমূলক, 3 marks).
            5. Question (ঘ) - Higher Ability (উচ্চতর দক্ষতা, 4 marks).

            Output format must be strictly a valid JSON object in this exact schema, without any markdown formatting wrappers or ```json codeblocks. Output ONLY the JSON:
            {
              "stem": "The stimulus text in Bengali",
              "qA": "Question A in Bengali",
              "qB": "Question B in Bengali",
              "qC": "Question C in Bengali",
              "qD": "Question D in Bengali"
            }
        """.trimIndent()

        askGemini(prompt) { jsonText ->
            try {
                // Strip markdown wrappers if any
                val cleanJson = jsonText.substringAfter("{").substringBeforeLast("}")
                val json = JSONObject("{$cleanJson}")
                val stem = json.optString("stem", "")
                val qA = json.optString("qA", "")
                val qB = json.optString("qB", "")
                val qC = json.optString("qC", "")
                val qD = json.optString("qD", "")
                if (stem.isNotEmpty()) {
                    onGenerated(stem, qA, qB, qC, qD)
                }
            } catch (e: Exception) {
                aiError = "প্রশ্ন তৈরি করার সময়ে ত্রুটি হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
            }
        }
    }

    // Auto-generate HSC MCQ Questions using Gemini
    fun generateHscMcqAi(subject: String, topic: String, count: Int = 5, onGenerated: (JSONArray) -> Unit) {
        val prompt = """
            You are an expert Bangladesh HSC Board examiner.
            Please generate $count highly standard HSC Multiple Choice Questions (বহুনির্বাচনী প্রশ্ন - MCQ) in Bengali for Subject: $subject, Chapter/Topic: $topic.
            
            Each MCQ must contain:
            1. The question text in Bengali.
            2. Exactly 4 options in Bengali.
            3. The correct answer index (0, 1, 2, or 3).

            Output format must be strictly a valid JSON array of objects in this exact schema, without any markdown formatting wrappers or ```json codeblocks. Output ONLY the JSON array:
            [
              {
                "question": "Question text in Bengali",
                "options": ["Option 1", "Option 2", "Option 3", "Option 4"],
                "correct": 0
              }
            ]
        """.trimIndent()

        askGemini(prompt) { jsonText ->
            try {
                val cleanJson = jsonText.substringAfter("[").substringBeforeLast("]")
                val array = JSONArray("[$cleanJson]")
                if (array.length() > 0) {
                    onGenerated(array)
                }
            } catch (e: Exception) {
                aiError = "বহুনির্বাচনী প্রশ্ন তৈরি করার সময়ে ত্রুটি হয়েছে।"
            }
        }
    }

    // --- Spreadsheet Formula Evaluator Engine ---
    fun evaluateFormula(cellValue: String, cells: Map<String, String>): String {
        if (!cellValue.startsWith("=")) return cellValue
        
        try {
            val uppercaseFormula = cellValue.uppercase().trim()
            
            // 1. SUM function, e.g., =SUM(B2:B5)
            if (uppercaseFormula.startsWith("=SUM(") && uppercaseFormula.endsWith(")")) {
                val rangeStr = uppercaseFormula.removePrefix("=SUM(").removeSuffix(")")
                val cellValues = getCellValuesFromRange(rangeStr, cells)
                val sum = cellValues.sumOf { it.toDoubleOrNull() ?: 0.0 }
                return formatDouble(sum)
            }
            
            // 2. AVERAGE function, e.g., =AVERAGE(B2:B5)
            if (uppercaseFormula.startsWith("=AVERAGE(") && uppercaseFormula.endsWith(")")) {
                val rangeStr = uppercaseFormula.removePrefix("=AVERAGE(").removeSuffix(")")
                val cellValues = getCellValuesFromRange(rangeStr, cells)
                val doubles = cellValues.mapNotNull { it.toDoubleOrNull() }
                if (doubles.isEmpty()) return "0"
                val avg = doubles.average()
                return formatDouble(avg)
            }

            // 3. COUNT function, e.g., =COUNT(B2:B5)
            if (uppercaseFormula.startsWith("=COUNT(") && uppercaseFormula.endsWith(")")) {
                val rangeStr = uppercaseFormula.removePrefix("=COUNT(").removeSuffix(")")
                val cellValues = getCellValuesFromRange(rangeStr, cells)
                val count = cellValues.count { it.toDoubleOrNull() != null }
                return count.toString()
            }

            // 4. Simple arithmetic between two cells, e.g., =A1+B1, =A1*B1
            val expression = uppercaseFormula.removePrefix("=")
            val operators = listOf("+", "-", "*", "/")
            for (op in operators) {
                if (expression.contains(op)) {
                    val parts = expression.split(op)
                    if (parts.size == 2) {
                        val val1 = getCellValueEvaluated(parts[0].trim(), cells).toDoubleOrNull() ?: 0.0
                        val val2 = getCellValueEvaluated(parts[1].trim(), cells).toDoubleOrNull() ?: 0.0
                        val result = when (op) {
                            "+" -> val1 + val2
                            "-" -> val1 - val2
                            "*" -> val1 * val2
                            "/" -> if (val2 != 0.0) val1 / val2 else Double.NaN
                            else -> 0.0
                        }
                        if (result.isNaN()) return "#DIV/0!"
                        return formatDouble(result)
                    }
                }
            }

            // 5. Reference to another cell, e.g., =A1
            if (expression.matches(Regex("^[A-Z]+\\d+$"))) {
                return getCellValueEvaluated(expression, cells)
            }

        } catch (e: Exception) {
            return "#ERROR!"
        }
        
        return "#VALUE!"
    }

    private fun getCellValueEvaluated(cellName: String, cells: Map<String, String>): String {
        val raw = cells[cellName] ?: return "0"
        if (raw.startsWith("=")) {
            return evaluateFormula(raw, cells)
        }
        return raw
    }

    private fun getCellValuesFromRange(rangeStr: String, cells: Map<String, String>): List<String> {
        val parts = rangeStr.split(":")
        if (parts.size != 2) return emptyList()
        
        val startCell = parts[0].trim()
        val endCell = parts[1].trim()
        
        val startCol = startCell.takeWhile { it.isLetter() }
        val startRow = startCell.dropWhile { it.isLetter() }.toIntOrNull() ?: return emptyList()
        
        val endCol = endCell.takeWhile { it.isLetter() }
        val endRow = endCell.dropWhile { it.isLetter() }.toIntOrNull() ?: return emptyList()
        
        if (startCol.length != 1 || endCol.length != 1) return emptyList()
        
        val colStartChar = startCol[0]
        val colEndChar = endCol[0]
        
        val list = mutableListOf<String>()
        for (col in colStartChar..colEndChar) {
            for (row in startRow..endRow) {
                val cellName = "$col$row"
                val value = getCellValueEvaluated(cellName, cells)
                list.add(value)
            }
        }
        return list
    }

    private fun formatDouble(d: Double): String {
        return if (d % 1.0 == 0.0) {
            d.toInt().toString()
        } else {
            String.format("%.2f", d)
        }
    }

    // --- JSON template helpers ---

    private fun getEmptyHscJson(title: String): String {
        return JSONObject().apply {
            put("header", JSONObject().apply {
                put("board", "ঢাকা শিক্ষা বোর্ড")
                put("subject", title)
                put("subjectCode", "২৭৫")
                put("time", "৩ ঘণ্টা")
                put("fullMarks", "১০০")
            })
            put("cqs", JSONArray())
            put("mcqs", JSONArray())
        }.toString()
    }

    private fun getIctTemplateJson(): String {
        return JSONObject().apply {
            put("header", JSONObject().apply {
                put("board", "ঢাকা বোর্ড - এইচএসসি নির্বাচনী পরীক্ষা")
                put("subject", "তথ্য ও যোগাযোগ প্রযুক্তি (ICT)")
                put("subjectCode", "২৭৫")
                put("time", "৩ ঘণ্টা")
                put("fullMarks", "১০০")
            })
            put("cqs", JSONArray().apply {
                put(JSONObject().apply {
                    put("stem", "রহিম সাহেব তাঁর অফিসে এমন একটি প্রযুক্তি ব্যবহার করেন যেখানে কর্মচারীরা হাতের আঙুলের ছাপ দিয়ে হাজিরা দেয়। কিন্তু তাঁর ছেলে করিম একটি সফটওয়্যার ব্যবহার করে যা মানুষের কণ্ঠস্বর শুনে লক খুলতে পারে।")
                    put("qA", "বায়োমেট্রিক্স কী?")
                    put("qB", "ক্রায়োসার্জারিতে ঠাণ্ডা বাতাস কীভাবে কাজ করে? বুঝিয়ে লেখ।")
                    put("qC", "রহিম সাহেবের অফিসে ব্যবহৃত প্রযুক্তিটি ব্যাখ্যা কর।")
                    put("qD", "করিমের ব্যবহৃত প্রযুক্তি এবং রহিম সাহেবের প্রযুক্তির তুলনামূলক বিশ্লেষণ কর।")
                })
                put(JSONObject().apply {
                    put("stem", "উদ্দীপক: কানিজ তাসনিম তার ব্যক্তিগত ডায়েরিতে কিছু গুরুত্বপূর্ণ বিষয় লিখে রাখতে চায়। তবে সে কম্পিউটার বিজ্ঞান নিয়ে পড়াশোনা করায় একটি বিশেষ ভাষা ব্যবহার করে তার ডায়েরিটিকে একটি ছোট ওয়েবসাইটে রূপান্তর করেছে, যেন তা ব্রাউজার দিয়ে পড়া যায়।")
                    put("qA", "HTML এর পূর্ণরূপ কী?")
                    put("qB", "ডোমেইন নেম ও আইপি এড্রেসের মধ্যে পার্থক্য ব্যাখ্যা কর।")
                    put("qC", "কানিজ তাসনিম ডায়েরিটি তৈরিতে যে ভাষা ব্যবহার করেছে তার গঠন ব্যাখ্যা কর।")
                    put("qD", "একটি সাধারণ ওয়েবসাইটের চেয়ে তার ব্যবহৃত ভাষার সুবিধাসমূহ বিশ্লেষণ কর।")
                })
            })
            put("mcqs", JSONArray().apply {
                put(JSONObject().apply {
                    put("question", "নিচের কোনটি বায়োমেট্রিক্স এর আচরণগত বৈশিষ্ট্য?")
                    put("options", JSONArray().apply {
                        put("আঙুলের ছাপ")
                        put("কণ্ঠস্বর")
                        put("ডিএনএ")
                        put("আইরিশ")
                    })
                    put("correct", 1)
                })
                put(JSONObject().apply {
                    put("question", "এইচটিএমএল (HTML) এর জনক কে?")
                    put("options", JSONArray().apply {
                        put("টিম বার্নার্স লি")
                        put("মার্ক জাকারবার্গ")
                        put("বিল গেটস")
                        put("স্ティブ জবস")
                    })
                    put("correct", 0)
                })
                put(JSONObject().apply {
                    put("question", "ক্রায়োসার্জারিতে নিচের কোন গ্যাসটি ব্যবহার করা হয়?")
                    put("options", JSONArray().apply {
                        put("অক্সিজেন")
                        put("তরল নাইট্রোজেন")
                        put("হাইড্রোজেন")
                        put("কার্বন ডাই অক্সাইড")
                    })
                    put("correct", 1)
                })
            })
        }.toString()
    }

    private fun getSheetTemplateJson(): String {
        return JSONObject().apply {
            put("cells", JSONObject().apply {
                put("A1", "শিক্ষার্থীর নাম")
                put("B1", "সৃজনশীল (CQ)")
                put("C1", "বহুনির্বাচনী (MCQ)")
                put("D1", "ব্যবহারিক (PR)")
                put("E1", "মোট নম্বর")

                put("A2", "মারুফ বিল্লাহ")
                put("B2", "৪৫")
                put("C2", "২২")
                put("D2", "২৪")
                put("E2", "=SUM(B2:D2)")

                put("A3", "তাহসিন আহমেদ")
                put("B3", "৩৮")
                put("C3", "২৫")
                put("D3", "২৩")
                put("E3", "=SUM(B3:D3)")

                put("A4", "রাফসান ইসলাম")
                put("B4", "৪২")
                put("C4", "১৮")
                put("D4", "২৪")
                put("E4", "=SUM(B4:D4)")

                put("A5", "গড় নম্বর")
                put("E5", "=AVERAGE(E2:E4)")
            })
            put("rowCount", 15)
            put("colCount", 6)
        }.toString()
    }

    private fun getSlideTemplateJson(): String {
        return JSONObject().apply {
            put("slides", JSONArray().apply {
                put(JSONObject().apply {
                    put("title", "এইচএসসি আইসিটি ক্লাস")
                    put("body", "• অধ্যায় ১: বিশ্ব ও বাংলাদেশ প্রেক্ষিত\n• আজকের আলোচনার বিষয়: বায়োমেট্রিক্স ও ভার্চুয়াল রিয়েলিটি\n• লেকচারার: মারুফ বিল্লাহ")
                    put("bg", "#1E3A8A")
                })
                put(JSONObject().apply {
                    put("title", "বায়োমেট্রিক্স কী?")
                    put("body", "• বায়োমেট্রিক্স হলো এমন একটি প্রযুক্তি যেখানে মানুষের শারীরিক গঠন এবং আচরণগত বৈশিষ্ট্যের ওপর ভিত্তি করে তাকে অদ্বিতীয়ভাবে সনাক্ত করা হয়।\n• ব্যবহার: নিরাপত্তা ব্যবস্থা, উপস্থিতি সনাক্তকরণ, পাসপোর্ট ভেরিফিকেশন।")
                    put("bg", "#0F766E")
                })
                put(JSONObject().apply {
                    put("title", "ভার্চুয়াল রিয়েলিটি (VR)")
                    put("body", "• এটি এমন একটি কম্পিউটার নিয়ন্ত্রিত পরিবেশ যা দেখতে বাস্তব মনে হয় কিন্তু আসলে কৃত্রিম।\n• ব্যবহারের ক্ষেত্র: চিকিৎসা ক্ষেত্রে সার্জারি ট্রেইনিং, বিমান চালনা ট্রেইনিং, গেমিং শিল্প ও যুদ্ধ কৌশল অনুশীলন।")
                    put("bg", "#6B21A8")
                })
            })
        }.toString()
    }
}
