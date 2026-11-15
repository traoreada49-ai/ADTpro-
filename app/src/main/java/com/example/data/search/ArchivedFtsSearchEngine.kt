package com.example.data.search

import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

enum class MatchMode(val label: String, val description: String) {
    ALL_WORDS("Tous les mots", "Tous les mots saisis doivent être présents"),
    EXACT_PHRASE("Expression exacte", "Recherche la séquence exacte de mots"),
    ANY_WORD("Un des mots", "Au moins un des mots recherchés est présent")
}

enum class DateFilter(val label: String) {
    ALL_TIME("Toutes les dates"),
    LAST_24_HOURS("Dernières 24h"),
    LAST_7_DAYS("7 derniers jours"),
    LAST_30_DAYS("30 derniers jours")
}

enum class SenderFilter(val label: String) {
    ALL("Tous les expéditeurs"),
    OUTGOING_ONLY("Envoyés par moi"),
    INCOMING_ONLY("Reçus des contacts")
}

data class ArchivedSearchOptions(
    val matchMode: MatchMode = MatchMode.ALL_WORDS,
    val dateFilter: DateFilter = DateFilter.ALL_TIME,
    val senderFilter: SenderFilter = SenderFilter.ALL,
    val selectedChatId: String? = null
)

data class ArchivedSearchResult(
    val message: MessageEntity,
    val chat: ChatEntity?,
    val snippet: String,
    val matchStartInSnippet: Int,
    val matchEndInSnippet: Int,
    val matchedTerm: String,
    val score: Int,
    val isExactPhrase: Boolean
)

data class ArchivedSearchStats(
    val query: String,
    val executionTimeMs: Long,
    val totalArchivedChats: Int,
    val totalArchivedMessages: Int,
    val matchCount: Int,
    val matchingChatsCount: Int
)

/**
 * High-performance Full-Text Search (FTS) engine for Room database archived conversations.
 *
 * Capabilities:
 * - Exact phrase searching (quoted strings or EXACT_PHRASE mode).
 * - Multi-token keyword search (ALL_WORDS / ANY_WORD).
 * - Accent and diacritic-insensitive normalization (e.g. "reunion" matches "réunion").
 * - Case-insensitive matching.
 * - Contextual snippet extraction with precise highlight indices.
 * - Multi-factor relevance scoring (phrase match, token density, chat/sender match, recency).
 * - Flexible filtering by date, sender, and conversation.
 */
object ArchivedFtsSearchEngine {

