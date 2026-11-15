package com.example.data.model

data class CountryItem(
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val exampleNumber: String,
    val defaultOperators: List<String>,
    val region: String = "Afrique"
)

object CountryData {
    val supportedCountries = listOf(
        // === MALI (PAYS PAR DÉFAUT) ===
        CountryItem("Mali", "+223", "🇲🇱", "66 12 34 56", listOf("Orange", "Moov Africa", "Telecel"), "Afrique"),

        // === AFRIQUE DE L'OUEST & CENTRALE ===
        CountryItem("Sénégal", "+221", "🇸🇳", "77 123 45 67", listOf("Orange", "Moov Africa", "Free", "Wave"), "Afrique"),
        CountryItem("Côte d'Ivoire", "+225", "🇨🇮", "07 12 34 56 78", listOf("Orange", "Moov Africa", "MTN", "Wave"), "Afrique"),
        CountryItem("Guinée", "+224", "🇬🇳", "620 12 34 56", listOf("Orange", "MTN", "Cellcom"), "Afrique"),
        CountryItem("Burkina Faso", "+226", "🇧🇫", "70 12 34 56", listOf("Orange", "Moov Africa", "Telecel"), "Afrique"),
        CountryItem("Niger", "+227", "🇳🇪", "90 12 34 56", listOf("Orange", "Moov Africa", "Airtel", "Niger Telecom"), "Afrique"),
        CountryItem("Togo", "+228", "🇹🇬", "90 12 34 56", listOf("Moov Africa", "Togocom"), "Afrique"),
        CountryItem("Bénin", "+229", "🇧🇯", "97 12 34 56", listOf("Moov Africa", "MTN", "Celtiis"), "Afrique"),
        CountryItem("Mauritanie", "+222", "🇲🇷", "45 12 34 56", listOf("Mauritel", "Chinguitel", "Mattel"), "Afrique"),
        CountryItem("Gambie", "+220", "🇬🇲", "712 3456", listOf("Africell", "QCell", "Gamcel"), "Afrique"),
        CountryItem("Guinée-Bissau", "+245", "🇬🇼", "95 123 45 67", listOf("Orange", "MTN"), "Afrique"),
        CountryItem("Sierra Leone", "+232", "🇸🇱", "76 123 456", listOf("Orange", "Africell", "QCell"), "Afrique"),
        CountryItem("Libéria", "+231", "🇱🇷", "88 123 4567", listOf("Lonestar Cell MTN", "Orange"), "Afrique"),
        CountryItem("Cap-Vert", "+238", "🇨🇻", "991 23 45", listOf("CV Movel", "Unitel T+"), "Afrique"),
        CountryItem("Ghana", "+233", "🇬🇭", "24 123 4567", listOf("MTN", "Telecel", "AirtelTigo"), "Afrique"),
        CountryItem("Nigeria", "+234", "🇳🇬", "803 123 4567", listOf("MTN", "Airtel", "Glo", "9mobile"), "Afrique"),
        CountryItem("Cameroun", "+237", "🇨🇲", "699 12 34 56", listOf("Orange", "MTN", "Camtel", "Nexttel"), "Afrique"),
        CountryItem("RD Congo", "+243", "🇨🇩", "81 234 56 78", listOf("Vodacom", "Orange", "Airtel", "Africell"), "Afrique"),
        CountryItem("Congo (Brazzaville)", "+242", "🇨🇬", "06 123 45 67", listOf("MTN", "Airtel"), "Afrique"),
        CountryItem("Gabon", "+241", "🇬🇦", "074 12 34 56", listOf("Moov Africa", "Airtel"), "Afrique"),
        CountryItem("Tchad", "+235", "🇹🇩", "66 12 34 56", listOf("Airtel", "Moov Africa"), "Afrique"),
        CountryItem("Centrafrique", "+236", "🇨🇫", "75 12 34 56", listOf("Orange", "Telecel"), "Afrique"),
        CountryItem("Guinée équatoriale", "+240", "🇬🇶", "222 123 456", listOf("Getesa", "Muni"), "Afrique"),
        CountryItem("Sao Tomé-et-Principe", "+239", "🇸🇹", "991 2345", listOf("CST", "Unitel STP"), "Afrique"),

        // === AFRIQUE DU NORD ===
        CountryItem("Maroc", "+212", "🇲🇦", "612 34 56 78", listOf("Maroc Telecom", "Orange", "inwi"), "Afrique"),
        CountryItem("Algérie", "+213", "🇩🇿", "550 12 34 56", listOf("Djezzy", "Mobilis", "Ooredoo"), "Afrique"),
        CountryItem("Tunisie", "+216", "🇹🇳", "98 12 34 56", listOf("Tunisie Telecom", "Ooredoo", "Orange"), "Afrique"),
        CountryItem("Libye", "+218", "🇱🇾", "91 123 4567", listOf("Libyana", "Al-Madar"), "Afrique"),
        CountryItem("Égypte", "+20", "🇪🇬", "10 1234 5678", listOf("Vodafone", "Orange", "Etisalat (e&)", "WE"), "Afrique"),
        CountryItem("Soudan", "+249", "🇸🇩", "91 234 5678", listOf("Zain", "MTN", "Sudani"), "Afrique"),
        CountryItem("Soudan du Sud", "+211", "🇸🇸", "92 123 4567", listOf("MTN", "Zain", "Digitel"), "Afrique"),

        // === AFRIQUE DE L'EST & OCÉAN INDIEN ===
        CountryItem("Kenya", "+254", "🇰🇪", "712 345 678", listOf("Safaricom", "Airtel", "Telkom Kenya"), "Afrique"),
        CountryItem("Tanzanie", "+255", "🇹🇿", "712 345 678", listOf("Vodacom", "Airtel", "Tigo", "Halotel"), "Afrique"),
        CountryItem("Ouganda", "+256", "🇺🇬", "772 123 456", listOf("MTN", "Airtel"), "Afrique"),
        CountryItem("Rwanda", "+250", "🇷🇼", "788 123 456", listOf("MTN", "Airtel"), "Afrique"),
        CountryItem("Burundi", "+257", "🇧🇮", "79 123 456", listOf("Econet", "Lumitel"), "Afrique"),
        CountryItem("Éthiopie", "+251", "🇪🇹", "91 123 4567", listOf("Ethio Telecom", "Safaricom"), "Afrique"),
        CountryItem("Somalie", "+252", "🇸🇴", "61 123 4567", listOf("Hormuud", "Telesom", "Golis"), "Afrique"),
        CountryItem("Djibouti", "+253", "🇩🇯", "77 12 34 56", listOf("Djibouti Telecom"), "Afrique"),
        CountryItem("Érythrée", "+291", "🇪🇷", "7 123 456", listOf("EriTel"), "Afrique"),
        CountryItem("Madagascar", "+261", "🇲🇬", "34 12 345 67", listOf("Orange", "Telma", "Airtel"), "Afrique"),
        CountryItem("Maurice", "+230", "🇲🇺", "57 12 34 56", listOf("Orange", "Emtel", "CHi"), "Afrique"),
        CountryItem("Comores", "+269", "🇰🇲", "32 12 345", listOf("Comores Telecom", "Telma"), "Afrique"),
        CountryItem("Seychelles", "+248", "🇸🇨", "251 2345", listOf("Cable & Wireless", "Airtel"), "Afrique"),

        // === AFRIQUE AUSTRALE ===
        CountryItem("Afrique du Sud", "+27", "🇿🇦", "82 123 4567", listOf("Vodacom", "MTN", "Telkom", "Cell C"), "Afrique"),
        CountryItem("Angola", "+244", "🇦🇴", "923 12 34 56", listOf("Unitel", "Movicel", "Africell"), "Afrique"),
        CountryItem("Mozambique", "+258", "🇲🇿", "84 123 4567", listOf("Vodacom", "Tmcel", "Movitel"), "Afrique"),
        CountryItem("Zambie", "+260", "🇿🇲", "97 123 4567", listOf("Airtel", "MTN", "Zamtel"), "Afrique"),
        CountryItem("Zimbabwe", "+263", "🇿🇼", "77 123 4567", listOf("Econet", "NetOne", "Telecel"), "Afrique"),
        CountryItem("Namibie", "+264", "🇳🇦", "81 123 4567", listOf("MTC", "TN Mobile"), "Afrique"),
        CountryItem("Malawi", "+265", "🇲🇼", "99 123 4567", listOf("Airtel", "TNM"), "Afrique"),
        CountryItem("Botswana", "+267", "🇧🇼", "71 234 567", listOf("Mascom", "Orange", "BTC"), "Afrique"),
        CountryItem("Lesotho", "+266", "🇱🇸", "58 12 34 56", listOf("Vodacom", "Econet"), "Afrique"),
        CountryItem("Eswatini", "+268", "🇸🇿", "76 12 34 56", listOf("MTN", "Eswatini Mobile"), "Afrique"),

        // === EUROPE ===
        CountryItem("France", "+33", "🇫🇷", "06 12 34 56 78", listOf("Orange", "SFR", "Bouygues", "Free"), "Europe"),
        CountryItem("Belgique", "+32", "🇧🇪", "470 12 34 56", listOf("Proximus", "Orange", "Base"), "Europe"),
        CountryItem("Suisse", "+41", "🇨🇭", "78 123 45 67", listOf("Swisscom", "Sunrise", "Salt"), "Europe"),
        CountryItem("Luxembourg", "+352", "🇱🇺", "621 12 34 56", listOf("POST", "Tango", "Orange"), "Europe"),
        CountryItem("Monaco", "+377", "🇲🇨", "6 12 34 56 78", listOf("Monaco Telecom"), "Europe"),
        CountryItem("Espagne", "+34", "🇪🇸", "612 345 678", listOf("Orange", "Movistar", "Vodafone", "Yoigo"), "Europe"),
        CountryItem("Portugal", "+351", "🇵🇹", "912 345 678", listOf("MEO", "NOS", "Vodafone"), "Europe"),
        CountryItem("Italie", "+39", "🇮🇹", "312 345 6789", listOf("TIM", "Vodafone", "WindTre", "Iliad"), "Europe"),
        CountryItem("Allemagne", "+49", "🇩🇪", "151 12345678", listOf("Telekom", "Vodafone", "O2"), "Europe"),
        CountryItem("Royaume-Uni", "+44", "🇬🇧", "7911 123456", listOf("EE", "Vodafone", "O2", "Three"), "Europe"),
        CountryItem("Irlande", "+353", "🇮🇪", "87 123 4567", listOf("Vodafone", "Three", "Eir"), "Europe"),
        CountryItem("Pays-Bas", "+31", "🇳🇱", "6 12345678", listOf("KPN", "Vodafone", "Odido"), "Europe"),
        CountryItem("Pologne", "+48", "🇵🇱", "512 345 678", listOf("Orange", "Play", "Plus", "T-Mobile"), "Europe"),
        CountryItem("Roumanie", "+40", "🇷🇴", "712 345 678", listOf("Orange", "Vodafone", "Digi"), "Europe"),
        CountryItem("Grèce", "+30", "🇬🇷", "69 1234 5678", listOf("Cosmote", "Vodafone", "Nova"), "Europe"),
        CountryItem("Suède", "+46", "🇸🇪", "70 123 45 67", listOf("Telia", "Tele2", "Telenor", "Tre"), "Europe"),
        CountryItem("Norvège", "+47", "🇳🇴", "912 34 567", listOf("Telenor", "Telia", "Ice"), "Europe"),
        CountryItem("Danemark", "+45", "🇩🇰", "20 12 34 56", listOf("TDC", "Telenor", "Telia", "3"), "Europe"),
        CountryItem("Finlande", "+358", "🇫🇮", "40 123 4567", listOf("Elisa", "Telia", "DNA"), "Europe"),
        CountryItem("Autriche", "+43", "🇦🇹", "664 123 4567", listOf("A1", "Magenta", "Drei"), "Europe"),
        CountryItem("République tchèque", "+420", "🇨🇿", "601 123 456", listOf("T-Mobile", "O2", "Vodafone"), "Europe"),
        CountryItem("Hongrie", "+36", "🇭🇺", "20 123 4567", listOf("Yettel", "Telekom", "Vodafone"), "Europe"),
        CountryItem("Slovaquie", "+421", "🇸🇰", "905 123 456", listOf("Orange", "Slovak Telekom", "O2"), "Europe"),
        CountryItem("Croatie", "+385", "🇭🇷", "91 234 5678", listOf("Hrvatski Telekom", "A1", "Telemach"), "Europe"),
        CountryItem("Serbie", "+381", "🇷🇸", "64 123 4567", listOf("mts", "Yettel", "A1"), "Europe"),
        CountryItem("Bulgarie", "+359", "🇧🇬", "88 123 4567", listOf("Vivacom", "A1", "Yettel"), "Europe"),
        CountryItem("Ukraine", "+380", "🇺🇦", "50 123 45 67", listOf("Kyivstar", "Vodafone", "lifecell"), "Europe"),
        CountryItem("Russie", "+7", "🇷🇺", "912 345 67 89", listOf("MTS", "MegaFon", "Beeline", "Tele2"), "Europe"),
        CountryItem("Islande", "+354", "🇮🇸", "612 3456", listOf("Síminn", "Vodafone", "Nova"), "Europe"),
        CountryItem("Turquie", "+90", "🇹🇷", "532 123 45 67", listOf("Turkcell", "Vodafone", "Türk Telekom"), "Europe"),

        // === AMÉRIQUES & CARAÏBES ===
        CountryItem("États-Unis", "+1", "🇺🇸", "202 555 0123", listOf("AT&T", "Verizon", "T-Mobile"), "Amériques"),
        CountryItem("Canada", "+1", "🇨🇦", "416 555 0199", listOf("Rogers", "Bell", "Telus"), "Amériques"),
        CountryItem("Mexique", "+52", "🇲🇽", "55 1234 5678", listOf("Telcel", "AT&T", "Movistar"), "Amériques"),
        CountryItem("Brésil", "+55", "🇧🇷", "11 91234 5678", listOf("Vivo", "Claro", "TIM"), "Amériques"),
        CountryItem("Argentine", "+54", "🇦🇷", "9 11 2345 6789", listOf("Claro", "Movistar", "Personal"), "Amériques"),
        CountryItem("Colombie", "+57", "🇨🇴", "300 123 4567", listOf("Claro", "Tigo", "Movistar"), "Amériques"),
        CountryItem("Chili", "+56", "🇨🇱", "9 1234 5678", listOf("Entel", "Movistar", "Claro", "WOM"), "Amériques"),
        CountryItem("Pérou", "+51", "🇵🇪", "912 345 678", listOf("Claro", "Movistar", "Entel", "Bitel"), "Amériques"),
        CountryItem("Venezuela", "+58", "🇻🇪", "414 123 4567", listOf("Movistar", "Digitel", "Movilnet"), "Amériques"),
        CountryItem("Équateur", "+593", "🇪🇨", "99 123 4567", listOf("Claro", "Movistar", "CNT"), "Amériques"),
        CountryItem("Bolivie", "+591", "🇧🇴", "7123 4567", listOf("Entel", "Tigo", "Viva"), "Amériques"),
        CountryItem("Paraguay", "+595", "🇵🇾", "981 123 456", listOf("Tigo", "Claro", "Personal"), "Amériques"),
        CountryItem("Uruguay", "+598", "🇺🇾", "99 123 456", listOf("Antel", "Claro", "Movistar"), "Amériques"),
        CountryItem("Haïti", "+509", "🇭🇹", "34 12 3456", listOf("Digicel", "Natcom"), "Amériques"),
        CountryItem("République dominicaine", "+1", "🇩🇴", "809 555 0123", listOf("Claro", "Altice", "Viva"), "Amériques"),
        CountryItem("Cuba", "+53", "🇨🇺", "5 123 4567", listOf("ETECSA (Cubacel)"), "Amériques"),
        CountryItem("Panama", "+507", "🇵🇦", "6123 4567", listOf("+Móvil", "Tigo", "Digicel"), "Amériques"),
        CountryItem("Costa Rica", "+506", "🇨🇷", "8123 4567", listOf("Kölbi", "Claro", "Liberty"), "Amériques"),
        CountryItem("Guatemala", "+502", "🇬🇹", "5123 4567", listOf("Tigo", "Claro"), "Amériques"),
        CountryItem("Honduras", "+504", "🇭🇳", "9123 4567", listOf("Tigo", "Claro"), "Amériques"),
        CountryItem("Salvador", "+503", "🇸🇻", "7123 4567", listOf("Tigo", "Claro", "Digicel", "Movistar"), "Amériques"),
        CountryItem("Nicaragua", "+505", "🇳🇮", "8123 4567", listOf("Claro", "Tigo"), "Amériques"),
        CountryItem("Jamaïque", "+1", "🇯🇲", "876 555 0123", listOf("Digicel", "Flow"), "Amériques"),

        // === MOYEN-ORIENT ===
        CountryItem("Arabie Saoudite", "+966", "🇸🇦", "50 123 4567", listOf("STC", "Mobily", "Zain"), "Moyen-Orient"),
        CountryItem("Émirats Arabes Unis", "+971", "🇦🇪", "50 123 4567", listOf("e& (Etisalat)", "du"), "Moyen-Orient"),
        CountryItem("Qatar", "+974", "🇶🇦", "3312 3456", listOf("Ooredoo", "Vodafone"), "Moyen-Orient"),
        CountryItem("Koweït", "+965", "🇰🇼", "9123 4567", listOf("Zain", "Ooredoo", "stc"), "Moyen-Orient"),
        CountryItem("Oman", "+968", "🇴🇲", "9123 4567", listOf("Omantel", "Ooredoo", "Vodafone"), "Moyen-Orient"),
        CountryItem("Bahreïn", "+973", "🇧🇭", "3612 3456", listOf("Batelco", "Zain", "stc"), "Moyen-Orient"),
        CountryItem("Jordanie", "+962", "🇯🇴", "7 9123 4567", listOf("Zain", "Orange", "Umniah"), "Moyen-Orient"),
        CountryItem("Liban", "+961", "🇱🇧", "70 123 456", listOf("Alfa", "touch"), "Moyen-Orient"),
        CountryItem("Irak", "+964", "🇮🇶", "770 123 4567", listOf("Zain", "Asiacell", "Korek"), "Moyen-Orient"),
        CountryItem("Yémen", "+967", "🇾🇪", "771 234 567", listOf("Yemen Mobile", "Sabafon", "YOU"), "Moyen-Orient"),
        CountryItem("Syrie", "+963", "🇸🇾", "944 123 456", listOf("Syriatel", "MTN"), "Moyen-Orient"),
        CountryItem("Israël", "+972", "🇮🇱", "50 123 4567", listOf("Cellcom", "Partner", "Pelephone", "Hot Mobile"), "Moyen-Orient"),

        // === ASIE & OCÉANIE ===
        CountryItem("Chine", "+86", "🇨🇳", "138 0013 8000", listOf("China Mobile", "China Telecom", "China Unicom"), "Asie"),
        CountryItem("Inde", "+91", "🇮🇳", "98123 45678", listOf("Jio", "Airtel", "Vi", "BSNL"), "Asie"),
        CountryItem("Japon", "+81", "🇯🇵", "90 1234 5678", listOf("NTT Docomo", "SoftBank", "au", "Rakuten"), "Asie"),
        CountryItem("Corée du Sud", "+82", "🇰🇷", "10 1234 5678", listOf("SK Telecom", "KT", "LG U+"), "Asie"),
        CountryItem("Indonésie", "+62", "🇮🇩", "812 3456 7890", listOf("Telkomsel", "Indosat", "XL Axiata"), "Asie"),
        CountryItem("Pakistan", "+92", "🇵🇰", "300 1234567", listOf("Jazz", "Telenor", "Zong", "Ufone"), "Asie"),
        CountryItem("Bangladesh", "+880", "🇧🇩", "1712 345678", listOf("Grameenphone", "Robi", "Banglalink", "Teletalk"), "Asie"),
        CountryItem("Philippines", "+63", "🇵🇭", "917 123 4567", listOf("Globe", "Smart", "DITO"), "Asie"),
        CountryItem("Vietnam", "+84", "🇻🇳", "91 234 5678", listOf("Viettel", "Vinaphone", "MobiFone"), "Asie"),
        CountryItem("Thaïlande", "+66", "🇹🇭", "81 234 5678", listOf("AIS", "TrueMove H", "dtac"), "Asie"),
        CountryItem("Malaisie", "+60", "🇲🇾", "12 345 6789", listOf("CelcomDigi", "Maxis", "U Mobile"), "Asie"),
        CountryItem("Singapour", "+65", "🇸🇬", "8123 4567", listOf("Singtel", "StarHub", "M1", "SIMBA"), "Asie"),
        CountryItem("Taïwan", "+886", "🇹🇼", "912 345 678", listOf("Chunghwa", "Taiwan Mobile", "FarEasTone"), "Asie"),
        CountryItem("Hong Kong", "+852", "🇭🇰", "9123 4567", listOf("csl", "SmarTone", "3 Hong Kong", "CMHK"), "Asie"),
        CountryItem("Sri Lanka", "+94", "🇱🇰", "77 123 4567", listOf("Dialog", "Mobitel", "Airtel", "Hutch"), "Asie"),
        CountryItem("Népal", "+977", "🇳🇵", "984 123 4567", listOf("Ncell", "Nepal Telecom"), "Asie"),
        CountryItem("Kazakhstan", "+7", "🇰🇿", "701 123 45 67", listOf("Beeline", "Kcell", "Tele2"), "Asie"),
        CountryItem("Ouzbékistan", "+998", "🇺🇿", "90 123 45 67", listOf("Beeline", "Ucell", "Mobiuz", "Uztelecom"), "Asie"),
        CountryItem("Azerbaïdjan", "+994", "🇦🇿", "50 123 45 67", listOf("Azercell", "Bakcell", "Nar"), "Asie"),
        CountryItem("Géorgie", "+995", "🇬🇪", "599 12 34 56", listOf("MagtiCom", "Silknet", "Cellfie"), "Asie"),
        CountryItem("Arménie", "+374", "🇦🇲", "77 12 34 56", listOf("Viva-MTS", "Team Telecom", "Ucom"), "Asie"),
        CountryItem("Australie", "+61", "🇦🇺", "412 345 678", listOf("Telstra", "Optus", "Vodafone"), "Océanie"),
        CountryItem("Nouvelle-Zélande", "+64", "🇳🇿", "21 123 4567", listOf("One NZ", "Spark", "2degrees"), "Océanie")
    )

