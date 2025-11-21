package com.cherrystudios.bamboo.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build

/**
 * ActivityExt
 *
 * @author john
 * @since 2025-11-13
 */
///////////////////////////////////////////////////////////////////////////
// 注册广播接收器
///////////////////////////////////////////////////////////////////////////
fun Activity.registerReceiverCompat(receiver: BroadcastReceiver?, filter: IntentFilter) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        registerReceiver(receiver, filter)
    }
}