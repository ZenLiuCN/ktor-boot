package cn.zenliu.ktor.boot.context

import cn.zenliu.ktor.boot.annotations.context.AutoWire
import cn.zenliu.ktor.boot.annotations.context.Ignore
import cn.zenliu.ktor.boot.exceptions.CanNotCreateInstance
import cn.zenliu.ktor.boot.exceptions.CanNotCreateInstanceClass
import cn.zenliu.ktor.boot.reflect.isClass
import cn.zenliu.ktor.boot.reflect.kClass
import cn.zenliu.ktor.boot.reflect.simpleDefaultValue
import kotlinx.coroutines.CoroutineScope
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend

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
    internal fun instanceOf(container: BeanContainer) = instanceOf(container.alias ?: container.name, container.clazz)

    /**
     * create kClass Instance
     * @param clazz KClass<*>
     * @return Any
     */
    fun instanceOf(name: String, clazz: KClass<*>): Any = when {
        name.isEmpty() -> Instant.now().let { "${it.toEpochMilli()}${it.nano}" }
        else -> name
    }.let { bname ->
        beanRegistry[bname]
            ?: (clazz.objectInstance
                ?: clazz.constructors.find { it.parameters.isEmpty() }?.call()
                ?: clazz.constructors.firstOrNull()?.let { fn ->
                    fn.parameters.map { param ->
                        when {
                            param.isOptional || param.type.isMarkedNullable -> null
                            param.kind == KParameter.Kind.INSTANCE || param.kind == KParameter.Kind.EXTENSION_RECEIVER -> throw CanNotCreateInstance(
                                fn
                            )
                            else -> param.type.simpleDefaultValue ?: instanceOf(
                                param.annotations.find { it is AutoWire }?.let { it as? AutoWire }?.name.let { if (it.isNullOrBlank()) null else it }
                                    ?: param.name
                                    ?: ""
                                , param.type.kClass!!
                            )
                        }
                    }.let {
                        fn.call(*it.toTypedArray())
                    }
                } ?: throw CanNotCreateInstanceClass(clazz)
                    ).apply { beanRegistry[bname] = this }
    }

    fun executeKFunction(target: Any, fn: KFunction<*>, vararg params: Any) = fn.parameters.map { param ->
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
        fn.call(*it.toTypedArray())
    }

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
