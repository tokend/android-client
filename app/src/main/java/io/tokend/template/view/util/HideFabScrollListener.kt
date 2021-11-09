package io.tokend.template.view.util

import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionMenu

open class HideFabScrollListener(
    private val fab: FloatingActionMenu
) : RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (dy > 2) {
            fab.hideMenuButton(true)
        } else if (dy < -2 && fab.isEnabled) {
            fab.showMenuButton(true)
        }
    }
}