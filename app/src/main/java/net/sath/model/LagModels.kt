package net.sath.model

enum class LagMode(val titleEn: String, val titleVi: String) {
    NORMAL("Normal", "Bình thường"),
    FREEZE("Freeze Lag", "Ngưng kết nối"),
    GHOST("Ghost Delay", "Bóng ma trễ"),
    TELEPORT("Teleport Jitter", "Dịch chuyển giật")
}

data class LagConfig(
    val sizeFreeze: Int = 150,
    val sizeGhost: Int = 90,
    val sizeTele: Int = 90,
    val alphaFreeze: Int = 100,
    val alphaGhost: Int = 100,
    val alphaTele: Int = 100,
    val switchPen: Boolean = false,
    val switchFreeze: Boolean = true,
    val switchGhost: Boolean = true,
    val switchTele: Boolean = true,
    val switchDownloadBoost: Boolean = false,
    val switchReduceFpsDrop: Boolean = true,
    val freezeDropMin: Int = 100,
    val freezeDropMax: Int = 500,
    val ghostDropMin: Int = 80,
    val ghostDropMax: Int = 350,
    val freezeDurationSeconds: Int = 5,
    val isFirewallEnabled: Boolean = true,
    val blockedIps: List<String> = listOf(
        "103.249.28.0/24",
        "157.240.241.35",
        "185.199.108.153"
    ),
    val blockedDomains: List<String> = listOf(
        "telemetry.game-analytics.com",
        "adservice.google.com",
        "crashlyticsreports-pa.googleapis.com"
    ),
    val language: String = "vi" // "vi" or "en"
)

data class DiagnosticsState(
    val currentPingMs: Int = 38,
    val jitterMs: Int = 4,
    val packetLossPercent: Float = 0.0f,
    val packetsSent: Long = 1420,
    val packetsDelayed: Long = 0,
    val packetsBlocked: Long = 0,
    val isVpnActive: Boolean = false,
    val isOverlayActive: Boolean = false,
    val currentMode: LagMode = LagMode.NORMAL
)

data class ServerPingTarget(
    val name: String,
    val region: String,
    val host: String,
    val pingMs: Int = -1,
    val isTesting: Boolean = false
)
