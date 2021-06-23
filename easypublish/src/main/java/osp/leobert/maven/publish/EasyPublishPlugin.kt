@file:Suppress("deprecation")

package osp.leobert.maven.publish

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.MavenPlugin.*
import org.gradle.api.plugins.MavenPluginConvention
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.publication.maven.internal.MavenFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.signing.SigningExtension
import osp.leobert.maven.publish.bean.EasyPublish
import java.net.URI
import java.util.*


class EasyPublishPlugin : Plugin<ProjectInternal> {

    var easyPublish = EasyPublish()
    var publishExtension: PublishingExtension? = null
    var signExtension: SigningExtension? = null


    @Suppress("PrivateApi", "Unchecked")
    override fun apply(project: ProjectInternal) {
        Logger.info("hello,I'am easy-publish")


        project.pluginManager.apply(BasePlugin::class.java)

        val mavenFactory: MavenFactory = project.services.get(MavenFactory::class.java)
        val pluginConvention: MavenPluginConvention = addConventionObject(project, mavenFactory)


        val plugins: PluginContainer = project.plugins

        try {
            val appPluginClass: Class<Plugin<*>> =
                    Class.forName("com.android.build.gradle.AppPlugin") as Class<Plugin<*>>
            val libraryPluginClass: Class<Plugin<*>> =
                    Class.forName("com.android.build.gradle.LibraryPlugin") as Class<Plugin<*>>
            val testPluginClass: Class<Plugin<*>> =
                    Class.forName("com.android.build.gradle.TestPlugin") as Class<Plugin<*>>


            plugins.withType(appPluginClass) {
                configureAndroidScopeMappings(project.configurations,
                        pluginConvention.conf2ScopeMappings)
            }
            plugins.withType(libraryPluginClass) {
                configureAndroidScopeMappings(project.configurations,
                        pluginConvention.conf2ScopeMappings)
            }
            plugins.withType(testPluginClass) {
                configureAndroidScopeMappings(project.configurations,
                        pluginConvention.conf2ScopeMappings)
            }
        } catch (ex: ClassNotFoundException) {
        }

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

            configPublishing(project, localProperties, sourcesJarTask, javadocJarTask, isAndroid)


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
            project: Project,
            properties: Properties?,
            sourcesJarTask: Task?,
            javadocJarTask: Task?,
            isAndroid: Boolean,
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

                if (!easyPublish.notStandardJavaComponent)
                    publication.from(project.components.getByName("java"))


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


                    if (easyPublish.notStandardJavaComponent) {
                        applyPomDeps(pom = pom, project = project)
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

    private val scopeMapping = mapOf<String, String?>(
            "api" to "compile",
            "implementation" to "compile",
            "compile" to "compile"
    )

    private fun applyPomDeps(pom: MavenPom, project: Project) {
        pom.withXml { xml ->

            val dependenciesNode = xml.asNode().appendNode("dependencies")

            //Iterate over the compile dependencies (we don't want the test ones), adding a <dependency> node for each
            scopeMapping.keys.forEach { key ->

                try {
                    project.configurations.getByName(key).allDependencies?.forEach { dependency ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dependency.group)
                        dependencyNode.appendNode("artifactId", dependency.name)
                        dependencyNode.appendNode("version", dependency.version)
                        dependencyNode.appendNode("scope", scopeMapping[key])
                    }
                } catch (thr: Throwable) {

                }
            }

        }
    }

    private fun configureAndroidScopeMappings(
            configurations: ConfigurationContainer,
            mavenScopeMappings: Conf2ScopeMappingContainer,
    ) {
        mavenScopeMappings.addMapping(COMPILE_PRIORITY,
                configurations.getByName(JavaPlugin.API_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.COMPILE)
        mavenScopeMappings.addMapping(COMPILE_PRIORITY,
                configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.COMPILE)
        mavenScopeMappings.addMapping(RUNTIME_PRIORITY,
                configurations.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.RUNTIME)
        mavenScopeMappings.addMapping(TEST_COMPILE_PRIORITY,
                configurations.getByName(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.TEST)
        mavenScopeMappings.addMapping(TEST_RUNTIME_PRIORITY,
                configurations.getByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME),
                Conf2ScopeMappingContainer.TEST)
    }

    private fun addConventionObject(
            project: ProjectInternal,
            mavenFactory: MavenFactory,
    ): MavenPluginConvention {
        val mavenConvention = MavenPluginConvention(project, mavenFactory)
        val convention = project.convention
        convention.plugins.forEach { s, any ->
            Logger.info(" convention.plugins$s")
        }
        convention.plugins["maven-publish"] = mavenConvention
        return mavenConvention
    }

}