package com.ycs.netdisk


import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.tencent.mmkv.MMKV
import com.ycs.netdisk.ui.theme.*

class LoginActivity : ComponentActivity() {

    private val viewModel by lazy {
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(LoginViewModel::class.java)
    }
    private val context by lazy {
        this@LoginActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLibrary()
        initView()
        viewModel.initData()
    }

    private fun initLibrary() {
        MMKV.initialize(this)
    }


    private fun initView() {
        setContent {
            NetDiskTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LoginUI(viewModel, context = context)
                }
            }
        }
        setStatusBarColor(R.color.white)
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

    private fun setDarkStatusBar() {
        val flags = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.decorView.systemUiVisibility = flags xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    override fun onBackPressed() {
        if (viewModel.isDelete.value == true) {
            viewModel.isDelete.value = false
            return
        }
        if (viewModel.isLogin.value!! && !viewModel.isAddAccountEmpty.value!!) {
            viewModel.isLogin.value = false
            if (viewModel.inputName.value!!.isBlank() || viewModel.inputIP.value!!.isBlank()) {
                viewModel.inputName.value = viewModel.accountList[viewModel.indexShow].name
                viewModel.inputIP.value = viewModel.accountList[viewModel.indexShow].ip
            }
        } else if (viewModel.isExpanded.value!! && !viewModel.isAddAccountEmpty.value!!) {
            viewModel.isExpanded.value = false
        } else {
            super.onBackPressed()
        }

    }


}

