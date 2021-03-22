package com.dsd.mrcd.transform.asm

import java.io.IOException
import java.io.InputStream

/**
 * Created by quinn on 06/09/2018
 */
interface IBytecodeAdapter {
    /**
     * Check a certain file is weavable
     */
    @Throws(IOException::class)
    fun isConvertAbleClass(filePath: String?): Boolean

    /**
     * Weave single class to byte array
     */
    @Throws(IOException::class)
    fun transformClazz2ByteArray(inputStream: InputStream?): ByteArray?
}