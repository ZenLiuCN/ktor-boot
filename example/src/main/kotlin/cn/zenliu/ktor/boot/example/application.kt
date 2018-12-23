/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

/**
 * @Author: Zen.Liu
 * @Date: 2018-12-22 10:57
 * @Project: ktor-boot
 */
package cn.zenliu.ktor.boot.example

import cn.zenliu.ktor.boot.annotations.context.*
import cn.zenliu.ktor.boot.annotations.context.ApplicationConfiguration
import cn.zenliu.ktor.boot.annotations.request.*
import cn.zenliu.ktor.boot.annotations.routing.*
import cn.zenliu.ktor.boot.context.*
import io.ktor.application.*
import io.ktor.features.CallLogging
import io.ktor.routing.Routing
import io.ktor.util.*
import org.slf4j.LoggerFactory


fun main(args: Array<String>) {
    Start<App>(args)
}

class App(val conf: Conf){
    val log=LoggerFactory.getLogger(App::class.java)
    init {
        log.trace("properties test of $conf")
    }
    @ApplicationConfiguration
    fun configuartion(app:Application){
        app.install(CallLogging)
    }
}
@Controller
class Ctrl{
    val attr=AttributeKey<String>("test")
    @Interceptor
    fun intercepthor(ctx:InterceptorContext){
        log.info("interceptor called")
        ctx.context.attributes.put(attr,"a")
    }
    @RawRoute
    fun raw(ctx:Routing){
        ctx{

        }
    }
    @RequestMapping("/info")
    fun info()="1"
    @RequestMapping("/info/a")
    fun infoa()="1"
    @RequestMapping("/info1/a")
    fun info1a()="1"
    @RequestMapping("/info2/")
    fun info2(attributes: Attributes)=attributes[attr]
    @RequestMapping("/info2/a/{id}",method = RequestMapping.METHOD.POST)
    fun info2a(@PathVariable id:String, @RequestBody body:String, @QueryParameter a:Int?)=id+body+a.toString()
    companion object {
        private val log=LoggerFactory.getLogger(Ctrl::class.java)
    }
}
@Properties("app.configuration")
data class Conf(val conf:Boolean=true)