@Composable
fun addDialog(
    name: MutableState<String>,
    openDialog: MutableState<Boolean>,
    viewModel: LoginViewModel
) {
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = { Text(text = "提示") },
            //shape = RoundedCornerShape(10.dp),
            text = {
                Text(
                    text = "确认删除${name.value}账号信息？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    openDialog.value = false
                    viewModel.defaultMMKV.remove(name.value)
                    viewModel.initData()
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
fun LoginUI(viewModel: LoginViewModel, context: Activity) {
    val login by viewModel.isLogin.observeAsState()
    val isExpanded by viewModel.isExpanded.observeAsState()
    val isDelete by viewModel.isDelete.observeAsState()
    val isAddAccountEmpty by viewModel.isAddAccountEmpty.observeAsState()
    var indexShow = 0
    var height = animateDpAsState(targetValue = if (login!!) 250.dp else 95.dp)
    var boxHeight = animateDpAsState(targetValue = if (login!!) 250.dp else 80.dp)
    var ipcheck by remember {
        mutableStateOf(false)
    }
    var namecheck by remember {
        mutableStateOf(false)
    }

    var openDialog = remember {
        mutableStateOf(false)
    }
    var name = remember {
        mutableStateOf("")
    }
    val inputIP by viewModel.inputIP.observeAsState()
    val inputName by viewModel.inputName.observeAsState()
    val inputPwd by viewModel.inputPwd.observeAsState()
    var shake by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = shake, label = "shake")
    val shakeOffset by transition.animateDp(
        transitionSpec = {
            keyframes {
                //持续时间，
                durationMillis = 300
                0.dp at 0 //如果觉得这个动画太硬朗，在这里是可以制定插值函数的。比如下面的
                (-20).dp at 25 with LinearOutSlowInEasing //自己翻译吧
                0.dp at 50
                20.dp at 75
                0.dp at 100
                (-10).dp at 125
                0.dp at 150
                10.dp at 175
                0.dp at 200
                (-5).dp at 225
                0.dp at 250
                5.dp at 275
                0.dp at 300
            }
        }, label = "shakeOffset"
    ) { state ->
        if (state) 0.dp else 0.dp
    }
    var shake2 by remember { mutableStateOf(false) }
    val transition2 = updateTransition(targetState = shake2, label = "shake")
    val shakeOffset2 by transition2.animateDp(
        transitionSpec = {
            keyframes {
                //持续时间，
                durationMillis = 300
                0.dp at 0 //如果觉得这个动画太硬朗，在这里是可以制定插值函数的。比如下面的
                (-20).dp at 25 with LinearOutSlowInEasing //自己翻译吧
                0.dp at 50
                20.dp at 75
                0.dp at 100
                (-10).dp at 125
                0.dp at 150
                10.dp at 175
                0.dp at 200
                (-5).dp at 225
                0.dp at 250
                5.dp at 275
                0.dp at 300
            }
        }, label = "shakeOffset"
    ) { state ->
        if (state) 0.dp else 0.dp
    }
    var shake3 by remember { mutableStateOf(false) }
    val transition3 = updateTransition(targetState = shake3, label = "shake")
    val shakeOffset3 by transition3.animateDp(
        transitionSpec = {
            keyframes {
                //持续时间，
                durationMillis = 300
                0.dp at 0 //如果觉得这个动画太硬朗，在这里是可以制定插值函数的。比如下面的
                (-20).dp at 25 with LinearOutSlowInEasing //自己翻译吧
                0.dp at 50
                20.dp at 75
                0.dp at 100
                (-10).dp at 125
                0.dp at 150
                10.dp at 175
                0.dp at 200
                (-5).dp at 225
                0.dp at 250
                5.dp at 275
                0.dp at 300
            }
        }, label = "shakeOffset"
    ) { state ->
        if (state) 0.dp else 0.dp
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(GREY)
    ) {
        //返回键toolbar
        Row(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .background(Color.White)
        ) {
            AnimatedVisibility(
                visible = (login!! || isExpanded!!) && !isAddAccountEmpty!!
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.CenterVertically)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {},
                                onTap = {
                                    if (isDelete == true) {
                                        viewModel.isDelete.value = false
                                        return@detectTapGestures
                                    }
                                    viewModel.isLogin.value = false
                                    viewModel.isExpanded.value = false
                                    if (inputName!!.isBlank() || inputIP!!.isBlank()) {
                                        viewModel.inputName.value =
                                            viewModel.accountList[indexShow].name
                                        viewModel.inputIP.value =
                                            viewModel.accountList[indexShow].ip
                                    }
                                })
                        }

                )
            }
            Text(

                text = if (!isExpanded!!) "" else if (!isDelete!!) "管理" else "取消",
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 20.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {},
                            onTap = {
                                viewModel.isDelete.value = !isDelete!!
                            })
                    },
                textAlign = TextAlign.End,
                color = Color(0xff666666),
                fontSize = 14.sp
            )

        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        ) {
            //title
            item {
                Column() {

                    Text(
                        text = if (!isExpanded!!) "欢迎使用装备信息软件" else "选择一个账号进行登录",
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 40.dp, bottom = 40.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center

                    )
                }

            }

            //列表
            itemsIndexed(viewModel.accountList) { index, message ->
                AnimatedVisibility(visible = isExpanded!!) {
                    AccountItem(
                        account = message,
                        index,
                        viewModel = viewModel,
                        modifier = Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {},
                                onTap = {
                                    if (isDelete == true) {
                                        name.value = viewModel.accountList[index].name
                                        openDialog.value = true
                                        return@detectTapGestures
                                    }
                                    viewModel.isLogin.value = true
                                    viewModel.isExpanded.value = false
                                    indexShow = index
                                    viewModel.indexShow = index
                                    viewModel.inputIP.value = viewModel.accountList[indexShow].ip
                                    viewModel.inputName.value =
                                        viewModel.accountList[indexShow].name
                                    viewModel.inputPwd.value = ""
                                    ipcheck=false
                                })
                        })
                }
            }
            //添加其他账号
            item {

                AnimatedVisibility(visible = isExpanded!! && !isDelete!!) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 15.dp)
                            .clip(shape = RoundedCornerShape(15.dp))
                            .background(Color.White)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {},
                                    onTap = {
                                        viewModel.isExpanded.value = false
                                        viewModel.isLogin.value = true
                                        viewModel.inputIP.value = ""
                                        viewModel.inputName.value = ""
                                        viewModel.inputPwd.value = ""
                                        viewModel.isAddAccountEmpty.value = true
                                    })
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .size(50.dp)
                                    .background(Color(0xfff1f1f1))
                                    .align(Alignment.CenterVertically)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(35.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .align(alignment = Alignment.Center)

                                )
                            }
                            Text(
                                text = "添加其他账号",
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(start = 10.dp)
                            )

                        }

                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

            }

            //默认账号

            item {
                AnimatedVisibility(visible = !isExpanded!!) {
                    Column(modifier = Modifier
                        .height(height.value)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                // 长按事件
                                onLongPress = {},
                                // 点击事件
                                onTap = {
                                    viewModel.isLogin.value = true
                                    ipcheck=false
                                })
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()

                                .height(boxHeight.value)
                                .padding(horizontal = 15.dp)
                                .clip(shape = RoundedCornerShape(15.dp))
                                .background(Color.White)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp)
                                    .fillMaxHeight()
                            ) {
                                AnimatedVisibility(visible = !login!!) {
                                    Image(
                                        painter = painterResource(id = R.drawable.icon),
                                        contentDescription = "",
                                        modifier = Modifier
                                            .padding(top = 19.dp, bottom = 19.dp)
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(10.dp))

                                    )
                                }
                                Column(

                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),

                                    ) {

                                    AnimatedVisibility(visible = !login!!) {
                                        Text(
                                            text = inputIP!!,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier

                                                .padding(top = 13.dp, start = 20.dp),
                                            color = Color(0xff666666),
                                            fontSize = 18.sp
                                        )
                                    }
                                    AnimatedVisibility(visible = !login!!) {
                                        Text(
                                            text = inputName!!,
                                            textAlign = TextAlign.Center,
                                            color = Color(0xffaaaaaa),
                                            modifier = Modifier
                                                .padding(start = 20.dp, top = 5.dp)
                                        )
                                    }

                                    AnimatedVisibility(
                                        visible = login!!,
                                        modifier = Modifier.offset(shakeOffset)
                                    ) {
                                        CustomEdit(
                                            text = inputIP!!,
                                            onValueChange = {
                                                viewModel.inputIP.value = it
                                                ipcheck=false
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 16.dp,
                                                    top = 25.dp,
                                                    end = 16.dp
                                                )
                                                .height(50.dp)
                                                .background(
                                                    Color(0xFFEEEEEE),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 16.dp),
                                            hint = "请输入IP地址",
                                            startIcon = R.drawable.ip,
                                            iconSpacing = 16.dp,
                                            textStyle = Typography.bodyMedium,

                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        )

                                    }
                                    AnimatedVisibility(visible = ipcheck) {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "IP格式错误",
                                                color = Color(0xff888888),
                                                fontSize = 12.sp,
                                                modifier = Modifier
                                                    .align(Alignment.CenterEnd)
                                                    .padding(horizontal = 20.dp, vertical = 10.dp),

                                                )
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = login!!,
                                        modifier = Modifier.offset(shakeOffset2)
                                    ) {
                                        CustomEdit(
                                            text = inputName!!,
                                            onValueChange = {
                                                viewModel.inputName.value = it
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 16.dp,
                                                    top = if(!ipcheck)20.dp else 0.dp,
                                                    end = 16.dp
                                                )
                                                .height(50.dp)
                                                .background(
                                                    Color(0xFFEEEEEE),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 16.dp),
                                            hint = "请输入用户名",
                                            startIcon = R.drawable.name,
                                            iconSpacing = 14.dp,
                                            iconPadding = 8.dp,
                                            textStyle = TextStyle(color = Color(0xff666666)),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = login!!,
                                        modifier = Modifier.offset(shakeOffset3)
                                    ) {
                                        CustomEdit(
                                            text = inputPwd!!,
                                            onValueChange = {
                                                viewModel.inputPwd.value = it
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    start = 16.dp,
                                                    top = 20.dp,
                                                    end = 16.dp
                                                )
                                                .height(50.dp)
                                                .background(
                                                    Color(0xFFEEEEEE),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(horizontal = 16.dp),
                                            hint = "请输入密码",
                                            startIcon = R.drawable.password,
                                            iconSpacing = 16.dp,
                                            iconPadding = 12.dp,
                                            iconStartPadding = 5.dp,
                                            textStyle = TextStyle(color = Color(0xff666666)),
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            keyboardActions = KeyboardActions(onDone = null)
                                        )
                                    }


                                }
                            }

                        }

                    }
                }

            }

            //按钮
            item {
                Column() {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                if (!isExpanded!!) 100.dp
                                //else if (!isExpanded && !login) 200.dp
                                else 0.dp
                            )
                            .animateContentSize()
                    )
                    val horizontalGradientBrush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE2E2E2),
                            Color(0xFFE2E2E2)
                        )
                    )
                    val horizontalGradientBrushLogin = Brush.horizontalGradient(
                        colors = listOf(
                            BLUE,
                            BLUE2
                        )
                    )
                    if (!isExpanded!!) {

                        Button(modifier = Modifier
                            .padding(horizontal = 25.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .height(height = 40.dp)
                            .fillMaxWidth()
                            .background(brush = horizontalGradientBrushLogin)
                            .align(Alignment.CenterHorizontally),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            onClick = {
                                ipcheck=false;
                                if (!viewModel.isLogin.value!!) {
                                    viewModel.isLogin.value = true
                                } else {
                                    if (!viewModel.checkIp()) {
                                        shake = !shake
                                        ipcheck=true
                                    } else if (inputName!!.isBlank()) {
                                        shake2 = !shake2
                                    } else if (inputPwd!!.isBlank()) {
                                        shake3 = !shake3
                                    } else {
                                        viewModel.defaultMMKV.encode(inputName, inputIP)
                                        viewModel.accountList.add(Account(inputName!!, inputIP!!))
                                        Toast.makeText(context, "登录成功！", Toast.LENGTH_SHORT).show()
                                        context.startActivity(
                                            Intent(
                                                context,
                                                MainActivity::class.java
                                            )
                                        )
                                        context.finish()
                                    }

                                }
                            }) {
                            Text(
                                text = "登录",
                                color = Color.White
                            )

                        }

                        AnimatedVisibility(visible = !login!!) {
                            Button(modifier = Modifier
                                .padding(start = 25.dp, end = 25.dp, top = 20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .height(height = 40.dp)
                                .fillMaxWidth()
                                .background(brush = horizontalGradientBrush)
                                .align(Alignment.CenterHorizontally),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                onClick = {
                                    viewModel.isExpanded.value = !viewModel.isExpanded.value!!
                                }) {
                                Text(
                                    text = "登录其他账号",
                                    color = Color(0xff444444)
                                )

                            }
                        }
                    }
                    Spacer(
                        modifier = if (!isExpanded!!) Modifier.fillMaxHeight() else Modifier.height(
                            50.dp
                        )
                    )
                }

            }

        }
    }
    addDialog(name = name, openDialog = openDialog, viewModel)
}


