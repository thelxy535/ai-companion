package com.companion.cc.domain.capability

enum class Capability {
    IMAGE, VIDEO, VOICE, VISION, TIME, EMAIL
}

data class CapabilityFlags(
    val image: Boolean = false,
    val video: Boolean = false,
    val voice: Boolean = false,
    val vision: Boolean = false,
    val time: Boolean = false,
    val email: Boolean = false
)

object CapabilityScope {
    fun key(userId: String, characterId: String): String =
        "capability:${userId}:${characterId}"
}

object CapabilityGate {
    fun allows(capability: Capability, flags: CapabilityFlags): Boolean = when (capability) {
        Capability.IMAGE -> flags.image
        Capability.VIDEO -> flags.video
        Capability.VOICE -> flags.voice
        Capability.VISION -> flags.vision
        Capability.TIME -> flags.time
        Capability.EMAIL -> flags.email
    }

    fun requireAllowed(capability: Capability, flags: CapabilityFlags) {
        if (!allows(capability, flags)) {
            throw UnsupportedOperationException(
                "Capability $capability is not enabled for this character"
            )
        }
    }
}
