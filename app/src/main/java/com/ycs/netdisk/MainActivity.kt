package com.ycs.netdisk

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.fade
import com.google.accompanist.placeholder.material.placeholder
import com.ycs.netdisk.Util.Companion.LIST_ACTION
import com.ycs.netdisk.Util.Companion.TAG
import com.ycs.netdisk.ui.theme.BLUE2
import com.ycs.netdisk.ui.theme.LIGHTYELLO
import com.ycs.netdisk.ui.theme.NetDiskTheme
import com.ycs.netdisk.ui.theme.YELLO
import com.ycs.netdisk.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(MainViewModel::class.java)
    }


    // cvm=new ViewModelProvider((ViewModelStoreOwner) getActivity()).get(CookbookViewModel.class);
    private val context by lazy {
        this@MainActivity
    }

    private fun UIListDir(path: String) {
        var intent = Intent(LIST_ACTION)
        intent.putExtra("path", path)
        sendBroadcast(intent)
    }

    private val downloadListener = object : ConnectService.DownloadListener {
        override fun onStart(name: String) {
            runOnUiThread {
                Toast.makeText(context, "${name}开始下载", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onProgrss(progress: Float) {
            runOnUiThread {
                viewModel.progress.value = progress
            }
            // viewModel.progress.postValue(progress)
        }

        override fun onFail(msg: String) {
            runOnUiThread {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onSuccess(path: String?) {
            runOnUiThread {
                Toast.makeText(context, "下载成功！文件存储路径为$path", Toast.LENGTH_SHORT).show()
            }
        }

    }
    private val listDirListener = object : ConnectService.ListDirListener {

        override fun onList(list: MutableList<Bean.File>) {
            runOnUiThread {
                if (viewModel.isRefreshing) {
                    Toast.makeText(context, "刷新成功", Toast.LENGTH_SHORT)
                        .show()
                    viewModel.isRefreshing = false
                }
                viewModel.isLoading.value = false
                viewModel.cancel()
                viewModel.fileList.clear()
                if (list.size == 0) {
                    viewModel.isEmpty.value = true
                    return@runOnUiThread
                }
                viewModel.isEmpty.value = false
                for (item in list) {
                    var type = 0
                    if (item.type == "dir") {
                        type = 1
                    } else {
                        var s = item.name.split(".")
                        if (s.size == 2) {
                            type = when (s[1]) {
                                "pdf" -> 2
                                "jpg", "png" -> 3
                                "docx", "doc" -> 4
                                "xlsx", "xlx" -> 5
                                "zip" -> 6
                                "txt" -> 7
                                else -> 8
                            }
                        }
                    }
                    var t = item.date.split('.')
                    var time = ""
                    if (t.size == 2) {
                        time = t[0].replace('T', ' ')
                    }
                    viewModel.fileList.add(
                        FileItem(
                            name = item.name,
                            time = time.ifEmpty { item.date },
                            type = type,
                            id = item.id,
                            path = item.path,
                            pic = item.pic,
                            size = item.size,
                            date = item.date,
                            create_date = item.create_date,
                            source_enabled = item.source_enabled
                        )
                    )
                }
                viewModel.isError.value = false
            }
        }

        override fun onfail() {
            runOnUiThread {
                Toast.makeText(context, "网络异常", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onNotLoggin() {
            Log.d(TAG, "onNotLoggin: ")
            runOnUiThread {
                Toast.makeText(context, "登录过期，请重新登录", Toast.LENGTH_SHORT).show()
                viewModel.mmkv.clearAll()
                startActivity(Intent(context, LoginActivity::class.java))
                finish()
            }
        }

    }
    val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {}
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            //返回一个MsgService对象
            Log.i("yyy", "onServiceConnected$service")
            viewModel.connectService = (service as ConnectService.MyBinder).getService()
            viewModel.connectService?.setListDirListener(listDirListener)
            viewModel.connectService?.setDownloadListener(downloadListener)
            var keys = viewModel.mmkv.allKeys()
            if (keys == null || keys?.isEmpty()!!) {
            } else {
                viewModel.connectService?.listDir("/")
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.checkLogin(context)
        viewModel.initdata()
        initView(viewModel, context)
        initBinder()
        viewModel.delayError(5000)
    }

    private fun initBinder() {
        val intent = Intent(this, ConnectService::class.java)
        bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    private fun initView(viewModel: MainViewModel, context: MainActivity) {
        setContent {
            NetDiskTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainUI(viewModel, context)
                }
            }
        }
        setStatusBarColor(R.color.fb)
        setLightStatusBar()
    }

    var firstTime = 0L
    override fun onBackPressed() {
        if (viewModel.dirList.size == 0) {
            val secondTime = System.currentTimeMillis()
            if (secondTime - firstTime > 2000) {
                Toast.makeText(this@MainActivity, "再按一次退出网盘", Toast.LENGTH_SHORT).show()
                firstTime = secondTime
                return
            } else {
                super.onBackPressed()
            }
        } else {
            viewModel.dirList.removeLast()
            viewModel.isLoading.value = true
            viewModel.delayError(5000)
            viewModel.connectService?.listDir(viewModel.getPath())
        }
    }

    private fun setStatusBarColor(color: Int) {
        val window = window
        window.statusBarColor = ContextCompat.getColor(this, color)
    }

    private fun setLightStatusBar() {
        val flags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun onDestroy() {
        unbindService(conn)
        super.onDestroy()
    }
}

@Composable
fun AddMoreDialog(openDialog: MutableState<Boolean>, activity: Activity, viewModel: MainViewModel) {
    AnimatedVisibility(
        visible = openDialog.value, enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {},
                        onTap = {
                            openDialog.value = false
                        })
                }
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(Alignment.TopEnd),
                elevation = 10.dp
            ) {
                Column {
                    Text(
                        text = "设置       ",
                        modifier = Modifier
                            .clickable {

                            }
                            .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 5.dp),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "退出登录",
                        modifier = Modifier
                            .clickable {
                                viewModel.mmkv.clearAll()
                                viewModel.connectService?.logout()
                                activity.startActivity(Intent(activity, LoginActivity::class.java))
                                activity.finish()
                            }
                            .padding(vertical = 15.dp, horizontal = 20.dp),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "账号管理",
                        modifier = Modifier
                            .clickable {

                            }
                            .padding(start = 20.dp, end = 20.dp, top = 5.dp, bottom = 25.dp),
                        fontSize = 15.sp
                    )
                }
            }

        }
    }
}

@Composable
fun AddSortDialog(openDialog: MutableState<Boolean>,activity: Activity) {

    if (openDialog.value) {
        Dialog(
            onDismissRequest = { openDialog.value = false },
            content = {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column {
                        Text(
                            text = "按文件名称排序",
                            modifier = Modifier
                                .clickable {
                                    Toast
                                        .makeText(activity, "功能暂未实现", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(start = 40.dp, end = 40.dp, top = 25.dp, bottom = 15.dp)
                        )
                        Text(
                            text = "按修改时间排序",
                            modifier = Modifier
                                .clickable {
                                    Toast
                                        .makeText(activity, "功能暂未实现", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(vertical = 20.dp, horizontal = 40.dp)
                        )
                        Text(
                            text = "按文件大小排序",
                            modifier = Modifier
                                .clickable {
                                    Toast
                                        .makeText(activity, "功能暂未实现", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                .padding(start = 40.dp, end = 40.dp, top = 15.dp, bottom = 25.dp)
                        )
                    }
                }

            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainUI(viewModel: MainViewModel, activity: Activity) {
    val listState = rememberLazyListState()
    // Remember a CoroutineScope to be able to launch
    val coroutineScope = rememberCoroutineScope()
    val openDialog = remember {
        mutableStateOf(false)
    }

    val openMoreDialog = remember {
        mutableStateOf(false)
    }
    val openDownloadDialog = remember {
        mutableStateOf(false)
    }
    val filename = remember {
        mutableStateOf("")
    }
    val fileId = remember {
        mutableStateOf("")
    }
    val filetype = remember {
        mutableStateOf(0)
    }
    val isLoading by viewModel.isLoading.observeAsState()
    val isEmpty by viewModel.isEmpty.observeAsState()
    val isError by viewModel.isError.observeAsState()
    val progress by viewModel.progress.observeAsState()
    Column {
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .background(Color(0xfffbfbfb))

        ) {
            Image(

                // painter = painterResource(id = R.drawable.back),
                imageVector = Icons.Filled.Backup,
                contentDescription = "",
                colorFilter = ColorFilter.lighting(
                    Color(0xffbbbbbb),
                    Color(0xffbbbbbb)
                ),
                modifier = Modifier
                    .padding(start = 30.dp)
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {},
                            onTap = {

                            })
                    }

            )
            Text(
                text = "我的云盘",
                modifier = Modifier
                    .align(Alignment.Center),
                fontSize = 20.sp,
                color = Color(0xff333333)
            )
            var enable by remember {
                mutableStateOf(true)
            }
            var ro by remember {
                mutableStateOf(0f)
            }
            val change = animateFloatAsState(
                targetValue = if (enable) ro else ro,
                animationSpec = tween(
                    durationMillis = 500,
                    delayMillis = 0,
                    easing = LinearEasing
                )
            )
            Column(
                modifier = Modifier
                    .padding(end = 150.dp)
                    .align(Alignment.CenterEnd)
            ) {
                CircularProgressIndicator(
                    progress = progress!!,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(18.dp),
                    color = BLUE2,
                    strokeWidth = 15.dp
                )
//                Text(
//                    text = if (progress == 0f) "" else "下载进度：${progress}",
//                    color = Color(0xff888888),
//                    fontSize = 5.sp,
//                    textAlign = TextAlign.Center
//                )

            }

            Image(
                painter = painterResource(id = R.drawable.refresh),
                modifier = Modifier
                    .padding(end = 95.dp)
                    .align(Alignment.CenterEnd)
                    .padding(10.dp)
                    .fillMaxHeight()
                    .rotate(change.value)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {

                            },
                            onTap = {
                                ro += 360f
                                var intent = Intent(LIST_ACTION)
                                intent.putExtra("path", viewModel.getPath())
                                activity.sendBroadcast(intent)
                                viewModel.isError.value = false
                                viewModel.isLoading.value = true
                                viewModel.isEmpty.value = false
                                viewModel.delayError(5000)
                                enable = !enable
                                viewModel.isRefreshing = true
                            })
                    },
                contentDescription = ""
            )
            Image(
                painter = painterResource(id = R.drawable.sort),
                modifier = Modifier
                    .padding(end = 50.dp)
                    .align(Alignment.CenterEnd)
                    .padding(10.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {},
                            onTap = {
                                openDialog.value = true
                            })
                    },
                contentDescription = ""
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
                                openMoreDialog.value = true
                            })
                    },
                contentDescription = ""
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 15.dp, bottom = 5.dp)
                .height(35.dp)
                .padding(start = 20.dp)
        ) {
            Text(
                text = "我的云盘",
                Modifier
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xffeeeeee))
                    .padding(horizontal = 20.dp, vertical = 5.dp)
                    .clickable {
                        viewModel.dirList.clear()
                        var intent = Intent(LIST_ACTION)
                        intent.putExtra("path", viewModel.getPath())
                        activity.sendBroadcast(intent)
                    },
                fontSize = 13.sp,
                color = Color(0xff777777)

            )

            Image(
                painter = painterResource(id = R.drawable.turn_right),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(vertical = 8.dp),
                contentDescription = ""
            )
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides null
            ) {
                LazyRow(state = listState) {
                    itemsIndexed(viewModel.dirList) { index, name ->
                        DirItem(
                            name = name,
                            viewModel.dirList.size == index.plus(1),
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {},
                                    onTap = {
                                        var len = viewModel.dirList.size
                                        for (i in index until len - 1) {
                                            viewModel.dirList.removeLast()
                                        }
                                        viewModel.isLoading.value = true
                                        viewModel.delayError(5000)
                                        var intent = Intent(LIST_ACTION)
                                        intent.putExtra("path", viewModel.getPath())
                                        activity.sendBroadcast(intent)
                                    })
                            }
                        )
                    }
                }
            }
        }

        if (isLoading!!) {
            LazyColumn(Modifier.padding(top = 30.dp)) {
                items(6) {
                    Column() {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(horizontal = 10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.file),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .padding(
                                            start = 40.dp,
                                            top = 15.dp,
                                            bottom = 15.dp,
                                            end = 15.dp
                                        )
                                        .size(45.dp)
                                        .placeholder(
                                            true,
                                            highlight = PlaceholderHighlight.fade(),
                                            //color = Color(0xffeeeeee)
                                        )
                                )
                                Text(
                                    text = "",
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .padding(start = 10.dp, end = 50.dp)
                                        .fillMaxWidth()
                                        .placeholder(
                                            true,
                                            highlight = PlaceholderHighlight.fade(),
                                        )
                                        .align(Alignment.CenterVertically)

                                )

                            }
                        }
                    }
                }
            }
        } else {

            if (!isEmpty!! && !isError!!) {
                CompositionLocalProvider(
                    LocalOverscrollConfiguration provides null
                ) {
                    LazyColumn {
                        itemsIndexed(viewModel.fileList) { index, file ->
                            ItemFile(file = file, modifier = Modifier
                                .clickable {
                                    if (file.type == 1) {
                                        var intent = Intent(LIST_ACTION)
                                        intent.putExtra("path", file.path + file.name)
                                        activity.sendBroadcast(intent)
                                        viewModel.dirList.add(file.name)
                                        if (viewModel.dirList.size != 0) {
                                            coroutineScope.launch {
                                                listState.scrollToItem(
                                                    index = viewModel.dirList.size.minus(
                                                        1
                                                    )
                                                )
                                            }
                                        }
                                        viewModel.isLoading.value = true
                                        viewModel.delayError(5000)
                                    } else {
                                        openDownloadDialog.value = true
                                        filename.value = file.name
                                        fileId.value = file.id
                                        filetype.value = 0

                                    }
                                }

                            )
                        }
                    }
                }
            }
        }
        EmptyPage(viewModel)
        ErrorPage(viewModel = viewModel,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {},
                    onTap = {
                        viewModel.isLoading.value = true
                        viewModel.isError.value = false
                        viewModel.delayError(5000)
                        viewModel.connectService?.listDir(viewModel.getPath())
                    })
            })
    }

    //AddSortDialog(openDialog = openDialog,activity)
    AddMoreDialog(openDialog = openMoreDialog, activity = activity, viewModel)
    AddDownloadDialog(
        openDialog = openDownloadDialog,
        name = filename,
        viewModel = viewModel,
        id = fileId,
        type = filetype
    )
}

