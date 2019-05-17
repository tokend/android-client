package org.tokend.template.view.util

import android.support.design.widget.AppBarLayout
import android.support.v4.widget.SwipeRefreshLayout

object SwipeRefreshDependencyUtil {

    fun addDependency(swipeRefresh: SwipeRefreshLayout, appBar: AppBarLayout) {
        var canRefresh = true

        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            canRefresh
        }

        appBar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
                canRefresh = verticalOffset != 0
            }
        })
    }
}