package net.sath.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.sath.MainActivity
import net.sath.data.PreferencesManager
import net.sath.model.LagMode
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class GameBoosterVpnService : VpnService() {

    companion object {
        const val CHANNEL_ID = "vpn_game_booster_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START = "net.sath.md.s1"
        const val ACTION_STOP = "net.sath.md.sx"
        const val ACTION_FREEZE = "net.sath.md.f1"
        const val ACTION_GHOST = "net.sath.md.g1"
        const val ACTION_TELEPORT = "net.sath.md.tp"
        const val ACTION_STATUS = "net.sath.md.st"
        const val ACTION_UPDATE_CONFIG = "net.sath.md.CFG"

        const val BROADCAST_STATS = "net.sath.action.VPN_STATS"
        const val EXTRA_IS_RUNNING = "is_running"
        const val EXTRA_CURRENT_MODE = "current_mode"
        const val EXTRA_PACKETS_SENT = "packets_sent"
        const val EXTRA_PACKETS_DELAYED = "packets_delayed"
        const val EXTRA_PACKETS_BLOCKED = "packets_blocked"
        const val EXTRA_PING = "current_ping"

        @Volatile
        var isServiceRunning = false
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var vpnJob: Job? = null
    private var metricsJob: Job? = null

    private lateinit var prefs: PreferencesManager

    private val isFreezing = AtomicBoolean(false)
    private val isGhosting = AtomicBoolean(false)
    private val isTeleporting = AtomicBoolean(false)

    private val packetsSent = AtomicLong(0)
    private val packetsDelayed = AtomicLong(0)
    private val packetsBlocked = AtomicLong(0)
    private var currentPingMs = 38

    private data class DelayedPacket(
        val data: ByteArray,
        val deliverAt: Long
    )

    private val delayedQueue = ConcurrentLinkedQueue<DelayedPacket>()

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startVpn()
            }
            ACTION_STOP -> {
                stopVpn()
            }
            ACTION_FREEZE -> {
                val enable = intent.getBooleanExtra("enabled", !isFreezing.get())
                setFreezeMode(enable)
            }
            ACTION_GHOST -> {
                val enable = intent.getBooleanExtra("enabled", !isGhosting.get())
                setGhostMode(enable)
            }
            ACTION_TELEPORT -> {
                val enable = intent.getBooleanExtra("enabled", !isTeleporting.get())
                setTeleportMode(enable)
            }
            ACTION_STATUS -> {
                broadcastStatus()
            }
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (isServiceRunning) return

        val notification = createNotification("Game Booster Active - Latency Optimizer Running")
        startForeground(NOTIFICATION_ID, notification)

        try {
            val builder = Builder()
                .setSession("FakePingZzz Booster")
                .addAddress("10.8.0.2", 24)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")
                .setMtu(1400)
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
            isServiceRunning = true
            broadcastStatus()

            startPacketEngine()
            startMetricsLoop()
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun startPacketEngine() {
        vpnJob = scope.launch {
            val pfd = vpnInterface ?: return@launch
            val inStream = FileInputStream(pfd.fileDescriptor)
            val outStream = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32768)

            while (isActive && isServiceRunning) {
                try {
                    // Check delayed queue for delivery
                    val now = System.currentTimeMillis()
                    val it = delayedQueue.iterator()
                    while (it.hasNext()) {
                        val pkt = it.next()
                        if (now >= pkt.deliverAt) {
                            it.remove()
                            outStream.write(pkt.data)
                        }
                    }

                    val length = inStream.read(buffer.array())
                    if (length > 0) {
                        val config = prefs.loadConfig()
                        val packetData = buffer.array().copyOf(length)

                        packetsSent.incrementAndGet()

                        if (isFreezing.get()) {
                            // Freeze mode: drop or delay packets heavily
                            packetsDelayed.incrementAndGet()
                            val delayMs = Random.nextLong(
                                config.freezeDropMin.toLong(),
                                config.freezeDropMax.toLong().coerceAtLeast(config.freezeDropMin.toLong() + 10)
                            )
                            delayedQueue.add(DelayedPacket(packetData, now + delayMs))
                        } else if (isGhosting.get()) {
                            // Ghost mode: inject subtle jitter delay
                            packetsDelayed.incrementAndGet()
                            val delayMs = Random.nextLong(
                                config.ghostDropMin.toLong(),
                                config.ghostDropMax.toLong().coerceAtLeast(config.ghostDropMin.toLong() + 10)
                            )
                            delayedQueue.add(DelayedPacket(packetData, now + delayMs))
                        } else if (isTeleporting.get()) {
                            // Teleport mode: burst delay alternation
                            if (Random.nextInt(100) < 40) {
                                packetsDelayed.incrementAndGet()
                                delayedQueue.add(DelayedPacket(packetData, now + Random.nextLong(300, 800)))
                            } else {
                                outStream.write(packetData)
                            }
                        } else {
                            outStream.write(packetData)
                        }
                    } else {
                        delay(5)
                    }
                } catch (e: Exception) {
                    if (isActive) delay(10)
                }
            }
        }
    }

    private fun startMetricsLoop() {
        metricsJob = scope.launch {
            while (isActive && isServiceRunning) {
                delay(1000)
                currentPingMs = when {
                    isFreezing.get() -> 999
                    isGhosting.get() -> Random.nextInt(180, 420)
                    isTeleporting.get() -> Random.nextInt(90, 750)
                    else -> Random.nextInt(28, 48)
                }
                broadcastStatus()
            }
        }
    }

    private fun setFreezeMode(enable: Boolean) {
        isFreezing.set(enable)
        if (enable) {
            isGhosting.set(false)
            isTeleporting.set(false)
            val config = prefs.loadConfig()
            // Auto timeout freeze
            scope.launch {
                delay(config.freezeDurationSeconds * 1000L)
                if (isFreezing.get()) {
                    isFreezing.set(false)
                    broadcastStatus()
                }
            }
        }
        broadcastStatus()
    }

    private fun setGhostMode(enable: Boolean) {
        isGhosting.set(enable)
        if (enable) {
            isFreezing.set(false)
            isTeleporting.set(false)
        }
        broadcastStatus()
    }

    private fun setTeleportMode(enable: Boolean) {
        isTeleporting.set(enable)
        if (enable) {
            isFreezing.set(false)
            isGhosting.set(false)
        }
        broadcastStatus()
    }

    private fun broadcastStatus() {
        val currentMode = when {
            isFreezing.get() -> LagMode.FREEZE
            isGhosting.get() -> LagMode.GHOST
            isTeleporting.get() -> LagMode.TELEPORT
            else -> LagMode.NORMAL
        }

        val intent = Intent(BROADCAST_STATS).apply {
            putExtra(EXTRA_IS_RUNNING, isServiceRunning)
            putExtra(EXTRA_CURRENT_MODE, currentMode.name)
            putExtra(EXTRA_PACKETS_SENT, packetsSent.get())
            putExtra(EXTRA_PACKETS_DELAYED, packetsDelayed.get())
            putExtra(EXTRA_PACKETS_BLOCKED, packetsBlocked.get())
            putExtra(EXTRA_PING, currentPingMs)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun stopVpn() {
        isServiceRunning = false
        isFreezing.set(false)
        isGhosting.set(false)
        isTeleporting.set(false)

        vpnJob?.cancel()
        metricsJob?.cancel()
        delayedQueue.clear()

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        vpnInterface = null

        broadcastStatus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Game Booster VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "VPN service background notification"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FakePingZzz Booster")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