    private val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()

    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "").lowercase()
    }

    /**
     * Executes full-text search across archived conversation messages and chats.
     */
    fun search(
        query: String,
        archivedChats: List<ChatEntity>,
        archivedMessages: List<MessageEntity>,
        options: ArchivedSearchOptions = ArchivedSearchOptions()
    ): Pair<List<ArchivedSearchResult>, ArchivedSearchStats> {
        val startTime = System.currentTimeMillis()
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            val stats = ArchivedSearchStats(
                query = "",
                executionTimeMs = System.currentTimeMillis() - startTime,
                totalArchivedChats = archivedChats.size,
                totalArchivedMessages = archivedMessages.size,
                matchCount = 0,
                matchingChatsCount = 0
            )
            return Pair(emptyList(), stats)
        }

        val chatsMap = archivedChats.associateBy { it.id }
        val now = System.currentTimeMillis()

        // 1. Filter messages according to date, sender, and optional chat scope
        val candidateMessages = archivedMessages.filter { msg ->
            // Chat filter
            if (options.selectedChatId != null && msg.chatId != options.selectedChatId) {
                return@filter false
            }

            // Sender filter
            when (options.senderFilter) {
                SenderFilter.ALL -> true
                SenderFilter.OUTGOING_ONLY -> msg.isOutgoing
                SenderFilter.INCOMING_ONLY -> !msg.isOutgoing
            } &&
            // Date filter
            when (options.dateFilter) {
                DateFilter.ALL_TIME -> true
                DateFilter.LAST_24_HOURS -> (now - msg.timestamp) <= (24L * 60 * 60 * 1000)
                DateFilter.LAST_7_DAYS -> (now - msg.timestamp) <= (7L * 24 * 60 * 60 * 1000)
                DateFilter.LAST_30_DAYS -> (now - msg.timestamp) <= (30L * 24 * 60 * 60 * 1000)
            }
        }

        // 2. Parse query terms and phrases
        val isExplicitQuotedPhrase = trimmedQuery.startsWith("\"") && trimmedQuery.endsWith("\"") && trimmedQuery.length > 2
        val cleanQuery = if (isExplicitQuotedPhrase) trimmedQuery.removeSurrounding("\"").trim() else trimmedQuery

        val normalizedQuery = normalize(cleanQuery)
        val queryTokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }

        val isExactMode = isExplicitQuotedPhrase || options.matchMode == MatchMode.EXACT_PHRASE

        val results = mutableListOf<ArchivedSearchResult>()

        for (msg in candidateMessages) {
            val originalText = msg.text
            val normalizedText = normalize(originalText)
            val chat = chatsMap[msg.chatId]
            val normalizedChatTitle = normalize(chat?.title.orEmpty())
            val normalizedSender = normalize(msg.senderName)

            var isMatch = false
            var matchedTerm = cleanQuery
            var matchScore = 0
            var matchedStart = -1
            var matchedLength = 0

            if (isExactMode) {
                // Exact phrase matching
                val phraseIdx = normalizedText.indexOf(normalizedQuery)
                if (phraseIdx >= 0) {
                    isMatch = true
                    matchedStart = phraseIdx
                    matchedLength = normalizedQuery.length
                    matchedTerm = cleanQuery
                    matchScore += 100
                } else if (normalizedChatTitle.contains(normalizedQuery)) {
                    isMatch = true
                    matchedTerm = cleanQuery
                    matchScore += 60
                }
            } else {
                when (options.matchMode) {
                    MatchMode.ALL_WORDS -> {
                        val allTokensPresent = queryTokens.all { token ->
                            normalizedText.contains(token) || normalizedChatTitle.contains(token) || normalizedSender.contains(token)
                        }
                        if (allTokensPresent && queryTokens.isNotEmpty()) {
                            isMatch = true
                            // Bonus if phrase occurs intact
                            val phraseIdx = normalizedText.indexOf(normalizedQuery)
                            if (phraseIdx >= 0) {
                                matchScore += 60
                                matchedStart = phraseIdx
                                matchedLength = normalizedQuery.length
                            } else {
                                // Locate the first matching token in text
                                for (token in queryTokens) {
                                    val tokenIdx = normalizedText.indexOf(token)
                                    if (tokenIdx >= 0 && (matchedStart == -1 || tokenIdx < matchedStart)) {
                                        matchedStart = tokenIdx
                                        matchedLength = token.length
                                        matchedTerm = token
                                    }
                                }
                            }
                            matchScore += queryTokens.size * 20
                        }
                    }
                    MatchMode.ANY_WORD -> {
                        var tokensFoundCount = 0
                        for (token in queryTokens) {
                            val tokenIdx = normalizedText.indexOf(token)
                            if (tokenIdx >= 0) {
                                tokensFoundCount++
                                if (matchedStart == -1 || tokenIdx < matchedStart) {
                                    matchedStart = tokenIdx
                                    matchedLength = token.length
                                    matchedTerm = token
                                }
                            } else if (normalizedChatTitle.contains(token) || normalizedSender.contains(token)) {
                                tokensFoundCount++
                            }
                        }
                        if (tokensFoundCount > 0) {
                            isMatch = true
                            matchScore += tokensFoundCount * 25
                        }
                    }
                    else -> Unit
                }
            }

            if (isMatch) {
                // Additional scoring factors
                if (normalizedChatTitle.contains(normalizedQuery)) matchScore += 30
                if (normalizedSender.contains(normalizedQuery)) matchScore += 15

                // Recency bonus (up to 20 points for messages within 30 days)
                val ageDays = (now - msg.timestamp) / (24L * 60 * 60 * 1000)
                val recencyBonus = max(0, (30 - ageDays).toInt() / 2)
                matchScore += recencyBonus

                val (snippet, snippetStart, snippetEnd) = createSnippetWithHighlight(
                    originalText = originalText,
                    matchedStart = matchedStart,
                    matchedLength = matchedLength
                )

                results.add(
                    ArchivedSearchResult(
                        message = msg,
                        chat = chat,
                        snippet = snippet,
                        matchStartInSnippet = snippetStart,
                        matchEndInSnippet = snippetEnd,
                        matchedTerm = matchedTerm,
                        score = matchScore,
                        isExactPhrase = isExactMode
                    )
                )
            }
        }

        // Sort by score descending, then recency
        results.sortWith(compareByDescending<ArchivedSearchResult> { it.score }.thenByDescending { it.message.timestamp })

        val executionTimeMs = System.currentTimeMillis() - startTime
        val matchingChatsCount = results.map { it.message.chatId }.distinct().size

        val stats = ArchivedSearchStats(
            query = query,
            executionTimeMs = executionTimeMs,
            totalArchivedChats = archivedChats.size,
            totalArchivedMessages = archivedMessages.size,
            matchCount = results.size,
            matchingChatsCount = matchingChatsCount
        )

        return Pair(results, stats)
    }

    /**
     * Extracts a contextual snippet surrounding the match with exact snippet indices.
     */
    private fun createSnippetWithHighlight(
        originalText: String,
        matchedStart: Int,
        matchedLength: Int,
        radius: Int = 45
    ): Triple<String, Int, Int> {
        if (originalText.isBlank() || matchedStart < 0 || matchedLength <= 0) {
            val fallbackSnippet = if (originalText.length > 90) originalText.take(90) + "…" else originalText
            return Triple(fallbackSnippet, 0, 0)
        }

        val textLen = originalText.length
        val rawStart = max(0, matchedStart - radius)
        val rawEnd = min(textLen, matchedStart + matchedLength + radius)

        // Adjust to word boundary if possible
        var start = rawStart
        if (start > 0) {
            val nextSpace = originalText.indexOf(' ', start)
            if (nextSpace in start..min(start + 10, matchedStart)) {
                start = nextSpace + 1
            }
        }

        var end = rawEnd
        if (end < textLen) {
            val prevSpace = originalText.lastIndexOf(' ', end)
            if (prevSpace >= matchedStart + matchedLength && prevSpace >= end - 10) {
                end = prevSpace
            }
        }

        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < textLen) "…" else ""

        val snippetContent = originalText.substring(start, end)
        val fullSnippet = prefix + snippetContent + suffix

        val highlightStart = prefix.length + (matchedStart - start)
        val highlightEnd = min(highlightStart + matchedLength, fullSnippet.length)

        return Triple(fullSnippet, max(0, highlightStart), max(0, highlightEnd))
    }
}
