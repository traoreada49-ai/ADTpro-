package com.example

import com.example.data.auth.DialCodeValidationResult
import com.example.data.auth.PhoneNumberValidator
import com.example.data.auth.PhoneValidationResult
import com.example.data.model.CountryData
import org.junit.Assert.*
import org.junit.Test

class PhoneNumberValidatorTest {

    @Test
    fun `validateDialCode rejects empty, non-digit, leading-zero, and unknown dial codes`() {
        assertTrue(PhoneNumberValidator.validateDialCode("") is DialCodeValidationResult.Empty)
        assertTrue(PhoneNumberValidator.validateDialCode("+") is DialCodeValidationResult.Empty)
        assertTrue(PhoneNumberValidator.validateDialCode("abc") is DialCodeValidationResult.InvalidFormat)
        assertTrue(PhoneNumberValidator.validateDialCode("0223") is DialCodeValidationResult.InvalidFormat)
        assertTrue(PhoneNumberValidator.validateDialCode("1234") is DialCodeValidationResult.InvalidFormat)

        val unknown = PhoneNumberValidator.validateDialCode("999")
        assertTrue(unknown is DialCodeValidationResult.UnknownDialCode)
        assertEquals("+999", (unknown as DialCodeValidationResult.UnknownDialCode).dialCode)
    }

    @Test
    fun `validateDialCode resolves shared dial codes preserving preferred country`() {
        val canada = CountryData.supportedCountries.first { it.name == "Canada" }
        val res = PhoneNumberValidator.validateDialCode("+1", preferredCountry = canada)
        assertTrue(res is DialCodeValidationResult.Valid)
        val valid = res as DialCodeValidationResult.Valid
        assertEquals("+1", valid.normalizedDialCode)
        assertEquals("Canada", valid.primaryCountry.name)
        assertTrue(valid.matchingCountries.size > 1)
    }

    @Test
    fun `validate Mali, Senegal, and Cote dIvoire numbers strictly`() {
        val mali = CountryData.findByDialCode("+223")!!
        val maliShort = PhoneNumberValidator.validate("223", "661234", mali)
        assertTrue(maliShort is PhoneValidationResult.TooShort)

        val maliLong = PhoneNumberValidator.validate("223", "661234567", mali)
        assertTrue(maliLong is PhoneValidationResult.TooLong)

        val maliValid = PhoneNumberValidator.validate("223", "66 12 34 56", mali)
        assertTrue(maliValid is PhoneValidationResult.Valid)
        assertEquals("+22366123456", (maliValid as PhoneValidationResult.Valid).e164Number)

        val senegal = CountryData.findByDialCode("+221")!!
        val senValid = PhoneNumberValidator.validate("+221", "77 123 45 67", senegal)
        assertTrue(senValid is PhoneValidationResult.Valid)
        assertEquals("+221771234567", (senValid as PhoneValidationResult.Valid).e164Number)

        // Côte d'Ivoire keeps leading zero in 10-digit numbering plan
        val ivoryCoast = CountryData.findByDialCode("+225")!!
        val civValid = PhoneNumberValidator.validate("225", "07 12 34 56 78", ivoryCoast)
        assertTrue(civValid is PhoneValidationResult.Valid)
        assertEquals("+2250712345678", (civValid as PhoneValidationResult.Valid).e164Number)
    }

    @Test
    fun `validate France strips trunk zero and enforces 9 E164 national digits`() {
        val france = CountryData.findByDialCode("+33")!!

        val withTrunkZero = PhoneNumberValidator.validate("33", "06 12 34 56 78", france)
        assertTrue(withTrunkZero is PhoneValidationResult.Valid)
        assertEquals("+33612345678", (withTrunkZero as PhoneValidationResult.Valid).e164Number)

        val withoutTrunkZero = PhoneNumberValidator.validate("33", "6 12 34 56 78", france)
        assertTrue(withoutTrunkZero is PhoneValidationResult.Valid)
        assertEquals("+33612345678", (withoutTrunkZero as PhoneValidationResult.Valid).e164Number)

        val tooShort = PhoneNumberValidator.validate("33", "06 12 34 56", france)
        assertTrue(tooShort is PhoneValidationResult.TooShort)
    }

    @Test
    fun `validate rejects unrecognized dial code even with digits in phone field`() {
        val result = PhoneNumberValidator.validate("999", "66123456", null)
        assertTrue(result is PhoneValidationResult.InvalidDialCode)
        val msg = PhoneNumberValidator.getErrorMessage(result)
        assertNotNull(msg)
        assertTrue(msg!!.contains("+999"))
    }

    @Test
    fun `validate rejects all-zero phone numbers`() {
        val mali = CountryData.findByDialCode("+223")!!
        val result = PhoneNumberValidator.validate("223", "00000000", mali)
        assertTrue(result is PhoneValidationResult.InvalidFormat)
    }
}
