/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.Ignore
import cn.zenliu.ktor.boot.jackson.toJsonNode
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.log
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

//<editor-fold desc="Context">
internal val BootCoroutineContext = GlobalScope.coroutineContext


/**
 * Application Context manage all application dependencies
 */
@Ignore
object Context : CoroutineScope {
    private val log=LoggerFactory.getLogger(Context::class.java)
    override val coroutineContext: CoroutineContext = BootCoroutineContext
    /**
     * start application
     * @param clazz KClass<*>
     * @param application Application
     */
    @KtorExperimentalAPI
    fun start(clazz: KClass<*>, application: Application) {
        ClassManager.register(clazz)
        log.trace("find bean classes: ${ClassManager.clazzRegistry}")
        configuration(application)
        log.trace("configure application: ${ClassManager.getConfigurations()}")
        RouteManager.instance(application, ClassManager.getControllers(),ClassManager.getRouteFunctions())
    }

    @KtorExperimentalAPI
    private fun configuration(app: Application) =
        ClassManager.getConfigurations().filter { !it.clazz.isAbstract }.map {
            it to BeanManager.instanceOf(it)
        }.forEach { (container, instance) ->
            if (container.isConfigurationClass) {
                (instance as ApplicationConfiguration).applicationConfiguration(app)
            } else {
                container.configurationFunctions().forEach {
                    BeanManager.executeKFunction(instance, it, app)
                }
            }
        }






}

//</editor-fold>
