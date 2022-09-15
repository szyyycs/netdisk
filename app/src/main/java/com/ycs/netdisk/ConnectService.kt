package com.ycs.netdisk

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.RemoteException
import android.util.Log
import com.dzsb.configmanage.IConfigCallback
import com.google.gson.Gson
import com.hisense.configserver.aidl.IConfigService
import com.ycs.netdisk.Util.Companion.LIST_ACTION
import com.ycs.netdisk.Util.Companion.LOGIN_ACTION
import com.ycs.netdisk.Util.Companion.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject


class ConnectService : Service() {


    private val mReceiver: MyReceiver by lazy {
        MyReceiver()
    }
    private var iConfigService: IConfigService? = null
    private var connected = false
    private var loginListener: LoginListener? = null
    private var listListener: ListDirListener? = null
    private var downloadListener: DownloadListener? = null
    override fun onBind(intent: Intent?): IBinder? {
        return MyBinder()
    }

    inner class MyBinder : Binder() {
        fun getService(): ConnectService {
            return this@ConnectService
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: ")
        bindService()
        initReceiver()

    }

    private val deathRecipient: DeathRecipient = object : DeathRecipient {
        override fun binderDied() {
            Log.d(TAG, "binderDied，链接断开");
            if (iConfigService == null) return
            iConfigService!!.asBinder().unlinkToDeath(this, 0)
            iConfigService = null
            connected = false
        }
    }


    private val listener: IConfigCallback = object : IConfigCallback.Stub() {


        override fun onDownloadResponse(type: Int, result: Int, filepath: String?, percent: Int) {
            Log.d(
                TAG,
                "onDownloadResponse:type= $type result=$result filepath=$filepath percent=${percent.toFloat()/100f}"
            )
            if(result==0&&percent!=100){
                downloadListener?.onProgrss(percent.toFloat()/100f)
            }
            if(result==0&&percent==100){
                downloadListener?.onSuccess(filepath)
                downloadListener?.onProgrss( 0f)
            }else if(result!=0){
                downloadListener?.onFail("下载失败")
                downloadListener?.onProgrss( 0f)
            }
        }

        override fun onDataContentResponse(type: Int, result: Int, data: ByteArray?) {
            data?.let {
                Log.d(TAG, "onDataContentResponse: type:$type result:$result data:${data}")
            }
            //result=200成功，不是就失败
            //type=0 login 1：查询 2：下载
            //code=0登录成功
            when (type) {
                0 -> {//login
                    if (result == 200) {
                        val message = data?.let { String(it) }
                        val jsonObject = JSONObject(message)
                        var code = jsonObject.getInt("code")
                        if (code == 0) {
                            Log.d(TAG, "onDataContentResponse: code==$code")
                            loginListener?.onSuccess()
                        } else {
                            var msg = jsonObject.getString("msg")
                            loginListener?.onFail(msg)
                        }
                    } else {
                        loginListener?.onFail("登录失败")
                    }
                }
                1 -> {//查询路径
                    if (result == 200) {
                        val message = data?.let { String(it) }
                        Log.d(TAG, "onDataContentResponse: message:${message}")
                        val jsonObject = JSONObject(message)
                        var code = jsonObject.getInt("code")
                        if (code == 0) {
                            Log.d(TAG, "code==0")
                            var dataS = jsonObject.getJSONObject("data")
                            Log.d(TAG, "onDataContentResponse: dataS==$dataS")
                            var objects = dataS.getString("objects")
                            Log.d(TAG, "onDataContentResponse: objects==$objects")
                            var list = dataS.getJSONArray("objects")
                            val gson = Gson()
                            var fileList = mutableListOf<Bean.File>()
                            //var fileList= gson.toJavaObject(list, )
                            for (i in 0 until list.length()) {
                                val file = gson.fromJson(list[i].toString(), Bean.File::class.java)
                                fileList.add(file)
                            }
                            Log.d(TAG, "onDataContentResponse: filelist:${fileList}")
                            listListener?.onList(fileList)
                        } else if (code == 401) {
                            Log.d(TAG, "onDataContentResponse: code==401")
                            var msg = jsonObject.getString("msg")

                            if (msg == "未登录") {
//                                Log.d(TAG, "onDataContentResponse: msg:${msg}")
                                listListener?.onNotLoggin()
                            }

                        }
                    }
                }
                2 -> {//下载
                    if (result == 200) {
                        val message = data?.let { String(it) }
                        Log.d(TAG, "onDataContentResponse:下载的 message$message")
                        val jsonObject = JSONObject(message)
                        val name=jsonObject.getString("filename")
                        downloadListener?.onStart(name)
                    }

                }
                3->{//下载成功？
                    if (result == 200) {
                        val message = data?.let { String(it) }
                        Log.d(TAG, "onDataContentResponse:下载的 message$message")
                    }
                }

            }

        }

    }

    override fun onUnbind(intent: Intent?): Boolean {
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        unbindService(serviceConnection)
        unregisterReceiver(mReceiver)
        super.onDestroy()
    }

    private fun bindService() {
        val intent = Intent(IConfigService::class.java.name)
        intent.setPackage("com.hisense.configserver")
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun initReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(LIST_ACTION)
        intentFilter.addAction(LOGIN_ACTION)
        registerReceiver(mReceiver, intentFilter);
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(TAG, "onServiceConnected")
            iConfigService = IConfigService.Stub.asInterface(service)
            connected = true
            //设置监听
            try {
                iConfigService?.asBinder()?.linkToDeath(deathRecipient, 0)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            try {
                if (iConfigService == null) return
                iConfigService!!.registerCallback(listener)
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            Log.d(TAG, "onServiceConnected: "+getMeidId())

        }

        override fun onServiceDisconnected(name: ComponentName) {
            Log.i(TAG, "onServiceDisconnected")
            iConfigService = null
            connected = false
        }
    }

    fun login(name: String, pwd: String,ip:String) {
        try {
            if (iConfigService != null && connected) {
                GlobalScope.launch(Dispatchers.IO) {
                    val result = async {
                        iConfigService?.login(name, pwd , ip)
                        Log.d(TAG, "login name:${name}\npwd:$pwd)")
                    }
                    val response = result.await()
                    if (response == 1) {
                    } else {
                        loginListener?.onFail("网络异常")
                    }
                    Log.d(TAG, "login: $response")
                }

            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun logout(){
        try {
            if (iConfigService != null && connected) {
                GlobalScope.launch(Dispatchers.IO) {
                    iConfigService?.exit()
                }
            }
        }catch (e: RemoteException){
            e.printStackTrace()
        }
    }
    fun getMeidId():String?{
        try {
            if (iConfigService != null && connected) {
                Log.d(TAG, "getMeidId: ")
                return iConfigService?.getMeidId()
            }
        }catch (e: RemoteException){
            e.printStackTrace()
        }
        return ""
    }
    fun listDir(path: String) {
        try {
            if (iConfigService != null && connected) {

                GlobalScope.launch(Dispatchers.IO) {
                    val result = async {
                        Log.d(TAG, "listDir $path")
                        iConfigService?.listDir(path)
                    }
                    val response = result.await()
                    if (response == 1) {
                        listListener?.onfail()
                    }
                    Log.d(TAG, "listDir: response$response")
                }


            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    //type=0文件，1目录
    fun download(id: String, name: String, type: Int) {
        try {
            if (iConfigService != null && connected) {

                GlobalScope.launch(Dispatchers.IO) {
                    val result = async {
                        Log.d(TAG, "download")
                        iConfigService?.download(id, name, type)
                    }
                    val response = result.await()
                    if (response == 1) {


                    }
                    Log.d(TAG, "download: response$response")
                }


            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    fun setLoginListener(listener: LoginListener?) {
        this.loginListener = listener
    }

    fun setListDirListener(listener: ListDirListener) {
        this.listListener = listener
    }
    fun setDownloadListener(listener: DownloadListener) {
        this.downloadListener = listener
    }
    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                LOGIN_ACTION -> {
                    val name: String = intent?.getStringExtra("name").toString()
                    val pwd: String = intent?.getStringExtra("pwd").toString()
                    val ip: String = intent?.getStringExtra("ip").toString()
                    login(name = name, pwd = pwd , ip = ip)
                }
                LIST_ACTION -> {
                    val path: String = intent?.getStringExtra("path").toString()
                    listDir(path = path)
                }
                else -> {

                }
            }
        }

    }

    interface LoginListener {
        fun onSuccess()
        fun onFail(msg: String)
    }

    interface ListDirListener {
        fun onList(list: MutableList<Bean.File>)
        fun onfail()
        fun onNotLoggin()
    }
    interface DownloadListener {
        fun onStart(name:String)
        fun onProgrss(progress:Float)
        fun onFail(msg: String)
        fun onSuccess(path: String?)
    }
}