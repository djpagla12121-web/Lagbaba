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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.ConcurrentHashMap
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
    private var delayedPacketJob: Job? = null

    private lateinit var prefs: PreferencesManager

    private val isFreezing = AtomicBoolean(false)
    private val isGhosting = AtomicBoolean(false)
    private val isTeleporting = AtomicBoolean(false)

    private val packetsSent = AtomicLong(0)
    private val packetsDelayed = AtomicLong(0)
    private val packetsBlocked = AtomicLong(0)
    private var currentPingMs = 38

    private data class DelayedUdpPacket(
        val channel: DatagramChannel,
        val payload: ByteArray,
        val deliverAt: Long
    )

    private val delayedQueue = ConcurrentLinkedQueue<DelayedUdpPacket>()

    private class UdpFlow(
        val channel: DatagramChannel,
        val srcPort: Int,
        val dstIp: InetAddress,
        val dstPort: Int,
        var lastActive: Long
    )

    private val udpFlows = ConcurrentHashMap<String, UdpFlow>()
    private var selector: Selector? = null
    private var isRootDevice = false

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        createNotificationChannel()
        checkRootAvailability()
    }

    private fun checkRootAvailability() {
        scope.launch {
            try {
                val suPaths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk")
                isRootDevice = suPaths.any { File(it).exists() }
            } catch (ignored: Exception) {
                isRootDevice = false
            }
        }
    }

    private fun executeRootIptables(dropUdp: Boolean) {
        if (!isRootDevice) return
        scope.launch {
            try {
                val cmd = if (dropUdp) {
                    "iptables -I OUTPUT -p udp --dport 1024:65535 -j DROP\n"
                } else {
                    "iptables -D OUTPUT -p udp --dport 1024:65535 -j DROP\n"
                }
                val process = Runtime.getRuntime().exec("su")
                val os = java.io.DataOutputStream(process.outputStream)
                os.writeBytes(cmd)
                os.writeBytes("exit\n")
                os.flush()
                process.waitFor()
            } catch (ignored: Exception) {}
        }
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

            try {
                builder.addDisallowedApplication(packageName)
            } catch (ignored: Exception) {}

            vpnInterface = builder.establish()
            isServiceRunning = true
            broadcastStatus()

            selector = Selector.open()
            startPacketEngine()
            startDelayedDeliveryLoop()
            startMetricsLoop()
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun startDelayedDeliveryLoop() {
        delayedPacketJob = scope.launch {
            while (isActive && isServiceRunning) {
                val now = System.currentTimeMillis()
                val it = delayedQueue.iterator()
                while (it.hasNext()) {
                    val item = it.next()
                    if (now >= item.deliverAt) {
                        it.remove()
                        try {
                            if (item.channel.isOpen && item.channel.isConnected) {
                                item.channel.write(ByteBuffer.wrap(item.payload))
                            }
                        } catch (ignored: Exception) {}
                    }
                }
                delay(15)
            }
        }
    }

    private fun startPacketEngine() {
        val pfd = vpnInterface ?: return
        val inStream = FileInputStream(pfd.fileDescriptor)
        val outStream = FileOutputStream(pfd.fileDescriptor)

        // 1. Inbound selector loop (Reads UDP responses from remote game servers and writes to TUN)
        vpnJob = scope.launch {
            val recvBuffer = ByteBuffer.allocate(32768)
            while (isActive && isServiceRunning) {
                try {
                    val sel = selector ?: break
                    if (sel.select(50) > 0) {
                        val selectedKeys = sel.selectedKeys()
                        val keyIterator = selectedKeys.iterator()
                        while (keyIterator.hasNext()) {
                            val key = keyIterator.next()
                            keyIterator.remove()

                            if (key.isValid && key.isReadable) {
                                val channel = key.channel() as? DatagramChannel ?: continue
                                val flow = key.attachment() as? UdpFlow ?: continue

                                recvBuffer.clear()
                                val bytesRead = channel.read(recvBuffer)
                                if (bytesRead > 0) {
                                    recvBuffer.flip()
                                    val replyPayload = ByteArray(bytesRead)
                                    recvBuffer.get(replyPayload)

                                    flow.lastActive = System.currentTimeMillis()

                                    // Build IPv4 UDP packet for local TUN device
                                    val ipPacket = buildIpUdpPacket(
                                        srcIp = flow.dstIp.address,
                                        dstIp = byteArrayOf(10, 8, 0, 2),
                                        srcPort = flow.dstPort,
                                        dstPort = flow.srcPort,
                                        payload = replyPayload
                                    )
                                    synchronized(outStream) {
                                        outStream.write(ipPacket)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) delay(10)
                }
            }
        }

        // 2. Outbound TUN reader loop (Reads game outbound packets and applies FREEZE/GHOST/TELE)
        scope.launch {
            val rawBuffer = ByteArray(32768)
            while (isActive && isServiceRunning) {
                try {
                    val length = inStream.read(rawBuffer)
                    if (length > 0) {
                        handleOutboundPacket(rawBuffer, length)
                    } else {
                        delay(2)
                    }
                } catch (e: Exception) {
                    if (isActive) delay(10)
                }
            }
        }
    }

    private fun handleOutboundPacket(rawBuffer: ByteArray, length: Int) {
        if (length < 20) return

        val versionAndIhl = rawBuffer[0].toInt() and 0xFF
        val version = versionAndIhl ushr 4
        if (version != 4) return // IPv4 only

        val ihl = (versionAndIhl and 0x0F) * 4
        if (length < ihl + 8) return

        val protocol = rawBuffer[9].toInt() and 0xFF
        if (protocol != 17) {
            // Non-UDP: drop or ignore (games use UDP for all real-time movement/damage sync)
            return
        }

        // Parse UDP
        val srcPort = ((rawBuffer[ihl].toInt() and 0xFF) shl 8) or (rawBuffer[ihl + 1].toInt() and 0xFF)
        val dstPort = ((rawBuffer[ihl + 2].toInt() and 0xFF) shl 8) or (rawBuffer[ihl + 3].toInt() and 0xFF)
        val udpLen = ((rawBuffer[ihl + 4].toInt() and 0xFF) shl 8) or (rawBuffer[ihl + 5].toInt() and 0xFF)

        val payloadOffset = ihl + 8
        val payloadLen = length - payloadOffset
        if (payloadLen <= 0) return

        val payload = rawBuffer.copyOfRange(payloadOffset, length)
        val dstIpBytes = rawBuffer.copyOfRange(16, 20)
        val dstIp = InetAddress.getByAddress(dstIpBytes)

        packetsSent.incrementAndGet()

        val flowKey = "$srcPort->${dstIp.hostAddress}:$dstPort"
        val flow = getOrCreateUdpFlow(flowKey, srcPort, dstIp, dstPort) ?: return

        val now = System.currentTimeMillis()
        val isDns = (dstPort == 53 || srcPort == 53)

        // DNS is always forwarded directly to maintain active connectivity
        if (isDns) {
            try {
                flow.channel.write(ByteBuffer.wrap(payload))
            } catch (ignored: Exception) {}
            return
        }

        // Game Packet Filtering / Fake Lag Engine
        val config = prefs.loadConfig()

        if (isFreezing.get()) {
            // FREEZE ACTIVE: Drop outbound game UDP packet (Server sees 999ms desync standing still)
            packetsBlocked.incrementAndGet()
            return
        }

        if (isGhosting.get()) {
            // GHOST ACTIVE: Add randomized jitter delay to simulate stutter
            packetsDelayed.incrementAndGet()
            val delayMs = Random.nextLong(
                config.ghostDropMin.toLong().coerceAtLeast(150),
                config.ghostDropMax.toLong().coerceAtLeast(350)
            )
            delayedQueue.add(DelayedUdpPacket(flow.channel, payload, now + delayMs))
            return
        }

        if (isTeleporting.get()) {
            // TELEPORT ACTIVE: Burst delay alternation
            if (Random.nextInt(100) < 55) {
                packetsDelayed.incrementAndGet()
                delayedQueue.add(DelayedUdpPacket(flow.channel, payload, now + Random.nextLong(250, 650)))
            } else {
                try {
                    flow.channel.write(ByteBuffer.wrap(payload))
                } catch (ignored: Exception) {}
            }
            return
        }

        // NORMAL MODE: Immediate forwarding
        try {
            flow.channel.write(ByteBuffer.wrap(payload))
        } catch (ignored: Exception) {}
    }

    private fun getOrCreateUdpFlow(
        flowKey: String,
        srcPort: Int,
        dstIp: InetAddress,
        dstPort: Int
    ): UdpFlow? {
        val existing = udpFlows[flowKey]
        if (existing != null && existing.channel.isOpen) {
            existing.lastActive = System.currentTimeMillis()
            return existing
        }

        return try {
            val channel = DatagramChannel.open().apply {
                configureBlocking(false)
                socket().reuseAddress = true
                protect(socket()) // Bypass VPN TUN loopback
                connect(InetSocketAddress(dstIp, dstPort))
            }

            val sel = selector ?: return null
            val flow = UdpFlow(channel, srcPort, dstIp, dstPort, System.currentTimeMillis())
            channel.register(sel, SelectionKey.OP_READ, flow)
            sel.wakeup()

            udpFlows[flowKey] = flow
            cleanupOldFlows()
            flow
        } catch (e: Exception) {
            null
        }
    }

    private fun cleanupOldFlows() {
        if (udpFlows.size > 200) {
            val now = System.currentTimeMillis()
            val it = udpFlows.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                if (now - entry.value.lastActive > 60_000) {
                    try {
                        entry.value.channel.close()
                    } catch (ignored: Exception) {}
                    it.remove()
                }
            }
        }
    }

    private fun buildIpUdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLen = 20 + 8 + payload.size
        val packet = ByteArray(totalLen)
        val bb = ByteBuffer.wrap(packet)

        // 1. IP Header (20 bytes)
        bb.put(0x45.toByte()) // Version 4, IHL 5
        bb.put(0x00.toByte()) // DSCP/ECN
        bb.putShort(totalLen.toShort()) // Total Length
        bb.putShort(Random.nextInt(1, 65535).toShort()) // Identification
        bb.putShort(0x4000.toShort()) // Flags (Don't Fragment)
        bb.put(64.toByte()) // TTL
        bb.put(17.toByte()) // Protocol UDP
        bb.putShort(0.toShort()) // Header checksum placeholder
        bb.put(srcIp) // Source IP
        bb.put(dstIp) // Destination IP

        // IP Checksum calculation
        var sum = 0
        for (i in 0 until 10) {
            val word = ((packet[i * 2].toInt() and 0xFF) shl 8) or (packet[i * 2 + 1].toInt() and 0xFF)
            sum += word
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val ipChecksum = (sum.inv() and 0xFFFF).toShort()
        packet[10] = (ipChecksum.toInt() ushr 8).toByte()
        packet[11] = (ipChecksum.toInt() and 0xFF).toByte()

        // 2. UDP Header (8 bytes)
        bb.position(20)
        bb.putShort(srcPort.toShort())
        bb.putShort(dstPort.toShort())
        bb.putShort((8 + payload.size).toShort())
        bb.putShort(0.toShort()) // UDP Checksum (0 = disabled/optional in IPv4)

        // 3. Payload
        bb.put(payload)

        return packet
    }

    private fun startMetricsLoop() {
        metricsJob = scope.launch {
            while (isActive && isServiceRunning) {
                delay(1000)
                currentPingMs = when {
                    isFreezing.get() -> 999
                    isGhosting.get() -> Random.nextInt(220, 480)
                    isTeleporting.get() -> Random.nextInt(120, 850)
                    else -> Random.nextInt(28, 45)
                }
                broadcastStatus()
            }
        }
    }

    private fun setFreezeMode(enable: Boolean) {
        isFreezing.set(enable)
        executeRootIptables(enable)
        if (enable) {
            isGhosting.set(false)
            isTeleporting.set(false)
            val config = prefs.loadConfig()
            // Auto timeout freeze for safety
            scope.launch {
                delay(config.freezeDurationSeconds * 1000L)
                if (isFreezing.get()) {
                    isFreezing.set(false)
                    executeRootIptables(false)
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
            executeRootIptables(false)
            isTeleporting.set(false)
        }
        broadcastStatus()
    }

    private fun setTeleportMode(enable: Boolean) {
        isTeleporting.set(enable)
        if (enable) {
            isFreezing.set(false)
            executeRootIptables(false)
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
        executeRootIptables(false)

        vpnJob?.cancel()
        metricsJob?.cancel()
        delayedPacketJob?.cancel()
        delayedQueue.clear()

        udpFlows.forEach { (_, flow) ->
            try {
                flow.channel.close()
            } catch (ignored: Exception) {}
        }
        udpFlows.clear()

        try {
            selector?.close()
        } catch (ignored: Exception) {}
        selector = null

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
