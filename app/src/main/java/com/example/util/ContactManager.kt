package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ContactEntity
import com.example.data.repository.AdtRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Model representing a raw contact fetched from the device address book via ContentResolver.
 */
data class DeviceContact(
    val id: String,
    val name: String,
    val rawPhoneNumber: String,
    val normalizedPhoneNumber: String,
    val photoUri: String? = null
)

/**
 * Result of comparing device contacts with registered ADT Pro app users.
 */
data class ContactSyncResult(
    val adtUsers: List<ContactEntity>,
    val nonAdtContacts: List<ContactEntity>,
    val allCombined: List<ContactEntity>,
    val totalDeviceContactsCount: Int,
    val fromDeviceResolver: Boolean
)

/**
 * ContactManager utility to fetch device contacts using ContentResolver,
 * normalize phone numbers, compare against known/registered app users,
 * and build the initial 'Start Chat' contact list with instant chat or invite affordances.
 */
object ContactManager {

    /**
     * Checks if the app has been granted the READ_CONTACTS permission.
     */
    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Normalizes phone numbers by stripping non-digit symbols (except leading +),
     * trimming leading zeros, and formatting standard international prefixes.
     */
    fun normalizePhoneNumber(rawPhone: String, defaultCountryCode: String = "+223"): String {
        val trimmed = rawPhone.trim()
        if (trimmed.isEmpty()) return ""

        // Keep leading '+' if present, remove all other non-digits
        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }

