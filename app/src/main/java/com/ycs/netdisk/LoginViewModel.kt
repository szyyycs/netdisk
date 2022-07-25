package com.ycs.netdisk

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tencent.mmkv.MMKV

/**
 * <pre>
 *     author : yangchaosheng
 *     e-mail : yangchaosheng@hisense.com
 *     time   : 2022/07/14
 *     desc   :
 * </pre>
 */
class LoginViewModel : ViewModel() {
    var accountList: MutableList<Account> = mutableStateListOf()
    var isExpanded = MutableLiveData<Boolean>(false)
    var isLogin = MutableLiveData(false)
    var isAddAccountEmpty=MutableLiveData(false)
    var isDelete = MutableLiveData(false)
    val defaultMMKV: MMKV by lazy {
        MMKV.defaultMMKV()
    }
    var indexShow = 0
    val inputIP = MutableLiveData("")
    val inputName = MutableLiveData("")
    val inputPwd = MutableLiveData("")
    init {

    }
    fun initData() {
        accountList.clear()
        val keys = defaultMMKV.allKeys()
        if (keys == null || keys.isEmpty()) {

        } else {
            for (name in keys) {
                defaultMMKV.decodeString(name)?.let { Account(it, name) }
                    ?.let { accountList.add(it) }
            }

        }


        if (accountList.size <= 0) {
            isAddAccountEmpty.value = true
            isLogin.value = true
            isExpanded.value = false
        }else{
            inputIP.value=accountList[0].ip
            inputName.value=accountList[0].name
        }

    }
    fun checkIp():Boolean{
        if(inputIP.value!!.isBlank()) return false
        val array=inputIP.value!!.split(".")
        if(array.size!=4) return false
        try {
            for(i in array){
                if(i.toInt()>255){
                    return false
                }
            }
        }catch (e:Exception){
            return false
        }

        return true
    }

}