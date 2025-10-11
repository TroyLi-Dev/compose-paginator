package com.rui.compose.paginator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description: 收集分页数据
 * Copyright (c) 2025 All rights reserved.
 */


@Composable
fun <T : Any> Paginator<T, *>.collectAsLazyPagingItems(
    context: CoroutineContext = EmptyCoroutineContext,
): LazyPagingItems<T> {
    val lazyPagingItems = remember(this) { LazyPagingItems<T>(this) }
    LaunchedEffect(lazyPagingItems) {
        if (context == EmptyCoroutineContext) {
            lazyPagingItems.collectPagingData()
        } else {
            withContext(context) {
                lazyPagingItems.collectPagingData()
            }
        }
    }
    return lazyPagingItems
}