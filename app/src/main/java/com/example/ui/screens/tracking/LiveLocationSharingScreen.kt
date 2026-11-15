package com.example.ui.screens.tracking

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLocationSharingScreen(viewModel: AdtViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partage de position en direct", fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    Icons.Default.ShareLocation,
                    contentDescription = null,
                    tint = AdtPrimary,
                    modifier = Modifier.size(56.dp)
                )
                Text("Partage de position GPS temps réel", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Votre trajet est chiffré de bout en bout et mis à jour de manière fluide.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { viewModel.navigateBack() }) {
                    Text("Fermer")
                }
            }
        }
    }
}
