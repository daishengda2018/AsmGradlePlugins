package com.dsd.asm

import android.app.Application
import com.dsd.asm.helper.ContextExtend

/**
 *
 * Create by im_dsd 2021/3/31 19:59
 */
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ContextExtend.app = this
    }
}