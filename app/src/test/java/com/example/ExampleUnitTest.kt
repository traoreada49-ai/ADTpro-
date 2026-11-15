package com.example

import com.example.data.datastore.PrivacyPreferences
import com.example.data.datastore.VisibilityOption
import com.example.data.model.CameraEffectsData
import com.example.data.model.WallpaperCategory
import com.example.data.model.WallpaperData
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun privacyPreferences_defaultValues_areCorrect() {
    val prefs = PrivacyPreferences()
    assertTrue(prefs.readReceipts)
    assertEquals("EVERYONE", prefs.profilePhotoVisibility)
    assertEquals("EVERYONE", prefs.lastSeenVisibility)
    assertEquals("EVERYONE", prefs.onlineStatusVisibility)
    assertEquals("OFF", prefs.defaultMessageTimer)
  }

  @Test
  fun visibilityOption_fromCode_resolvesProperly() {
    assertEquals(VisibilityOption.EVERYONE, VisibilityOption.fromCode("EVERYONE"))
    assertEquals(VisibilityOption.CONTACTS, VisibilityOption.fromCode("contacts"))
    assertEquals(VisibilityOption.NOBODY, VisibilityOption.fromCode("NOBODY"))
    assertEquals(VisibilityOption.EVERYONE, VisibilityOption.fromCode("UNKNOWN_VALUE"))
  }

  @Test
  fun wallpaperThemes_containAllRequestedCategoriesAndItems() {
    val allWallpapers = WallpaperData.allWallpapers
    assertTrue(allWallpapers.isNotEmpty())

    // Fleurs
    val flowers = allWallpapers.filter { it.category == WallpaperCategory.FLOWERS }
    assertTrue("Should have flowers wallpapers", flowers.isNotEmpty())

    // Arbre
    val trees = allWallpapers.filter { it.category == WallpaperCategory.TREES }
    assertTrue("Should have tree wallpapers", trees.isNotEmpty())

    // Voiture
    val cars = allWallpapers.filter { it.category == WallpaperCategory.CARS }
    assertTrue("Should have car wallpapers", cars.isNotEmpty())

    // Étoile
    val stars = allWallpapers.filter { it.category == WallpaperCategory.STARS }
    assertTrue("Should have star wallpapers", stars.isNotEmpty())

    // Lune
    val moon = allWallpapers.filter { it.category == WallpaperCategory.MOON }
    assertTrue("Should have moon wallpapers", moon.isNotEmpty())

    // Soleil
    val sun = allWallpapers.filter { it.category == WallpaperCategory.SUN }
    assertTrue("Should have sun wallpapers", sun.isNotEmpty())

    // Fallback lookup
    val foundItem = WallpaperData.findById("FLEURS_ROSES")
    assertNotNull(foundItem)
    assertEquals("Roses Sauvages", foundItem.title)
  }

  @Test
  fun cameraEffects_containAllRequestedEffects() {
    val allEffects = CameraEffectsData.allEffects
    val effectIds = allEffects.map { it.id }

    assertTrue("Should contain Citron", effectIds.contains("CITRON"))
    assertTrue("Should contain Fraisier", effectIds.contains("FRAISIER"))
    assertTrue("Should contain Orange", effectIds.contains("ORANGE"))
    assertTrue("Should contain White / Lumineux", effectIds.contains("WHITE"))
    assertTrue("Should contain Noir & Blanc", effectIds.contains("NOIR_BLANC"))
    assertTrue("Should contain Bleu", effectIds.contains("BLEU"))

    val citron = CameraEffectsData.findById("CITRON")
    assertEquals("Citron", citron.name)
    assertEquals("🍋", citron.emoji)

    val fraise = CameraEffectsData.findById("FRAISIER")
    assertEquals("Fraisier", fraise.name)
    assertEquals("🍓", fraise.emoji)

    val noirBlanc = CameraEffectsData.findById("NOIR_BLANC")
    assertEquals("Noir & Blanc", noirBlanc.name)
    assertEquals("⚫", noirBlanc.emoji)
  }

  @Test
  fun liveLocation_durationPresets_haveExpectedDurations() {
    val options = com.example.data.model.LocationDurationPresets.options
    assertEquals(3, options.size)

    val fifteenMin = options.find { it.minutes == 15 }
    assertNotNull("Should contain 15 minutes preset", fifteenMin)

    val oneHour = options.find { it.minutes == 60 }
    assertNotNull("Should contain 1 hour preset", oneHour)

    val eightHours = options.find { it.minutes == 480 }
    assertNotNull("Should contain 8 hours preset", eightHours)
  }

  @Test
  fun quickReactionEmojis_containsAllStandardReactions() {
    val quickEmojis = com.example.ui.components.QUICK_REACTION_EMOJIS
    assertEquals(7, quickEmojis.size)
    assertTrue(quickEmojis.contains("👍"))
    assertTrue(quickEmojis.contains("❤️"))
    assertTrue(quickEmojis.contains("😂"))
    assertTrue(quickEmojis.contains("😮"))
    assertTrue(quickEmojis.contains("😢"))
    assertTrue(quickEmojis.contains("🙏"))
    assertTrue(quickEmojis.contains("🔥"))
  }

  @Test
  fun emojiCategories_containsRichCategoriesAndItems() {
    val categories = com.example.ui.components.EMOJI_CATEGORIES
    assertTrue(categories.isNotEmpty())

    val popular = categories.find { it.title == "Populaires" }
    assertNotNull("Should have Populaires category", popular)
    assertTrue(popular!!.emojis.contains("❤️"))

    val smiles = categories.find { it.title == "Sourires" }
    assertNotNull("Should have Sourires category", smiles)
    assertTrue(smiles!!.emojis.isNotEmpty())

    val gestures = categories.find { it.title == "Gestes" }
    assertNotNull("Should have Gestes category", gestures)
    assertTrue(gestures!!.emojis.contains("👏"))
  }

  @Test
  fun messageEntity_reactionField_canBeSetAndUpdated() {
    val msg = com.example.data.local.entity.MessageEntity(
        id = "msg-123",
        chatId = "chat-1",
        senderPhone = "+33600000001",
        senderName = "Jean Dupont",
        text = "Salut tout le monde !",
        timestamp = System.currentTimeMillis(),
        isOutgoing = false,
        reaction = null
    )
    assertNull(msg.reaction)

    val reacted = msg.copy(reaction = "❤️")
    assertEquals("❤️", reacted.reaction)

    val changed = reacted.copy(reaction = "🔥")
    assertEquals("🔥", changed.reaction)

    val cleared = changed.copy(reaction = null)
    assertNull(cleared.reaction)
  }
}
