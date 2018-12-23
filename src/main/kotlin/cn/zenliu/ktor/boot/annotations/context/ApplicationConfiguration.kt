/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */
@file:Ignore
package cn.zenliu.ktor.boot.annotations.context

import io.ktor.application.Application

/**
 * mark class is configuration class [cn.zenliu.ktor.boot.context.ApplicationConfiguration] or configuration function [ConfiugrationFunction]
 * @property order Int
 * @constructor
 */
@Ignore
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ApplicationConfiguration(val order: Int = 0)

typealias ApplicationConfiugrationFunction = (Application) -> Unit
