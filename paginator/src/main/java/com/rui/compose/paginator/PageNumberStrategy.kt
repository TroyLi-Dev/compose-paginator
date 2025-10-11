package com.rui.compose.paginator

/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description: 使用PageStringStrategy  [PageResult.nextKey] 不为空会使用 [PageResult.nextKey] 作为下一页页码，为空 则使用当前页面+1
 * Copyright (c) 2025 All rights reserved.
 */
class PageNumberStrategy(private val firstPage: Int = 1) : PaginationStrategy<Int> {
    override fun initialKey() = firstPage
    override fun nextKeyAfter(currentKey: Int, pageResult: PageResult<*, Int>): Int? {
        return if (pageResult.isEndReached) {
            null
        } else {
            pageResult.nextKey ?: (currentKey + 1)
        }
    }
}