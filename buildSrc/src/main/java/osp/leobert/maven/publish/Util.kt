package osp.leobert.maven.publish

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.internal.DefaultPublishingExtension
import org.gradle.plugins.signing.SigningExtension
import java.io.FileInputStream
import java.io.InputStream
import java.util.*

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

    //publishing
    fun publishingExtension(project: Project): PublishingExtension? {
        return project.extensions.findByType(PublishingExtension::class.java)
    }

    fun signingExtension(project: Project): SigningExtension? {
        return project.extensions.findByType(SigningExtension::class.java)
    }

    fun <T> require(name: String, t: T?): T? {
        return t.apply {
            if (this == null) {
                Logger.error("need $name but it is null")
            }
        }
    }

    fun localProperty(project: Project): Properties? {
        var inputStream: InputStream? = null
        try {
            val properties = Properties()
            val localProperties = project.rootProject.file("local.properties")
            inputStream = FileInputStream(localProperties)
            properties.load(inputStream)

            return properties
        } catch (e: Exception) {
            Logger.error("error when load localProperty", e)
            return null
        } finally {
            try {
                inputStream?.close()
            } catch (e2: Exception) {

            }
        }
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