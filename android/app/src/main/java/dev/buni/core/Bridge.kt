package dev.buni.core

object Bridge {

    init {
        try {
            System.loadLibrary("bunipy")
        } catch (ignored: UnsatisfiedLinkError) {
        }
    }

    external fun startShield(fd: Int, blocklistData: String, dohEndpoint: String): String
    external fun stopShield()
    external fun getStats(): String
    external fun pollLog(): String

    fun mockStartShield(fd: Int): String {
        return ""
    }

    fun mockStopShield() {}

    fun mockGetStats(): Triple<Long, Long, Long> {
        return Triple(142L, 38L, 6L)
    }

    fun mockPollLog(): String {
        return ""
    }
}
