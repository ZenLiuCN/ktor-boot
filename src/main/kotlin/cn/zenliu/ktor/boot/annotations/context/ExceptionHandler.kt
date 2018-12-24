/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.annotations.context

import cn.zenliu.ktor.boot.context.HandlerContext
import cn.zenliu.ktor.boot.reflect.AnyType
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

@Ignore
@Target(FUNCTION)
@MustBeDocumented
annotation class ExceptionHandler(val klass:KClass<*> =AnyType::class,val order:Int=0)

typealias ExcepthionHandlerFunction=(HandlerContext)->Boolean