    val defaultCountry: CountryItem get() = supportedCountries.first()

    val availableRegions: List<String> = listOf("Tous", "Afrique", "Europe", "Amériques", "Moyen-Orient", "Asie", "Océanie")

    fun normalize(text: String): String {
        return text.lowercase()
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("ë", "e")
            .replace("à", "a")
            .replace("á", "a")
            .replace("â", "a")
            .replace("î", "i")
            .replace("ï", "i")
            .replace("ô", "o")
            .replace("ö", "o")
            .replace("ù", "u")
            .replace("ú", "u")
            .replace("ü", "u")
            .replace("ç", "c")
            .replace("l'", "")
            .replace("d'", "")
            .trim()
    }

    fun matchesSearch(item: CountryItem, query: String, selectedRegion: String = "Tous"): Boolean {
        if (selectedRegion != "Tous" && !item.region.equals(selectedRegion, ignoreCase = true)) {
            return false
        }
        if (query.isBlank()) return true
        val cleanQuery = normalize(query)
        val cleanName = normalize(item.name)
        val cleanDial = item.dialCode.replace("+", "").trim()
        val queryDial = query.replace("+", "").trim()

        return cleanName.contains(cleanQuery) ||
                item.name.contains(query, ignoreCase = true) ||
                item.dialCode.contains(query) ||
                (queryDial.isNotBlank() && cleanDial.startsWith(queryDial))
    }

