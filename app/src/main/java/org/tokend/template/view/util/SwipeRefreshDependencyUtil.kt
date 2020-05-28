package org.tokend.template.view.util

import com.google.android.material.appbar.AppBarLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

object SwipeRefreshDependencyUtil {

    fun addDependency(swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout, appBar: AppBarLayout) {
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