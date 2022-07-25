package com.ycs.netdisk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.ycs.netdisk.ui.theme.LIGHTYELLO
import com.ycs.netdisk.ui.theme.NetDiskTheme
import com.ycs.netdisk.ui.theme.YELLO
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(MainViewModel::class.java)
    }
    private val context by lazy {
        this@MainActivity
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initdata()
        setContent {
            NetDiskTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainUI(viewModel,context)
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
}

@Composable
fun AddMoreDialog(openDialog: MutableState<Boolean>, activity : Activity) {
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
                                activity.startActivity(Intent(activity,LoginActivity::class.java))
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
fun AddSortDialog(openDialog: MutableState<Boolean>) {

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

                                }
                                .padding(start = 40.dp, end = 40.dp, top = 25.dp, bottom = 15.dp)
                        )
                        Text(
                            text = "按修改时间排序",
                            modifier = Modifier
                                .clickable {

                                }
                                .padding(vertical = 20.dp, horizontal = 40.dp)
                        )
                        Text(
                            text = "按文件大小排序",
                            modifier = Modifier
                                .clickable {

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
fun MainUI(viewModel: MainViewModel,activity:Activity) {
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
    val name = remember {
        mutableStateOf("")
    }
    val isEmpty by viewModel.isEmpty.observeAsState()
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
                .padding(top = 10.dp)
                .height(35.dp)
                .padding(start = 20.dp)
        ) {
            Text(
                text = "我的云盘",
                Modifier
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xffeeeeee))
                    .padding(horizontal = 20.dp, vertical = 5.dp),
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
                LocalOverScrollConfiguration provides null
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
                                            var len=viewModel.dirList.size
                                            for(i in index until len-1){
                                                viewModel.dirList.removeLast()
                                            }
                                        })
                                }
                        )
                    }
                }
            }
        }
        if (!isEmpty!!) {
            CompositionLocalProvider(
                LocalOverScrollConfiguration provides null
            ) {
                LazyColumn {
                    itemsIndexed(viewModel.fileList) { index, file ->
                        ItemFile(file = file, modifier = Modifier
                            .clickable {
                                if (file.type == 1) {
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
                                } else if (file.type == 2) {
                                    viewModel.isEmpty.value = true
                                } else {
                                    openDownloadDialog.value = true
                                    name.value = file.name
                                }
                            })
                    }
                }
            }
        }
        EmptyPage(viewModel)

    }
    AddSortDialog(openDialog = openDialog)
    AddMoreDialog(openDialog = openMoreDialog, activity = activity)
    AddDownloadDialog(openDialog = openDownloadDialog, name = name, viewModel = viewModel)
}

@Composable
fun AddDownloadDialog(
    name: MutableState<String>,
    openDialog: MutableState<Boolean>,
    viewModel: MainViewModel
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
fun EmptyPage(viewModel: MainViewModel) {
    if (viewModel.isEmpty.value!!) {

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Image(
                    painter = painterResource(id = R.drawable.empty),
                    contentDescription = "",
                    modifier = Modifier
                        .align(alignment = Alignment.CenterHorizontally)
                        .size(100.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "没有文件",
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    color = Color(0xffaaaaaa)
                )

            }

        }
    }
}

@Composable
fun DirItem(name: String, isLastOne: Boolean, modifier: Modifier) {
    Row (modifier){
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
    val app = remember {
        context.applicationContext as Application
    }
    val vm = MainViewModel()
    vm.initdata()
    NetDiskTheme {
        MainUI(viewModel = vm,context as Activity)
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
                    modifier = Modifier.padding(if (file.type == 1) 10.dp else 13.dp)
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
                        fontSize = 18.sp
                    )
                    Text(
                        text = file.time,
                        textAlign = TextAlign.Center,
                        color = Color(0xffaaaaaa),
                        fontSize = 12.sp
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

data class FileItem(var name: String, var time: String, var type: Int)