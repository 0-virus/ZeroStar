package com.zerostar.game.game

/** Monotonic time, with remainder retained across successful foreground saves. */
class ForegroundClock {
    private var baseline: Long? = null
    fun start(now: Long) { if (baseline == null) baseline = now }
    fun seconds(now: Long): Long = baseline?.let { (now - it).coerceAtLeast(0) / 1000 } ?: 0
    fun commit(now: Long) { baseline = baseline?.plus(seconds(now) * 1000) }
    fun stop(now: Long): Long = seconds(now).also { baseline = null }
}
