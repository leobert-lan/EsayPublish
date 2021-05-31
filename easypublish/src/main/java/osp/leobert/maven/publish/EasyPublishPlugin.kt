package osp.leobert.maven.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.SigningExtension
import osp.leobert.maven.publish.bean.EasyPublish
import java.net.URI
import java.util.*

class EasyPublishPlugin : Plugin<Project> {

    var easyPublish = EasyPublish()
    var publishExtension: PublishingExtension? = null
    var signExtension: SigningExtension? = null

    override fun apply(project: Project) {
        Logger.info("hello,I'am easy-publish")
        easyPublish = project.extensions.create("EasyPublish", EasyPublish::class.java)

        val localProperties = Util.localProperty(project)

        project.afterEvaluate { _ ->
            Logger.info("find config:$easyPublish")


            project.apply {
                it.plugin("maven-publish")
                if (easyPublish.needSign)
                    it.plugin("signing")
                Logger.info("apply plugin 'maven-publish'${" and 'signing'".takeIf { easyPublish.needSign } ?: ""} to ${project.name}")
            }

            publishExtension = Util.publishingExtension(project)
            signExtension = Util.signingExtension(project)
            Logger.info("test:${publishExtension}${publishExtension?.publications}${publishExtension?.repositories}")


            project.container(EasyPublish::class.java)

            val isAndroid = Util.isAndroidModule(project)

            if (isAndroid) {
                addJavaDocTaskForAndroid(project)
            }

            val sourcesJarTask = addSourceJar(project, easyPublish)
            val javadocJarTask = addJavadocJar(project)

            project.tasks.withType(Javadoc::class.java) {
                it.options.encoding = "UTF-8"
//            it.options.addStringOption('Xdoclint:none', '-quiet')
//            options.addStringOption('encoding', 'UTF-8')
//            options.addStringOption('charSet', 'UTF-8')
            }

            configPublishing(localProperties, sourcesJarTask, javadocJarTask)


        }
    }

    private fun addJavadocJar(project: Project): Task? {
        val javadoc = project.tasks.findByName("javadoc").takeIfInstance<Javadoc>()

        return try {
            project.tasks.create(
                    "javadocJar", Jar::class.java
            ).apply {
                this.classifier = "javadoc"
                this.from(javadoc?.destinationDir)
                this.setDependsOn(arrayListOf(javadoc))
            }
        } catch (e: Exception) {
            Logger.error(e.message ?: "empty message", e)
            null
        }
    }

    private fun addSourceJar(project: Project, easyPublish: EasyPublish): Task? {
        return try {
            project.tasks.create(
                    "sourcesJar", Jar::class.java
            ).takeIfInstance<AbstractArchiveTask>()?.apply {
                this.classifier = "sources"
                this.from(
                        easyPublish.sourceSet
                                ?: throw IllegalArgumentException("must config sourceSet in EasyPublish")
                )
            }
        } catch (e: Exception) {
            Logger.error(e.message ?: "empty message", e)
            null
        }
    }

//    task javadoc(type: Javadoc) {
//        options.encoding "UTF-8"
//        options.charSet 'UTF-8'
//        source = android.sourceSets.main.java.srcDirs
//        classpath += project.files(android.getBootClasspath().join(File.pathSeparator))
//    }

    private fun addJavaDocTaskForAndroid(project: Project): Task? {
        return try {
            project.tasks.create(
                    "javadoc", Javadoc::class.java
            ).apply {
                this.options.encoding("UTF-8")

                easyPublish.docClassPathAppend?.let { docClassPathAppend ->
                    this.classpath.plus(docClassPathAppend)
                }
                easyPublish.sourceSet
                        ?: throw IllegalArgumentException("must config sourceSet in EasyPublish")
                easyPublish.docExcludes?.let { exclude ->
                    this.exclude(exclude)
                    exclude.forEach { e ->
                        Logger.info("exclude doc of $e")
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error(e.message ?: "empty message", e)
            null
        }

    }

    private fun configPublishing(
            properties: Properties?,
            sourcesJarTask: Task?,
            javadocJarTask: Task?
    ) {
        val publishingExtension = publishExtension
        if (publishingExtension == null) {
            Logger.error("publishingExtension is null something error")
            return
        }

        publishingExtension.publications {
            it.register<MavenPublication>(
                    "easyMavenPublish",
                    MavenPublication::class.java
            ) { publication ->

                publication.groupId = Util.require("EasyPublish.groupId", easyPublish.groupId)
                publication.artifactId =
                        Util.require("EasyPublish.artifactId", easyPublish.artifactId)
                publication.version = Util.require("EasyPublish.version", easyPublish.version)

                javadocJarTask?.let { t ->
                    Logger.info("add javadocJarTask to publication artifact")
                    publication.artifact(t)
                }
                sourcesJarTask?.let { t ->
                    Logger.info("add sourcesJarTask to publication artifact")
                    publication.artifact(t)
                }
                easyPublish.artifactsAppend?.forEach { a ->
                    Logger.info("add $a to publication artifact")
                    publication.artifact(a)
                }

                //pom config
                publication.pom { pom ->
                    pom.packaging = Util.require("EasyPublish.packaging", easyPublish.packaging)
                    pom.name.set(
                            Util.require("EasyPublish.artifactId", easyPublish.artifactId)
                    )
                    pom.description.set(
                            Util.require("EasyPublish.description", easyPublish.description)
                    )
                    pom.url.set(
                            Util.require("EasyPublish.siteUrl", easyPublish.siteUrl)
                    )
                    pom.licenses { licenses ->

                        licenses.license { license ->

                            license.name.set(
                                    Util.require("EasyPublish.licenseName", easyPublish.licenseName)
                            )

                            license.url.set(
                                    Util.require("EasyPublish.licenseUrl", easyPublish.licenseUrl)
                            )
                        }
                    }
                    pom.developers { develops ->
                        easyPublish.mDevelopers.forEach { dev ->
                            develops.developer { developer ->
                                developer.id.set(dev.id)
                                developer.name.set(dev.name)
                                developer.email.set(dev.email)
                            }
                        }
                    }

                    pom.scm { scm ->
                        scm.connection.set(
                                Util.require("EasyPublish.siteUrl", easyPublish.siteUrl)
                        )
                        scm.developerConnection.set(
                                Util.require("EasyPublish.gitUrl", easyPublish.gitUrl)
                        )
                        scm.url.set(
                                Util.require("EasyPublish.siteUrl", easyPublish.siteUrl)
                        )
                    }
                }
            }
        }


        publishingExtension.repositories {
            it.maven { repo ->
                repo.url = URI.create(
                        Util.require("EasyPublish.mavenRepoUrl", easyPublish.mavenRepoUrl) ?: ""
                )
                val account = properties?.get("nexus_user")?.toString()
                val password = properties?.get("nexus_pwd")?.toString()
                repo.credentials { credential ->
                    credential.username = Util.require("local.properties.nexus_user", account)
                    credential.password = Util.require("local.properties.nexus_pwd", password)
                }
            }
        }
        signExtension?.sign(
                publishingExtension.publications.findByName("easyMavenPublish")
        )
    }

}