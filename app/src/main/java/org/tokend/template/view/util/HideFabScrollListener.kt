package org.tokend.template.view.util

import androidx.recyclerview.widget.RecyclerView
import com.github.clans.fab.FloatingActionMenu

open class HideFabScrollListener(
        private val fab: FloatingActionMenu
) : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
    override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
        if (dy > 2) {
            fab.hideMenuButton(true)
        } else if (dy < -2 && fab.isEnabled) {
            fab.showMenuButton(true)
        }
    }
}