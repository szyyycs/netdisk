package com.ycs.netdisk

import android.app.Application
import android.content.Context
import android.content.Intent
import com.tencent.mmkv.MMKV
import org.json.JSONObject




/**
 * <pre>
 *     author : yangchaosheng
 *     e-mail : yangchaosheng@hisense.com
 *     time   : 2022/07/27
 *     desc   :
 * </pre>
 */
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val context=baseContext
        MMKV.initialize(context)
      //  startService(Intent(context,ConnectService::class.java))

}
}