package cn.zenliu.ktor.boot.reflect

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.kotlinFunction

//<editor-fold desc="KClassExt">
@Suppress("NOTHING_TO_INLINE")
val String.tryKClass: KClass<*>?
    inline get() = try {
        Thread.currentThread().contextClassLoader.loadClass(this).kotlin
    } catch (e: ClassNotFoundException) {
        null
    }
@Suppress("NOTHING_TO_INLINE")
val String.classExists: Boolean
    inline get() = try {
        Thread.currentThread().contextClassLoader.loadClass(this)
        true
    } catch (e: ClassNotFoundException) {
        false
    }

fun KType.isClass(cls: KClass<*>): Boolean = this.classifier == cls

val KType.isTypeString: Boolean get() = this.isClass(String::class)
val KType.isTypeInt: Boolean get() = this.isClass(Int::class) || this.isClass(java.lang.Integer::class)
val KType.isTypeLong: Boolean get() = this.isClass(Long::class) || this.isClass(java.lang.Long::class)
val KType.isTypeByte: Boolean get() = this.isClass(Byte::class) || this.isClass(java.lang.Byte::class)
val KType.isTypeShort: Boolean get() = this.isClass(Short::class) || this.isClass(java.lang.Short::class)
val KType.isTypeChar: Boolean get() = this.isClass(Char::class) || this.isClass(java.lang.Character::class)
val KType.isTypeBoolean: Boolean get() = this.isClass(Boolean::class) || this.isClass(java.lang.Boolean::class)
val KType.isTypeFloat: Boolean get() = this.isClass(Float::class) || this.isClass(java.lang.Float::class)
val KType.isTypeDouble: Boolean get() = this.isClass(Double::class) || this.isClass(java.lang.Double::class)
val KType.isTypeByteArray: Boolean get() = this.isClass(ByteArray::class)
//val KType.isTypeUInt: Boolean get() = this.isClass(UInt::class)

val KType.kClass: KClass<*>?
    get() = when {
        isTypeString -> String::class
        isTypeInt -> Int::class
        isTypeLong -> Long::class
        isTypeByte -> Byte::class
        isTypeShort -> Short::class
        isTypeChar -> Char::class
        isTypeBoolean -> Boolean::class
        isTypeFloat -> Float::class
        isTypeDouble -> Double::class
        isTypeByteArray -> ByteArray::class
        else -> this.javaType.typeName.tryKClass
    }

val KType.simpleDefaultValue: Any?
    get() = when {
        isTypeString -> ""
        isTypeInt -> 0
        isTypeLong -> 0L
        isTypeByte -> 0.toByte()
        isTypeShort -> 0.toShort()
        isTypeChar -> 0.toChar()
        isTypeBoolean -> false
        isTypeFloat -> 0.toFloat()
        isTypeDouble -> 0.toDouble()
        isTypeByteArray -> "".toByteArray()
        else -> null
    }

inline fun <reified T : Annotation> KClass<*>.findAnnotationSafe() = try {
    this.annotations.firstOrNull { it is T } as T?
} catch (e: UnsupportedOperationException) {
    this.java.annotations.firstOrNull { it is T } as T?
}

@Suppress("NOTHING_TO_INLINE")
inline val KClass<*>.declaredFunctionsSafe
    get() = try {
        this.declaredFunctions
    } catch (e: UnsupportedOperationException) {
        this.java.methods.map { it.kotlinFunction }.filterNotNull()
    }

@Suppress("NOTHING_TO_INLINE")
inline val KClass<*>.functionsSafe
    get() = try {
        this.functions
    } catch (e: UnsupportedOperationException) {
        this.java.methods.map { it.kotlinFunction }.filterNotNull()
    }
@Suppress("NOTHING_TO_INLINE")
inline val KClass<*>.annotationsSafe
    get() = try {
        this.annotations
    } catch (e: UnsupportedOperationException) {
        this.java.annotations.filterNotNull()
    }
//</editor-fold>
