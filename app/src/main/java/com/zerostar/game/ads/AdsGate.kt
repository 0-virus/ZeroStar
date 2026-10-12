package com.zerostar.game.ads

/** UMP supplies permission; this gate only prevents duplicate initialization and stale callbacks. */
class AdsGate(private val initialize: (() -> Unit) -> Unit, private val changed: () -> Unit) {
    private var requested = false
    private var initialized = false
    private var allowed = false
    private var closed = false
    val ready: Boolean get() = !closed && allowed && initialized
    fun update(canRequestAds: Boolean) {
        if (closed) return
        allowed = canRequestAds
        if (allowed && !requested) {
            requested = true
            initialize {
                if (!closed) { initialized = true; changed() }
            }
        }
        changed()
    }
    fun close() { closed = true; allowed = false; changed() }
}
