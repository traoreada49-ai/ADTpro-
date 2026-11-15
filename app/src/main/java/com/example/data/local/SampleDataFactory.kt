package com.example.data.local

import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChannelPostEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.GroupMemberEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity

/**
 * Factory providing rich, realistic, multicultural business and telecom sample data
 * for ADT Pro: Contacts, Chats, Group Members, Messages, Channels, Posts, Statuses, and Calls.
 */
object SampleDataFactory {

    fun getEnrichedContacts(): List<ContactEntity> {
        return listOf(
            ContactEntity("+221770001122", "Moussa Diop (Directeur ADT)", "+221 77 000 11 22", "+221", "avatar_1", "Consultant Télécom & Réseaux Pro", true, true, "En ligne"),
            ContactEntity("+225078899001", "Amina Traoré", "+225 07 88 99 00 11", "+225", "avatar_2", "Responsable Ventes Orange & Moov", true, true, "En ligne"),
            ContactEntity("+223661122334", "Ibrahim Koné", "+223 66 11 22 33", "+223", "avatar_3", "Développeur Solutions Telecel", true, false, "Aujourd'hui à 14:15"),
            ContactEntity("+221778901234", "Fatou Ndiaye (Dakar)", "+221 77 890 12 34", "+221", "avatar_4", "Directrice Partenariats Sonatel & Orange", true, true, "En ligne"),
            ContactEntity("+22376543210", "Dr. Cheick Modibo Diarra", "+223 76 54 32 10", "+223", "avatar_5", "Expert Énergie Solaire & Télécoms", true, true, "En ligne"),
            ContactEntity("+33612345678", "Sophie Laurent (Paris)", "+33 6 12 34 56 78", "+33", "avatar_6", "Partenariats Internationaux ADT Europe", true, false, "Hier à 22:05"),
            ContactEntity("+22670112233", "Oumar Ouédraogo", "+226 70 11 22 33", "+226", "avatar_7", "Logistique & E-commerce Sahel", true, false, "En ligne"),
            ContactEntity("+234803112233", "Chinedu Okafor (Lagos)", "+234 803 112 2334", "+234", "avatar_8", "Fintech & Payments Lead Africa", true, true, "En ligne"),
            ContactEntity("+234805566778", "Suleiman Bello (Kano)", "+234 805 566 7788", "+234", "avatar_1", "Agribusiness & International Shipping", true, false, "Il y a 30 min"),
            ContactEntity("+224620112233", "Fatoumata Camara (Conakry)", "+224 620 11 22 33", "+224", "avatar_2", "Gestionnaire Projets Pro & Mines", true, false, "Il y a 2h"),
            ContactEntity("+22890112233", "Koffi Mensah (Lomé)", "+228 90 11 22 33", "+228", "avatar_3", "Architecte Systèmes Togocom & Moov", true, true, "En ligne"),
            ContactEntity("+237699112233", "Paul Atangana (Yaoundé)", "+237 699 11 22 33", "+237", "avatar_4", "Solutions Entreprises MTN & Camtel", true, false, "Hier à 18:40"),
            ContactEntity("+237677445566", "Jean-Paul Mbarga (Douala)", "+237 677 44 55 66", "+237", "avatar_5", "Déploiement Fibre & Datacenters", true, true, "En ligne"),
            ContactEntity("+243812345678", "Kabongo Mukendi (Kinshasa)", "+243 81 234 56 78", "+243", "avatar_6", "Ingénieur Télécom Vodacom & RDC", true, true, "En ligne"),
            ContactEntity("+254712345678", "David Kimani (Nairobi)", "+254 712 345 678", "+254", "avatar_7", "Chef de Projet Safaricom M-Pesa", true, false, "En ligne"),
            ContactEntity("+254722998877", "Grace Nyambura (Nairobi)", "+254 722 998 877", "+254", "avatar_8", "Directrice Mobile Money & FinTech", true, true, "En ligne"),
            ContactEntity("+255712345678", "Asha Mwangi (Dar es Salaam)", "+255 712 345 678", "+255", "avatar_1", "Spécialiste Réseaux Tigo Tanzanie", true, false, "Il y a 15 min"),
            ContactEntity("+244923112233", "Guilherme Santos (Luanda)", "+244 923 11 22 33", "+244", "avatar_2", "Opérations Réseau Unitel Angola", true, true, "En ligne"),
            ContactEntity("+212612345678", "Yassine Benali (Casablanca)", "+212 612 34 56 78", "+212", "avatar_3", "Ingénieur Réseaux Maroc Telecom", true, true, "En ligne"),
            ContactEntity("+212661998877", "Khadija Mansoor (Rabat)", "+212 661 99 88 77", "+212", "avatar_4", "Cybersécurité & Audit ISO 27001", true, true, "En ligne"),
            ContactEntity("+34612987654", "Carlos Mendez (Madrid)", "+34 612 987 654", "+34", "avatar_5", "Relations Fournisseurs Télécom Europe", true, false, "Il y a 10 min"),
            ContactEntity("+12025550123", "Michael Smith (New York)", "+1 202 555 0123", "+1", "avatar_6", "Directeur Cloud & Infrastructure", true, true, "En ligne"),
            ContactEntity("+32470123456", "Lucas Peeters (Bruxelles)", "+32 470 12 34 56", "+32", "avatar_7", "Architecte Réseaux Proximus", true, true, "En ligne"),
            ContactEntity("+41781234567", "Thomas Meier (Genève)", "+41 78 123 45 67", "+41", "avatar_8", "Fintech & Sécurité Bancaire", true, false, "Il y a 30 min"),
            ContactEntity("+447911123456", "Oliver Brown (Londres)", "+44 7911 123456", "+44", "avatar_1", "Partenaire Stratégique EMEA", true, false, "En ligne"),
            ContactEntity("+4915112345678", "Lukas Schmidt (Berlin)", "+49 151 12345678", "+49", "avatar_2", "Ingénieur VoIP & Systèmes Télécom", true, false, "Hier"),
            ContactEntity("+8613800138000", "Wei Zhang (Pékin)", "+86 138 0013 8000", "+86", "avatar_3", "Senior Architect China Mobile", true, true, "En ligne"),
            ContactEntity("+966501234567", "Mohammed Al-Otaibi (Riyad)", "+966 50 123 4567", "+966", "avatar_4", "Directeur Télécom STC KSA", true, true, "En ligne"),
            ContactEntity("+971501234567", "Rashid Al-Maktoum (Dubaï)", "+971 50 123 4567", "+971", "avatar_5", "Partenaire e& (Etisalat) UAE", true, false, "En ligne")
        )
    }

