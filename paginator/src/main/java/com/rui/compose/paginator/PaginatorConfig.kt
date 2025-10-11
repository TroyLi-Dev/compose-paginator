package com.rui.compose.paginator

import android.util.Log

/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description:配置
 * Copyright (c) 2025 All rights reserved.
 */
data class PaginatorConfig<T>(
    /**  开启日志 */
    internal var openLogger: Boolean = false,
    /**  日志打印 */
    internal var logger: (message: String) -> Unit = { message -> Log.d("Paginator", message) },
    /** 加载操作的超时时间 (毫秒) */
    internal var timeoutMillis: Long = 60_000L, // 默认 60 秒
    /** 当加载更多数据时，如何合并新旧列表 */
    internal var mergeStrategy: (currentItems: List<T>, newItems: List<T>) -> List<T> = { current, new -> current + new },
    /** 预加载距离：滚动到距离底部多少项时开始加载更多 */
    internal var prefetchDistance: Int = 5, // 默认提前 5 项
) {

    fun openLogger(open: Boolean) {
        this.openLogger = open
    }

    fun setLogger(logger: (message: String) -> Unit) {
        this.logger = logger
    }

    fun setTimeoutMillis(timeoutMillis: Long) {
        this.timeoutMillis = timeoutMillis
    }

    fun setMergeStrategy(mergeStrategy: (currentItems: List<T>, newItems: List<T>) -> List<T>) {
        this.mergeStrategy = mergeStrategy
    }

    fun setPrefetchDistance(prefetchDistance: Int) {
        this.prefetchDistance = prefetchDistance
    }

}
