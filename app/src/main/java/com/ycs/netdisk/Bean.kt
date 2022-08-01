package com.ycs.netdisk

/**
 * <pre>
 *     author : yangchaosheng
 *     e-mail : yangchaosheng@hisense.com
 *     time   : 2022/07/29
 *     desc   :
 * </pre>
 */
class Bean {
    data class File(
        var id: String,
        var name: String,
        var path: String,
        var pic: String,
        var size: Long,
        var type: String,
        var date: String,
        var create_date:String,
        var source_enabled:Boolean
    )
}