/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context

/**
 * define some package should not be scan use on Application class
 * @property packages Array<String>
 * @constructor
 */
@Ignore
@Target(AnnotationTarget.CLASS)
annotation class ExcludePackage(val packages: Array<String>)
