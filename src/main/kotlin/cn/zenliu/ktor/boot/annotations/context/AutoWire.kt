/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context

/**
 * mark property should generate from BeanManager
 * @property name String
 * @constructor
 */
@Ignore
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY_GETTER)
annotation class AutoWire(val name: String)

