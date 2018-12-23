/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.example

import org.junit.Test

operator fun Boolean.rem(value: Pair<Any?, Any?>) = if (this) value.first else value.second

class TestOp {
    @Test
    fun test() {
        println( (1 == 2 )% ("1" to "2"))
    }
}
