/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.exceptions

import cn.zenliu.ktor.boot.annotations.context.Ignore
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * Throwable for create instance failed with constructor
 * @property fn KFunction<*>
 * @property clazz KClass<*>
 * @property throwable Throwable?
 * @constructor
 */
@Ignore
data class NotValidRawRouteFunction(val fn: KFunction<*>, val clazz: KClass<*>, val throwable: Throwable? = null) :
    Throwable("${clazz.qualifiedName}::${fn.name} is not valid RawRouteFunction", throwable)
