package cn.zenliu.ktor.boot.annotations.context

/**
 * class should be Singleton
 * @property name String
 * @constructor
 */
@Experimental(Experimental.Level.WARNING)
@Target(AnnotationTarget.CLASS)
annotation class Singleton(val name: String = "")

