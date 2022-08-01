package com.ycs.netdisk.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Handler
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.tencent.mmkv.MMKV
import com.ycs.netdisk.ConnectService
import com.ycs.netdisk.FileItem
import com.ycs.netdisk.LoginActivity

/**
 * <pre>
 *     author : yangchaosheng
 *     e-mail : yangchaosheng@hisense.com
 *     time   : 2022/07/21
 *     desc   :
 * </pre>
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    var fileList: MutableList<FileItem> = mutableStateListOf()
    var dirList: MutableList<String> = mutableStateListOf()
    var isEmpty = MutableLiveData(false)
    var isLoading=MutableLiveData(true)
    var progress=MutableLiveData(0f)
    var isError=MutableLiveData(false)
    var connectService: ConnectService? = null
    var isRefreshing=false
    val mmkv: MMKV by lazy {
        MMKV.mmkvWithID("login")
    }
    val handler by lazy {
        Handler()
    }
    val context by lazy {
        application.applicationContext
    }
   fun getPath():String{
        var sb=StringBuilder("/")
       for(i in dirList){
           sb.append("$i/")
       }
       sb.deleteCharAt(sb.length-1)
        return sb.toString()
    }
    fun initdata(){
//        fileList.add(FileItem("Baidu","5天前",1))
//        fileList.add(FileItem("Tencent","21小时前",1))
//        fileList.add(FileItem("youku","5天前",1))
//        fileList.add(FileItem("xl","21小时前",1))
//        fileList.add(FileItem("Xioami","5天前",1))
//        fileList.add(FileItem("Wechat","21小时前",1))
//        fileList.add(FileItem("UC","5天前",1))
//        fileList.add(FileItem("test","21小时前",1))
//        fileList.add(FileItem("test.pdf","21小时前",2))
//        fileList.add(FileItem("test.png","21小时前",3))
//        fileList.add(FileItem("test.xlsx","21小时前",5))
//        fileList.add(FileItem("test.zip","21小时前",6))
//        fileList.add(FileItem("1.text","43分钟前",7))
//        fileList.add(FileItem("22.word","34秒前",4))
//        Handler().postDelayed({
//            isLoading.value=false
//        },2000)
    }
    fun checkLogin(activity: Activity){
        var keys=mmkv.allKeys()
        if(keys==null ||keys?.isEmpty()!!){
            activity.startActivity(Intent(activity,LoginActivity::class.java))
            activity.finish()
        }
    }
    fun delayError(delay:Long){
        handler.postDelayed(runnable,delay)

    }
    fun cancel(){
        handler.removeCallbacks(runnable)
        handler.removeCallbacksAndMessages(null);
        isError.value=false
        isLoading.value=false
    }
    val runnable={
        isLoading.value=false
        isError.value=true
    }
}