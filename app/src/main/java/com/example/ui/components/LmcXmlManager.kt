package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LmcConfig
import com.example.ui.LmcViewModel

@Composable
fun LmcXmlManager(
    viewModel: LmcViewModel,
    modifier: Modifier = Modifier
) {
    val configs by viewModel.configsList.collectAsState()
    val activeConfigName by viewModel.activeConfigName.collectAsState()

    var isAddingConfig by remember { mutableStateOf(false) }
    var configNameInput by remember { mutableStateOf("") }
    var configDescInput by remember { mutableStateOf("") }
    var watermarkInput by remember { mutableStateOf("") }

    var selectedStylesTab by remember { mutableStateOf("All") } // "All", "System", "Custom"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0F))
            .padding(16.dp)
    ) {
        // XML Manager Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LMC 8.4 XML PROFILES",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Load or capture customized lens calibrations",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { viewModel.navigateTo("camera") },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .testTag("xml_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close XML Manager",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Create Custom XML Card Button
        if (!isAddingConfig) {
            Button(
                onClick = {
                    configNameInput = "LMC_Tuned_${(10..99).random()}"
                    configDescInput = "User custom lens profile with active slider settings."
                    watermarkInput = "LMC 8.4 • LEICA SPECIAL"
                    isAddingConfig = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFDD835).copy(alpha = 0.15f),
                    contentColor = Color(0xFFFDD835)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFFDD835).copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .testTag("show_add_xml_btn")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = "Save slider")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bake Current Sliders as XML Preset", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Expandable XML Creator Card Form
        AnimatedVisibility(
            visible = isAddingConfig,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14141A), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Bake Sliders into XML Config",
                    color = Color(0xFFFDD835),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = configNameInput,
                    onValueChange = { configNameInput = it },
                    label = { Text("Profile Filename (.xml)", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFDD835),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color(0xFFFDD835)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("xml_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = configDescInput,
                    onValueChange = { configDescInput = it },
                    label = { Text("Calibration Description", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFDD835),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color(0xFFFDD835)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = watermarkInput,
                    onValueChange = { watermarkInput = it },
                    label = { Text("Leica Watermark Branding Text", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFDD835),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = Color(0xFFFDD835)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { isAddingConfig = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (configNameInput.isNotEmpty()) {
                                viewModel.saveAsCustomConfig(
                                    name = configNameInput,
                                    description = configDescInput,
                                    watermark = watermarkInput
                                )
                                isAddingConfig = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFDD835), contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_xml_submit_btn")
                    ) {
                        Text("Save Calibration", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Category Filters (All, System, Custom User)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF14141A), RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("All", "System", "Custom")
            tabs.forEach { tab ->
                val isSelected = selectedStylesTab == tab
                val bg = if (isSelected) Color(0xFF24242D) else Color.Transparent
                val borderCol = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent
                val tc = if (isSelected) Color(0xFFFDD835) else Color.White.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                        .clickable { selectedStylesTab = tab }
                        .padding(vertical = 8.dp)
                        .testTag("xml_tab_$tab"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = tc,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Config lists
        val filteredConfigs = remember(configs, selectedStylesTab) {
            when (selectedStylesTab) {
                "System" -> configs.filter { it.isSystemPreset }
                "Custom" -> configs.filter { !it.isSystemPreset }
                else -> configs
            }
        }

        if (filteredConfigs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Empty XML",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No compatible configuration xml files",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Customize sliders and click 'Bake Sliders' above.",
                        color = Color.White.copy(alpha = 0.25f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredConfigs, key = { it.id }) { config ->
                    val isActive = activeConfigName == config.name
                    XmlPresetItem(
                        config = config,
                        isActive = isActive,
                        onLoad = {
                            viewModel.loadConfigPreset(config)
                            viewModel.navigateTo("camera")
                        },
                        onDelete = {
                            viewModel.deleteConfig(config.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun XmlPresetItem(
    config: LmcConfig,
    isActive: Boolean,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val cardColor = if (isActive) Color(0xFF1D1B13) else Color(0xFF141418)
    val borderColor = if (isActive) Color(0xFFFDD835).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = config.name,
                        color = if (isActive) Color(0xFFFDD835) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    // Profile Category Label
                    Box(
                        modifier = Modifier
                            .background(
                                if (config.isSystemPreset) Color(0xFFFDD835).copy(alpha = 0.12f) else Color(0xFF00E5FF).copy(alpha = 0.12f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (config.isSystemPreset) "LMC ENGINE" else "USER XML",
                            color = if (config.isSystemPreset) Color(0xFFFDD835) else Color(0xFF00E5FF),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = config.description,
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            // Load trigger state
            Button(
                onClick = onLoad,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) Color(0xFFFDD835) else Color.White.copy(alpha = 0.06f),
                    contentColor = if (isActive) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .testTag("apply_xml_btn_${config.id}")
                    .padding(start = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.Done else Icons.Default.FolderOpen,
                        contentDescription = "Apply Preset",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isActive) "ACTIVE" else "LOAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Specs pill row inside the config card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val configIso = if (config.iso == -1) "Auto ISO" else "ISO ${config.iso}"
            val configFocus = if (config.focusValue == -1.0f) "Auto Focus" else "MF ${(config.focusValue * 100).toInt()}%"
            val configWatermark = if (config.leicaWatermark) "Watermark: Yes" else "Watermark: No"

            Column {
                Text("EXPOSURE", color = Color.White.copy(alpha = 0.35f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("EV ${config.ev} • ${config.shutterSpeed}", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Divider(modifier = Modifier.height(18.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))

            Column {
                Text("SENSORS", color = Color.White.copy(alpha = 0.35f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("$configIso • $configFocus", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            Divider(modifier = Modifier.height(18.dp).width(1.dp), color = Color.White.copy(alpha = 0.1f))

            Column {
                Text("BRAND STYLE", color = Color.White.copy(alpha = 0.35f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("Leica ${config.leicaStyle} (${config.hdrMode})", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }

            if (!config.isSystemPreset) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
                        .testTag("delete_xml_btn_${config.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Configuration Preset",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
