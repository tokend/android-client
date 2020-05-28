package org.tokend.template.fragments

import androidx.appcompat.widget.Toolbar
import io.reactivex.subjects.BehaviorSubject

interface ToolbarProvider {
    val toolbarSubject: BehaviorSubject<Toolbar>
}