package cn.zenliu.ktor.boot.annotations.context


/**
 * mark on any class in default application package to add other scanner packages
 * @property packages Array<String>
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class ComponentPackage(val packages: Array<String>,val exclude:Array<String> = emptyArray())
