package com.ycs.netdisk

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.ycs.netdisk.Util.Companion.REQUEST_CODE
import com.ycs.netdisk.Util.Companion.TAG
import com.ycs.netdisk.ui.theme.NetDiskTheme


class DownloadActivity : ComponentActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(DownloadViewModel::class.java)
    }
    val context by lazy {
        this@DownloadActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()

        getPermission()

    }

    private fun initView() {
        setContent {
            NetDiskTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                ) {
                    DownloadUI(viewModel, context)
                }
            }
        }
        setStatusBarColor(R.color.fb)
        setLightStatusBar()
    }

    private fun setStatusBarColor(color: Int) {
        val window = window
        window.statusBarColor = ContextCompat.getColor(this, color)
    }

    private fun setLightStatusBar() {
        val flags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }


    @SuppressLint("NewApi")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.e(TAG, "onActivityResult: 一次")
        if (requestCode == REQUEST_CODE) {
            if (Environment.isExternalStorageManager()) {

            } else {
                Toast.makeText(this, "暂未取得读取文件权限，请前往获取", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }
    private fun checkPermission(): Boolean {
        return if (SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val result =
                ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)
            val result1 =
                ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
            result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }
    }
    private fun getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, viewModel.permissions, 111)
            } else {
               // viewModel.initData()
            }
            if(!checkPermission()){
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                    startActivityForResult(intent, 2296)
                } catch (e: Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivityForResult(intent, 2296)
                }
            }else{
                viewModel.initData()
            }

        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionss: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissionss, grantResults)
        if (requestCode == 111) {
            Log.e("yyy", "获取权限返回")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "未允许权限，同意后才可以查看下载文件列表", Toast.LENGTH_SHORT).show()
                    ActivityCompat.requestPermissions(this, viewModel.permissions, 111)
                } else {
                    // viewModel.initData()
                }
            }
        }
        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {

                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if(requestCode == 10012){
            viewModel.checkIsAndroidO(this,viewModel.apkPath)
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview3() {
//        val vm=DownloadViewModel()
//        vm.initData()
        NetDiskTheme {
            FileItemUI(Modifier, DownloadItem("111", "222", 3, "22mb"))
        }
    }
}

@Composable
fun DownloadUI(viewModel: DownloadViewModel, context: Activity) {
    val isEmpty by viewModel.isEmpty.observeAsState()
    Column(modifier = Modifier.background(Color.White)) {
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .background(Color(0xfffbfbfb))

        ) {
            Text(
                text = "我的下载",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 20.sp
            )
            Image(
                painter = painterResource(id = R.drawable.more),
                modifier = Modifier
                    .padding(end = 10.dp)
                    .align(Alignment.CenterEnd)
                    .padding(10.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {},
                            onTap = {

                            })
                    },
                contentDescription = ""
            )
        }
        if (isEmpty!!) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Image(
                        painter = painterResource(id = R.drawable.empty2),
                        contentDescription = "",
                        modifier = Modifier
                            .align(alignment = Alignment.CenterHorizontally)
                            .size(200.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "下载目录为空，快去下载一个文件吧~",
                        modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                        color = Color(0xffaaaaaa),
                        textAlign = TextAlign.Center
                    )

                }

            }
        } else {
            LazyColumn(modifier = Modifier.background(Color.White)) {
                itemsIndexed(viewModel.downloadList) { index: Int, item: DownloadItem ->
                    FileItemUI(modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {},
                            onTap = {
                                var s=item.name.split(".")
                                if(s.size>=2){
                                    if(s[1] == "apk"){
                                        Log.d(TAG, "DownloadUI: ")
                                        viewModel.apkPath=viewModel.url + item.name
                                        viewModel.checkIsAndroidO(context, apkPath =viewModel.apkPath )
                                        return@detectTapGestures
                                    }
                                }
                                try {
                                    viewModel.openFile(context = context,viewModel.url + item.name)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "没有找到启动应用程序", Toast.LENGTH_SHORT)
                                        .show()
                                }


                            })
                    }, file = item)
                }
            }
        }

    }

}

@Composable
fun FileItemUI(modifier: Modifier, file: DownloadItem) {
    Column(
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .height(10.dp)
                .fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(shape = RoundedCornerShape(15.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 15.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = when (file.type) {
                            1 -> R.drawable.file
                            2 -> R.drawable.pdf
                            3 -> R.drawable.jpg
                            4 -> R.drawable.docx
                            5 -> R.drawable.xlsx
                            6 -> R.drawable.zip
                            7 -> R.drawable.txt
                            9 ->R.drawable.ppt
                            10 -> R.drawable.mp4
                            11->R.drawable.apk
                            else -> R.drawable.unknow
                        }
                    ),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(if (file.type == 1) 10.dp else 13.dp)

                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Text(
                        text = file.name,
                        textAlign = TextAlign.Center,
                        color = Color(0xff666666),
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis

                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = file.time,
                            textAlign = TextAlign.Center,
                            color = Color(0xffaaaaaa),
                            fontSize = 12.sp,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                        Text(
                            text = file.size,
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = Color(0xffaaaaaa),
                            fontSize = 12.sp,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }


                }
            }


        }
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .background(Color(0xfffEfEfE))
        )
    }
}

