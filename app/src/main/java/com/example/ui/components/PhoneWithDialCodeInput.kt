package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.DialCodeValidationResult
import com.example.data.auth.PhoneNumberValidator
import com.example.data.auth.PhoneValidationResult
import com.example.data.model.CountryData
import com.example.data.model.CountryItem
import com.example.ui.theme.AdtPrimary

/**
 * Reusable international phone number input with separate country dial code selector & validator.
 * Validates the country calling code against ITU/supported countries in CountryData, normalizes
 * trunk zeroes and national digit counts per country, and surfaces real-time E.164 status.
 */
@Composable
fun PhoneWithDialCodeInput(
    selectedCountry: CountryItem,
    enteredDialCode: String,
    phoneNumber: String,
    onCountrySelected: (CountryItem) -> Unit,
    onDialCodeChanged: (String) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Numéro de téléphone",
    placeholder: String = selectedCountry.exampleNumber,
    isError: Boolean = false,
    errorMessage: String? = null,
    showValidationStatus: Boolean = true,
    onDone: (() -> Unit)? = null
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    val dialValidation = remember(enteredDialCode, selectedCountry) {
        PhoneNumberValidator.validateDialCode(enteredDialCode, selectedCountry)
    }
    val phoneValidation = remember(enteredDialCode, phoneNumber, selectedCountry) {
        PhoneNumberValidator.validate(enteredDialCode, phoneNumber, selectedCountry)
    }

    val isDialCodeInvalid = dialValidation is DialCodeValidationResult.InvalidFormat ||
        dialValidation is DialCodeValidationResult.UnknownDialCode ||
        (dialValidation is DialCodeValidationResult.Empty && phoneNumber.isNotBlank())

    val isPhoneFieldInvalid = isError || (phoneNumber.isNotBlank() && (
        phoneValidation is PhoneValidationResult.TooShort ||
            phoneValidation is PhoneValidationResult.TooLong ||
            phoneValidation is PhoneValidationResult.InvalidFormat
        ))

    val activeRule = remember(selectedCountry) {
        PhoneNumberValidator.getRuleForCountry(selectedCountry)
    }
    val expectedDigitsText = remember(activeRule) {
        PhoneNumberValidator.formatExpectedDigits(activeRule)
    }
    val cleanNationalDigits = remember(phoneNumber, activeRule) {
        val rawDigits = phoneNumber.filter { it.isDigit() }
        if (activeRule.stripLeadingZeroForE164 && rawDigits.startsWith("0") && rawDigits.length > 1) {
            rawDigits.removePrefix("0")
        } else {
            rawDigits
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Country Selector Card with Dial Code Verification Pill
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .clickable { showCountryPicker = true }
                .testTag("btn_select_country_tile"),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (isDialCodeInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
            ),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (dialValidation is DialCodeValidationResult.Valid) {
                        Text(selectedCountry.flagEmoji, fontSize = 22.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = selectedCountry.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedCountry.region} • Format attendu : $expectedDigitsText chiffres",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Indicatif inconnu",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Pays non reconnu",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Touchez pour choisir un pays dans la liste",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDialCodeInvalid) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            AdtPrimary.copy(alpha = 0.12f)
                        }
                    ) {
                        Text(
                            text = when (dialValidation) {
                                is DialCodeValidationResult.Valid -> dialValidation.normalizedDialCode
                                is DialCodeValidationResult.UnknownDialCode -> dialValidation.dialCode
                                else -> selectedCountry.dialCode
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDialCodeInvalid) MaterialTheme.colorScheme.error else AdtPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Changer pays",
                        tint = if (isDialCodeInvalid) MaterialTheme.colorScheme.error else AdtPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Shared dial code disambiguation chips (e.g. +1 USA / Canada / Rép. Dom., +7 Russie / Kazakhstan)
        if (dialValidation is DialCodeValidationResult.Valid && dialValidation.matchingCountries.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dialValidation.matchingCountries, key = { it.name }) { candidate ->
                    val isSelected = candidate.name == selectedCountry.name
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCountrySelected(candidate) },
                        label = { Text("${candidate.flagEmoji} ${candidate.name}", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdtPrimary.copy(alpha = 0.16f),
                            selectedLabelColor = AdtPrimary
                        )
                    )
                }
            }
        }

        // Dual Input Row: Separate Country Calling Code (+Code) & National Phone Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Separate Country Dial Code Input
            OutlinedTextField(
                value = enteredDialCode.removePrefix("+"),
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(3)
                    onDialCodeChanged(digits)
                    if (digits.isNotEmpty()) {
                        val matches = CountryData.findAllByDialCode("+$digits")
                        if (matches.isNotEmpty() && matches.none { it.name == selectedCountry.name }) {
                            onCountrySelected(matches.first())
                        }
                    }
                },
                label = { Text("Indicatif", fontSize = 11.sp) },
                prefix = {
                    Text(
                        "+",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDialCodeInvalid) MaterialTheme.colorScheme.error else AdtPrimary
                    )
                },
                isError = isDialCodeInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(108.dp)
                    .testTag("input_separate_dial_code"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdtPrimary,
                    focusedLabelColor = AdtPrimary
                )
            )

            // Separate National Phone Number Input
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input ->
                    val detected = CountryData.detectCountryAndNumber(input)
                    if (detected != null) {
                        val (c, remaining) = detected
                        onCountrySelected(c)
                        onDialCodeChanged(c.dialCode.removePrefix("+"))
                        onPhoneNumberChanged(remaining)
                    } else {
                        val dial = enteredDialCode.trim().removePrefix("+")
                        val countryDial = selectedCountry.dialCode
                        val clean = when {
                            input.startsWith(countryDial) -> input.removePrefix(countryDial).trim()
                            dial.isNotEmpty() && input.startsWith("+$dial") -> input.removePrefix("+$dial").trim()
                            else -> input.filter { it.isDigit() || it == ' ' }
                        }
                        onPhoneNumberChanged(clean)
                    }
                },
                label = { Text(label, fontSize = 11.sp) },
                placeholder = { Text(placeholder, fontSize = 13.sp) },
                trailingIcon = {
                    if (phoneNumber.isNotBlank()) {
                        IconButton(onClick = { onPhoneNumberChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Effacer", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onDone?.invoke() }
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                isError = isPhoneFieldInvalid,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_separate_phone_number"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdtPrimary,
                    focusedLabelColor = AdtPrimary
                )
            )
        }

        // Inline digit progress & validation status pill
        val resolvedErrorMessage = errorMessage ?: PhoneNumberValidator.getErrorMessage(phoneValidation)
        if (isError && !errorMessage.isNullOrBlank()) {
            ValidationFeedbackBanner(
                isError = true,
                isValid = false,
                text = errorMessage,
                testTag = "phone_validation_error"
            )
        } else if (showValidationStatus) {
            when (phoneValidation) {
                is PhoneValidationResult.Valid -> {
                    ValidationFeedbackBanner(
                        isError = false,
                        isValid = true,
                        text = "${selectedCountry.flagEmoji} Numéro E.164 valide : ${phoneValidation.e164Number}",
                        testTag = "phone_validation_valid"
                    )
                }
                is PhoneValidationResult.InvalidDialCode -> {
                    ValidationFeedbackBanner(
                        isError = true,
                        isValid = false,
                        text = phoneValidation.reason,
                        testTag = "phone_validation_error"
                    )
                }
                is PhoneValidationResult.TooShort -> {
                    ValidationFeedbackBanner(
                        isError = false,
                        isValid = false,
                        text = "Saisie en cours (${cleanNationalDigits.length}/$expectedDigitsText chiffres) • Ex : ${selectedCountry.exampleNumber}",
                        testTag = "phone_validation_progress"
                    )
                }
                is PhoneValidationResult.TooLong,
                is PhoneValidationResult.InvalidFormat -> {
                    ValidationFeedbackBanner(
                        isError = true,
                        isValid = false,
                        text = resolvedErrorMessage ?: "Numéro invalide",
                        testTag = "phone_validation_error"
                    )
                }
                PhoneValidationResult.Empty -> Unit
            }
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            onDismiss = { showCountryPicker = false },
            onSelectCountry = { country ->
                onCountrySelected(country)
                onDialCodeChanged(country.dialCode.removePrefix("+"))
                showCountryPicker = false
            }
        )
    }
}

