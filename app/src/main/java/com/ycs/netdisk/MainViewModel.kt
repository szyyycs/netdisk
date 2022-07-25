package com.ycs.netdisk

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * <pre>
 *     author : yangchaosheng
 *     e-mail : yangchaosheng@hisense.com
 *     time   : 2022/07/21
 *     desc   :
 * </pre>
 */
class MainViewModel() :ViewModel() {
    var fileList: MutableList<FileItem> = mutableStateListOf()
    var dirList: MutableList<String> = mutableStateListOf()
    var isEmpty = MutableLiveData(false)

    fun initdata(){
        fileList.add(FileItem("Baidu","5天前",1))
        fileList.add(FileItem("Tencent","21小时前",1))
        fileList.add(FileItem("youku","5天前",1))
        fileList.add(FileItem("xl","21小时前",1))
        fileList.add(FileItem("Xioami","5天前",1))
        fileList.add(FileItem("Wechat","21小时前",1))
        fileList.add(FileItem("UC","5天前",1))
        fileList.add(FileItem("test","21小时前",1))
        fileList.add(FileItem("test.pdf","21小时前",2))
        fileList.add(FileItem("test.png","21小时前",3))
        fileList.add(FileItem("test.xlsx","21小时前",5))
        fileList.add(FileItem("test.zip","21小时前",6))
        fileList.add(FileItem("1.text","43分钟前",7))
        fileList.add(FileItem("22.word","34秒前",4))
    }
}