@Composable
fun AddDownloadDialog(
    name: MutableState<String>,
    openDialog: MutableState<Boolean>,
    viewModel: MainViewModel,
    id: MutableState<String>,
    type: MutableState<Int>
) {
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = { Text(text = "提示") },
            //shape = RoundedCornerShape(10.dp),
            text = {
                Text(
                    text = "下载${name.value}？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    openDialog.value = false
                    viewModel.connectService?.download(
                        name = name.value,
                        id = id.value,
                        type = type.value
                    )
                }) {
                    Text(text = "确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog.value = false }) {
                    Text(text = "取消")
                }
            })
    }

}

@Composable
fun ErrorPage(modifier: Modifier, viewModel: MainViewModel) {
    if (viewModel.isError.value!!) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth()


        ) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Image(
                    painter = painterResource(id = R.drawable.empty2),
                    contentDescription = "",
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .size(300.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "连接错误或网络错误\n请查看连接和网络后重试",
                    //text="当前目录为空",
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    color = Color(0xffaaaaaa),
                    textAlign = TextAlign.Center
                )

            }

        }
    }
}

@Composable
fun EmptyPage(viewModel: MainViewModel) {
    if (viewModel.isEmpty.value!!) {

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()

        ) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Image(
                    painter = painterResource(id = R.drawable.empty2),
                    contentDescription = "",
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .size(300.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "当前目录为空",
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    color = Color(0xffaaaaaa),
                    textAlign = TextAlign.Center
                )

            }

        }
    }
}

