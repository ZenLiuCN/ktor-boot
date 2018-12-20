package cn.zenliu.ktor.boot.annotations.context

import io.ktor.application.Application

/**
 * mark class is configuration class [cn.zenliu.ktor.boot.context.ApplicationConfiguration] or configuration function [ConfiugrationFunction]
 * @property order Int
 * @constructor
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class ApplicationConfiguration(val order: Int = 0)

typealias ApplicationConfiugrationFunction = (Application) -> Unit
