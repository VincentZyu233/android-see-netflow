package com.vincentzyu.androidseenetflow

object RustBridge {
    private var loaded = false

    init {
        loaded = try {
            System.loadLibrary("android_see_netflow_core")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    external fun stringFromRust(): String

    fun describe(): String {
        if (!loaded) {
            return "Rust bridge not packaged yet"
        }
        return try {
            val payload = stringFromRust()
            "Rust bridge ready (${payload.length} chars)"
        } catch (_: Throwable) {
            "Rust bridge call failed"
        }
    }
}
