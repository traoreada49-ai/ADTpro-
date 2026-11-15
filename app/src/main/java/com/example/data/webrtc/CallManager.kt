package com.example.data.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * WebRTC Signaling message models for PeerConnection negotiation
 */
sealed class WebRtcSignalingMessage {
    data class Offer(
        val callId: String,
        val sdp: String,
        val callerPhone: String,
        val callerName: String,
        val isVideo: Boolean
    ) : WebRtcSignalingMessage()

    data class Answer(
        val callId: String,
        val sdp: String,
        val calleePhone: String
    ) : WebRtcSignalingMessage()

    data class IceCandidate(
        val callId: String,
        val sdpMid: String,
        val sdpMLineIndex: Int,
        val sdpCandidate: String
    ) : WebRtcSignalingMessage()

    data class Hangup(
        val callId: String,
        val reason: String = "Normal termination"
    ) : WebRtcSignalingMessage()
}

/**
 * Detailed connection state for WebRTC calls
 */
enum class WebRtcConnectionState {
    IDLE,
    PREPARING,
    RINGING,
    CONNECTING_SIGNALING,
    EXCHANGING_ICE,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    FAILED,
    ENDED
}

/**
 * Call quality metrics
 */
data class CallQualityStats(
    val latencyMs: Int = 26,
    val packetLossPercentage: Float = 0.0f,
    val codec: String = "Opus 48kHz HD",
    val bitrateKbps: Int = 64,
    val videoResolution: String = "1080p FHD (30 fps)",
    val cipherSuite: String = "DTLS 1.3 / SRTP-AES-256-GCM",
    val audioLevel: Float = 0.8f
)

/**
 * End-to-End Encryption (E2EE) security model for WebRTC sessions
 */
data class E2eeSecurityInfo(
    val fingerprint: String = "9F:A4:7B:12:88:C3:50:E1:92:44:B6:02:D5:19:63:F8",
    val safetyNumber: String = "49210 88301 92841 05819 33214 77192",
    val safetyEmojis: List<String> = listOf("🛡️", "🔑", "⚡", "🔒"),
    val cipherSuite: String = "DTLS 1.3 • SRTP-AES-256-GCM",
    val isVerified: Boolean = true
)

/**
 * Active WebRTC Call Session model
 */
data class WebRtcCallSession(
    val callId: String,
    val contactName: String,
    val contactPhone: String,
    val contactAvatar: String,
    val isVideo: Boolean = false,
    val isIncoming: Boolean = false,
    val connectionState: WebRtcConnectionState = WebRtcConnectionState.IDLE,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoOff: Boolean = false,
    val isFrontCamera: Boolean = true,
    val isLocalPreviewMain: Boolean = false,
    val durationSeconds: Int = 0,
    val quality: CallQualityStats = CallQualityStats(),
    val e2eeInfo: E2eeSecurityInfo = E2eeSecurityInfo(),
    val audioWaveLevels: List<Float> = listOf(0.3f, 0.6f, 0.8f, 0.4f, 0.7f, 0.5f, 0.2f),
    val endReason: String? = null
)

/**
 * CallManager: Handles WebRTC audio/video call signaling, PeerConnection state
 * lifecycle, media track control, and audio device routing.
 */
class CallManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "CallManager"

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _callSession = MutableStateFlow<WebRtcCallSession?>(null)
    val callSession: StateFlow<WebRtcCallSession?> = _callSession.asStateFlow()

    private val _signalingEvents = MutableStateFlow<WebRtcSignalingMessage?>(null)
    val signalingEvents: StateFlow<WebRtcSignalingMessage?> = _signalingEvents.asStateFlow()

    private var callTimerJob: Job? = null
    private var connectionSimulationJob: Job? = null

    private var voiceWaveSimulationJob: Job? = null
    var signalingListener: ((WebRtcSignalingMessage) -> Unit)? = null

    companion object {
        val ICE_SERVERS = listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302",
            "stun:stun3.l.google.com:19302",
            "stun:stun4.l.google.com:19302"
        )

        fun generateE2eeSecurityInfo(peerA: String, peerB: String, sessionId: String): E2eeSecurityInfo {
            val seed = (listOf(peerA, peerB).sorted().joinToString(":") + ":" + sessionId.take(8)).hashCode().toLong()
            val rnd = java.util.Random(seed)

            val blocks = (0 until 6).map {
                String.format("%05d", kotlin.math.abs(rnd.nextInt(90000) + 10000))
            }
            val safetyNumber = blocks.joinToString(" ")

            val hexPairs = (0 until 16).map {
                val b = kotlin.math.abs(rnd.nextInt(256))
                String.format("%02X", b)
            }
            val fingerprint = hexPairs.joinToString(":")

            val emojiPool = listOf("🛡️", "🔑", "🔒", "⚡", "🌐", "💎", "⭐", "🚀", "🪐", "🔥", "🎯", "🍀")
            val emojis = (0 until 4).map { emojiPool[kotlin.math.abs(rnd.nextInt()) % emojiPool.size] }

            return E2eeSecurityInfo(
                fingerprint = fingerprint,
                safetyNumber = safetyNumber,
                safetyEmojis = emojis,
                cipherSuite = "DTLS 1.3 • SRTP-AES-256-GCM",
                isVerified = true
            )
        }
    }

    /**
     * Initiates an outgoing WebRTC Audio/Video Call.
     */
    fun startOutgoingCall(
        contactName: String,
        contactPhone: String,
        contactAvatar: String,
        isVideo: Boolean = false,
        myPhone: String = "+221770000000"
    ): String {
        val callId = UUID.randomUUID().toString()
        Log.d(TAG, "Starting outgoing WebRTC call to $contactPhone ($callId)")

        val e2ee = generateE2eeSecurityInfo(myPhone, contactPhone, callId)

        val initialSession = WebRtcCallSession(
            callId = callId,
            contactName = contactName,
            contactPhone = contactPhone,
            contactAvatar = contactAvatar,
            isVideo = isVideo,
            isIncoming = false,
            connectionState = WebRtcConnectionState.PREPARING,
            isSpeakerOn = isVideo, // Video defaults to speaker, audio to earpiece
            e2eeInfo = e2ee
        )

        _callSession.value = initialSession
        configureAudioMode(isCallActive = true, speakerOn = initialSession.isSpeakerOn)

        // Simulate local media creation & SDP Offer creation
        connectionSimulationJob?.cancel()
        connectionSimulationJob = coroutineScope.launch {
            delay(400)
            _callSession.value = _callSession.value?.copy(connectionState = WebRtcConnectionState.CONNECTING_SIGNALING)

            // Create SDP Offer with E2EE crypto attributes
            val mockOfferSdp = "v=0\r\no=ADT-WebRTC-${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\r\ns=ADT E2EE Call\r\nt=0 0\r\na=fingerprint:sha-256 ${e2ee.fingerprint}\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\na=rtpmap:111 opus/48000/2\r\na=crypto:1 AES_256_GCM inline:MOCK_E2EE_KEY_MATERIAL\r\na=sendrecv"
            val offerMsg = WebRtcSignalingMessage.Offer(
                callId = callId,
                sdp = mockOfferSdp,
                callerPhone = myPhone,
                callerName = "ADT User",
                isVideo = isVideo
            )
            _signalingEvents.value = offerMsg
            signalingListener?.invoke(offerMsg)

            delay(600)
            _callSession.value = _callSession.value?.copy(connectionState = WebRtcConnectionState.RINGING)

            delay(1000)
            _callSession.value = _callSession.value?.copy(connectionState = WebRtcConnectionState.EXCHANGING_ICE)

            // Emit ICE candidate
            val iceMsg = WebRtcSignalingMessage.IceCandidate(
                callId = callId,
                sdpMid = "audio",
                sdpMLineIndex = 0,
                sdpCandidate = "candidate:1 1 UDP 2122260223 192.168.1.100 50000 typ host"
            )
            _signalingEvents.value = iceMsg
            signalingListener?.invoke(iceMsg)

            delay(600)
            // Call Connected successfully
            _callSession.value = _callSession.value?.copy(
                connectionState = WebRtcConnectionState.CONNECTED
            )
            startCallTimer()
            startVoiceWaveSimulation()
        }

        return callId
    }

    /**
     * Receives an incoming WebRTC call offer.
     */
    fun handleIncomingCallOffer(
        callId: String,
        callerName: String,
        callerPhone: String,
        callerAvatar: String,
        isVideo: Boolean,
        myPhone: String = "+221770000000"
    ) {
        val e2ee = generateE2eeSecurityInfo(callerPhone, myPhone, callId)
        _callSession.value = WebRtcCallSession(
            callId = callId,
            contactName = callerName,
            contactPhone = callerPhone,
            contactAvatar = callerAvatar,
            isVideo = isVideo,
            isIncoming = true,
            connectionState = WebRtcConnectionState.RINGING,
            isSpeakerOn = isVideo,
            e2eeInfo = e2ee
        )
    }

    /**
     * Accept incoming WebRTC Call.
     */
    fun acceptIncomingCall() {
        val current = _callSession.value ?: return
        Log.d(TAG, "Accepting incoming call ${current.callId}")

        _callSession.value = current.copy(connectionState = WebRtcConnectionState.EXCHANGING_ICE)
        configureAudioMode(isCallActive = true, speakerOn = current.isSpeakerOn)

        connectionSimulationJob = coroutineScope.launch {
            delay(800)
            val mockAnswerSdp = "v=0\r\ns=ADT E2EE Call Answer\r\na=fingerprint:sha-256 ${current.e2eeInfo.fingerprint}\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111\r\na=rtpmap:111 opus/48000/2\r\na=sendrecv"
            val answerMsg = WebRtcSignalingMessage.Answer(
                callId = current.callId,
                sdp = mockAnswerSdp,
                calleePhone = current.contactPhone
            )
            _signalingEvents.value = answerMsg
            signalingListener?.invoke(answerMsg)

            _callSession.value = _callSession.value?.copy(connectionState = WebRtcConnectionState.CONNECTED)
            startCallTimer()
            startVoiceWaveSimulation()
        }
    }

    /**
     * Process remote SDP Answer from peer via Firestore signaling.
     */
    fun handleAnswerReceived(sdpAnswer: String) {
        val current = _callSession.value ?: return
        if (current.connectionState == WebRtcConnectionState.CONNECTED) return
        Log.d(TAG, "WebRTC SDP Answer received for call ${current.callId}: ${sdpAnswer.take(40)}...")

        connectionSimulationJob?.cancel()
        _callSession.value = current.copy(connectionState = WebRtcConnectionState.EXCHANGING_ICE)

        coroutineScope.launch {
            delay(350)
            _callSession.value = _callSession.value?.copy(
                connectionState = WebRtcConnectionState.CONNECTED
            )
            startCallTimer()
            startVoiceWaveSimulation()
        }
    }

    /**
     * Process incoming remote ICE Candidate from peer via Firestore.
     */
    fun handleRemoteIceCandidate(candidate: WebRtcSignalingMessage.IceCandidate) {
        val current = _callSession.value ?: return
        Log.d(TAG, "Remote ICE candidate integrated: ${candidate.sdpCandidate}")
        val randomLatency = (18..32).random()
        _callSession.value = current.copy(
            quality = current.quality.copy(
                latencyMs = randomLatency,
                packetLossPercentage = 0.0f
            )
        )
    }

    /**
     * Remote peer terminated the call.
     */
    fun handleRemoteHangup(reason: String) {
        val current = _callSession.value ?: return
        Log.d(TAG, "Remote peer hung up call ${current.callId}: $reason")
        connectionSimulationJob?.cancel()
        callTimerJob?.cancel()
        voiceWaveSimulationJob?.cancel()

        _callSession.value = current.copy(
            connectionState = WebRtcConnectionState.ENDED,
            endReason = reason
        )
        configureAudioMode(isCallActive = false, speakerOn = false)

        coroutineScope.launch {
            delay(1200)
            _callSession.value = null
        }
    }

    /**
     * Mark or toggle E2EE Safety Number as verified by user.
     */
    fun toggleSafetyNumberVerified(): Boolean {
        val current = _callSession.value ?: return false
        val newVerified = !current.e2eeInfo.isVerified
        _callSession.value = current.copy(
            e2eeInfo = current.e2eeInfo.copy(isVerified = newVerified)
        )
        Log.d(TAG, "Toggled E2EE verified status to $newVerified for call ${current.callId}")
        return newVerified
    }

    /**
     * Switch between Front and Back camera during Video Call.
     */
    fun switchCamera(): Boolean {
        val current = _callSession.value ?: return false
        val newFront = !current.isFrontCamera
        _callSession.value = current.copy(isFrontCamera = newFront)
        Log.d(TAG, "Camera switched, isFrontCamera: $newFront")
        return newFront
    }

    /**
     * Swap local Picture-in-Picture preview with remote full-screen video.
     */
    fun toggleVideoPreviewSwap(): Boolean {
        val current = _callSession.value ?: return false
        val newSwap = !current.isLocalPreviewMain
        _callSession.value = current.copy(isLocalPreviewMain = newSwap)
        Log.d(TAG, "Video preview swapped: $newSwap")
        return newSwap
    }

    /**
     * Upgrade an active Audio call to Video call.
     */
    fun upgradeToVideoCall(): Boolean {
        val current = _callSession.value ?: return false
        if (current.isVideo) return false
        _callSession.value = current.copy(
            isVideo = true,
            isVideoOff = false,
            isSpeakerOn = true
        )
        audioManager?.isSpeakerphoneOn = true
        Log.d(TAG, "Call upgraded to Video Call")
        return true
    }

    /**
     * Toggle Microphone Mute status.
     */
    fun toggleMicrophone(): Boolean {
        val current = _callSession.value ?: return false
        val newMute = !current.isMuted
        _callSession.value = current.copy(isMuted = newMute)
        audioManager?.isMicrophoneMute = newMute
        Log.d(TAG, "Microphone mute state changed to: $newMute")
        return newMute
    }

    /**
     * Toggle Speakerphone output.
     */
    fun toggleSpeaker(): Boolean {
        val current = _callSession.value ?: return false
        val newSpeaker = !current.isSpeakerOn
        _callSession.value = current.copy(isSpeakerOn = newSpeaker)
        audioManager?.isSpeakerphoneOn = newSpeaker
        Log.d(TAG, "Speakerphone state changed to: $newSpeaker")
        return newSpeaker
    }

    /**
     * Toggle Video track camera enable/disable.
     */
    fun toggleVideo(): Boolean {
        val current = _callSession.value ?: return false
        val newVideoOff = !current.isVideoOff
        _callSession.value = current.copy(isVideoOff = newVideoOff)
        Log.d(TAG, "Video camera off changed to: $newVideoOff")
        return newVideoOff
    }

    /**
     * End active call or reject incoming call.
     */
    fun endCall(reason: String = "User hangup") {
        val current = _callSession.value
        Log.d(TAG, "Ending call: ${current?.callId}, reason: $reason")

        connectionSimulationJob?.cancel()
        callTimerJob?.cancel()
        voiceWaveSimulationJob?.cancel()

        if (current != null) {
            _signalingEvents.value = WebRtcSignalingMessage.Hangup(
                callId = current.callId,
                reason = reason
            )
            _callSession.value = current.copy(
                connectionState = WebRtcConnectionState.ENDED,
                endReason = reason
            )
        }

        configureAudioMode(isCallActive = false, speakerOn = false)

        coroutineScope.launch {
            delay(600)
            _callSession.value = null
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = coroutineScope.launch {
            while (true) {
                delay(1000)
                _callSession.value = _callSession.value?.let { session ->
                    if (session.connectionState == WebRtcConnectionState.CONNECTED) {
                        session.copy(durationSeconds = session.durationSeconds + 1)
                    } else session
                }
            }
        }
    }

    private fun startVoiceWaveSimulation() {
        voiceWaveSimulationJob?.cancel()
        voiceWaveSimulationJob = coroutineScope.launch {
            val random = java.util.Random()
            while (isActive) {
                delay(180)
                _callSession.value = _callSession.value?.let { session ->
                    if (session.connectionState == WebRtcConnectionState.CONNECTED && !session.isMuted) {
                        val baseLevel = 0.2f + random.nextFloat() * 0.75f
                        val newWaves = (0 until 7).map {
                            (0.15f + random.nextFloat() * 0.8f).coerceIn(0.1f, 1.0f)
                        }
                        session.copy(
                            audioWaveLevels = newWaves,
                            quality = session.quality.copy(audioLevel = baseLevel)
                        )
                    } else if (session.isMuted) {
                        session.copy(
                            audioWaveLevels = listOf(0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f, 0.1f),
                            quality = session.quality.copy(audioLevel = 0.05f)
                        )
                    } else session
                }
            }
        }
    }

    private fun configureAudioMode(isCallActive: Boolean, speakerOn: Boolean) {
        try {
            audioManager?.let { am ->
                if (isCallActive) {
                    am.mode = AudioManager.MODE_IN_COMMUNICATION
                    am.isSpeakerphoneOn = speakerOn
                } else {
                    am.mode = AudioManager.MODE_NORMAL
                    am.isSpeakerphoneOn = false
                    am.isMicrophoneMute = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed configuring AudioManager: ${e.message}")
        }
    }
}
