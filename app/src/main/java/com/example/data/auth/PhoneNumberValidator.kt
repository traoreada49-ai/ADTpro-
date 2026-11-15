package com.example.data.auth

import com.example.data.model.CountryData
import com.example.data.model.CountryItem

/**
 * Rules defining valid national subscriber number lengths and E.164 trunk-zero behavior.
 */
data class CountryPhoneRule(
    val minDigits: Int,
    val maxDigits: Int,
    val stripLeadingZeroForE164: Boolean = false
)

/**
 * Validation result for the country calling code (indicatif pays) alone.
 */
sealed interface DialCodeValidationResult {
    data object Empty : DialCodeValidationResult
    data class InvalidFormat(val reason: String) : DialCodeValidationResult
    data class UnknownDialCode(val dialCode: String) : DialCodeValidationResult
    data class Valid(
        val normalizedDialCode: String,
        val matchingCountries: List<CountryItem>,
        val primaryCountry: CountryItem
    ) : DialCodeValidationResult
}

/**
 * Validation result for the combined country calling code + national subscriber phone number.
 */
sealed interface PhoneValidationResult {
    data object Empty : PhoneValidationResult
    data class InvalidDialCode(val dialCode: String, val reason: String) : PhoneValidationResult
    data class InvalidFormat(val reason: String) : PhoneValidationResult
    data class TooShort(
        val currentDigits: Int,
        val minDigits: Int,
        val country: CountryItem
    ) : PhoneValidationResult
    data class TooLong(
        val currentDigits: Int,
        val maxDigits: Int,
        val country: CountryItem
    ) : PhoneValidationResult
    data class Valid(
        val e164Number: String,
        val nationalDigits: String,
        val dialCode: String,
        val country: CountryItem
    ) : PhoneValidationResult
}

/**
 * Validates country calling codes (indictatifs pays) and national phone numbers,
 * normalizing valid inputs to strict ITU-T E.164 format required by Firebase Phone Auth.
 */
object PhoneNumberValidator {