@Composable
fun DirItem(name: String, isLastOne: Boolean, modifier: Modifier) {
    Row(modifier) {
        Text(
            text = name,
            Modifier
                .padding(top = if (isLastOne) 3.dp else 0.dp)
                .align(Alignment.CenterVertically)
                .clip(RoundedCornerShape(20.dp))
                .background(if (!isLastOne) Color(0xffeeeeee) else LIGHTYELLO)
                .padding(start = 15.dp, end = 15.dp, top = 3.dp, bottom = 4.dp),
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = if (isLastOne) YELLO else Color(0xff777777)
        )
        if (!isLastOne) {
            Image(
                painter = painterResource(id = R.drawable.turn_right),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(vertical = 8.dp),
                contentDescription = ""
            )
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview2() {
    var context = LocalContext.current
//    val app = remember {
//        context.applicationContext as Application
//    }
//    val vm = MainViewModel(app)
//    vm.initdata()
    NetDiskTheme {
        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .background(Color(0xfffbfbfb))

        ) {
            Image(

                // painter = painterResource(id = R.drawable.back),
                imageVector = Icons.Filled.Backup,
                contentDescription = "",
                colorFilter = ColorFilter.lighting(
                    Color(0xffbbbbbb),
                    Color(0xffbbbbbb)
                ),
                modifier = Modifier
                    .padding(start = 30.dp)
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .size(40.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {},
                            onTap = {

                            })
                    }

            )
            Text(
                text = "我的云盘",
                modifier = Modifier
                    .align(Alignment.Center),
                fontSize = 20.sp,
                color = Color(0xff333333)
            )
            var enable by remember {
                mutableStateOf(true)
            }
            var ro by remember {
                mutableStateOf(0f)
            }
            val change = animateFloatAsState(
                targetValue = if (enable) ro else ro,
                animationSpec = tween(
                    durationMillis = 500,
                    delayMillis = 0,
                    easing = LinearEasing
                )
            )
            Column(
                modifier = Modifier
                    .padding(end = 150.dp)
                    .align(Alignment.CenterEnd)


            ) {
                CircularProgressIndicator(
                    progress = 0.5f,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterHorizontally)

                    ,
                    color = BLUE2,
                    strokeWidth = 15.dp,

                )
//                Text(
//                    text = "下载进度：20%",
//                    color = Color(0xff888888),
//                    fontSize = 5.sp,
//                    textAlign = TextAlign.Center,
//
//                )

            }

            Image(
                painter = painterResource(id = R.drawable.refresh),
                modifier = Modifier
                    .padding(end = 95.dp)
                    .align(Alignment.CenterEnd)
                    .padding(10.dp)
                    .fillMaxHeight()
                    .rotate(change.value)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {

                            },
                            onTap = {

                            })
                    },
                contentDescription = ""
            )
            Image(
                painter = painterResource(id = R.drawable.sort),
                modifier = Modifier
                    .padding(end = 50.dp)
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
    }

}

@Composable
fun PlaceholderDefaults() {
    Column() {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 15.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.file), contentDescription = "",
                    modifier = Modifier
                        .padding(10.dp)
                        .placeholder(
                            true, highlight = PlaceholderHighlight.fade()
                        )
                )
                Text(
                    text = "",
                    fontSize = 18.sp,

                    modifier = Modifier
                        .padding(start = 10.dp)
                        .fillMaxWidth()
                        .placeholder(
                            true,
                            highlight = PlaceholderHighlight.fade(),
                        )
                        .align(Alignment.CenterVertically)

                )

            }
        }
    }
}

@Composable
fun ItemFile(file: FileItem, modifier: Modifier) {

    Column(
        modifier = modifier

    ) {

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
                    Text(
                        text = file.time,
                        textAlign = TextAlign.Center,
                        color = Color(0xffaaaaaa),
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1

                    )


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

data class FileItem(
    var name: String, var time: String, var type: Int,
    var id: String,
    var path: String,
    var pic: String,
    var size: Long,
    var date: String,
    var create_date: String,
    var source_enabled: Boolean
)