    fun findByDialCode(code: String): CountryItem? {
        val clean = if (code.startsWith("+")) code else "+$code"
        return supportedCountries.firstOrNull { it.dialCode == clean }
    }

    fun findAllByDialCode(code: String): List<CountryItem> {
        val clean = if (code.startsWith("+")) code else "+$code"
        return supportedCountries.filter { it.dialCode == clean }
    }

    /**
     * Auto-detect country and remaining phone number if input starts with a known international dial code
     * (e.g. +1, +33, +221, +225, +243, or 00...). Only activates when user explicitly enters international prefix (+ or 00),
     * avoiding unwanted switches when entering ordinary local subscriber digits.
     */
    fun detectCountryAndNumber(input: String): Pair<CountryItem, String>? {
        val trimmed = input.trim()
        val hasInternationalPrefix = trimmed.startsWith("+") || trimmed.startsWith("00")
        if (!hasInternationalPrefix) {
            return null
        }
        val sanitized = trimmed
            .replace(" ", "")
            .replace("(", "")
            .replace(")", "")
            .replace("-", "")
            .replace(".", "")
        val withPlus = if (sanitized.startsWith("+")) sanitized else "+" + sanitized.substring(2)
        
        // Sort descending by dial code length to match longest prefix first
        for (country in supportedCountries.sortedByDescending { it.dialCode.length }) {
            if (withPlus.startsWith(country.dialCode) && withPlus.length > country.dialCode.length) {
                var remaining = withPlus.substring(country.dialCode.length)
                // If remaining starts with leading 0 (e.g. +33 06...), strip the trunk prefix
                if (remaining.startsWith("0") && remaining.length > 1) {
                    remaining = remaining.removePrefix("0")
                }
                return Pair(country, remaining)
            }
        }
        return null
    }
}
