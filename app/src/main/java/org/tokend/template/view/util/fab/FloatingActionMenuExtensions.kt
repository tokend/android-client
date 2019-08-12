package org.tokend.template.view.util.fab

import android.view.View
import com.github.clans.fab.FloatingActionMenu

fun FloatingActionMenu.addActions(actions: Collection<FloatingActionMenuAction>) {
    if (actions.isEmpty()) {
        visibility = View.GONE
        isEnabled = false
    } else {
        visibility = View.VISIBLE
        isEnabled = true
        actions.forEach(this::addAction)
    }
}

fun FloatingActionMenu.addAction(action: FloatingActionMenuAction) {
    addMenuButton(action.toButton(context))
}