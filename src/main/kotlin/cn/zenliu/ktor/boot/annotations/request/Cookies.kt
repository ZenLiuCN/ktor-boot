/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.annotations.request

import cn.zenliu.ktor.boot.annotations.context.Ignore
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@Ignore
@Target(VALUE_PARAMETER)
@MustBeDocumented
annotation class Cookies(val name: String = "")
