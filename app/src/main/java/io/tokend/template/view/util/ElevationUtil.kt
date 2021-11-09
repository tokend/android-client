package io.tokend.template.view.util

import android.view.View
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout

object ElevationUtil {

    fun initScrollElevation(scrollView: NestedScrollView, elevationView: View) {
        elevationView.visibility = View.GONE

        var wasInScroll = false
        val animationDuration =
            scrollView.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        scrollView.setOnScrollChangeListener { _: NestedScrollView?,
                                               _: Int,
                                               scrollY: Int,
                                               _: Int,
                                               _: Int ->
            val inScroll = scrollY > 0.01f
            if (inScroll != wasInScroll) {
                if (inScroll) {
                    AnimationUtil.fadeInView(elevationView, animationDuration)
                } else {
                    AnimationUtil.fadeOutView(elevationView, animationDuration)
                }
                wasInScroll = inScroll
            }
        }
    }

    fun initScrollElevation(recyclerView: RecyclerView, elevationView: View) {
        elevationView.visibility = View.GONE

        var onTop = true
        val animationDuration =
            recyclerView.context.resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong()

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy <= 0 && !recyclerView.canScrollVertically(-1)) {
                    AnimationUtil.fadeOutView(elevationView, animationDuration)
                    onTop = true
                } else if (dy > 0 && onTop) {
                    AnimationUtil.fadeInView(elevationView, animationDuration)
                    onTop = false
                }
            }
        })
    }

    fun initScrollElevation(movingAppbar: AppBarLayout, elevationView: View) {
        elevationView.visibility = View.GONE

        var onZeroOffset = true
        val animationDuration =
            movingAppbar.context.resources.getInteger(android.R.integer.config_shortAnimTime)
                .toLong()

        movingAppbar.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                if (verticalOffset < 0 && onZeroOffset) {
                    AnimationUtil.fadeInView(elevationView, animationDuration)
                    onZeroOffset = false
                } else if (verticalOffset == 0 && !onZeroOffset) {
                    AnimationUtil.fadeOutView(elevationView, animationDuration)
                    onZeroOffset = true
                }
            }
        )
    }
}