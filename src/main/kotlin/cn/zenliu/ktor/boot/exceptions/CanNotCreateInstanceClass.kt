package cn.zenliu.ktor.boot.exceptions

import kotlin.reflect.KClass

data class CanNotCreateInstanceClass(val fn: KClass<*>, val throwable: Throwable? = null) :
    Throwable("can not instance ${fn.java.typeName}", throwable)
