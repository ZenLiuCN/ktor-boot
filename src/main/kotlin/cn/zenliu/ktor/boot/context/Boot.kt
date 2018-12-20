/*
 * Copyright (c) 2018.
 * Program and supplies by Zen.Liu<lcz20@163.com>
 */

package cn.zenliu.ktor.boot.context


import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.loadCommonConfiguration
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import kotlin.reflect.KClass


/**
 * *inline* start function for application, should be used in main function
 * @param args Array<String>
 */
@KtorExperimentalAPI
inline fun <reified T : Any> Start(args: Array<String>) {
    PropertiesManager.setConfiguration(args)
    embeddedServer(
        Netty,
        port = PropertiesManager.port(),
        host = PropertiesManager.host(),
        watchPaths = PropertiesManager.watchPaths(),
        configure = {
            PropertiesManager.config.let { conf ->
                loadCommonConfiguration(conf)
                conf.propertyOrNull("requestQueueLimit")?.getString()?.toInt()?.let {
                    requestQueueLimit = it
                }
                conf.propertyOrNull("shareWorkGroup")?.getString()?.toBoolean()?.let {
                    shareWorkGroup = it
                }
                conf.propertyOrNull("responseWriteTimeoutSeconds")?.getString()?.toInt()?.let {
                    responseWriteTimeoutSeconds = it
                }
            }

        }
    ) {
        Context.start(T::class, this)
    }.start(wait = true)
}

/**
 *  none inline start function for application, should be used in main function
 * @param clazz KClass<*>
 * @param args Array<String>
 */
@KtorExperimentalAPI
fun <T> BootStart(clazz: KClass<*>, args: Array<String>) {
    PropertiesManager.setConfiguration(args)
    embeddedServer(
        Netty,
        port = PropertiesManager.port(),
        host = PropertiesManager.host(),
        watchPaths = PropertiesManager.watchPaths(),
        configure = {
            PropertiesManager.config.let { conf ->
                loadCommonConfiguration(conf)
                conf.propertyOrNull("requestQueueLimit")?.getString()?.toInt()?.let {
                    requestQueueLimit = it
                }
                conf.propertyOrNull("shareWorkGroup")?.getString()?.toBoolean()?.let {
                    shareWorkGroup = it
                }
                conf.propertyOrNull("responseWriteTimeoutSeconds")?.getString()?.toInt()?.let {
                    responseWriteTimeoutSeconds = it
                }
            }

        }
    ) {
        Context.start(clazz, this)
    }.start(wait = true)
}
