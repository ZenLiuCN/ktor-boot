package cn.zenliu.ktor.boot.exceptions

import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType

data class CanNotCreateInstance(val fn: KFunction<*>, val throwable: Throwable? = null) :
    Throwable("can not instance ${fn.returnType.javaType.typeName} by constructor ${fn.parameters}", throwable)