    fun getEnrichedChats(): List<ChatEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ChatEntity(
                id = "chat_meta_ai",
                title = "✨ Meta AI",
                isGroup = false,
                avatarUrl = "meta_ai",
                lastMessage = "Tapez /imagine pour générer une image ou posez-moi n'importe quelle question 🎨",
                lastTimestamp = now - 1000 * 60,
                unreadCount = 0,
                isPinned = true,
                membersCount = 2,
                description = "Assistant d'intelligence artificielle officiel de Meta propulsé par Llama 3.3 et Gemini."
            ),
            ChatEntity(
                id = "chat_group_adt_pro",
                title = "🚀 ADT Pro Entreprises & Partenaires",
                isGroup = true,
                avatarUrl = "group_adt",
                lastMessage = "Moussa: Nouvelle mise à jour des liaisons fibre Orange et Moov Africa.",
                lastTimestamp = now - 1000 * 60 * 3,
                unreadCount = 2,
                isPinned = true,
                membersCount = 12,
                description = "Groupe officiel de coordination des partenaires télécoms et entreprises ADT Pro."
            ),
            ChatEntity(
                id = "chat_group_innovation",
                title = "💡 Hub Innovation & IA Afrique",
                isGroup = true,
                avatarUrl = "group_innovation",
                lastMessage = "Cheick: Le nouveau système de routage intelligent et IA locale est en phase de test.",
                lastTimestamp = now - 1000 * 60 * 15,
                unreadCount = 3,
                isPinned = true,
                membersCount = 18,
                description = "Échange technique sur l'intelligence artificielle, l'automatisation et le haut débit."
            ),
            ChatEntity(
                id = "chat_group_trade",
                title = "📈 Bourse & Import-Export Afrique-Monde",
                isGroup = true,
                avatarUrl = "group_trade",
                lastMessage = "Suleiman: Les connaissements maritimes et factures proforma ont été validés.",
                lastTimestamp = now - 1000 * 60 * 45,
                unreadCount = 0,
                isPinned = false,
                membersCount = 24,
                description = "Réseau d'affaires pour le négoce, la logistique portuaire et les paiements sécurisés."
            ),
            ChatEntity(
                id = "chat_moussa",
                title = "Moussa Diop (Directeur ADT)",
                isGroup = false,
                avatarUrl = "avatar_1",
                lastMessage = "🎤 Message vocal (0:18)",
                lastTimestamp = now - 1000 * 60 * 20,
                unreadCount = 1,
                isPinned = true,
                membersCount = 2
            ),
            ChatEntity(
                id = "chat_fatou",
                title = "Fatou Ndiaye (Sonatel Dakar)",
                isGroup = false,
                avatarUrl = "avatar_4",
                lastMessage = "Nous avons validé les tarifs préférentiels data pour l'ensemble des flottes ADT Pro.",
                lastTimestamp = now - 1000 * 60 * 65,
                unreadCount = 1,
                isPinned = false,
                membersCount = 2
            ),
            ChatEntity(
                id = "chat_cheick",
                title = "Dr. Cheick Modibo Diarra",
                isGroup = false,
                avatarUrl = "avatar_5",
                lastMessage = "Voici le plan de déploiement solaire pour les relais isolés. Excellent débit !",
                lastTimestamp = now - 1000 * 60 * 95,
                unreadCount = 0,
                isPinned = false,
                membersCount = 2
            ),
            ChatEntity(
                id = "chat_amina",
                title = "Amina Traoré",
                isGroup = false,
                avatarUrl = "avatar_2",
                lastMessage = "Le contrat Telecel et Moov est validé pour l'équipe !",
                lastTimestamp = now - 1000 * 60 * 120,
                unreadCount = 0,
                isPinned = false,
                membersCount = 2
            ),
            ChatEntity(
                id = "chat_grace",
                title = "Grace Nyambura (Nairobi)",
                isGroup = false,
                avatarUrl = "avatar_8",
                lastMessage = "The API integration between M-Pesa and ADT Pro secure wallet is finalized.",
                lastTimestamp = now - 1000 * 60 * 210,
                unreadCount = 0,
                isPinned = false,
                membersCount = 2
            ),
            ChatEntity(
                id = "chat_carlos",
                title = "Carlos Mendez (Madrid)",
                isGroup = false,
                avatarUrl = "avatar_5",
                lastMessage = "Confirmamos el envío de los equipos de red óptica para Dakar y Abidjan.",
                lastTimestamp = now - 1000 * 60 * 320,
                unreadCount = 0,
                isPinned = false,
                membersCount = 2
            ),
            ChatEntity(
                id = "chat_ibrahim",
                title = "Ibrahim Koné",
                isGroup = false,
                avatarUrl = "avatar_3",
                lastMessage = "Je viens de t'envoyer l'autocollant et les documents.",
                lastTimestamp = now - 1000 * 60 * 360,
                unreadCount = 0,
                isPinned = false,
                membersCount = 2
            ),
            ChatEntity(
                id = "chat_archived_audit",
                title = "📁 Audit Sécurité & Réseaux Télécom 2025",
                isGroup = true,
                avatarUrl = "avatar_6",
                lastMessage = "Rapport annuel de conformité et chiffrement AES-256 validé.",
                lastTimestamp = now - 1000L * 60 * 60 * 24 * 3,
                unreadCount = 0,
                isPinned = false,
                isArchived = true,
                membersCount = 5,
                description = "Archive des audits de sécurité télécom et cryptographie."
            ),
            ChatEntity(
                id = "chat_archived_contrats",
                title = "📑 Accords Cadres & Partenariats Sahel",
                isGroup = false,
                avatarUrl = "avatar_7",
                lastMessage = "Le contrat cadre Orange CI et Moov a été signé par la direction.",
                lastTimestamp = now - 1000L * 60 * 60 * 24 * 12,
                unreadCount = 0,
                isPinned = false,
                isArchived = true,
                membersCount = 2,
                description = "Historique archivé des contrats et accords commerciaux."
            ),
            ChatEntity(
                id = "chat_locked_confidentiel",
                title = "🔒 Direction Générale & Conseils Juridiques",
                isGroup = true,
                avatarUrl = "avatar_8",
                lastMessage = "Maître Sy: Les protocoles d'accord confidentiels ont été chiffrés et scellés.",
                lastTimestamp = now - 1000 * 60 * 30,
                unreadCount = 1,
                isPinned = true,
                isLocked = true,
                membersCount = 4,
                description = "Discussions hautement confidentielles protégées par authentification biométrique."
            ),
            ChatEntity(
                id = "chat_locked_finances",
                title = "🔒 Trésorerie Pro & Comptes Sécurisés",
                isGroup = false,
                avatarUrl = "avatar_2",
                lastMessage = "Virement interbancaire SWIFT confirmé avec signature cryptographique.",
                lastTimestamp = now - 1000 * 60 * 95,
                unreadCount = 0,
                isPinned = false,
                isLocked = true,
                membersCount = 2,
                description = "Canal financier privé verrouillé par biométrie."
            )
        )
    }

    fun getEnrichedGroupMembers(): List<GroupMemberEntity> {
        return listOf(
            GroupMemberEntity("chat_group_adt_pro_+221770001122", "chat_group_adt_pro", "+221 77 000 11 22", "Moussa Diop (Directeur ADT)", "avatar_1", isAdmin = true, role = "ADMIN", bio = "Consultant Télécom & Réseaux Pro"),
            GroupMemberEntity("chat_group_adt_pro_+225078899001", "chat_group_adt_pro", "+225 07 88 99 00 11", "Amina Traoré", "avatar_2", isAdmin = true, role = "ADMIN", bio = "Responsable Ventes Orange & Moov"),
            GroupMemberEntity("chat_group_adt_pro_+223661122334", "chat_group_adt_pro", "+223 66 11 22 33", "Ibrahim Koné", "avatar_3", isAdmin = false, role = "MEMBER", bio = "Développeur Solutions Telecel"),
            GroupMemberEntity("chat_group_adt_pro_+221778901234", "chat_group_adt_pro", "+221 77 890 12 34", "Fatou Ndiaye (Dakar)", "avatar_4", isAdmin = false, role = "MEMBER", bio = "Directrice Partenariats Sonatel"),
            GroupMemberEntity("chat_group_adt_pro_+33612345678", "chat_group_adt_pro", "+33 6 12 34 56 78", "Sophie Laurent (Paris)", "avatar_6", isAdmin = false, role = "MEMBER", bio = "Partenariats Internationaux ADT"),
            GroupMemberEntity("chat_group_adt_pro_+22670112233", "chat_group_adt_pro", "+226 70 11 22 33", "Oumar Ouédraogo", "avatar_7", isAdmin = false, role = "MEMBER", bio = "Logistique & E-commerce Sahel"),
            GroupMemberEntity("chat_group_adt_pro_+234803112233", "chat_group_adt_pro", "+234 803 112 2334", "Chinedu Okafor", "avatar_8", isAdmin = false, role = "MEMBER", bio = "Fintech & Payments Lead"),
            GroupMemberEntity("chat_group_adt_pro_+34612987654", "chat_group_adt_pro", "+34 612 987 654", "Carlos Mendez (Madrid)", "avatar_5", isAdmin = false, role = "MEMBER", bio = "Relations Fournisseurs Europe"),

            GroupMemberEntity("chat_group_innovation_+22376543210", "chat_group_innovation", "+223 76 54 32 10", "Dr. Cheick Modibo Diarra", "avatar_5", isAdmin = true, role = "ADMIN", bio = "Expert Énergie Solaire & Télécoms"),
            GroupMemberEntity("chat_group_innovation_+221770001122", "chat_group_innovation", "+221 77 000 11 22", "Moussa Diop", "avatar_1", isAdmin = true, role = "ADMIN", bio = "Direction ADT Pro"),
            GroupMemberEntity("chat_group_innovation_+254722998877", "chat_group_innovation", "+254 722 998 877", "Grace Nyambura", "avatar_8", isAdmin = false, role = "MEMBER", bio = "Directrice Mobile Money"),
            GroupMemberEntity("chat_group_innovation_+237677445566", "chat_group_innovation", "+237 677 44 55 66", "Jean-Paul Mbarga", "avatar_5", isAdmin = false, role = "MEMBER", bio = "Déploiement Fibre & Datacenters")
        )
    }

    fun getEnrichedMessages(): List<MessageEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            MessageEntity(
                id = "m_grp_1",
                chatId = "chat_group_adt_pro",
                senderPhone = "+225078899001",
                senderName = "Amina Traoré",
                text = "Bonjour à tous les collègues et partenaires de l'écosystème ADT Pro !",
                timestamp = now - 1000 * 60 * 50,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_grp_2",
                chatId = "chat_group_adt_pro",
                senderPhone = "+221778901234",
                senderName = "Fatou Ndiaye (Dakar)",
                text = "Les accords d'interconnexion 4G/5G avec la Sonatel sont officiellement opérationnels.",
                timestamp = now - 1000 * 60 * 25,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_grp_3",
                chatId = "chat_group_adt_pro",
                senderPhone = "+221770001122",
                senderName = "Moussa Diop",
                text = "Nouvelle mise à jour des liaisons fibre Orange et Moov Africa.",
                timestamp = now - 1000 * 60 * 3,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_inno_1",
                chatId = "chat_group_innovation",
                senderPhone = "+221770001122",
                senderName = "Moussa Diop",
                text = "Bienvenue dans le Hub Innovation & IA. L'objectif est d'accélérer les services pro décentralisés.",
                timestamp = now - 1000 * 60 * 60,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_inno_2",
                chatId = "chat_group_innovation",
                senderPhone = "+22376543210",
                senderName = "Dr. Cheick Modibo Diarra",
                text = "Le nouveau système de routage intelligent et IA locale est en phase de test.",
                timestamp = now - 1000 * 60 * 15,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_moussa_1",
                chatId = "chat_moussa",
                senderPhone = "+221770001122",
                senderName = "Moussa Diop",
                text = "Salut ! Bienvenue sur ADT Pro. Voici le message vocal de cadrage du projet.",
                timestamp = now - 1000 * 60 * 25,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_moussa_2",
                chatId = "chat_moussa",
                senderPhone = "+221770001122",
                senderName = "Moussa Diop",
                text = "Note vocale de briefing de sécurité",
                timestamp = now - 1000 * 60 * 20,
                mediaType = "AUDIO",
                mediaDuration = 18,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_moussa_3",
                chatId = "chat_moussa",
                senderPhone = "+221770001122",
                senderName = "Moussa Diop",
                text = "Photos des nouvelles antennes 5G Sonatel installées ce matin sur le site pilote.",
                timestamp = now - 1000 * 60 * 8,
                mediaType = "PHOTO",
                mediaUrl = "antennes_relais_5g_dakar.jpg",
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_fatou_1",
                chatId = "chat_fatou",
                senderPhone = "+221778901234",
                senderName = "Fatou Ndiaye",
                text = "Bonjour ! Nous avons finalisé l'examen du cahier des charges réseau.",
                timestamp = now - 1000 * 60 * 90,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_fatou_2",
                chatId = "chat_fatou",
                senderPhone = "+221778901234",
                senderName = "Fatou Ndiaye",
                text = "Nous avons validé les tarifs préférentiels data pour l'ensemble des flottes ADT Pro.",
                timestamp = now - 1000 * 60 * 65,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_fatou_3",
                chatId = "chat_fatou",
                senderPhone = "+221778901234",
                senderName = "Fatou Ndiaye",
                text = "Vidéo du test de bascule d'alimentation du datacenter vers l'énergie solaire.",
                timestamp = now - 1000 * 60 * 12,
                mediaType = "VIDEO",
                mediaUrl = "datacenter_switch_solar.mp4",
                mediaDuration = 36,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_cheick_1",
                chatId = "chat_cheick",
                senderPhone = "+22376543210",
                senderName = "Dr. Cheick Modibo Diarra",
                text = "Voici le plan de déploiement solaire pour les relais isolés. Excellent débit !",
                timestamp = now - 1000 * 60 * 95,
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_cheick_2",
                chatId = "chat_cheick",
                senderPhone = "+22376543210",
                senderName = "Dr. Cheick Modibo Diarra",
                text = "Spécifications techniques du protocole WebRTC basse consommation.",
                timestamp = now - 1000 * 60 * 5,
                mediaType = "DOCUMENT",
                mediaUrl = "specs_low_power_webrtc_adt.pdf",
                isOutgoing = false,
                status = "READ"
            ),
            MessageEntity(
                id = "m_amina_1",
                chatId = "chat_amina",
                senderPhone = "+225078899001",
                senderName = "Amina Traoré",
                text = "Le contrat Telecel et Moov est validé pour l'équipe !",
                timestamp = now - 1000 * 60 * 120,
                isOutgoing = false,
                status = "READ"
            )
        )
    }

    fun getEnrichedChannels(): List<ChannelEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ChannelEntity(
                id = "chan_adt_official",
                name = "📢 ADT Pro Officiel",
                description = "Chaîne certifiée d'actualités technologiques, télécoms et affaires professionnelles.",
                avatarUrl = "chan_adt",
                category = "Actualités & Business",
                followersCount = 19850,
                isVerified = true,
                isFollowing = true,
                adminName = "Direction ADT Pro",
                adminPhone = "+221770001122",
                isOwner = false,
                latestPostText = "🔐 Le chiffrement de bout en bout de nouvelle génération est désormais actif sur toutes les chaînes et groupes ADT Pro.",
                latestPostTimestamp = now - 1000 * 60 * 20
            ),
            ChannelEntity(
                id = "chan_operators_africa",
                name = "⚡ Télécoms Afrique (Orange, Moov, Telecel)",
                description = "Toutes les nouveautés forfaits, déploiement 4G/5G et codes de recharge rapide.",
                avatarUrl = "chan_telecom",
                category = "Opérateurs & Réseaux",
                followersCount = 11400,
                isVerified = true,
                isFollowing = true,
                adminName = "Cellule Télécoms",
                adminPhone = "+225078899001",
                isOwner = false,
                latestPostText = "Offres spéciales data pour les professionnels inscrits via Moov Africa et Telecel.",
                latestPostTimestamp = now - 1000 * 60 * 90
            ),
            ChannelEntity(
                id = "chan_opportunities",
                name = "💼 Opportunités Pro & Recrutement",
                description = "Offres d'emploi, consultations et appels d'offres en Afrique de l'Ouest, Centrale et Europe.",
                avatarUrl = "chan_jobs",
                category = "Emploi & Carrière",
                followersCount = 32800,
                isVerified = true,
                isFollowing = false,
                adminName = "Pôle Recrutement Sahel & Diaspora",
                adminPhone = "+33612345678",
                isOwner = false,
                latestPostText = "35 nouveaux postes d'ingénieurs et commerciaux ouverts à Dakar, Abidjan, Bamako et Paris.",
                latestPostTimestamp = now - 1000 * 60 * 180
            ),
            ChannelEntity(
                id = "chan_tech_ai",
                name = "🤖 IA & Technologies Émergentes",
                description = "Veille technologique sur l'IA, la cybersécurité et les infrastructures numériques souveraines.",
                avatarUrl = "chan_ai",
                category = "Technologie & IA",
                followersCount = 15200,
                isVerified = true,
                isFollowing = true,
                adminName = "Lab Innovation ADT",
                adminPhone = "+221770001122",
                isOwner = false,
                latestPostText = "Intégration des modèles d'IA légers sur appareil pour la transcription vocale instantanée en langues locales.",
                latestPostTimestamp = now - 1000 * 60 * 45
            ),
            ChannelEntity(
                id = "chan_fintech",
                name = "💳 FinTech & Paiements Mobiles Afrique",
                description = "Interconnexion Wave, Orange Money, Moov Money, M-Pesa et virements bancaires instantanés.",
                avatarUrl = "chan_fintech",
                category = "Finance & Paiement",
                followersCount = 24100,
                isVerified = true,
                isFollowing = false,
                adminName = "Fintech Alliance",
                adminPhone = "+234803112233",
                isOwner = false,
                latestPostText = "Nouveau protocole d'authentification à deux facteurs pour sécuriser les transactions professionnelles B2B.",
                latestPostTimestamp = now - 1000 * 60 * 240
            )
        )
    }

    fun getEnrichedChannelPosts(): List<ChannelPostEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            ChannelPostEntity(
                id = "post_adt_1",
                channelId = "chan_adt_official",
                authorName = "Direction Technique ADT",
                authorRole = "Administrateur Certifié",
                text = "🔐 COMMUNIQUÉ OFFICIEL : Le chiffrement post-quantique de bout en bout et les protocoles de synchronisation instantanée sont maintenant actifs pour toutes les communications, appels et diffusions sur le réseau ADT Pro.",
                mediaType = "ANNOUNCEMENT",
                timestamp = now - 1000 * 60 * 30,
                viewsCount = 16420,
                reactionsHeart = 420,
                reactionsLike = 680,
                reactionsRocket = 950,
                reactionsFire = 530,
                reactionsClap = 380,
                isPinned = true
            ),
            ChannelPostEntity(
                id = "post_adt_2",
                channelId = "chan_adt_official",
                authorName = "Pôle Produit & Innovation",
                authorRole = "Administrateur",
                text = "📊 CONSULTATION DES ABONNÉS : Quelle fonctionnalité professionnelle préférez-vous voir priorisée pour la prochaine mise à jour de la suite ADT Pro ?",
                mediaType = "POLL",
                timestamp = now - 1000 * 60 * 120,
                viewsCount = 11200,
                reactionsHeart = 210,
                reactionsLike = 340,
                reactionsRocket = 410,
                pollQuestion = "Quelle fonctionnalité prioriser ?",
                pollOption1 = "Signature électronique certifiée de documents",
                pollOption2 = "Visioconférence sécurisée jusqu'à 150 personnes",
                pollOption3 = "Passerelle de paiement Orange / Moov / Wave",
                pollOption4 = "Espace Cloud d'entreprise ultra-chiffré",
                pollVotes1 = 492,
                pollVotes2 = 385,
                pollVotes3 = 780,
                pollVotes4 = 340
            ),
            ChannelPostEntity(
                id = "post_op_1",
                channelId = "chan_operators_africa",
                authorName = "Cellule Réseaux & Data",
                authorRole = "Administrateur",
                text = "⚡ Flash Forfaits : Nouveaux forfaits data 4G/5G entreprise avec tarification groupée chez Orange, Moov Africa et Telecel. Utilisez les codes rapides intégrés pour recharger sans quitter l'application.",
                mediaType = "ANNOUNCEMENT",
                timestamp = now - 1000 * 60 * 90,
                viewsCount = 6800,
                reactionsLike = 290,
                reactionsRocket = 210,
                reactionsFire = 145,
                isPinned = true
            ),
            ChannelPostEntity(
                id = "post_ai_1",
                channelId = "chan_tech_ai",
                authorName = "Lab Innovation ADT",
                authorRole = "Administrateur",
                text = "🤖 IA & MULTILINGUISME : L'application supporte désormais nativement le Français, le Bambara, le Wolof, le Hausa, l'Espagnol, le Portugais, le Chinois, le Russe, l'Anglais, l'Allemand, le Swahili, le Franglais et l'Arabe !",
                mediaType = "ANNOUNCEMENT",
                timestamp = now - 1000 * 60 * 45,
                viewsCount = 9400,
                reactionsLike = 512,
                reactionsHeart = 390,
                reactionsRocket = 680,
                reactionsFire = 410,
                isPinned = true
            ),
            ChannelPostEntity(
                id = "post_jobs_1",
                channelId = "chan_opportunities",
                authorName = "Pôle Recrutement",
                authorRole = "Administrateur",
                text = "💼 RECRUTEMENT : 35 postes ouverts ce mois-ci dans les secteurs Fintech, Télécoms, Cybersécurité et Logistique à Dakar, Abidjan, Bamako, Ouagadougou et Paris.",
                mediaType = "TEXT",
                timestamp = now - 1000 * 60 * 180,
                viewsCount = 21400,
                reactionsLike = 980,
                reactionsHeart = 510,
                reactionsClap = 730,
                isPinned = true
            )
        )
    }

    fun getEnrichedStatuses(): List<StatusEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            StatusEntity(
                id = "stat_1",
                authorName = "Moussa Diop",
                authorPhone = "+221770001122",
                authorAvatar = "avatar_1",
                text = "En réunion stratégique au siège ADT. Partenariat télécom en cours 🤝",
                backgroundColorHex = "#075E54",
                timestamp = now - 1000 * 60 * 60 * 2,
                viewsCount = 38,
                isSelf = false
            ),
            StatusEntity(
                id = "stat_1_b",
                authorName = "Moussa Diop",
                authorPhone = "+221770001122",
                authorAvatar = "avatar_1",
                text = "Signature du protocole d'accord validée avec succès ! 🎉🚀",
                backgroundColorHex = "#1E88E5",
                timestamp = now - 1000 * 60 * 35,
                viewsCount = 29,
                isSelf = false
            ),
            StatusEntity(
                id = "stat_2",
                authorName = "Amina Traoré",
                authorPhone = "+225078899001",
                authorAvatar = "avatar_2",
                text = "Excellente journée de travail à Abidjan avec les équipes Moov Africa 🌟",
                backgroundColorHex = "#128C7E",
                timestamp = now - 1000 * 60 * 60 * 4,
                viewsCount = 54,
                isSelf = false
            ),
            StatusEntity(
                id = "stat_fatou",
                authorName = "Fatou Ndiaye",
                authorPhone = "+221778901234",
                authorAvatar = "avatar_4",
                text = "Inauguration du nouveau datacenter écologique et souverain à Diamniadio 🌿⚡",
                backgroundColorHex = "#2E7D32",
                timestamp = now - 1000 * 60 * 60 * 5,
                viewsCount = 42,
                isSelf = false
            ),
            StatusEntity(
                id = "stat_cheick",
                authorName = "Dr. Cheick Modibo Diarra",
                authorPhone = "+22376543210",
                authorAvatar = "avatar_5",
                text = "Déploiement réussi de 12 nouveaux relais solaires 5G aujourd'hui ! ☀️📡",
                backgroundColorHex = "#E65100",
                timestamp = now - 1000 * 60 * 60 * 7,
                viewsCount = 31,
                isSelf = false
            ),
            StatusEntity(
                id = "stat_3",
                authorName = "Ibrahim Koné",
                authorPhone = "+223661122334",
                authorAvatar = "avatar_3",
                text = "Nouvelle formation sur la sécurité des données et le chiffrement ADT Pro 🔒",
                backgroundColorHex = "#005C4B",
                timestamp = now - 1000 * 60 * 60 * 8,
                viewsCount = 26,
                isSelf = false
            ),
            StatusEntity(
                id = "stat_grace",
                authorName = "Grace Nyambura",
                authorPhone = "+254722998877",
                authorAvatar = "avatar_8",
                text = "Nairobi Tech Summit: financial inclusion reaches new all-time heights 🇰🇪🚀",
                backgroundColorHex = "#6A1B9A",
                timestamp = now - 1000 * 60 * 60 * 10,
                viewsCount = 49,
                isSelf = false
            )
        )
    }

    fun getEnrichedCalls(): List<CallLogEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            CallLogEntity(
                id = "call_1",
                contactName = "Moussa Diop",
                contactPhone = "+221 77 000 11 22",
                contactAvatar = "avatar_1",
                timestamp = now - 1000 * 60 * 60 * 2,
                durationSeconds = 245,
                isVideo = true,
                isOutgoing = false,
                isMissed = false
            ),
            CallLogEntity(
                id = "call_fatou",
                contactName = "Fatou Ndiaye (Dakar)",
                contactPhone = "+221 77 890 12 34",
                contactAvatar = "avatar_4",
                timestamp = now - 1000 * 60 * 60 * 4,
                durationSeconds = 620,
                isVideo = true,
                isOutgoing = true,
                isMissed = false
            ),
            CallLogEntity(
                id = "call_2",
                contactName = "Amina Traoré",
                contactPhone = "+225 07 88 99 00 11",
                contactAvatar = "avatar_2",
                timestamp = now - 1000 * 60 * 60 * 6,
                durationSeconds = 180,
                isVideo = false,
                isOutgoing = true,
                isMissed = false
            ),
            CallLogEntity(
                id = "call_cheick",
                contactName = "Dr. Cheick Modibo Diarra",
                contactPhone = "+223 76 54 32 10",
                contactAvatar = "avatar_5",
                timestamp = now - 1000 * 60 * 60 * 9,
                durationSeconds = 310,
                isVideo = false,
                isOutgoing = false,
                isMissed = false
            ),
            CallLogEntity(
                id = "call_3",
                contactName = "Ibrahim Koné",
                contactPhone = "+223 66 11 22 33",
                contactAvatar = "avatar_3",
                timestamp = now - 1000 * 60 * 60 * 12,
                durationSeconds = 0,
                isVideo = false,
                isOutgoing = false,
                isMissed = true
            ),
            CallLogEntity(
                id = "call_grace",
                contactName = "Grace Nyambura",
                contactPhone = "+254 722 998 877",
                contactAvatar = "avatar_8",
                timestamp = now - 1000 * 60 * 60 * 18,
                durationSeconds = 0,
                isVideo = true,
                isOutgoing = false,
                isMissed = true
            ),
            CallLogEntity(
                id = "call_4",
                contactName = "Sophie Laurent (Paris)",
                contactPhone = "+33 6 12 34 56 78",
                contactAvatar = "avatar_6",
                timestamp = now - 1000 * 60 * 60 * 24,
                durationSeconds = 412,
                isVideo = true,
                isOutgoing = true,
                isMissed = false
            )
        )
    }
}
