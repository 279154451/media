package com.qt.media.test.ui

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 描述：子线程的handler
 *
 */
class WorkHandlerThread(name: String, val onLooperPrepared: ((WorkHandlerThread) -> Unit)? = null, val block: (Message) -> Unit) :
    HandlerThread(name) {

    private var mWorkHandler: CustomHandler? = null


    override fun onLooperPrepared() {
        super.onLooperPrepared()
        onLooperPrepared?.invoke(this)
    }

    override fun start() {
        super.start()
        mWorkHandler = CustomHandler(looper, block)
    }
    fun getHandler(): Handler? = mWorkHandler

    /**
     * 是否取消任务
     */
    fun isCanceled(): Boolean = mWorkHandler?.isCanceled() == true


    fun reset() {
        mWorkHandler?.reset()
    }

    fun queueEvent(msg: Message) {
        mWorkHandler?.queueEvent(msg)
    }

    /**
     * 取消当前所有任务
     */
    fun cancel() {
        mWorkHandler?.cancel()
        //移除所有还未执行的任务
        mWorkHandler?.removeCallbacksAndMessages(null)
    }


    /**
     * 自定义 Handler
     */
    private class CustomHandler(looper: Looper, val block: (Message) -> Unit) : Handler(looper) {


        /**
         * 当前任务是否取消
         */
        private val cancel = AtomicBoolean(false)

        fun queueEvent(msg: Message) {
            sendMessage(msg)
        }

        fun reset() {
            cancel.set(false)
        }

        fun cancel() {
            cancel.set(true)
        }

        /**
         * 是否取消任务
         */
        fun isCanceled(): Boolean = cancel.get()

        override fun handleMessage(msg: Message) {
            if (isCanceled()) return
            block.invoke(msg)
        }
    }
}