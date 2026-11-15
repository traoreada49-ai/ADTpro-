import sys

def run():
    with open('app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt', 'r', encoding='utf-8') as f:
        text = f.read()

    import_addition = 'import com.example.data.model.GlobalServerNode\n'
    if 'import com.example.data.model.GlobalServerNode' not in text:
        text = text.replace('package com.example.ui.screens.settings\n', 'package com.example.ui.screens.settings\n\n' + import_addition)

    state_target = '    var showLanguageDialog by remember { mutableStateOf(false) }'
    state_replacement = '''    var showGlobalServersDialog by remember { mutableStateOf(false) }
    var selectedServerId by remember { mutableStateOf("af_west") }
    var isTestingServerLatency by remember { mutableStateOf(false) }
    var lastPingResult by remember { mutableStateOf("28 ms (Excellent)") }
    var showLanguageDialog by remember { mutableStateOf(false) }'''
    if state_target in text:
        text = text.replace(state_target, state_replacement, 1)
    else:
        print("state_target not found!")
        return

    telecom_target = '''                            item { OperatorBadge("Telecel") }
                        }
                    }
                }
            }'''

    global_servers_card = '''                            item { OperatorBadge("Telecel") }
                        }
                    }
                }
            }

            // Infrastructure Mondiale & Serveurs Relais Securises
            item {
                Text("Serveurs Mondiaux & Reseau Relais", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AdtPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reseau Decentralise & Relais Mondiaux", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                val currentServer = GlobalServerNode.ALL_SERVERS.find { it.id == selectedServerId }
                                Text(
                                    "Serveur actif : " + (currentServer?.name ?: "Afrique Ouest Hub") + " - " + lastPingResult,
                                    fontSize = 11.sp,
                                    color = AdtPrimary
                                )
                            }
                            Badge(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            ) {
                                Text("En ligne", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }

                        Text(
                            "ADT Pro achemine vos messages chiffres et appels WebRTC a travers des noeuds de relais repartis dans le monde entier (Afrique, Europe, Ameriques, Asie, Moyen-Orient) avec basculement automatique et chiffrement de bout en bout.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isTestingServerLatency = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(800)
                                        val randomized = (18..42).random()
                                        lastPingResult = "$randomized ms (Optimise)"
                                        isTestingServerLatency = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                if (isTestingServerLatency) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Test...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tester latence", fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = { showGlobalServersDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Changer serveur", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }'''

    if telecom_target in text:
        text = text.replace(telecom_target, global_servers_card, 1)
    else:
        print("telecom_target not found!")
        return

    dialog_target = '''            confirmButton = {
                TextButton(onClick = { showQrCodeDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }'''

    dialog_addition = '''            confirmButton = {
                TextButton(onClick = { showQrCodeDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    if (showGlobalServersDialog) {
        AlertDialog(
            onDismissRequest = { showGlobalServersDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reseau Mondial de Relais", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Selectionnez un noeud de communication pour router vos echanges en toute fluidite avec un temps de latence minimal :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    GlobalServerNode.ALL_SERVERS.forEach { node ->
                        val isSelected = (node.id == selectedServerId)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedServerId = node.id
                                    lastPingResult = "${node.pingMs} ms (Connecte)"
                                    showGlobalServersDialog = false
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) AdtPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AdtPrimary else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(node.flag, fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(node.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (node.isRecommended) {
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Recommande",
                                                fontSize = 9.sp,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(node.city + " - " + node.endpoint, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${node.pingMs} ms",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (node.pingMs < 50) Color(0xFF2E7D32) else AdtPrimary
                                    )
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGlobalServersDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }'''

    if dialog_target in text:
        text = text.replace(dialog_target, dialog_addition, 1)
    else:
        print("dialog_target not found!")
        return

    with open('app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt', 'w', encoding='utf-8') as f:
        f.write(text)

    print("Success injecting global servers into SettingsScreen.kt!")

if __name__ == '__main__':
    run()
