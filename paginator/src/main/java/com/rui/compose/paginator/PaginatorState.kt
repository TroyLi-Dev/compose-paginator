package com.rui.compose.paginator

/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description: 状态
 * Copyright (c) 2025 All rights reserved.
 */
data class PaginatorState<T>(
    val items: List<T> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val refreshError: Throwable? = null,
    val loadMoreError: Throwable? = null,
    val isEndReached: Boolean = false,
    internal val bottomReachedLoadMore: Boolean = false,
)