@Composable
private fun ValidationFeedbackBanner(
    isError: Boolean,
    isValid: Boolean,
    text: String,
    testTag: String
) {
    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        isValid -> AdtPrimary.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val contentColor = when {
        isError -> MaterialTheme.colorScheme.error
        isValid -> AdtPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when {
        isError -> Icons.Default.ErrorOutline
        isValid -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isValid || isError) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

/**
 * Searchable modal dialog for picking country & dial code with region filtering.
 */
@Composable
fun CountryPickerDialog(
    onDismiss: () -> Unit,
    onSelectCountry: (CountryItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf("Tous") }

    val filteredCountries = remember(searchQuery, selectedRegion) {
        CountryData.supportedCountries.filter {
            CountryData.matchesSearch(it, searchQuery, selectedRegion)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = AdtPrimary)
                Column {
                    Text("Sélectionner l'indicatif pays", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "${filteredCountries.size} pays disponibles",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher pays ou indicatif...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_country_picker_input")
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(CountryData.availableRegions) { region ->
                        val isSelected = region == selectedRegion
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedRegion = region },
                            label = { Text(region, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdtPrimary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(filteredCountries, key = { it.name + it.dialCode }) { country ->
                        val rule = PhoneNumberValidator.getRuleForCountry(country)
                        val digitsLabel = PhoneNumberValidator.formatExpectedDigits(rule)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { onSelectCountry(country) }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("country_item_${country.dialCode.removePrefix("+")}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(country.flagEmoji, fontSize = 22.sp)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(country.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = country.region,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Ex: ${country.exampleNumber} ($digitsLabel chiffres)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AdtPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = country.dialCode,
                                    color = AdtPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
