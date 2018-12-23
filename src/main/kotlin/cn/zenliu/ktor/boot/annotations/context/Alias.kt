/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context

/**
 * define bean class alias
 * @property name String
 * @constructor
 */
@Ignore
@Target(AnnotationTarget.CLASS)
annotation class Alias(val name: String)

