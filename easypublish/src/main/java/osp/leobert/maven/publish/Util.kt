package osp.leobert.maven.publish

import org.gradle.api.Project

/**
 * <p><b>Package:</b> osp.leobert.maven.publish </p>
 * <p><b>Classname:</b> Util </p>
 * Created by leobert on 2021/5/10.
 */
object Util {
    fun isAndroidModule(project: Project): Boolean {
        val ret = project.hasProperty("android")
        if (ret) {
            Logger.info("judge ${project.name} is Android Module")
        } else {
            Logger.info("judge ${project.name} is not Android Module")
        }

        return ret
    }
}

inline fun <reified R> Any?.takeIfInstance(): R? {
    if (this is R) return this
    return null.apply {
        Logger.error(
            "want get ${R::class.java.name} but got ${this@takeIfInstance?.javaClass?.name}"
        )
    }
}