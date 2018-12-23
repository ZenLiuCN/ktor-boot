/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.annotations.routing

import cn.zenliu.ktor.boot.annotations.context.Ignore

/**
 * define a RawRoute configuration function
 */
@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
@Ignore
annotation class RawRoute
