package com.rui.compose.paginator

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val paginator = Paginator.Builder<String, Int>()
        .paginationStrategy(PageNumberStrategy())
        .loadPage(::getList)
        .config {
            openLogger(true)
        }
        .build(lifecycleScope)

    private var isTestRefreshRetry = true
    private var isTestLoadRetry = true

    private suspend fun getList(page: Int): PageResult<String, Int> {
        delay(3000)

        if (page == 1 && isTestRefreshRetry) {
            isTestRefreshRetry = false
            throw IOException("测试一次刷新失败")
        }

        if (page == 2 && isTestLoadRetry) {
            isTestLoadRetry = false
            throw IOException("测试一次加载失败")
        }

        val items = (0..20).map {
            "page $page item $it"
        }
        return PageResult(
            items = items,
            isEndReached = items.isEmpty() //是否已达到终点
        )
    }

    private var addItemIndex = 1

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val lazyPagingItems = paginator.collectAsLazyPagingItems()
            val pullRefreshState = rememberPullRefreshState(
                refreshing = lazyPagingItems.isRefreshing,
                onRefresh = paginator::refresh
            )
            MaterialTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Paginator") },
                            actions = {
                                TextButton(
                                    onClick = {
                                        paginator.updateItems {
                                            listOf("add item $addItemIndex") + it
                                        }
                                        addItemIndex++
                                    }
                                ) {
                                    Text("add top")
                                }
                                TextButton(
                                    onClick = {
                                        paginator.updateItems {
                                            it + listOf("add item $addItemIndex")
                                        }
                                        addItemIndex++
                                    }
                                ) {
                                    Text("add bottom")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .pullRefresh(state = pullRefreshState)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            pagingItems(lazyPagingItems) { index, item ->
                                Text(
                                    modifier = Modifier
                                        .animateItem()
                                        .fillMaxWidth()
                                        .clickable(onClick = {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "删除 $item",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            paginator.updateItems { items ->
                                                items.filter { it != item }
                                            }
                                        })
                                        .padding(vertical = 16.dp, horizontal = 24.dp),
                                    text = item
                                )
                            }
                            if (lazyPagingItems.visibleLoadingStateItem) {
                                //加载更多
                                item(
                                    key = "LoadMoreStatePage",
                                    contentType = "LoadMoreStatePage",
                                ) {
                                    Box(modifier = Modifier.fillParentMaxWidth()) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(vertical = 16.dp)
                                                .size(size = 36.dp),
                                            strokeWidth = 3.dp
                                        )
                                    }
                                }
                            }
                            if (lazyPagingItems.visibleEmptyStateItem) {
                                item(
                                    key = "EmptyStatePage",
                                    contentType = "EmptyStatePage",
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            modifier = Modifier,
                                            text = "无数据",
                                        )
                                    }
                                }
                            }
                            if (lazyPagingItems.visibleRefreshErrorStateItem) {
                                item(
                                    key = "RefreshErrorStatePage",
                                    contentType = "RefreshErrorStatePage",
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillParentMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            modifier = Modifier,
                                            text = "刷新失败，请重试",
                                        )
                                    }
                                }
                            }
                            if (lazyPagingItems.visibleLoadMoreErrorStateItem) {
                                item(
                                    key = "LoadMoreFailureStatePage",
                                    contentType = "LoadMoreFailureStatePage",
                                ) {
                                    Box(
                                        modifier = Modifier.fillParentMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(vertical = 16.dp)
                                                .clip(shape = CircleShape)
                                                .border(
                                                    width = 1.dp,
                                                    color = Color(0xFFFF0000),
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                modifier = Modifier
                                                    .padding(
                                                        horizontal = 16.dp,
                                                        vertical = 4.dp
                                                    )
                                                    .clickable(onClick = {
                                                        paginator.retry()
                                                    }),
                                                color = Color(0xFFFF0000),
                                                text = "重试",
                                            )
                                        }
                                    }
                                }
                            }


                        }

                        PullRefreshIndicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            state = pullRefreshState,
                            refreshing = lazyPagingItems.isRefreshing
                        )
                    }
                }
            }
        }
    }
}

