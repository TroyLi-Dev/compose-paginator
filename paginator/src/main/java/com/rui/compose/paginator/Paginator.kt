package com.rui.compose.paginator


import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description: Jetpack Compose 通用分页器，用于支持刷新、加载更多、错误重试和状态管理。
 * Copyright (c) 2025 All rights reserved.
 *
 * @param T 数据项类型
 * @param P 分页参数类型（例如页码或游标）
 * @property scope 使用的 CoroutineScope，推荐使用 ViewModelScope
 * @property loadPage 加载指定分页参数的数据
 * @property strategy 分页策略，决定初始 key 及如何生成下一页的 key
 * @property config 配置项，包括超时、合并策略、是否开启日志等
 *
 * 功能：
 * - 支持刷新（refresh）和加载更多（loadMore）
 * - 自动维护分页状态（如是否正在加载、是否到底等）
 * - 支持错误重试（retry）
 * - 线程安全的数据更新（内部使用 Mutex）
 *
 * 用法示例：
 * ```
 * val paginator = Paginator.Builder<MyItem, Int>()
 *     .paginationStrategy(PageNumberStrategy())
 *     .loadPage(::getList)
 *     .config {
 *          openLogger(true)
 *          setTimeoutMillis(timeoutMillis = 500)
 *     }
 *     .build(viewModelScope)
 *
 * paginator.refresh() //刷新
 * paginator.loadMore() //加载
 * paginator.retry() //重试
 * paginator.updateItems{  } //更新items
 *
 * private suspend fun getList(page: Int): PageResult<String, Int> {
 *     return PageResult(
 *            items = emptyList(), //列表数据
 *            nextKey = page+1, //paginationStrategy(PageNumberStrategy())内部，如果设置了会使用nextKey，不设置则使用内部默认方式
 *            isEndReached = false //是否已经加载完成
 *     )
 * }
 *
 *  使用
 *     val lazyPagingItems = paginator.collectAsLazyPagingItems()
 *      pagingItems(pagingItems = lazyPagingItems) { index,item->
 *          Text(item)
 *      }
 *
 * ```
 */

class Paginator<T, P> private constructor(
    private val scope: CoroutineScope,
    private val loadPage: suspend (P) -> PageResult<T, P>,
    private val strategy: PaginationStrategy<P>,
    internal val config: PaginatorConfig<T>,
) {
    internal val state = MutableStateFlow(value = PaginatorState<T>())
    private var currentKey: P = strategy.initialKey()
    private var refreshJob: Job? = null
    private var loadMoreJob: Job? = null
    private val loadMutex = Mutex()

    internal fun log(message: String) {
        if (config.openLogger) {
            config.logger(message)
        }
    }

    fun refresh() {
        log("开始刷新")
        refreshJob?.cancel()
        loadMoreJob?.cancel()
        refreshJob = scope.launch {
            loadMutex.withLock {
                state.update {
                    it.copy(
                        updateTime = System.currentTimeMillis(),
                        isRefreshing = true,
                        refreshError = null,
                        isLoadingMore = false,
                        loadMoreError = null,
                        bottomReachedLoadMore = false
                    )
                }
                try {
                    withTimeout(config.timeoutMillis) {
                        val initialKey = strategy.initialKey()
                        log("请求刷新数据，起始页Key = $initialKey")
                        val result = loadPage(initialKey)
                        currentKey = strategy.nextKeyAfter(initialKey, result) ?: initialKey
                        log("刷新成功，加载项数：${result.items.size}，是否已到底：${result.isEndReached}，下一页Key：$currentKey")

                        state.update {
                            it.copy(
                                updateTime = System.currentTimeMillis(),
                                items = result.items,
                                isRefreshing = false,
                                isEndReached = result.isEndReached,
                                bottomReachedLoadMore = !result.isEndReached
                            )
                        }
                    }
                } catch (t: Throwable) {
                    ensureActive()
                    log("刷新失败：${t.localizedMessage}")
                    state.update {
                        it.copy(
                            updateTime= System.currentTimeMillis(),
                            isRefreshing = false,
                            refreshError = t,
                            isEndReached = true
                        )
                    }
                }
            }
        }
    }

    fun loadMore() {
        if (state.value.isLoadingMore || state.value.isRefreshing || state.value.isEndReached) {
            log("跳过加载更多：正在加载中=${state.value.isLoadingMore}，正在刷新=${state.value.isRefreshing}，已到底=${state.value.isEndReached}")
            return
        }

        log("开始加载更多，当前页Key = $currentKey")

        loadMoreJob?.cancel()
        loadMoreJob = scope.launch {
            loadMutex.withLock {
                state.update {
                    it.copy(
                        updateTime= System.currentTimeMillis(),
                        isLoadingMore = true,
                        loadMoreError = null,
                        bottomReachedLoadMore = false
                    )
                }
                try {
                    withTimeout(config.timeoutMillis) {
                        val result = loadPage(currentKey)
                        val nextKey = strategy.nextKeyAfter(currentKey, result) ?: currentKey
                        log("加载更多成功，新加载项数：${result.items.size}，是否已到底：${result.isEndReached}，下一页Key：$nextKey")

                        currentKey = nextKey

                        state.update {
                            it.copy(
                                updateTime= System.currentTimeMillis(),
                                items = config.mergeStrategy(it.items, result.items),
                                isLoadingMore = false,
                                isEndReached = result.isEndReached,
                                bottomReachedLoadMore = !result.isEndReached
                            )
                        }
                    }
                } catch (t: Throwable) {
                    ensureActive()
                    log("加载更多失败：${t.localizedMessage}")
                    state.update {
                        it.copy(
                            updateTime= System.currentTimeMillis(),
                            isLoadingMore = false,
                            loadMoreError = t
                        )
                    }
                }
            }
        }
    }

    fun getItems(): List<T> {
        return state.value.items
    }


    fun updateItems(action: (List<T>) -> List<T>) {
        scope.launch {
            loadMutex.withLock {
                log("在本地更新项目.")
                state.update {
                    it.copy(
                        updateTime= System.currentTimeMillis(),
                        items = action(it.items)
                    )
                }
            }
        }
    }


    fun retry() {
        if (state.value.refreshError != null) {
            log("重试刷新")
            refresh()
        } else if (state.value.loadMoreError != null) {
            log("重试加载更多")
            loadMore()
        }
    }

    class Builder<T, P> {
        private var _loadPage: (suspend (P) -> PageResult<T, P>)? = null
        private var _strategy: PaginationStrategy<P>? = null
        private var _config: PaginatorConfig<T> = PaginatorConfig()

        /** 设置自定义配置 */
        fun config(config: PaginatorConfig<T>) = apply {
            this._config = config
        }

        /** 设置自定义配置 (链式) */
        fun config(block: PaginatorConfig<T>.() -> Unit) = apply {
            val newConfig = this._config.copy()
            newConfig.block()
            this._config = newConfig
        }

        /** 设置分页策略 (必需) */
        fun paginationStrategy(strategy: PaginationStrategy<P>) = apply {
            this._strategy = strategy
        }

        /** 设置数据加载函数 (必需) */
        fun loadPage(loader: suspend (P) -> PageResult<T, P>) = apply {
            this._loadPage = loader
        }

        fun build(scope: CoroutineScope): Paginator<T, P> {
            return Paginator(
                scope = scope,
                loadPage = this._loadPage ?: throw IllegalStateException("LoadPage not set"),
                strategy = this._strategy
                    ?: throw IllegalStateException("PaginationStrategy not set"),
                config = this._config
            )
        }
    }
}
