/*
 * Copyright (c) 2018.
 * written by Zen.Liu(http://github.com/ZenLiuCN/), supported by AS IS.
 */

/**
 * @Author: Zen.Liu
 * @Date: 2018-12-22 17:20
 * @Project: ktor-boot
 */
package cn.zenliu.ktor.boot.context

import io.ktor.application.ApplicationCall
import io.ktor.util.pipeline.PipelineContext


typealias HandlerContext = PipelineContext<Unit, ApplicationCall>
typealias InterceptorContext = PipelineContext<Unit, ApplicationCall>
