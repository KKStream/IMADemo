package com.kkstream.imademo.app

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.multidex.MultiDexApplication

class BaseApp : MultiDexApplication() {
    companion object {
        @SuppressLint("HardwareIds")
        fun getDeviceId(context: Context): String {
            return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
    }
}