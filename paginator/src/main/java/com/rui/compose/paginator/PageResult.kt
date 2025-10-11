package com.rui.compose.paginator

/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description:
 * Copyright (c) 2025 All rights reserved.
 */
data class PageResult<T, P>(
    val items: List<T>,
    val nextKey: P? = null,   // 下一页需要用的 Key
    val isEndReached: Boolean,
)