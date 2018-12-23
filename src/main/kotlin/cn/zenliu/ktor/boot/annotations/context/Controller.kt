/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context

/**
 * marker for class contains route functions
 * [cn.zenliu.ktor.boot.annotations.routingRawRoute], [cn.zenliu.ktor.boot.annotations.routing.RequestMapping]
 * @property name String
 * @property order Int
 * @constructor
 */
@MustBeDocumented
@Ignore
@Target(AnnotationTarget.CLASS)
annotation class Controller(val name: String = "", val order: Int = 0)

