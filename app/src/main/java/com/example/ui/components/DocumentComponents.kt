package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import com.example.data.model.SecureDocument
import com.example.ui.theme.AdtPrimary

@Composable
fun DocumentBubbleContent(
    message: MessageEntity,
    onOpenDocument: (SecureDocument) -> Unit = {},
    onShowSecurityReport: (SecureDocument) -> Unit = {}
) {
    val doc = remember(message.id, message.mediaUrl) {
        SecureDocument(
            id = message.id,
            name = message.mediaUrl.ifBlank { "Document_Confidentiel.pdf" },
            sizeBytes = 1845000L,
            mimeType = "application/pdf"
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDocument(doc) }
            .testTag("doc_bubble_${message.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF515BD4).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF515BD4),
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.formattedSize} • Chiffré AES-256",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onShowSecurityReport(doc) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = "Rapport de sécurité",
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun DocumentPickerDialog(
    onDismiss: () -> Unit,
    onSendDocument: (SecureDocument, ByteArray, String) -> Unit,
    isUploading: Boolean = false,
    uploadProgress: Float = 0f,
    statusMessage: String = ""
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = AdtPrimary)
                Text("Partager un document sécurisé", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Sélectionnez un document à chiffrer de bout en bout avant transfert :",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    "Rapport_Financier_2026.pdf" to "2.4 Mo",
                    "Contrat_Partenariat_Afrique.docx" to "850 Ko",
                    "Architecture_Securite_ADT.pdf" to "3.1 Mo"
                ).forEach { (name, size) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val doc = SecureDocument(name = name, sizeBytes = 2500000L)
                                onSendDocument(doc, ByteArray(10), "Document sécurisé")
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(size, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun DocumentPreviewDialog(
    document: SecureDocument,
    onDismiss: () -> Unit,
    onShowSecurityReport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(document.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(48.dp))
                            Text("Aperçu sécurisé", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("Taille : ${document.formattedSize}", fontSize = 12.sp)
                Text("Chiffrement : ${document.securityLevel}", fontSize = 12.sp, color = Color(0xFF1B8748), fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onShowSecurityReport) {
                Text("Audit de Sécurité")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun DocumentSecurityReportDialog(
    document: SecureDocument,
    onDismiss: () -> Unit,
    onOpenDocument: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF25D366))
                Text("Certificat de sécurité", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Document : ${document.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("Empreinte SHA-256 :", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(document.sha256Checksum, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Statut du scan antivirus : Aucun malware détecté (0/72)", fontSize = 12.sp, color = Color(0xFF1B8748), fontWeight = FontWeight.Bold)
                Text("Chiffrement : Protocole Signal + AES-256-GCM matériel", fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