@Composable
fun AccountItem(account: Account, index: Int, modifier: Modifier, viewModel: LoginViewModel) {

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 15.dp)
                .clip(shape = RoundedCornerShape(15.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .fillMaxHeight()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "",
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .align(alignment = Alignment.CenterVertically)
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                ) {
                    Text(
                        text = account.ip,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 13.dp, start = 20.dp),
                        color = Color(0xff666666),
                        fontSize = 18.sp
                    )
                    Text(
                        text = account.name,
                        textAlign = TextAlign.Center,
                        color = Color(0xffaaaaaa),
                        modifier = Modifier.padding(start = 20.dp, top = 5.dp)

                    )
                }
                AnimatedVisibility(
                    visible = viewModel.isDelete.value!!,
                    enter = expandVertically(
                        expandFrom = Alignment.Top
                    ),
                    exit = slideOutVertically() + shrinkVertically(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        Image(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "",
                            modifier = Modifier
                                .size(35.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .fillMaxWidth()
                                .align(alignment = Alignment.CenterEnd),
                            colorFilter = ColorFilter.lighting(
                                Color(0xffbbbbbb),
                                Color(0xffbbbbbb)
                            ),

                            )
                    }
                }


            }

        }
        Spacer(modifier = Modifier.height(15.dp))
    }


}


