package de.halfbit.knot3.sample.books2

import androidx.lifecycle.ViewModel
import de.halfbit.knot3.CompositeKnot
import de.halfbit.knot3.compositeKnot
import de.halfbit.knot3.sample.books2.delegates.ClearButtonDelegate
import de.halfbit.knot3.sample.books2.delegates.LoadButtonDelegate
import de.halfbit.knot3.sample.books2.model.Event
import de.halfbit.knot3.sample.books2.model.State
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.functions.Consumer

abstract class BooksViewModel : ViewModel() {
    abstract val state: Observable<State>
    abstract val event: Consumer<Event>
}

interface Delegate {
    fun CompositeKnot<State>.register()
    fun CompositeKnot<State>.onEvent(event: Event): Boolean = false
}

class DefaultBooksViewModel(
    private val delegates: List<Delegate> = listOf(
        LoadButtonDelegate(),
        ClearButtonDelegate()
    ),
    private val observeOnScheduler: Scheduler = AndroidSchedulers.mainThread()
) : BooksViewModel() {

    override val state: Observable<State>
        get() = knot.state

    override val event: Consumer<Event> =
        Consumer<Event> { event ->
            delegates.any {
                with(it) { knot.onEvent(event) }
            }
        }

    private val knot = compositeKnot<State> {
        state {
            initial = State.Empty
            observeOn = observeOnScheduler
        }
    }

    init {
        for (delegate in delegates) {
            with(delegate) { knot.register() }
        }
        knot.compose()
    }
}
