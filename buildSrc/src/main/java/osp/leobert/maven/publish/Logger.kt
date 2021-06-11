package osp.leobert.maven.publish

/**
 * <p><b>Package:</b> osp.leobert.maven.publish </p>
 * <p><b>Classname:</b> Logger </p>
 * Created by leobert on 2021/5/10.
 */
object Logger {

    private const val plugin_name = "[easy publish]"

    private const val info_level_tag = "[info]"

    private const val error_level_tag = "[error]"

    fun info(msg: String) {
        print(info_level_tag, msg, null)
    }

    fun error(msg: String, throwable: Throwable? = null) {
        print(error_level_tag, msg, throwable)
    }

    fun print(level: String, msg: String, throwable: Throwable? = null) {
        println("$plugin_name $level $msg")
        throwable?.printStackTrace()
    }
}