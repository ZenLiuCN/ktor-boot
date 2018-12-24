/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

package cn.zenliu.ktor.boot.example

import cn.zenliu.ktor.boot.reflect.*
import org.junit.Test



class TestOp {
    @Test
    fun test() {
        println("${TestOp::T.parameters[1].type.isClass<AnyType>()} ${TestOp::T.parameters[1].type.arguments}")
        println("${TestOp::T.parameters[1].type.isClassContainer1<Collection<*>,AnyType>()} ${TestOp::T.parameters[1].type.arguments}")

    }
    fun T(map:Collection<*>){

    }
}
