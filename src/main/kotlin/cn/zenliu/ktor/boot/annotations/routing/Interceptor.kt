/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

/**
 * @Author: Zen.Liu
 * @Date: 2018-12-22 15:47
 * @Project: ktor-boot
 */
package cn.zenliu.ktor.boot.annotations.routing

import cn.zenliu.ktor.boot.annotations.context.Ignore


@MustBeDocumented
@Ignore
@Target(AnnotationTarget.FUNCTION)
annotation class Interceptor(val path:String="",val order:Int=0,val method:Array<RequestMapping.METHOD> = emptyArray())
