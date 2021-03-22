package com.dsd.mrcd.transform.concurrent

import java.io.IOException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class Worker internal constructor(var executor: ExecutorService) {
    private val futures: LinkedList<Future<*>> = LinkedList<Future<*>>()

    fun execute(runnable: Runnable?) {
        runnable ?: return
        futures.add(executor.submit(runnable))
    }

    fun <T> submit(callable: Callable<T>?): Future<T> {
        val future = executor.submit(callable)
        futures.add(future)
        return future
    }

    @Throws(IOException::class)
    fun await() {
        var future: Future<*>
        while (futures.pollFirst().also { future = it } != null) {
            try {
                future.get()
            } catch (e: ExecutionException) {
                when (e.cause) {
                    is IOException -> {
                        throw (e.cause as IOException)
                    }
                    is RuntimeException -> {
                        throw (e.cause as RuntimeException)
                    }
                    is Error -> {
                        throw (e.cause as Error)
                    }
                    else -> throw RuntimeException(e.cause)
                }
            } catch (e: InterruptedException) {
                when (e.cause) {
                    is IOException -> {
                        throw (e.cause as IOException)
                    }
                    is RuntimeException -> {
                        throw (e.cause as RuntimeException)
                    }
                    is Error -> {
                        throw (e.cause as Error)
                    }
                    else -> throw RuntimeException(e.cause)
                }
            }
        }
    }
}