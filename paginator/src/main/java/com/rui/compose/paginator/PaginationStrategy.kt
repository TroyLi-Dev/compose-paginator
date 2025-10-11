package com.rui.compose.paginator


/**
 * @author: rui
 * @date: 2025/10/10 14:42
 * @description:
 * Copyright (c) 2025 All rights reserved.
 */
interface PaginationStrategy<P> {
    fun initialKey(): P
    fun nextKeyAfter(currentKey: P, pageResult: PageResult<*, P>): P?
}