    private val countryRulesByDialAndName: Map<String, CountryPhoneRule> = mapOf(
        // Afrique de l'Ouest & Centrale
        "+223" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Mali
        "+221" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Sénégal
        "+225" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = false), // Côte d'Ivoire (plan à 10 chiffres)
        "+224" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Guinée
        "+226" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Burkina Faso
        "+227" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Niger
        "+228" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Togo
        "+229" to CountryPhoneRule(minDigits = 8, maxDigits = 10, stripLeadingZeroForE164 = false), // Bénin (transition 8 -> 10 chiffres)
        "+222" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Mauritanie
        "+220" to CountryPhoneRule(minDigits = 7, maxDigits = 7, stripLeadingZeroForE164 = false), // Gambie
        "+245" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Guinée-Bissau
        "+232" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = true), // Sierra Leone
        "+231" to CountryPhoneRule(minDigits = 8, maxDigits = 9, stripLeadingZeroForE164 = true), // Libéria
        "+238" to CountryPhoneRule(minDigits = 7, maxDigits = 7, stripLeadingZeroForE164 = false), // Cap-Vert
        "+233" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Ghana
        "+234" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = true), // Nigeria
        "+237" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Cameroun
        "+243" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // RD Congo
        "+242" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Congo (Brazzaville)
        "+241" to CountryPhoneRule(minDigits = 8, maxDigits = 9, stripLeadingZeroForE164 = false), // Gabon
        "+235" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Tchad
        "+236" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Centrafrique
        "+240" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Guinée équatoriale
        "+239" to CountryPhoneRule(minDigits = 7, maxDigits = 7, stripLeadingZeroForE164 = false), // Sao Tomé-et-Principe

        // Afrique du Nord, Est & Australe
        "+212" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Maroc
        "+213" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Algérie
        "+216" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Tunisie
        "+218" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Libye
        "+20" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = true), // Égypte
        "+249" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Soudan
        "+211" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Soudan du Sud
        "+254" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Kenya
        "+255" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Tanzanie
        "+256" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Ouganda
        "+250" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Rwanda
        "+257" to CountryPhoneRule(minDigits = 8, maxDigits = 8, stripLeadingZeroForE164 = false), // Burundi
        "+251" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Éthiopie
        "+27" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Afrique du Sud

        // Europe
        "+33" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // France (06... -> +336...)
        "+32" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Belgique
        "+41" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Suisse
        "+352" to CountryPhoneRule(minDigits = 8, maxDigits = 9, stripLeadingZeroForE164 = false), // Luxembourg
        "+34" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Espagne
        "+351" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = false), // Portugal
        "+39" to CountryPhoneRule(minDigits = 9, maxDigits = 10, stripLeadingZeroForE164 = false), // Italie
        "+49" to CountryPhoneRule(minDigits = 10, maxDigits = 11, stripLeadingZeroForE164 = true), // Allemagne
        "+44" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = true), // Royaume-Uni
        "+31" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Pays-Bas
        "+7" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = false), // Russie / Kazakhstan
        "+90" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = true), // Turquie

        // Amériques
        "+1" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = false), // USA / Canada / NANP
        "+52" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = false), // Mexique
        "+55" to CountryPhoneRule(minDigits = 10, maxDigits = 11, stripLeadingZeroForE164 = false), // Brésil
        "+57" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = false), // Colombie

        // Moyen-Orient & Asie / Océanie
        "+966" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Arabie Saoudite
        "+971" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Émirats Arabes Unis
        "+972" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true), // Israël
        "+86" to CountryPhoneRule(minDigits = 11, maxDigits = 11, stripLeadingZeroForE164 = false), // Chine
        "+91" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = true), // Inde
        "+81" to CountryPhoneRule(minDigits = 10, maxDigits = 10, stripLeadingZeroForE164 = true), // Japon
        "+82" to CountryPhoneRule(minDigits = 9, maxDigits = 10, stripLeadingZeroForE164 = true), // Corée du Sud
        "+61" to CountryPhoneRule(minDigits = 9, maxDigits = 9, stripLeadingZeroForE164 = true) // Australie
    )

    /**
     * Returns the digit rule for a given country. Falls back to the length of its exampleNumber.
     */
    fun getRuleForCountry(country: CountryItem): CountryPhoneRule {
        countryRulesByDialAndName[country.dialCode]?.let { return it }
        val exampleDigits = country.exampleNumber.filter { it.isDigit() }.length
        return if (exampleDigits in 6..12) {
            CountryPhoneRule(
                minDigits = (exampleDigits - 1).coerceAtLeast(6),
                maxDigits = (exampleDigits + 1).coerceAtMost(12),
                stripLeadingZeroForE164 = false
            )
        } else {
            CountryPhoneRule(minDigits = 7, maxDigits = 12, stripLeadingZeroForE164 = false)
        }
    }

    /**
     * Validates the entered dial code (e.g. "223", "+223", "999").
     * If `preferredCountry` already has the matching dial code (for shared dial codes like +1 or +7),
     * it is preserved as the `primaryCountry`.
     */
    fun validateDialCode(
        rawDialCode: String,
        preferredCountry: CountryItem? = null
    ): DialCodeValidationResult {
        val trimmed = rawDialCode.trim()
        if (trimmed.isEmpty()) {
            return DialCodeValidationResult.Empty
        }

        val withoutPlus = trimmed.removePrefix("+")
        if (withoutPlus.isEmpty()) {
            return DialCodeValidationResult.Empty
        }
        if (!withoutPlus.all { it.isDigit() }) {
            return DialCodeValidationResult.InvalidFormat("L'indicatif ne doit contenir que des chiffres.")
        }
        if (withoutPlus.startsWith("0")) {
            return DialCodeValidationResult.InvalidFormat("Un indicatif pays ne commence pas par 0.")
        }
        if (withoutPlus.length > 3) {
            return DialCodeValidationResult.InvalidFormat("L'indicatif pays comporte au maximum 3 chiffres.")
        }

        val normalized = "+$withoutPlus"
        val matchingCountries = CountryData.findAllByDialCode(normalized)
        if (matchingCountries.isEmpty()) {
            return DialCodeValidationResult.UnknownDialCode(normalized)
        }

        val primary = if (preferredCountry != null && matchingCountries.any { it.name == preferredCountry.name }) {
            preferredCountry
        } else {
            matchingCountries.first()
        }

        return DialCodeValidationResult.Valid(
            normalizedDialCode = normalized,
            matchingCountries = matchingCountries,
            primaryCountry = primary
        )
    }

    /**
     * Validates both dial code and phone number together, normalizing trunk zeros and returning E.164.
     */
    fun validate(
        rawDialCode: String,
        rawPhoneNumber: String,
        preferredCountry: CountryItem? = null
    ): PhoneValidationResult {
        val dialResult = validateDialCode(rawDialCode, preferredCountry)

        if (rawPhoneNumber.isBlank()) {
            return when (dialResult) {
                is DialCodeValidationResult.UnknownDialCode ->
                    PhoneValidationResult.InvalidDialCode(dialResult.dialCode, "Indicatif ${dialResult.dialCode} non reconnu.")
                is DialCodeValidationResult.InvalidFormat ->
                    PhoneValidationResult.InvalidDialCode(rawDialCode, dialResult.reason)
                else -> PhoneValidationResult.Empty
            }
        }

        when (dialResult) {
            DialCodeValidationResult.Empty ->
                return PhoneValidationResult.InvalidDialCode("", "Veuillez saisir un indicatif pays (ex: +223).")
            is DialCodeValidationResult.InvalidFormat ->
                return PhoneValidationResult.InvalidDialCode(rawDialCode, dialResult.reason)
            is DialCodeValidationResult.UnknownDialCode ->
                return PhoneValidationResult.InvalidDialCode(
                    dialResult.dialCode,
                    "Indicatif pays ${dialResult.dialCode} inconnu. Sélectionnez un pays valide."
                )
            is DialCodeValidationResult.Valid -> Unit
        }

        val validDial = dialResult
        val country = validDial.primaryCountry
        val dialDigits = validDial.normalizedDialCode.removePrefix("+")

        val trimmedPhone = rawPhoneNumber.trim()
        val allowedChars = trimmedPhone.all { it.isDigit() || it in setOf(' ', '-', '.', '(', ')', '+') }
        if (!allowedChars) {
            return PhoneValidationResult.InvalidFormat("Caractères non valides dans le numéro de téléphone.")
        }

        var digits = trimmedPhone.filter { it.isDigit() }
        // Strip leading international dial code if user duplicated it inside the national field
        if (trimmedPhone.startsWith("+") && digits.startsWith(dialDigits) && digits.length > dialDigits.length) {
            digits = digits.removePrefix(dialDigits)
        } else if (trimmedPhone.startsWith("00$dialDigits") && digits.length > dialDigits.length + 2) {
            digits = digits.removePrefix("00$dialDigits")
        }

        if (digits.isEmpty()) {
            return PhoneValidationResult.Empty
        }

        val rule = getRuleForCountry(country)
        val normalizedNational = if (rule.stripLeadingZeroForE164 && digits.startsWith("0") && digits.length > 1) {
            digits.removePrefix("0")
        } else {
            digits
        }

        if (normalizedNational.all { it == '0' }) {
            return PhoneValidationResult.InvalidFormat("Le numéro saisi ne peut pas contenir uniquement des zéros.")
        }

        if (normalizedNational.length < rule.minDigits) {
            return PhoneValidationResult.TooShort(
                currentDigits = normalizedNational.length,
                minDigits = rule.minDigits,
                country = country
            )
        }

        if (normalizedNational.length > rule.maxDigits) {
            return PhoneValidationResult.TooLong(
                currentDigits = normalizedNational.length,
                maxDigits = rule.maxDigits,
                country = country
            )
        }

        val e164 = "${validDial.normalizedDialCode}$normalizedNational"
        val totalE164Digits = e164.removePrefix("+").length
        if (totalE164Digits > 15) {
            return PhoneValidationResult.InvalidFormat("Le numéro international dépasse la limite E.164 (15 chiffres max).")
        }

        return PhoneValidationResult.Valid(
            e164Number = e164,
            nationalDigits = normalizedNational,
            dialCode = validDial.normalizedDialCode,
            country = country
        )
    }

    /**
     * Produces a clear French human-readable feedback string for any validation failure.
     */
    fun getErrorMessage(result: PhoneValidationResult): String? = when (result) {
        PhoneValidationResult.Empty -> null
        is PhoneValidationResult.InvalidDialCode -> result.reason
        is PhoneValidationResult.InvalidFormat -> result.reason
        is PhoneValidationResult.TooShort -> {
            val expected = formatExpectedDigits(getRuleForCountry(result.country))
            "Numéro incomplet pour ${result.country.name} (${result.country.dialCode}) : ${result.currentDigits}/$expected chiffres"
        }
        is PhoneValidationResult.TooLong -> {
            val expected = formatExpectedDigits(getRuleForCountry(result.country))
            "Numéro trop long pour ${result.country.name} (${result.country.dialCode}) : ${result.currentDigits}/$expected chiffres"
        }
        is PhoneValidationResult.Valid -> null
    }

    fun formatExpectedDigits(rule: CountryPhoneRule): String {
        return if (rule.minDigits == rule.maxDigits) {
            "${rule.minDigits}"
        } else {
            "${rule.minDigits}-${rule.maxDigits}"
        }
    }
}
