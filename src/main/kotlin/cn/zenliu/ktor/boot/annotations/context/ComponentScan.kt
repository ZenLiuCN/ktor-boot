package cn.zenliu.ktor.boot.annotations.context


/**
 * define extra package to scan
 * @property packages Array<String>
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class ScanPackage(val packages: Array<String>)
