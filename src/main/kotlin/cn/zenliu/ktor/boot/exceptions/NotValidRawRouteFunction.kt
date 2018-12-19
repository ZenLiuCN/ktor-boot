package cn.zenliu.ktor.boot.exceptions

import cn.zenliu.ktor.boot.annotations.context.Ignore
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

@Ignore
data class NotValidRawRouteFunction(val fn: KFunction<*>, val clazz: KClass<*>, val throwable: Throwable? = null) :
    Throwable("${clazz.qualifiedName}::${fn.name} is not valid RawRouteFunction", throwable)
