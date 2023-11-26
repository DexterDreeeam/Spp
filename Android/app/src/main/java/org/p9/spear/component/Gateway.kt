package org.p9.spear.component

import android.net.VpnService
import android.util.Log
import org.p9.spear.entity.Packet
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

abstract class IGateway : Runnable {

    private var running: Boolean? = false
    var subGateways = mutableListOf<IGateway>()
    protected lateinit var fd: FileDescriptor
    private lateinit var thread: Thread

    protected open fun setup() {}

    protected open fun clean() {}

    fun start(fd: FileDescriptor) {
        if (this::thread.isInitialized && thread.isAlive) {
            throw IllegalStateException("Running already")
        }

        this.fd = fd
        running = null
        setup()
        thread = Thread(this).apply { start() }
        subGateways.forEach { g -> g.start(fd) }
    }

    fun stop() {
        running = null
        subGateways.forEach { g -> g.stop() }
        subGateways.clear()
        if (this::thread.isInitialized) {
            thread.interrupt()
        }
    }

    override fun run() {
        running = true
        if (firstIterate()) {
            while (!this.thread.isInterrupted && iterate()) {
            }
        }
        clean()
        running = false
    }

    protected open fun firstIterate(): Boolean {
        return true
    }

    protected open fun iterate(): Boolean {
        return true
    }
}

class Gateway(vpn: VpnService, endpoint: String) : IGateway() {

    private val port: IPort = TestPort(vpn, endpoint)

    override fun setup() {
        subGateways.clear()
        subGateways.add(GatewaySend(port))
        subGateways.add(GatewayReceive(port))
    }

    override fun firstIterate(): Boolean {
        port.connect()
        return true
    }
}

class GatewaySend(private val port: IPort) : IGateway() {

    private lateinit var stream: FileInputStream
    private val buffer: ByteBuffer = ByteBuffer.allocate(UShort.MAX_VALUE.toInt())

    override fun setup() {
        stream = FileInputStream(fd)
    }

    override fun iterate(): Boolean {
        return try {
            val len = stream.read(buffer.array())
            if (len > 0) {
                Log.i(javaClass.name, "+++ $len")
                buffer.limit(len)
                val packet = Packet(buffer)
                port.send(packet)
                buffer.clear()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

class GatewayReceive(port: IPort) : IGateway() {

    private lateinit var stream: FileOutputStream
    private val buffer: ByteBuffer = ByteBuffer.allocate(16384)

    init {
        port.onReceive = { packet, len ->
            Log.i(javaClass.name, "--- $len")
            stream.write(packet.buffer.array(), 0, len)
            true
        }
    }

    override fun setup() {
        stream = FileOutputStream(fd)
    }
}
