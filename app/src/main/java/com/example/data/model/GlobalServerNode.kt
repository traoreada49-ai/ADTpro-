package com.example.data.model

data class GlobalServerNode(
    val id: String,
    val name: String,
    val region: String,
    val flag: String,
    val city: String,
    val endpoint: String,
    val pingMs: Int,
    val status: String = "Opérationnel",
    val encryption: String = "Quantum-Resistant E2EE",
    val isRecommended: Boolean = false
) {
    companion object {
        val ALL_SERVERS = listOf(
            GlobalServerNode(
                id = "eu_west",
                name = "Europe Ouest Relay",
                region = "Europe",
                flag = "🇪🇺",
                city = "Paris / Francfort",
                endpoint = "eu-relay-01.adtpro.net",
                pingMs = 24,
                isRecommended = true
            ),
            GlobalServerNode(
                id = "af_west",
                name = "Afrique Ouest Hub",
                region = "Afrique",
                flag = "🇸🇳",
                city = "Dakar / Abidjan / Bamako",
                endpoint = "af-west-01.adtpro.net",
                pingMs = 38,
                isRecommended = true
            ),
            GlobalServerNode(
                id = "af_central",
                name = "Afrique Centrale & Est",
                region = "Afrique",
                flag = "🇨🇲",
                city = "Douala / Nairobi / Kinshasa",
                endpoint = "af-central-02.adtpro.net",
                pingMs = 49
            ),
            GlobalServerNode(
                id = "na_east",
                name = "Amérique du Nord Est",
                region = "Amérique",
                flag = "🇺🇸",
                city = "New York / Montréal",
                endpoint = "na-east-01.adtpro.net",
                pingMs = 82
            ),
            GlobalServerNode(
                id = "asia_sg",
                name = "Asie & Pacifique Relay",
                region = "Asie",
                flag = "🇸🇬",
                city = "Singapour / Tokyo",
                endpoint = "asia-sg-01.adtpro.net",
                pingMs = 135
            ),
            GlobalServerNode(
                id = "me_dubai",
                name = "Moyen-Orient & Golfe",
                region = "Moyen-Orient",
                flag = "🇦🇪",
                city = "Dubaï / Riyad",
                endpoint = "me-gulf-01.adtpro.net",
                pingMs = 64
            )
        )
    }
}
