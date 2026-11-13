package org.f7.motoactions.util

import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList

object EventLog {
    data class Entry(val ts: Long, val message: String)

    private const val MAX = 200
    private val buffer = ArrayDeque<Entry>(MAX)
    private val listeners = CopyOnWriteArrayList<(List<Entry>) -> Unit>()

    @Synchronized
    fun add(message: String) {
        if (buffer.size >= MAX) buffer.removeFirst()
        buffer.addLast(Entry(System.currentTimeMillis(), message))
        val snapshot = buffer.toList()
        listeners.forEach { it(snapshot) }
    }

    @Synchronized
    fun snapshot(): List<Entry> = buffer.toList()

    fun subscribe(listener: (List<Entry>) -> Unit) {
        listeners.add(listener)
        listener(snapshot())
    }

    fun unsubscribe(listener: (List<Entry>) -> Unit) {
        listeners.remove(listener)
    }
}
