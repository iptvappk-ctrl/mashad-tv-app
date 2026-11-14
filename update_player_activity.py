with open('MadarTV/madar-player/src/main/java/com/madar/player/PlayerActivity.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
i = 0
while i < len(lines):
    line = lines[i]
    if 'val qualityChoices = listOf(' in line:
        new_lines.append(line)
        new_lines.append('\n')
        new_lines.append('    var showControls by remember { mutableStateOf(true) }\n')
        new_lines.append('    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }\n')
        new_lines.append('\n')
        new_lines.append('    LaunchedEffect(showControls, lastInteractionTime) {\n')
        new_lines.append('        if (showControls) {\n')
        new_lines.append('            kotlinx.coroutines.delay(4000)\n')
        new_lines.append('            showControls = false\n')
        new_lines.append('        }\n')
        new_lines.append('    }\n')
        i += 1
        continue

    if 'Box(Modifier.fillMaxSize().background(Color.Black)) {' in line:
        new_lines.append('    Box(\n')
        new_lines.append('        Modifier\n')
        new_lines.append('            .fillMaxSize()\n')
        new_lines.append('            .background(Color.Black)\n')
        new_lines.append('            .clickable(\n')
        new_lines.append('                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },\n')
        new_lines.append('                indication = null\n')
        new_lines.append('            ) {\n')
        new_lines.append('                showControls = !showControls\n')
        new_lines.append('                if (showControls) lastInteractionTime = System.currentTimeMillis()\n')
        new_lines.append('            }\n')
        new_lines.append('    ) {\n')
        i += 1
        continue

    if '// Top Navigation Bar' in line:
        # Skip until the end of the top bar row
        # We will write the new top bar with AnimatedVisibility
        new_lines.append('        // Top Navigation Bar (Close button + Quality Picker) - Auto-hides; no title blocking match scoreboard\n')
        new_lines.append('        androidx.compose.animation.AnimatedVisibility(\n')
        new_lines.append('            visible = showControls,\n')
        new_lines.append('            enter = androidx.compose.animation.fadeIn(),\n')
        new_lines.append('            exit = androidx.compose.animation.fadeOut(),\n')
        new_lines.append('            modifier = Modifier.align(Alignment.TopStart)\n')
        new_lines.append('        ) {\n')
        new_lines.append('            Row(\n')
        new_lines.append('                modifier = Modifier\n')
        new_lines.append('                    .fillMaxWidth()\n')
        new_lines.append('                    .padding(16.dp),\n')
        new_lines.append('                horizontalArrangement = Arrangement.SpaceBetween,\n')
        new_lines.append('                verticalAlignment = Alignment.CenterVertically,\n')
        new_lines.append('            ) {\n')
        new_lines.append('                // Close / Back button (clean, never obstructs the match score!)\n')
        new_lines.append('                Box(\n')
        new_lines.append('                    modifier = Modifier\n')
        new_lines.append('                        .size(38.dp)\n')
        new_lines.append('                        .clip(CircleShape)\n')
        new_lines.append('                        .background(Color(0xD9272C3D))\n')
        new_lines.append('                        .clickable { onClose() },\n')
        new_lines.append('                    contentAlignment = Alignment.Center\n')
        new_lines.append('                ) {\n')
        new_lines.append('                    Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)\n')
        new_lines.append('                }\n')
        new_lines.append('\n')
        new_lines.append('                // Right side: Quality Picker\n')
        new_lines.append('                if (!useWebViewFallback) {\n')
        new_lines.append('                    Box {\n')
        new_lines.append('                        Surface(\n')
        new_lines.append('                            onClick = {\n')
        new_lines.append('                                qualityMenuExpanded = true\n')
        new_lines.append('                                lastInteractionTime = System.currentTimeMillis()\n')
        new_lines.append('                            },\n')
        new_lines.append('                            color = Color(0xD9272C3D),\n')
        new_lines.append('                            shape = RoundedCornerShape(14.dp),\n')
        new_lines.append('                        ) {\n')
        new_lines.append('                            Text(\n')
        new_lines.append('                                text = "الجودة · " + selectedQuality,\n')
        new_lines.append('                                color = Color.White,\n')
        new_lines.append('                                fontSize = 13.sp,\n')
        new_lines.append('                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),\n')
        new_lines.append('                            )\n')
        new_lines.append('                        }\n')
        new_lines.append('                        DropdownMenu(\n')
        new_lines.append('                            expanded = qualityMenuExpanded,\n')
        new_lines.append('                            onDismissRequest = { qualityMenuExpanded = false },\n')
        new_lines.append('                        ) {\n')
        new_lines.append('                            qualityChoices.forEach { quality ->\n')
        new_lines.append('                                DropdownMenuItem(\n')
        new_lines.append('                                    text = { Text(quality) },\n')
        new_lines.append('                                    onClick = {\n')
        new_lines.append('                                        applyQuality(trackSelector, quality)\n')
        new_lines.append('                                        selectedQuality = quality\n')
        new_lines.append('                                        preferences.edit().putString("quality", quality).apply()\n')
        new_lines.append('                                        qualityMenuExpanded = false\n')
        new_lines.append('                                        lastInteractionTime = System.currentTimeMillis()\n')
        new_lines.append('                                    },\n')
        new_lines.append('                                )\n')
        new_lines.append('                            }\n')
        new_lines.append('                        }\n')
        new_lines.append('                    }\n')
        new_lines.append('                }\n')
        new_lines.append('            }\n')
        new_lines.append('        }\n')

        # Fast forward i past the old top bar row until lines before line 460
        while i < len(lines) and 'private fun applyQuality' not in lines[i]:
            i += 1
        # Backtrack 3 lines (the closing braces for Box and PlayerActivityScreen)
        new_lines.append('    }\n')
        new_lines.append('}\n')
        new_lines.append('\n')
        continue

    new_lines.append(line)
    i += 1

with open('MadarTV/madar-player/src/main/java/com/madar/player/PlayerActivity.kt', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

print("PlayerActivity.kt rewritten successfully!")
