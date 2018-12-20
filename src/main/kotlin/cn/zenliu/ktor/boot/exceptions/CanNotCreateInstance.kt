package cn.zenliu.ktor.boot.exceptions

import cn.zenliu.ktor.boot.annotations.context.Ignore
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType

/**
 * Throwable for create instance failed with constructor
 * @property fn KFunction<*>
 * @property throwable Throwable?
 * @constructor
 */
@Ignore
data class CanNotCreateInstance(val fn: KFunction<*>, val throwable: Throwable? = null) :
    Throwable("can not instance ${fn.returnType.javaType.typeName} by constructor ${fn.parameters}", throwable)
