# compose-paginator - compose 列表分页加载，支持增删改查  [![](https://jitpack.io/v/TroyLi-Dev/compose-paginator.svg)](https://jitpack.io/#TroyLi-Dev/compose-paginator)

A production-ready, coroutine-powered pagination engine for Jetpack Compose.

轻量级、高可扩展、线程安全的分页抽象层，聚焦易用性与业务弹性，支持 Refresh / LoadMore / Retry / 本地增量更新等全链路能力

🚀 Features

· 全链路分页能力：刷新、加载更多、错误重试。

· 线程安全数据管控：内部通过 Mutex 保证状态一致性。

· 可插拔分页策略：支持页码、游标、自定义策略等。

· 强可观测状态模型：可直接驱动 Compose UI。

· 可定制合并策略：去重、插队、排序、服务端合入等。

· 内置超时处理：每个加载动作均可配置超时。

· 一键开启 Debug 日志：快速诊断问题。

· 天然适配 Compose Lazy 列表。

## 使用示例 Demo

你可以在仓库中查看完整示例：  

 demo：[Demo 示例](app/src/main/java/com/rui/compose/paginator/MainActivity.kt)

To get a Git project into your build:

Step 1. Add the JitPack repository to your build file

Add it in your root settings.gradle at the end of repositories:
```
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```
Step 2. Add the dependency
```

	dependencies {
	        implementation 'com.github.TroyLi-Dev:compose-paginator:<latest-version>'
	}

```


