/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context


/**
 * mark on any class in default application package to add other scanner packages
 * @property packages Array<String>
 * @constructor
 */
@Ignore
@Target(AnnotationTarget.CLASS)
annotation class ComponentPackage(val packages: Array<String>,val exclude:Array<String> = emptyArray())
