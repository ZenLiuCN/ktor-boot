/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.*
import cn.zenliu.ktor.boot.exceptions.CanNotCreateInstance
import cn.zenliu.ktor.boot.exceptions.CanNotCreateInstanceClass
import cn.zenliu.ktor.boot.reflect.isClass
import cn.zenliu.ktor.boot.reflect.kClass
import cn.zenliu.ktor.boot.reflect.simpleDefaultValue
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

/**
 * BeanManager is application level manager for all bean instance
 */
@Ignore
object BeanManager : CoroutineScope {
    override val coroutineContext: CoroutineContext = BootCoroutineContext
    private val beanRegistry = mutableMapOf<String, Any>()
    /**
     * create instance from container
     * @param container BeanContainer
     * @return Any
     */
    @KtorExperimentalAPI
    internal fun instanceOf(container: BeanContainer) = instanceOf(container.alias ?: container.name, container.clazz)

    /**
     * create kClass Instance
     * @param clazz KClass<*>
     * @return Any
     */
    @KtorExperimentalAPI
    fun instanceOf(name: String, clazz: KClass<*>): Any = when {
        name.isEmpty() -> Instant.now().let { "${it.toEpochMilli()}${it.nano}" }
        else -> name
    }.let { bname ->
        beanRegistry[bname]
            ?: (clazz.objectInstance
                ?: clazz.constructors.find { it.parameters.isEmpty() }?.call()
                ?: clazz.constructors.firstOrNull()?.let { fn ->

                    fn.callBy(fn.parameters.map { param ->
                        param to when {
                            param.findAnnotation<Properties>() != null -> PropertiesManager.getProperties(param.type.kClass!!)
                            param.kind == KParameter.Kind.INSTANCE || param.kind == KParameter.Kind.EXTENSION_RECEIVER -> throw CanNotCreateInstance(
                                fn
                            )
                            else -> param.type.simpleDefaultValue ?: try {
                                instanceOf(
                                    param.findAnnotation<AutoWire>()?.name.let { if (it.isNullOrBlank()) null else it }
                                        ?: param.name
                                        ?: ""
                                    , param.type.kClass!!
                                )
                            } catch (e: Throwable) {
                                if (param.isOptional || param.type.isMarkedNullable) null
                                else throw CanNotCreateInstanceClass(clazz,e)
                            }
                        }
                    }.filter { !(it.first.isOptional && it.second == null) }.toMap())

                } ?: throw CanNotCreateInstanceClass(clazz)
                    ).apply { beanRegistry[bname] = this }
    }

    @KtorExperimentalAPI
    fun executeKFunction(target: Any, fn: KFunction<*>, vararg params: Any) = fn.parameters.map { param ->
        when {
            param.kind == KParameter.Kind.INSTANCE || param.kind == KParameter.Kind.EXTENSION_RECEIVER -> target
            else -> params.find { param.type.isClass(it::class) }
                ?: param.type.simpleDefaultValue
                ?: instanceOf(
                    param.findAnnotation<AutoWire>()?.name.let { if (it.isNullOrBlank()) null else it }
                        ?: param.name
                        ?: "", param.type.kClass!!
                )
        }
    }.let {
        fn.call(*it.toTypedArray())
    }

    @KtorExperimentalAPI
    suspend fun asyncExcuteKFunction(target: Any, fn: KFunction<*>, vararg params: Any) =
        fn.parameters.map { param ->
            when {
                param.kind == KParameter.Kind.INSTANCE || param.kind == KParameter.Kind.EXTENSION_RECEIVER -> target
                else -> params.find { param.type.isClass(it::class) }
                    ?: param.type.simpleDefaultValue
                    ?: instanceOf(
                        param.annotations.find { it is AutoWire }?.let { it as? AutoWire }?.name.let { if (it.isNullOrBlank()) null else it }
                            ?: param.name
                            ?: "", param.type.kClass!!
                    )
            }
        }.let {
            fn.callSuspend(*it.toTypedArray())
        }
}
