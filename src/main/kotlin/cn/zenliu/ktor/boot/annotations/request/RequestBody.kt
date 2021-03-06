/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.request

import cn.zenliu.ktor.boot.annotations.context.Ignore

/**
 * mark one route function parameter should be as request body
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class RequestBody
