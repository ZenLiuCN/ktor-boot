package cn.zenliu.ktor.boot.annotations.context

/**
 * define some package should not be scan use on Application class
 * @property packages Array<String>
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class ExcludePackage(val packages: Array<String>)
