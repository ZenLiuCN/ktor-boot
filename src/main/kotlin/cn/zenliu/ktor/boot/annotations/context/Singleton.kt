/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context

/**
 * class should be Singleton
 * @property name String
 * @constructor
 */
@Ignore
@Target(AnnotationTarget.CLASS)
annotation class Singleton(val name: String = "")