        return when {
            hasLeadingPlus -> "+$digitsOnly"
            digitsOnly.startsWith("00") -> "+${digitsOnly.substring(2)}"
            digitsOnly.length in 9..14 -> {
                // If already includes a country code (like 223..., 221..., 222..., 243..., 237..., 225..., 33...)
                if (digitsOnly.startsWith("223") || digitsOnly.startsWith("221") ||
                    digitsOnly.startsWith("222") || digitsOnly.startsWith("243") ||
                    digitsOnly.startsWith("237") || digitsOnly.startsWith("225") ||
                    digitsOnly.startsWith("33") || digitsOnly.startsWith("234") ||
                    digitsOnly.startsWith("226") || digitsOnly.startsWith("224") ||
                    digitsOnly.startsWith("34") || digitsOnly.startsWith("1")
                ) {
                    "+$digitsOnly"
                } else {
                    "$defaultCountryCode$digitsOnly"
                }
            }
            digitsOnly.length in 7..8 -> {
                // Local national number, prepend default country code
                "$defaultCountryCode$digitsOnly"
            }
            else -> if (digitsOnly.isNotEmpty()) "+$digitsOnly" else ""
        }
    }

    /**
     * Formats a phone number for user-friendly display (e.g. +223 66 12 34 56).
     */
    fun formatDisplayPhone(normalizedPhone: String): String {
        val digits = normalizedPhone.filter { it.isDigit() }
        return when {
            normalizedPhone.startsWith("+223") && digits.length >= 11 -> {
                "+223 ${digits.substring(3, 5)} ${digits.substring(5, 7)} ${digits.substring(7, 9)} ${digits.substring(9)}"
            }
            normalizedPhone.startsWith("+221") && digits.length >= 12 -> {
                "+221 ${digits.substring(3, 5)} ${digits.substring(5, 8)} ${digits.substring(8, 10)} ${digits.substring(10)}"
            }
            normalizedPhone.startsWith("+222") && digits.length >= 11 -> {
                "+222 ${digits.substring(3, 5)} ${digits.substring(5, 7)} ${digits.substring(7, 9)} ${digits.substring(9)}"
            }
            normalizedPhone.startsWith("+243") && digits.length >= 12 -> {
                "+243 ${digits.substring(3, 5)} ${digits.substring(5, 8)} ${digits.substring(8, 10)} ${digits.substring(10)}"
            }
            normalizedPhone.startsWith("+237") && digits.length >= 12 -> {
                "+237 ${digits.substring(3, 6)} ${digits.substring(6, 8)} ${digits.substring(8, 10)} ${digits.substring(10)}"
            }
            normalizedPhone.startsWith("+225") && digits.length >= 12 -> {
                "+225 ${digits.substring(3, 5)} ${digits.substring(5, 7)} ${digits.substring(7, 9)} ${digits.substring(9, 11)} ${digits.substring(11)}"
            }
            normalizedPhone.startsWith("+33") && digits.length >= 11 -> {
                "+33 ${digits.substring(2, 3)} ${digits.substring(3, 5)} ${digits.substring(5, 7)} ${digits.substring(7, 9)} ${digits.substring(9)}"
            }
            else -> normalizedPhone
        }
    }

    /**
     * Fetches phone contacts from the device ContentResolver.
     * If no contacts are returned (e.g. empty device or permission denied),
     * returns a curated set of address book contacts for realistic demonstration.
     */
    fun fetchDeviceContacts(context: Context): List<DeviceContact> {
        val contactsList = mutableListOf<DeviceContact>()
        val seenPhones = mutableSetOf<String>()

        if (hasContactsPermission(context)) {
            val contentResolver = context.contentResolver
            val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

            var cursor: Cursor? = null
            try {
                cursor = contentResolver.query(uri, projection, null, null, sortOrder)
                cursor?.let {
                    val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                    while (it.moveToNext()) {
                        val id = if (idIndex != -1) it.getString(idIndex) ?: "" else ""
                        val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Sans nom" else "Sans nom"
                        val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                        val photo = if (photoIndex != -1) it.getString(photoIndex) else null

                        val normalized = normalizePhoneNumber(number)
                        if (normalized.isNotBlank() && !seenPhones.contains(normalized)) {
                            seenPhones.add(normalized)
                            contactsList.add(
                                DeviceContact(
                                    id = if (id.isNotBlank()) id else normalized,
                                    name = name.trim(),
                                    rawPhoneNumber = number,
                                    normalizedPhoneNumber = normalized,
                                    photoUri = photo
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }

        // If device has no contacts or permission not yet active, provide sample phone book entries
        if (contactsList.isEmpty()) {
            return getSampleDeviceAddressBook()
        }

        return contactsList
    }

    /**
     * Compares device contacts against a list of known ADT Pro users.
     * Matches normalized phone numbers to identify who is registered vs. who can be invited.
     */
    fun compareAndBuildContactList(
        deviceContacts: List<DeviceContact>,
        registeredAppUsers: List<ContactEntity>
    ): ContactSyncResult {
        // Create a lookup map of registered users indexed by their normalized phone number
        val registeredMap = mutableMapOf<String, ContactEntity>()
        for (user in registeredAppUsers) {
            val norm = normalizePhoneNumber(user.phone)
            if (norm.isNotBlank()) {
                registeredMap[norm] = user
            }
            val idNorm = normalizePhoneNumber(user.id)
            if (idNorm.isNotBlank()) {
                registeredMap[idNorm] = user
            }
        }

        val adtUsers = mutableListOf<ContactEntity>()
        val nonAdtContacts = mutableListOf<ContactEntity>()
        val matchedUserIds = mutableSetOf<String>()

        for (deviceContact in deviceContacts) {
            val normalizedPhone = deviceContact.normalizedPhoneNumber
            val matchedUser = registeredMap[normalizedPhone]

            if (matchedUser != null) {
                matchedUserIds.add(matchedUser.id)
                // Use registered user details, keeping device contact's custom name if preferred
                adtUsers.add(
                    matchedUser.copy(
                        name = if (deviceContact.name.isNotBlank() && !deviceContact.name.equals("Sans nom", true)) {
                            deviceContact.name
                        } else {
                            matchedUser.name
                        },
                        isAdtUser = true
                    )
                )
            } else {
                // Non-ADT contact from device address book (invite candidate)
                val countryCode = if (normalizedPhone.startsWith("+")) {
                    normalizedPhone.take(4)
                } else "+223"

                nonAdtContacts.add(
                    ContactEntity(
                        id = "dev_${deviceContact.id}_${normalizedPhone.filter { it.isDigit() }}",
                        name = deviceContact.name.ifBlank { formatDisplayPhone(normalizedPhone) },
                        phone = formatDisplayPhone(normalizedPhone),
                        countryCode = countryCode,
                        avatarUrl = deviceContact.photoUri ?: "avatar_contact_placeholder",
                        bio = "Inviter sur le réseau ADT Pro",
                        isAdtUser = false,
                        isOnline = false,
                        lastSeenText = "Non inscrit"
                    )
                )
            }
        }

        // Also add any registered users that weren't in the device phonebook (e.g. system contacts or directory)
        for (registeredUser in registeredAppUsers) {
            if (!matchedUserIds.contains(registeredUser.id) && registeredUser.isAdtUser) {
                adtUsers.add(registeredUser)
            }
        }

        // Sort ADT users alphabetically, then non-ADT contacts alphabetically
        val sortedAdt = adtUsers.distinctBy { it.phone.filter { char -> char.isDigit() } }.sortedBy { it.name }
        val sortedNonAdt = nonAdtContacts.distinctBy { it.phone.filter { char -> char.isDigit() } }.sortedBy { it.name }

        val combined = sortedAdt + sortedNonAdt

        return ContactSyncResult(
            adtUsers = sortedAdt,
            nonAdtContacts = sortedNonAdt,
            allCombined = combined,
            totalDeviceContactsCount = deviceContacts.size,
            fromDeviceResolver = deviceContacts.isNotEmpty()
        )
    }

    /**
     * Executes the full sync pipeline:
     * 1. Fetches contacts using ContentResolver (or fallback address book).
     * 2. Reads existing registered contacts from Room database.
     * 3. Compares device contacts against registered app users.
     * 4. Updates and persists the initial 'Start Chat' contact list into Room database.
     */
    suspend fun syncAndPersistContacts(
        context: Context,
        repository: AdtRepository,
        database: AppDatabase
    ): ContactSyncResult = withContext(Dispatchers.IO) {
        val deviceContacts = fetchDeviceContacts(context)
        val existingContacts = database.contactDao().getContactsList().ifEmpty {
            getDefaultSeedAppUsers()
        }

        val result = compareAndBuildContactList(deviceContacts, existingContacts)

        // Insert or update all contacts in Room database
        database.contactDao().insertContacts(result.allCombined)

        result
    }

    /**
     * Returns a realistic address book for testing, demonstration, and offline emulation.
     */
    fun getSampleDeviceAddressBook(): List<DeviceContact> {
        return listOf(
            DeviceContact("1", "Moussa Diop (Directeur ADT)", "+221 77 000 11 22", "+221770001122"),
            DeviceContact("2", "Amina Traoré", "+225 07 88 99 00 11", "+2250788990011"),
            DeviceContact("3", "Ahmed Ould (Nouakchott)", "+222 45 12 34 56", "+22245123456"),
            DeviceContact("4", "Kabongo Mukendi (Kinshasa)", "+243 81 234 56 78", "+243812345678"),
            DeviceContact("5", "Samuel Eto (Douala)", "+237 699 12 34 56", "+237699123456"),
            DeviceContact("6", "Ibrahim Koné (Telecel Dev)", "+223 66 11 22 33", "+22366112233"),
            DeviceContact("7", "Sophie Laurent (Paris)", "+33 6 12 34 56 78", "+33612345678"),
            DeviceContact("8", "Oumar Ouédraogo", "+226 70 11 22 33", "+22670112233"),
            DeviceContact("9", "Chinedu Okafor (Fintech Lead)", "+234 803 112 2334", "+2348031122334"),
            DeviceContact("10", "Fatoumata Camara", "+224 620 11 22 33", "+224620112233"),
            DeviceContact("11", "Carlos Mendez (Madrid)", "+34 612 987 654", "+34612987654"),
            // Non-registered device contacts (invitees)
            DeviceContact("12", "Bakary Sissoko (Import-Export)", "+221 78 555 44 33", "+221785554433"),
            DeviceContact("13", "Mariam Diallo (Banque Atlantique)", "+221 76 999 88 77", "+221769998877"),
            DeviceContact("14", "Koffi Assan (Transports Abidjan)", "+225 05 12 34 56 78", "+2250512345678"),
            DeviceContact("15", "Dr. Jean-Paul Durand (Consultant Santé)", "+33 7 88 99 00 11", "+33788990011"),
            DeviceContact("16", "Assitan Coulibaly (Bamako Tech Hub)", "+223 76 44 33 22", "+22376443322")
        )
    }

    /**
     * Default registered ADT Pro users in the network directory.
     */
    fun getDefaultSeedAppUsers(): List<ContactEntity> {
        return listOf(
            ContactEntity("+221770001122", "Moussa Diop (Directeur ADT)", "+221 77 000 11 22", "+221", "avatar_1", "Consultant Télécom & Réseaux Pro", true, true, "En ligne"),
            ContactEntity("+22245123456", "Ahmed Ould (Mauritanie)", "+222 45 12 34 56", "+222", "avatar_3", "Expert Télécom Mauritel & Mattel", true, true, "En ligne"),
            ContactEntity("+243812345678", "Kabongo Mukendi (RD Congo)", "+243 81 234 56 78", "+243", "avatar_4", "Ingénieur Réseaux Vodacom & Airtel", true, true, "En ligne"),
            ContactEntity("+237699123456", "Samuel Eto (Cameroun)", "+237 699 12 34 56", "+237", "avatar_5", "Responsable Régional Camtel & MTN", true, false, "En ligne"),
            ContactEntity("+2250788990011", "Amina Traoré", "+225 07 88 99 00 11", "+225", "avatar_2", "Responsable Ventes Orange & Moov", true, true, "En ligne"),
            ContactEntity("+22366112233", "Ibrahim Koné", "+223 66 11 22 33", "+223", "avatar_3", "Développeur Solutions Telecel", true, false, "Aujourd'hui à 14:15"),
            ContactEntity("+33612345678", "Sophie Laurent (Paris)", "+33 6 12 34 56 78", "+33", "avatar_4", "Partenariats Internationaux ADT", true, false, "Hier à 22:05"),
            ContactEntity("+22670112233", "Oumar Ouédraogo", "+226 70 11 22 33", "+226", "avatar_5", "Logistique & E-commerce Sahel", true, false, "En ligne"),
            ContactEntity("+2348031122334", "Chinedu Okafor", "+234 803 112 2334", "+234", "avatar_6", "Fintech & Payments Lead", true, true, "En ligne"),
            ContactEntity("+224620112233", "Fatoumata Camara", "+224 620 11 22 33", "+224", "avatar_7", "Gestionnaire de Projets Pro", true, false, "Il y a 2h"),
            ContactEntity("+34612987654", "Carlos Mendez (Madrid)", "+34 612 987 654", "+34", "avatar_8", "Relations Fournisseurs Europe", true, false, "Il y a 10 min")
        )
    }
}
