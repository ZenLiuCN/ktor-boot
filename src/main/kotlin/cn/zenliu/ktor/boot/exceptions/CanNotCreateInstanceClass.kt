/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.exceptions

import cn.zenliu.ktor.boot.annotations.context.Ignore
import kotlin.reflect.KClass

/**
 * Throwable for create class instance failed
 * @property fn KClass<*>
 * @property throwable Throwable?
 * @constructor
 */
@Ignore
data class CanNotCreateInstanceClass(val fn: KClass<*>, val throwable: Throwable? = null) :
    Throwable("can not instance ${fn.java.typeName}", throwable)
