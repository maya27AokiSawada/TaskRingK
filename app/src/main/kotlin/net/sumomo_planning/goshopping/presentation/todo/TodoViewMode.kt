package net.sumomo_planning.goshopping.presentation.todo

/** ToDo ビューの表示モード (list_views_spec.md §3-1) */
enum class TodoViewMode(val label: String) {
    DAY("日"),
    WEEK("週"),
    MONTH("月"),
}
