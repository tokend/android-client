package io.tokend.template.view.util

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.transition.Fade
import androidx.transition.TransitionManager
import com.jakewharton.rxbinding2.support.v7.widget.RxSearchView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.PublishSubject
import io.tokend.template.R
import java.util.concurrent.TimeUnit

/**
 * Manages interaction and Rx for search field from the menu.
 */
class MenuSearchViewManager(
    private val searchMenuItem: MenuItem,
    private val toolbar: ViewGroup,
    compositeDisposable: CompositeDisposable,
    onActionExpand: (() -> Boolean)? = null,
    onActionCollapse: (() -> Boolean)? = null
) {
    private val searchView: SearchView = searchMenuItem.actionView as? SearchView
        ?: throw IllegalArgumentException("Given menu item has no SearchView action view")

    private val context = searchView.context

    private var currentQuery: String? = null
    private val queryChangesSubject = PublishSubject.create<String>()

    init {
        (searchView.findViewById(androidx.appcompat.R.id.search_src_text) as? EditText)
            ?.apply {
                setHintTextColor(ContextCompat.getColor(context!!, R.color.secondary_text))
                setTextColor(ContextCompat.getColor(context!!, R.color.primary_text))
            }

        searchView.setOnQueryTextFocusChangeListener { _, focused ->
            if (!focused && searchView.query.isBlank()) {
                searchMenuItem.collapseActionView()
            }
        }

        RxSearchView.queryTextChanges(searchView)
            .skipInitialValue()
            .debounce(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { query ->
                publishQueryChange(query.trim().toString().takeIf { it.isNotEmpty() })
            }
            .addTo(compositeDisposable)

        // Publish query update immediately on close button click.
        searchView
            .findViewById<View>(R.id.search_close_btn)
            ?.setOnClickListener {
                searchView.setQuery("", false)
                publishQueryChange(null)
            }

        // Use fade it animation for expand.
        searchMenuItem.setOnMenuItemClickListener {
            TransitionManager.beginDelayedTransition(
                toolbar, Fade().setDuration(
                    context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
                )
            )
            searchMenuItem.expandActionView()
            true
        }

        // Publish query update immediately on search collapse.
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return onActionExpand?.invoke() ?: true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                publishQueryChange(null)
                return onActionCollapse?.invoke() ?: true
            }
        })
    }

    /**
     * A text that will be displayed in the query text field.
     */
    var queryHint: CharSequence?
        get() = searchView.queryHint
        set(value) {
            searchView.queryHint = value
        }

    /**
     * Current query.
     */
    val query: String?
        get() = currentQuery

    /**
     * An observable that emits query changes with debounce for user input.
     */
    val queryChanges: Observable<String>
        get() = queryChangesSubject

    private fun publishQueryChange(newQuery: String?) {
        if (currentQuery != newQuery) {
            currentQuery = newQuery
            queryChangesSubject.onNext(newQuery ?: "")
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 400L
    }
}