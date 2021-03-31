package com.dsd.asm.helper

import android.os.SystemClock
import android.util.Log
import kotlin.math.abs

class DoubleClickCheck @JvmOverloads constructor(private val timeCheck: Int = TIME_CHECK) {

    private var downTimeTemp: Long = 0

    fun isNotDoubleTap(): Boolean {
        Log.i("isNotDoubleTap", "isNotDoubleTap:${abs(downTimeTemp - System.currentTimeMillis()) > timeCheck}")
        if (abs(downTimeTemp - System.currentTimeMillis()) > timeCheck) {
            downTimeTemp = System.currentTimeMillis()
            return true
        }
        ContextExtend.requireApplication().show("请勿双击")
        return false
    }

    fun update() {
        downTimeTemp = SystemClock.currentThreadTimeMillis()
    }

    companion object {
        const val TIME_CHECK = 1000
    }
}