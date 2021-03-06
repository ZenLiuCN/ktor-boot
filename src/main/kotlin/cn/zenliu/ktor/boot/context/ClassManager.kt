/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.*
import cn.zenliu.ktor.boot.reflect.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.io.File
import java.net.JarURLConnection
import java.net.URL
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.streams.toList

/**
 * object ClassManager manage all class in application (with out [cn.zenliu.ktor.boot.annotations.context.Ignore])
 */
@Ignore
object ClassManager : CoroutineScope {
    override val coroutineContext: CoroutineContext = BootCoroutineContext
    internal val clazzRegistry = mutableMapOf<String, BeanContainer>()

    internal fun beanClass(name: String) = clazzRegistry[name]
    internal fun beanClass(clazz: KClass<*>) = clazzRegistry[clazz.qualifiedName]
    internal fun beanClass(clazz: Class<*>) = clazzRegistry[clazz.name]

    init {
        //root package
        scanPackages(setOf("cn.zenliu.ktor.boot"))
    }

    /**
     * regist package from KClass
     * @param clazz KClass<*>
     */
    fun register(clazz: KClass<*>) {
        clazz.annotations
            .filter { it is ComponentPackage }
            .map { (it as ComponentPackage).packages.toList() }
            .flatten().filter { it.contains(".") }.toMutableSet().let {
                it.add(clazz.java.`package`.name)
                scanPackages(it.toSet())
            }
    }

    /**
     * scan package classes
     * @param packages Set<String>
     */
    fun scanPackages(packages: Set<String>) {
        clazzRegistry.putAll(
            packages.map { pkg ->
                Thread
                    .currentThread()
                    .contextClassLoader
                    .getResource(pkg.replace(".", "/")).let {
                        when (it.protocol) {
                            "jar" -> getJarClasses(it, pkg)
                            "file" -> getFileClasses(it, pkg, it.path.let { pth ->
                                pkg.replace(".", "/").let { p ->
                                    if (pth.contains(p)) pth.substring(0, pth.lastIndexOf(p))
                                    else pth
                                }
                            })
                            else -> setOf()
                        }
                    }
            }.flatten().filter { it != null && it.clazz.qualifiedName==it.pkg+"."+it.name }.map { it!!.pkg + "." + it.name to it }.toMap()
                .toMutableMap()
        )

    }

    /**
     * fetch all configuration class or class with configuration function
     * @return Map<String, BeanContainer>
     */
   internal fun getConfigurations() =
        clazzRegistry.filter { it.value.isConfigurationClass || it.value.hasConfigurationFunction }.map { it.value }.sortedBy {
            it.clazz.findAnnotationSafe<Order>()?.value ?: 0
        }
    @KtorExperimentalAPI
    fun getExceptionHanders()= clazzRegistry.filter { it.value.clazz.isExceptionHandler }.map { it.value.clazz.exceptionHandlerFunctions.sortedBy { it.findAnnotation<ExceptionHandler>()!!.order } to BeanManager.instanceOf(it.value) }
    @KtorExperimentalAPI
    fun getControllers() =
        clazzRegistry
            .filter { it.value.isController }
            .map { it.value.clazz to BeanManager.instanceOf(it.value) }
            .sortedBy {
                it.first.findAnnotationSafe<Order>()?.value ?: 0
            }.apply {
                log.trace("registered controller $this")
            }

    @KtorExperimentalAPI
    fun getRouteFunctions() = clazzRegistry.filter { it.value.routeFunctions.isNotEmpty() }.map {
        it.value.routeFunctions to BeanManager.instanceOf(
            it.value
        )
    }

    private fun getFileClasses(url: URL, pkg: String, root: String): Set<BeanContainer> =
        File(url.file).listFiles().map {
            when {
                it.isFile -> it.name.let {
                    when {
                        !it.contains("$") && it.endsWith(".class") -> {
                            try {
                                setOf(
                                    BeanContainer(
                                        pkg = pkg,
                                        path = url.path + it,
                                        name = it.replace(".class", ""),
                                        clazz = Class.forName(pkg + "." + it.replace(".class", "")).kotlin
                                    )
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                                setOf<BeanContainer>()
                            }
                        }
                        else -> setOf()
                    }
                }
                it.isDirectory -> getFileClasses(
                    it.toURI().toURL(),
                    it.toURI().toURL().path.let { it.removeSuffix("/") }.removePrefix(root).replace("/", "."), root
                )
                else -> setOf()
            }
        }.filter { !it.isEmpty() }.flatten().filter { it.clazz.findAnnotationSafe<Ignore>()==null }.toSet()

    private fun getJarClasses(url: URL, pkg: String) =
        (url.openConnection() as? JarURLConnection)
            ?.jarFile
            ?.stream()
            ?.map {
                it.name
            }
            ?.filter {
                it.replace("/", ".").startsWith(pkg) &&
                        !it.contains("$") &&
                        it.endsWith(".class")
            }
            ?.map {
                try {
                    BeanContainer(
                        pkg = pkg,
                        path = url.path + it.replace(pkg, ""),
                        name = it.substringAfterLast("/").replace(".class", ""),
                        clazz = Class.forName(it.replace("/", ".").replace(".class", "")).kotlin
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            ?.filter { it != null && it.clazz.findAnnotationSafe<Ignore>()==null}
            ?.toList()
            ?.toSet()
            ?: setOf()
    @JvmStatic
    private val log=LoggerFactory.getLogger(ClassManager::class.java)
}