/**
 * @param hint: 空字符时的提示
 * @param startIcon: 左侧图标;  -1 则不显示
 * @param iconSpacing: 左侧图标与文字的距离; 相当于: drawablePadding
 */
@Composable
fun CustomEdit(
    text: String = "",
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    hint: String = "请输入",
    @DrawableRes startIcon: Int = -1,
    iconSpacing: Dp = 4.dp,
    iconPadding: Dp = 10.dp,
    iconStartPadding: Dp = 0.dp,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary)
) {
    // 焦点, 用于控制是否显示 右侧叉号
    var hasFocus by remember { mutableStateOf(false) }
    var icon by remember {
        mutableStateOf(true)
    }
    BasicTextField(
        value = text,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged { hasFocus = it.isFocused },
        singleLine = true,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (icon) visualTransformation else VisualTransformation.None,
        cursorBrush = cursorBrush,
        decorationBox = @Composable { innerTextField ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -1 不显示 左侧Icon
                if (startIcon != -1) {
                    Image(
                        painter = painterResource(id = startIcon),
                        contentDescription = null,
                        modifier = Modifier.padding(
                            top = iconPadding,
                            bottom = iconPadding,
                            start = iconStartPadding
                        )
                    )
                    Spacer(modifier = Modifier.width(iconSpacing))
                }

                Box(modifier = Modifier.weight(1f)) {
                    // 当空字符时, 显示hint
                    if (text.isEmpty())
                        Text(text = hint, color = Color(0xFFBBBBBB), style = textStyle)

                    // 原本输入框的内容
                    innerTextField()
                }

                // 存在焦点 且 有输入内容时. 显示叉号
                if (hasFocus && text.isNotEmpty()) {
                    if (keyboardOptions == KeyboardOptions(keyboardType = KeyboardType.Password)) {

                        Image(imageVector = if (!icon) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, // 清除图标
                            contentDescription = null,
                            colorFilter = ColorFilter.lighting(
                                Color(0xffbbbbbb),
                                Color(0xffbbbbbb)
                            ),
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {},
                                        onTap = {
                                            icon = !icon
                                        })
                                }

                        )
                    } else {
                        Image(imageVector = Icons.Filled.Clear, // 清除图标
                            contentDescription = null,
                            colorFilter = ColorFilter.lighting(
                                Color(0xffbbbbbb),
                                Color(0xffbbbbbb)
                            ),
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = {},
                                        onTap = {
                                            onValueChange.invoke("")
                                        })
                                }

                        )
                    }

                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    var context = LocalContext.current

    NetDiskTheme() {
        LoginUI(LoginViewModel(), context as Activity)
    }

}

data class Account(var ip: String, var name: String)


