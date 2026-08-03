package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OfficeScreen
import com.example.ui.OfficeViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideEditorScreen(
    viewModel: OfficeViewModel,
    docId: String,
    modifier: Modifier = Modifier
) {
    val activeDoc = viewModel.activeDoc
    var title by remember { mutableStateOf("") }
    
    // Slide deck state
    val slides = remember { mutableStateListOf<JSONObject>() }
    var activeSlideIndex by remember { mutableStateOf(0) }
    
    // Immersive Presentation Mode state
    var isPresentationMode by remember { mutableStateOf(false) }

    // Synchronize initial state
    LaunchedEffect(activeDoc) {
        if (activeDoc != null && activeDoc.id == docId) {
            title = activeDoc.title
            try {
                val json = JSONObject(activeDoc.content)
                val slidesArray = json.optJSONArray("slides")
                slides.clear()
                if (slidesArray != null && slidesArray.length() > 0) {
                    for (i in 0 until slidesArray.length()) {
                        slides.add(slidesArray.getJSONObject(i))
                    }
                } else {
                    slides.add(JSONObject().apply {
                        put("title", "স্লাইডের শিরোনাম")
                        put("body", "• বিষয়বস্তু বুলেট আকারে লিখুন।")
                        put("bg", "#1E3A8A")
                    })
                }
            } catch (e: Exception) {
                // fallback empty
            }
        }
    }

    // Save slides to DB
    val saveSlides = {
        if (activeDoc != null) {
            val json = JSONObject().apply {
                val array = JSONArray()
                slides.forEach { array.put(it) }
                put("slides", array)
            }
            viewModel.saveDocumentContent(json.toString())
        }
    }

    // Active slide detail fields
    val activeSlide = slides.getOrNull(activeSlideIndex)
    var slideTitle by remember { mutableStateOf("") }
    var slideBody by remember { mutableStateOf("") }
    var slideBg by remember { mutableStateOf("#1E3A8A") }

    LaunchedEffect(activeSlideIndex, slides.size) {
        val current = slides.getOrNull(activeSlideIndex)
        if (current != null) {
            slideTitle = current.optString("title", "")
            slideBody = current.optString("body", "")
            slideBg = current.optString("bg", "#1E3A8A")
        }
    }

    val updateActiveSlide = { t: String, b: String, bg: String ->
        val current = slides.getOrNull(activeSlideIndex)
        if (current != null) {
            current.put("title", t)
            current.put("body", b)
            current.put("bg", bg)
            saveSlides()
        }
    }

    val bgThemes = listOf(
        "#1E3A8A" to "Indigo Blue",
        "#0F766E" to "Dark Teal",
        "#6B21A8" to "Mystic Purple",
        "#1F2937" to "Carbon Dark",
        "#B91C1C" to "Crimson Red",
        "#4B5563" to "Slate Gray"
    )

    if (isPresentationMode) {
        // Immersive presentation full screen player
        FullPresentationPlayer(
            slides = slides,
            initialIndex = activeSlideIndex,
            onClose = { isPresentationMode = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotEmpty()) {
                                viewModel.renameDocument(docId, it)
                            }
                        },
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
                        modifier = Modifier.fillMaxWidth().testTag("slide_title_input")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(OfficeScreen.Dashboard) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { isPresentationMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("start_presentation_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("প্রেজেন্টেশন শুরু")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Left sidebar: Slides list thumbnails (PC split view design)
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(width = 0.5.dp, color = Color.LightGray)
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(slides) { idx, item ->
                        val itemBgHex = item.optString("bg", "#1E3A8A")
                        val itemBgColor = Color(android.graphics.Color.parseColor(itemBgHex))
                        
                        val isSelected = activeSlideIndex == idx

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { activeSlideIndex = idx }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp, 50.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(itemBgColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = item.optString("title", "Slide"),
                                    fontSize = 7.sp,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Add slide floating button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            val newSlide = JSONObject().apply {
                                put("title", "নতুন স্লাইড ${slides.size + 1}")
                                put("body", "• নতুন বিষয়বস্তু যোগ করুন।")
                                put("bg", bgThemes.getOrNull(slides.size % bgThemes.size)?.first ?: "#1E3A8A")
                            }
                            slides.add(newSlide)
                            activeSlideIndex = slides.size - 1
                            saveSlides()
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Slide")
                    }
                    Text("যোগ করুন", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                }
            }

            // Right side: Active Slide Editor Canvas
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFF3F4F6)) // desk background
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Slide canvas mockup
                val canvasColor = Color(android.graphics.Color.parseColor(slideBg))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(canvasColor)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (slideTitle.isEmpty()) "স্লাইডের শিরোনাম" else slideTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = if (slideBody.isEmpty()) "• বিবরণ..." else slideBody,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(top = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "স্লাইড ${activeSlideIndex + 1}/${slides.size}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Edit inputs for Slide
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("স্লাইড এডিট করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            // Delete active slide if more than 1 slide exists
                            if (slides.size > 1) {
                                TextButton(
                                    onClick = {
                                        slides.removeAt(activeSlideIndex)
                                        activeSlideIndex = (activeSlideIndex - 1).coerceAtLeast(0)
                                        saveSlides()
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete slide", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("স্লাইড বাদ দিন", fontSize = 11.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = slideTitle,
                            onValueChange = {
                                slideTitle = it
                                updateActiveSlide(it, slideBody, slideBg)
                            },
                            label = { Text("স্লাইডের প্রধান শিরোনাম") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("slide_title_editor_field")
                        )

                        OutlinedTextField(
                            value = slideBody,
                            onValueChange = {
                                slideBody = it
                                updateActiveSlide(slideTitle, it, slideBg)
                            },
                            label = { Text("স্লাইড বিবরণ / বুলেট তালিকা") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("slide_body_editor_field")
                        )

                        // Bg customize row
                        Text("স্লাইডের ব্যাকগ্রাউন্ড থিম", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bgThemes.forEach { (hex, name) ->
                                val colorObj = Color(android.graphics.Color.parseColor(hex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colorObj)
                                        .border(
                                            width = if (slideBg == hex) 2.5.dp else 1.dp,
                                            color = if (slideBg == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            slideBg = hex
                                            updateActiveSlide(slideTitle, slideBody, hex)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullPresentationPlayer(
    slides: List<JSONObject>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val currentSlide = slides.getOrNull(currentIndex)
    
    val bgHex = currentSlide?.optString("bg", "#1E3A8A") ?: "#1E3A8A"
    val bgColor = Color(android.graphics.Color.parseColor(bgHex))
    val titleStr = currentSlide?.optString("title", "") ?: ""
    val bodyStr = currentSlide?.optString("body", "") ?: ""

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(32.dp)
    ) {
        // Overlay tap zones for PC slides click-next
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable {
                        if (currentIndex > 0) currentIndex--
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .clickable {
                        if (currentIndex < slides.size - 1) currentIndex++
                    }
            )
        }

        // Slideshow content presentation
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            // Close player button at top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Exit slideshow", tint = Color.White)
                }
                
                Text(
                    text = "${currentIndex + 1} / ${slides.size}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Big heading display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 40.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleStr,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 40.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = bodyStr,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Swipe instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "পেছনে যেতে বাম দিকে এবং সামনে যেতে ডান দিকে ট্যাপ করুন।",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
