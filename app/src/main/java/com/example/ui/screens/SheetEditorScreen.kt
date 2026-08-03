package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.OfficeScreen
import com.example.ui.OfficeViewModel
import org.json.JSONObject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetEditorScreen(
    viewModel: OfficeViewModel,
    docId: String,
    modifier: Modifier = Modifier
) {
    val activeDoc = viewModel.activeDoc
    var title by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    
    // Spreadsheet state
    val cellValues = remember { mutableStateMapOf<String, String>() }
    val cellBolds = remember { mutableStateMapOf<String, Boolean>() }
    val cellColors = remember { mutableStateMapOf<String, String>() }
    
    var selectedCell by remember { mutableStateOf("A1") }
    var formulaInput by remember { mutableStateOf("") }
    var rowCount by remember { mutableStateOf(15) }
    var colCount by remember { mutableStateOf(6) }

    // Parse DB content
    LaunchedEffect(activeDoc) {
        if (activeDoc != null && activeDoc.id == docId) {
            title = activeDoc.title
            try {
                val json = JSONObject(activeDoc.content)
                rowCount = json.optInt("rowCount", 15)
                colCount = json.optInt("colCount", 6)
                
                cellValues.clear()
                cellBolds.clear()
                cellColors.clear()
                
                val cellsJson = json.optJSONObject("cells")
                if (cellsJson != null) {
                    val keys = cellsJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val valueObj = cellsJson.opt(key)
                        if (valueObj is JSONObject) {
                            cellValues[key] = valueObj.optString("value", "")
                            cellBolds[key] = valueObj.optBoolean("bold", false)
                            cellColors[key] = valueObj.optString("color", "")
                        } else if (valueObj is String) {
                            cellValues[key] = valueObj
                        }
                    }
                }
                
                // Initialize selected cell input
                formulaInput = cellValues[selectedCell] ?: ""
            } catch (e: Exception) {
                // fallback empty
            }
        }
    }

    // Save state helper
    val saveSpreadsheet = {
        if (activeDoc != null) {
            val json = JSONObject().apply {
                put("rowCount", rowCount)
                put("colCount", colCount)
                put("cells", JSONObject().apply {
                    cellValues.forEach { (coord, rawValue) ->
                        val cellObj = JSONObject().apply {
                            put("value", rawValue)
                            put("bold", cellBolds[coord] ?: false)
                            put("color", cellColors[coord] ?: "")
                        }
                        put(coord, cellObj)
                    }
                })
            }
            viewModel.saveDocumentContent(json.toString())
        }
    }

    // Sync active cell and formula input
    LaunchedEffect(selectedCell) {
        formulaInput = cellValues[selectedCell] ?: ""
    }

    // Auto-save on edits
    var isSaving by remember { mutableStateOf(false) }
    val onCellValueChanged = { coord: String, value: String ->
        cellValues[coord] = value
        saveSpreadsheet()
        // Trigger recomposition/evaluation
        coroutineScope.launch {
            isSaving = true
            kotlinx.coroutines.delay(400)
            isSaving = false
        }
    }

    // Color definitions
    val lightColors = listOf(
        "" to "Default",
        "#EFF6FF" to "Blue",
        "#DCFCE7" to "Green",
        "#FEF9C3" to "Yellow",
        "#FEE2E2" to "Red"
    )

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
                        modifier = Modifier.fillMaxWidth().testTag("sheet_title_input")
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            Icon(Icons.Default.CloudSync, "Saving", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.CloudQueue, "Saved", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
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
            // Excel Formula input bar (CRITICAL PC-LIKE REQUIREMENT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = selectedCell,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                
                Text(
                    text = "fx",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                OutlinedTextField(
                    value = formulaInput,
                    onValueChange = {
                        formulaInput = it
                        onCellValueChanged(selectedCell, it)
                    },
                    placeholder = { Text("সূত্র লিখুন (=SUM(B2:D2), =A1+B1...)", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("formula_input_field"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }

            // Cell format bar (Bold, Color chooser, clear)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bold cell toggle
                    IconButton(
                        onClick = {
                            val currentBold = cellBolds[selectedCell] ?: false
                            cellBolds[selectedCell] = !currentBold
                            saveSpreadsheet()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (cellBolds[selectedCell] == true) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold cell", modifier = Modifier.size(20.dp))
                    }

                    // Background fill color chooser
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        lightColors.forEach { (hex, name) ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .border(
                                        width = if (cellColors[selectedCell] == hex) 2.dp else 1.dp,
                                        color = if (cellColors[selectedCell] == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        color = if (hex.isEmpty()) Color.White else Color(android.graphics.Color.parseColor(hex)),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        cellColors[selectedCell] = hex
                                        saveSpreadsheet()
                                    }
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Clear Cell
                    TextButton(
                        onClick = {
                            formulaInput = ""
                            onCellValueChanged(selectedCell, "")
                        }
                    ) {
                        Icon(Icons.Default.Backspace, contentDescription = "Clear cell", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সেল মুছুন", fontSize = 11.sp)
                    }

                    // Reset all
                    TextButton(
                        onClick = {
                            cellValues.clear()
                            cellBolds.clear()
                            cellColors.clear()
                            formulaInput = ""
                            saveSpreadsheet()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear sheet", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সব মুছুন", fontSize = 11.sp)
                    }
                }
            }

            // Scrollable Grid Layout
            val gridHorizontalScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFE5E7EB)) // Desk gray grid shadow
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(gridHorizontalScrollState)
                ) {
                    // Header row (A, B, C...)
                    Row(
                        modifier = Modifier.background(Color(0xFFF3F4F6))
                    ) {
                        // Empty top left corner
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(32.dp)
                                .border(0.5.dp, Color.LightGray)
                                .background(Color(0xFFE5E7EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("", fontSize = 11.sp)
                        }
                        
                        // Alphabet Column headers
                        for (col in 0 until colCount) {
                            val colChar = ('A' + col).toString()
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(32.dp)
                                    .border(0.5.dp, Color.LightGray)
                                    .background(Color(0xFFF3F4F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = colChar,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Grid Data Rows
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        items(rowCount) { rowIndex ->
                            val rowNum = rowIndex + 1
                            Row {
                                // Left index number box
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(40.dp)
                                        .border(0.5.dp, Color.LightGray)
                                        .background(Color(0xFFF3F4F6)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rowNum.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Cells in this row
                                for (colIndex in 0 until colCount) {
                                    val colChar = ('A' + colIndex).toString()
                                    val coord = "$colChar$rowNum"
                                    
                                    val rawValue = cellValues[coord] ?: ""
                                    // Evaluate formulas if starts with "="
                                    val evaluatedValue = if (rawValue.startsWith("=")) {
                                        viewModel.evaluateFormula(rawValue, cellValues)
                                    } else {
                                        rawValue
                                    }

                                    val isSelected = selectedCell == coord
                                    val cellBgHex = cellColors[coord] ?: ""
                                    val cellBgColor = when {
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        cellBgHex.isNotEmpty() -> Color(android.graphics.Color.parseColor(cellBgHex))
                                        else -> Color.White
                                    }

                                    Box(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .height(40.dp)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                                            )
                                            .background(cellBgColor)
                                            .clickable { selectedCell = coord }
                                            .padding(horizontal = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = evaluatedValue,
                                            fontSize = 13.sp,
                                            fontWeight = if (cellBolds[coord] == true) FontWeight.Bold else FontWeight.Normal,
                                            color = if (evaluatedValue.startsWith("#")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Simple Excel Formula Cheat Sheet / Quick Reference
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text(
                        text = "সূত্র ব্যবহারের নিয়ম: যোগফলের জন্য '=SUM(B2:D2)', গড় নম্বরের জন্য '=AVERAGE(E2:E4)', গণনার জন্য '=COUNT(B2:D2)' এবং সরাসরি গণিত করার জন্য '=B2+C2' লিখুন।",
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
