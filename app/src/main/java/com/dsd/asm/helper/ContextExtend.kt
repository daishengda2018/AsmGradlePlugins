package com.dsd.asm.helper

import android.app.Application
import android.widget.Toast

/**
 * @Author LiABao
 * @Since 2021/1/4
 */
object ContextExtend {
    var app: Application? = null

    fun requireApplication(): Application {
        return requireNotNull(app)
    }
}

fun Application.show(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}