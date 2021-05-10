package osp.leobert.maven.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import osp.leobert.maven.publish.bean.EasyPublish

class EasyPublishPlugin : Plugin<Project> {

    var easyPublish = EasyPublish()

    override fun apply(project: Project) {
        Logger.info("hello,I'am easy-publish")
        easyPublish = project.extensions.create("EasyPublish", EasyPublish::class.java)

        project.afterEvaluate { _ ->
            Logger.info("find config:$easyPublish")

            project.apply {
                it.plugin("maven-publish")
                it.plugin("signing")
                Logger.info("apply plugin 'maven-publish' and 'signing' to ${project.name}")
            }

            val isAndroid = Util.isAndroidModule(project)

            if (isAndroid) {
                addJavaDocTaskForAndroid(project)
            }

            addSourceJar(project, easyPublish)
            addJavadocJar(project)

            project.tasks.withType(Javadoc::class.java) {
                it.options.encoding = "UTF-8"
//            it.options.addStringOption('Xdoclint:none', '-quiet')
//            options.addStringOption('encoding', 'UTF-8')
//            options.addStringOption('charSet', 'UTF-8')
            }
        }
    }

    private fun addJavadocJar(project: Project) {
        //task javadocJar(type: Jar, dependsOn: javadoc) {
        //        classifier = 'javadoc'
        //        from javadoc.destinationDir
        //    }
        val javadoc = project.tasks.findByName("javadoc").takeIfInstance<Javadoc>()

        try {
            project.tasks.create(
                "javadocJar", Jar::class.java
            ).let { task ->
                task.classifier = "javadoc"
                task.from(javadoc?.destinationDir)
                task.setDependsOn(arrayListOf(javadoc))
            }
        } catch (e: Exception) {
            Logger.error(e.message ?: "empty message", e)
        }
    }

    private fun addSourceJar(project: Project, easyPublish: EasyPublish) {
        try {
            project.tasks.create(
                "sourcesJar", Jar::class.java
            ).takeIfInstance<AbstractArchiveTask>()?.let { sourceJarTask ->
                sourceJarTask.classifier = "sources"
                sourceJarTask.from(
                    easyPublish.sourceSet
                        ?: throw IllegalArgumentException("must config sourceSet in EasyPublish")
                )
            }
        } catch (e: Exception) {
            Logger.error(e.message ?: "empty message", e)
        }

//task sourcesJar(type: Jar) {
//        classifier = 'sources'
//        from android.sourceSets.main.java.srcDirs
//    }
//
//    task javadoc(type: Javadoc) {
//        options.encoding "UTF-8"
//        options.charSet 'UTF-8'
//        source = android.sourceSets.main.java.srcDirs
//        classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
//    }
    }

    private fun addJavaDocTaskForAndroid(project: Project) {
        try {
            project.tasks.create(
                "javadoc", Javadoc::class.java
            ).let {
                it.options.encoding("UTF-8")

                easyPublish.docClassPathAppend?.let { docClassPathAppend ->
                    it.classpath.plus(docClassPathAppend)
                }
                easyPublish.sourceSet
                    ?: throw IllegalArgumentException("must config sourceSet in EasyPublish")
                easyPublish.docExcludes?.let { exclude ->
                    it.exclude(exclude)
                    exclude.forEach { e ->
                        Logger.info("exclude doc of $e")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error(e.message ?: "empty message", e)
        }

    }
}