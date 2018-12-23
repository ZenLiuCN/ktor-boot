/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.jackson

import com.fasterxml.jackson.databind.JsonNode

//<editor-fold desc="Extension">

val Any?.toJsonNode: JsonNode
    get() = JsonMapper.toJsonNode(this)

inline fun <reified T> Any?.toClass(): T = JsonMapper.toClass(this, T::class.java)

fun <T> Any?.toClass(clazz: Class<T>): T = JsonMapper.toClass(this, clazz)
//</editor-fold>
