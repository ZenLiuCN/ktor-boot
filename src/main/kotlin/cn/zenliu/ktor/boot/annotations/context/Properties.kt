/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context

/**
 * marker of a data class will be defined in HOCON <application.conf>
 * @property path String
 * @constructor
 */
@MustBeDocumented
@Ignore
@Target(AnnotationTarget.CLASS)
annotation class Properties(val path: String="")
