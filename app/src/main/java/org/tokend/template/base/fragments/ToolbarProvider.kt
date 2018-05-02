package org.tokend.template.base.fragments

import android.support.v7.widget.Toolbar
import io.reactivex.subjects.BehaviorSubject

interface ToolbarProvider {
    val toolbarSubject: BehaviorSubject<Toolbar>
}