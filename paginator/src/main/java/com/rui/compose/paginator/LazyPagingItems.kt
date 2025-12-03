package com.rui.compose.paginator

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.StateFlow

/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description:
 * Copyright (c) 2025 All rights reserved.
 */
@Stable
class LazyPagingItems<T>(private val paginator: Paginator<T, *>) {

    internal val state: StateFlow<PaginatorState<T>> = paginator.state

    var items by mutableStateOf<List<T>>(value = emptyList())
        private set

    var totalItemCount: Int by mutableIntStateOf(value = 0)
        private set

    var isRefreshing: Boolean by mutableStateOf(value = false)
        private set

    var isLoadingMore: Boolean by mutableStateOf(value = false)
        private set

    var refreshError: Throwable? by mutableStateOf(value = null)
        private set

    var loadMoreError: Throwable? by mutableStateOf(value = null)
        private set

    var isEndReached: Boolean by mutableStateOf(value = false)
        private set

    internal var bottomReachedLoadMore: Boolean by mutableStateOf(value = false)
        private set

    var visibleEmptyStateView: Boolean by mutableStateOf(value = false)
        private set

    var visibleRefreshErrorStateView: Boolean by mutableStateOf(value = false)
        private set

    var visibleLoadingStateView: Boolean by mutableStateOf(value = false)
        private set

    var visibleLoadMoreErrorStateView: Boolean by mutableStateOf(value = false)
        private set

    var visibleEndReachedStateView: Boolean by mutableStateOf(value = false)
        private set


    operator fun get(index: Int): T? {
        prefetchLoadIfNeed(index = index)
        return items.getOrNull(index)
    }

    private fun prefetchLoadIfNeed(index: Int) {
        if (state.value.bottomReachedLoadMore) {
            if (index >= totalItemCount - 1 - paginator.config.prefetchDistance) {
                bottomReachedLoadMore = false
                paginator.log("到达底部触发距离，加载下一页")
                paginator.loadMore()
                return
            }
        }
    }

    internal suspend fun collectPagingData() {
        state.collect { state ->
            items = state.items
            totalItemCount = state.items.size
            isRefreshing = state.isRefreshing
            isLoadingMore = state.isLoadingMore
            refreshError = state.refreshError
            loadMoreError = state.loadMoreError
            isEndReached = state.isEndReached
            bottomReachedLoadMore = state.bottomReachedLoadMore

            visibleLoadingStateView = isLoadingMore && !state.isRefreshing && state.refreshError==null && !state.isEndReached

            visibleRefreshErrorStateView = refreshError != null && isEndReached && totalItemCount == 0

            visibleEmptyStateView = !visibleRefreshErrorStateView && isEndReached && totalItemCount == 0

            visibleLoadMoreErrorStateView = loadMoreError != null && totalItemCount != 0

            visibleEndReachedStateView = isEndReached && totalItemCount != 0

            if (totalItemCount == 0 && bottomReachedLoadMore) {
                bottomReachedLoadMore = false
                paginator.log("当前页面数据为空，但是还没加载完所有数据，加载下一页")
                paginator.loadMore()
            }
        }
    }

}


fun <T> LazyListScope.pagingItems(
    pagingItems: LazyPagingItems<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    itemContent: @Composable LazyItemScope.(index: Int, item: T) -> Unit,
) {
    val itemKey: ((index: Int) -> Any)? = if (key == null) {
        null
    } else {
        { index ->
            pagingItems.items.getOrNull(index)?.run { key.invoke(index, this) } ?: index
        }
    }
    items(
        count = pagingItems.totalItemCount,
        key = itemKey,
        contentType = { index ->
            pagingItems[index]?.let { item ->
                contentType(index, item)
            }
        },
        itemContent = { index ->
            pagingItems[index]?.let { item ->
                itemContent(index, item)
            }
        }
    )
}


fun <T> LazyGridScope.pagingItems(
    pagingItems: LazyPagingItems<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    span: (LazyGridItemSpanScope.(index: Int) -> GridItemSpan)? = null,
    contentType: (index: Int) -> Any? = { null },
    itemContent: @Composable LazyGridItemScope.(index: Int, item: T) -> Unit,
) {
    val itemKey: ((index: Int) -> Any)? = if (key == null) {
        null
    } else {
        { index ->
            pagingItems.items.getOrNull(index)?.run { key.invoke(index, this) } ?: index
        }
    }
    items(
        count = pagingItems.totalItemCount,
        key = itemKey,
        span = span,
        contentType = contentType,
        itemContent = { index ->
            pagingItems[index]?.let { item ->
                itemContent(index, item)
            }
        }
    )
}

fun <T> LazyStaggeredGridScope.pagingItems(
    pagingItems: LazyPagingItems<T>,
    key: ((index: Int, item: T) -> Any)? = null,
    contentType: (index: Int) -> Any? = { null },
    itemContent: @Composable LazyStaggeredGridItemScope.(index: Int, item: T) -> Unit,
) {
    val itemKey: ((index: Int) -> Any)? = if (key == null) {
        null
    } else {
        { index ->
            pagingItems.items.getOrNull(index)?.run { key.invoke(index, this) } ?: index
        }
    }
    items(
        count = pagingItems.totalItemCount,
        key = itemKey,
        contentType = contentType,
        itemContent = { index ->
            pagingItems[index]?.let { item ->
                itemContent(index, item)
            }
        }
    )
}
