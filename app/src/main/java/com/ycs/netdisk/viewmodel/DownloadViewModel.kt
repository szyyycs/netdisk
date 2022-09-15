package com.ycs.netdisk

import android.Manifest.permission
import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tencent.mmkv.MMKV
import com.ycs.netdisk.Util.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat


/**
 * <pre>
 *     author : yangchaosheng
 *     e-mail : yangchaosheng@hisense.com
 *     time   : 2022/08/01
 *     desc   :
 * </pre>
 */
class DownloadViewModel : ViewModel() {
    val mmkv: MMKV by lazy {
        MMKV.mmkvWithID("login")
    }
    val permissions = arrayOf(
        permission.WRITE_EXTERNAL_STORAGE,
        permission.READ_EXTERNAL_STORAGE,
        MANAGE_EXTERNAL_STORAGE,

    )
    var apkPath=""
    val url = Environment.getExternalStorageDirectory().toString() + "/Download/"
    var downloadList: MutableList<DownloadItem> = mutableStateListOf()
    var isEmpty = MutableLiveData(false)

    fun initData(){
        var path=mmkv.decodeString("isLogin")
        if(path!=null||path!=""){

            if(checkFileIsNull()){
                return
            }

            scanItemListFromFile(path!!)
        }else{
            //Log.d("yyy", "path=null ")
            isEmpty.value=true
        }
    }
    fun checkFileIsNull(): Boolean {
        val f = File(url)

        if (!f.exists()) {
            f.mkdirs()
        }

        if (f.list() == null || f.list().isEmpty()) {

            isEmpty.value=true
            return true
        }else{
            isEmpty.value=false
        }
        return isEmpty.value!!

    }

    fun checkIsAndroidO(context: Activity, apkPath:String) {

        if (Build.VERSION.SDK_INT >=26) {

            var b = context.packageManager.canRequestPackageInstalls()

            if (b) {

                InstallUtil.installApk(context, apkPath)

                //安装应用的逻辑(写自己的就可以)

            }else {
                Toast.makeText(context, "请同意应用安装权限", Toast.LENGTH_SHORT).show()

                var intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);

                context.startActivityForResult(intent, 10012);

            }

        }else {

            InstallUtil.installApk(context, apkPath);

        }

    }
    /**
     * TODO 打开某文件
     *
     * @param context     context
     * @param destination 文件目录
     */
    @Throws(Exception::class)
    fun openFile(context: Context, destination: String) {
        var destination = destination
        if (!File(destination).exists()) {
            //文件不存在
            return
        }
        var file=File(destination)
    //文件路径
//        if (!destination.contains("file://")) {
//            destination = "file:/$destination"
//        }
        //获取文件类型
        Log.d(TAG, "openFile: $destination")
        val nameType = destination.split(".")
        if(nameType.size<=1){
            return
        }
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(nameType[1])
        Log.d(TAG, "$mimeType")
        val intent = Intent()
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = Intent.ACTION_VIEW
        //设置文件的路径和文件类型
        val uriForFile: Uri
        if (Build.VERSION.SDK_INT > 23) {
            uriForFile = FileProvider.getUriForFile(
                context, "com.ycs.netdisk.provider",
                file!!
            )
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) //给目标文件临时授权
        } else {
            uriForFile = Uri.fromFile(file)
        }
//        val contentUri = FileProvider.getUriForFile(context, "com.ycs.servicetest.provider", file)
        intent.setDataAndType(uriForFile, mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //跳转
        context.startActivity(intent)
    }
    fun scanItemListFromFile(path:String) {
        downloadList.clear()
        viewModelScope.launch(Dispatchers.Default) { //获取远端数据需要耗时，创建一个协程运行在子线程，不会阻塞
            val getList = async {
                var newList = mutableListOf<DownloadItem>()
                val f = File(url)

                var list = f.list() ?: return@async newList
                for (s in list!!) {
                    Log.d(TAG, "scanItemListFromFile: ${list.size}")
                    val uu = url + s
                    var t=s.split(".")
                    var type = if(t.isNotEmpty()){
                        var tt=t[t.size-1]
                        when (tt.toLowerCase()) {
                            "pdf" -> 2
                            "jpg", "png","psd","bmp","svg" -> 3
                            "docx", "doc" -> 4
                            "xlsx", "xlx" -> 5
                            "zip","rar" -> 6
                            "txt" -> 7
                            "ppt","pptx"-> 9
                            "mp4","avi","m4v","wmv","mkv"->10
                            "apk"->11
                            else -> 8
                        }
                    }else{
                        0
                    }
                    val file = File(uu)
                    Log.d(TAG, "scanItemListFromFile: $uu")
                    val fileSize:String
                    if(file.length()<1024*1024){
                        var d = BigDecimal(file.length() / 1024 )
                            .setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
                       fileSize = d.toString() + "KB"
                    }else{
                       var d = BigDecimal(file.length() / (1024 * 1024.0))
                            .setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
                        fileSize = d.toString() + "MB"
                    }
                    var attr: BasicFileAttributes?
                    var instant = null
                    var time: String?
                    try {
                        var path: Path?
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            path = file.toPath()
                            attr = Files.readAttributes(path, BasicFileAttributes::class.java)
                            instant = attr.creationTime().toInstant() as Nothing?
                        }
                        time = if (instant != null) {
                            val temp = instant.toString().replace("T", " ").replace("Z", "").replace("-", "/")
                            temp.substring(0, temp.length - 3)
                        } else {
                            val timeee = file.lastModified()
                            val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                            formatter.format(timeee)
                        }
                    } catch (e: Exception) {
                        val timeee = file.lastModified()
                        val formatter = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                        time = formatter.format(timeee)

                    }
                    val i = DownloadItem(name = s,time= time ?: "",type=type, size = fileSize)

                    newList.add( i)
                }

                newList
            }
            val response = getList.await()  //等待deferred 的返回
            GlobalScope.launch(Dispatchers.Main) {
                for (s in response){
                    downloadList.add(0,s)
                }

                isEmpty.value = downloadList.size==0
            }

        }

    }
}

data class DownloadItem(var name:String, var time:String, var type:Int, var size:String)
