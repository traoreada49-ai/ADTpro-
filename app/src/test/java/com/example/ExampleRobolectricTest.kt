package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LiveLocationPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ADT Pro", appName)
  }

  @Test
  fun liveLocationPayload_serializeAndDeserialize_worksAccurately() {
    val now = System.currentTimeMillis()
    val payload = LiveLocationPayload(
        latitude = 48.8566,
        longitude = 2.3522,
        address = "Champ de Mars, Tour Eiffel, Paris",
        accuracyMeters = 5f,
        durationMinutes = 60,
        startedAt = now,
        expiresAt = now + 60 * 60 * 1000L,
        isActive = true,
        comment = "En route pour la réunion"
    )

    val serialized = payload.serialize()
    assertTrue(serialized.contains("Tour Eiffel"))
    assertTrue(serialized.contains("48.8566"))

    val deserialized = LiveLocationPayload.deserialize(serialized)
    assertNotNull(deserialized)
    assertEquals(payload.latitude, deserialized!!.latitude, 0.0001)
    assertEquals(payload.longitude, deserialized.longitude, 0.0001)
    assertEquals("Champ de Mars, Tour Eiffel, Paris", deserialized.address)
    assertEquals("En route pour la réunion", deserialized.comment)
    assertFalse(deserialized.isExpired())
    assertTrue(deserialized.remainingTimeFormatted().contains("min") || deserialized.remainingTimeFormatted().contains("h"))
  }

  @Test
  fun testBackupMetadataFormatting() {
    val meta = com.example.data.firebase.BackupMetadata(
        userPhone = "+221770001122",
        timestamp = 1757100000000L,
        chatsCount = 5,
        messagesCount = 120,
        sizeBytes = 2500000L, // ~2.4 Mo
        isEncrypted = true,
        storagePath = "backups/plus_221770001122/adt_chat_backup_latest.enc"
    )

    assertEquals("+221770001122", meta.userPhone)
    assertEquals(5, meta.chatsCount)
    assertEquals(120, meta.messagesCount)
    assertTrue(meta.isEncrypted)
    assertTrue(meta.formattedSize.contains("Mo") || meta.formattedSize.contains("Ko"))
    assertNotNull(meta.formattedDate)
    assertTrue(meta.formattedDate.isNotEmpty())

    val emptyMeta = com.example.data.firebase.BackupMetadata(
        userPhone = "+221770001122",
        timestamp = 0L,
        chatsCount = 0,
        messagesCount = 0,
        sizeBytes = 0L
    )
    assertEquals("Aucune sauvegarde", emptyMeta.formattedDate)
    assertEquals("0 Ko", emptyMeta.formattedSize)
  }

  @Test
  fun testAppSettingsAutoBackupDefaults() {
    val settings = com.example.data.local.entity.AppSettingsEntity()
    assertEquals("DAILY", settings.autoBackupFrequency)
    assertTrue(settings.backupOnlyWifi)
    assertFalse(settings.backupIncludeMedia)
  }

  @Test
  fun testSignalProtocolFingerprintGeneration() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = com.example.data.crypto.SignalProtocolEngine.getInstance(context)

    val identityFingerprint = engine.getMyIdentityFingerprint()
    assertNotNull(identityFingerprint)
    assertTrue("Fingerprint should have hex format with colons", identityFingerprint.contains(":"))
    val segments = identityFingerprint.split(":")
    assertEquals(32, segments.size)

    val sessionFingerprint = engine.getSessionKeyFingerprint("chat_test_123", "+221770001122")
    assertNotNull(sessionFingerprint)
    assertTrue(sessionFingerprint.contains(":"))
    assertEquals(32, sessionFingerprint.split(":").size)

    val safetyNumber = engine.getSafetyNumber("chat_test_123", "+221770001122")
    assertNotNull(safetyNumber)
    assertEquals(12, safetyNumber.split(" ").size) // 12 blocks of 5 digits = 60 digits
  }

  @Test
  fun testChatEncryptionFingerprintModel() {
    val hex = "E4:A1:8B:20:9C:5F:73:10:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:00:11:22:33:44:55:66:77"
    val colors = com.example.data.crypto.ChatEncryptionFingerprint.deriveVisualColors(hex)
    assertEquals(4, colors.size)

    val model = com.example.data.crypto.ChatEncryptionFingerprint(
        chatId = "chat_abc",
        title = "Aïssatou Sow",
        subtitle = "+221782345678",
        avatarUrl = "",
        isGroup = false,
        safetyNumber = "12345 67890 12345 67890 12345 67890 12345 67890 12345 67890 12345 67890",
        hexFingerprint = hex,
        shortFingerprint = hex.take(11),
        visualFingerprintColors = colors,
        ephemeralKeySnippet = "Curve25519-EPH-1024",
        ratchetStep = 5,
        isVerified = true
    )

    val summary = model.toSharableSummary()
    assertTrue(summary.contains("Aïssatou Sow"))
    assertTrue(summary.contains("Signal Double Ratchet"))
    assertTrue(summary.contains("VÉRIFIÉ"))
  }

  @Test
  fun testExportFormatMetadata() {
    assertEquals("json", com.example.data.export.ExportFormat.JSON.extension)
    assertEquals("application/json", com.example.data.export.ExportFormat.JSON.mimeType)
    assertEquals("pdf", com.example.data.export.ExportFormat.PDF.extension)
    assertEquals("application/pdf", com.example.data.export.ExportFormat.PDF.mimeType)
  }

  @Test
  fun testTextToSpeechLongMessageDetection() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ttsManager = com.example.data.audio.TextToSpeechManager(context)

    val shortMessage = "Salut, ça va ?"
    assertFalse("Short message should not be detected as long message", ttsManager.isLongMessage(shortMessage))

    val longMessage = "Bonjour à toute l'équipe, j'espère que vous passez une excellente journée. Voici le compte rendu détaillé de notre réunion de cadrage pour la version 2.0."
    assertTrue("Detailed message should be identified as long text message", ttsManager.isLongMessage(longMessage))

    val multilineMessage = "Point 1\nPoint 2\nPoint 3"
    assertTrue("Multiline message should be identified as long text message", ttsManager.isLongMessage(multilineMessage))

    val audioMediaMessage = "Message audio"
    assertFalse("Non-text media message should not trigger TTS long message bar", ttsManager.isLongMessage(audioMediaMessage, mediaType = "AUDIO"))
  }

  @Test
  fun testTextToSpeechSanitizationAndSpeechRate() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val ttsManager = com.example.data.audio.TextToSpeechManager(context)

    val rawWithUrl = "Consultez la documentation ici: https://example.com/api/v1 pour les détails."
    val sanitized = ttsManager.sanitizeTextForSpeech(rawWithUrl)
    assertFalse("Sanitized speech text must not contain raw url", sanitized.contains("https://"))
    assertTrue("Sanitized speech text should describe link", sanitized.contains("lien internet"))

    // Test cycle rate: 1.0 -> 1.25 -> 1.5 -> 0.8 -> 1.0
    ttsManager.setSpeechRate(1.0f)
    assertEquals(1.0f, ttsManager.speechRate.value, 0.01f)
    val nextRate1 = ttsManager.cycleSpeechRate()
    assertEquals(1.25f, nextRate1, 0.01f)
    val nextRate2 = ttsManager.cycleSpeechRate()
    assertEquals(1.5f, nextRate2, 0.01f)
    val nextRate3 = ttsManager.cycleSpeechRate()
    assertEquals(0.8f, nextRate3, 0.01f)
    val nextRate4 = ttsManager.cycleSpeechRate()
    assertEquals(1.0f, nextRate4, 0.01f)

    // Test reading duration estimation
    val sampleText = "Bonjour nous vous attendons demain matin à huit heures pour la présentation finale."
    val estimatedDuration = ttsManager.estimateReadingDurationSeconds(sampleText)
    assertTrue("Estimated duration should be greater than 0", estimatedDuration > 0)
  }
}
