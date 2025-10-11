package com.rui.compose.paginator


/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description:使用PageStringStrategy [PageResult.nextKey] 必须实现
 * Copyright (c) 2025 All rights reserved.
 *
 */
class PageStringStrategy(
    private val initKey: String = "",
) : PaginationStrategy<String> {

    override fun initialKey(): String {
        return initKey
    }

    override fun nextKeyAfter(
        currentKey: String,
        pageResult: PageResult<*, String>,
    ): String? {
        return if (pageResult.isEndReached) {
            null
        } else {
            pageResult.nextKey
        }
    }

}