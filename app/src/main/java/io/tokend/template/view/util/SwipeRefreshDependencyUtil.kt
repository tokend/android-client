package io.tokend.template.view.util

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.AppBarLayout

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