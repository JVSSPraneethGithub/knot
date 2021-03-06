![](https://img.shields.io/badge/production-ready-brightgreen.svg)
[![Build Status](https://travis-ci.org/beworker/knot.svg?branch=master)](https://travis-ci.org/beworker/knot)
[![codecov](https://codecov.io/gh/beworker/knot/branch/master/graph/badge.svg)](https://codecov.io/gh/beworker/knot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

# 🧶 Knot

Concise reactive state container library for Android applications.

# Concept

Knot helps managing application state by reacting on events and performing asynchronous actions in a structured way. There are five core concepts Knot defines: `State`, `Change`, `Action`, `Reducer` and `Effect`.

<img src="docs/diagrams/flowchart-knot.png" height="480" />

`State` represents an immutable state of an application. It can be a state of a screen or a state of an internal statefull headless component.

`Change` is an immutable data object with an optional payload intended for changing the `State`. A `Change` can be produced from an external source or be a result of execution of an `Action`.

`Action` is a synchronous or an asynchronous operation which, when completed, can – but doesn't have to – emit a new `Change`.

`Reducer` is a function that takes the previous `State` and a `Change` as arguments and returns the new `State` and an optional `Action` wrapped by the `Effect` class. `Reducer` in Knot is designed to stay side-effects free because each side-effect can be turned into an `Action` and returned from the reducer function together with a new state in a pure way.

`Effect` is a convenient wrapper class containing the new `State` and an optional `Action`. If `Action` is present, Knot will perform it and provide resulting `Change` (if any) back to the `Reducer`.

In addition to that each Knot can subscribe to `Events` coming from external sources and turn them into `Changes` for further processing.

# Main features

- **Clean DSL**. Knot provides a concise and easy to read DLS for declaring state container logic.
- **Actions as side-effects**. Common unanswered question of many state containers is how to handle side-effects in reducer, when the state does not reflect the change but the app, for instance, has to show a message to the user instead. Knot answers this by allowing to issue an `Action` from the reducer, which then can be processed outside. This keeps reducer a pure function and provides a structured way of handling side-effects.
- **Sources for external events**. Knot can mix external events like user location updates, database triggers, network state updates etc. into the loop by design. All events go through the reducer first, to be processed in accordance with the current state. Thus, the state handling is well synchronized and is localized at a single place - in the reducer.
- **Composition**. Knot offers a convenient and a structured way of splitting once big redicer into multiple smaller reducers when the code becomes complex.

# Getting Started

The example below declares a Knot capable of loading data, handling *Success* and *Failure* loading results and reloading data automatically when an external *"data changed"* signal gets received. It also logs all `State` mutations as well as all processed `Changes` and `Actions` in console.

```kotlin
sealed class State {
   object Initial : State()
   object Loading : State()
   data class Content(val data: String) : State()
   data class Failed(val error: Throwable) : State()
}

sealed class Change {
   object Load : Change() {
      data class Success(val data: String) : Change()
      data class Failure(val error: Throwable) : Change()
   }
}

sealed class Action {
   object Load : Action()
}

val knot = knot<State, Change, Action> {
    state { 
        initial = State.Initial 
    }
    changes {
        reduce { change ->
            when (change) {
                is Change.Load -> State.Loading + Action.Load
                is Change.Load.Success -> State.Content(data).only
                is Change.Load.Failure -> State.Failed(error).only
            }
        }
    }
    actions {
        perform<Action.Load> {
            switchMapSingle<String> { 
                loadData()
                    .map<Change> { Change.Load.Success(it) }
                    .onErrorReturn { Change.Load.Failure(it) }
            }
        }
    }
    events {
        source {
            dataChangeObserver.signal.map { Change.Load }
        }
    }
}

val states = knot.state.test()
knot.change.accept(Change.Load)

states.assertValues(
    State.Initial,
    State.Loading,
    State.Content("data")
)
```

Notice how inside the `reduce` function a new `State` can be combined with an `Action` using `+` operator. If only the `State` value should be returned from the reducer, the `.only` suffix is added to the `State`.

# Documentation
1. [Knot Sample App](https://github.com/beworker/knot/tree/master/knot3-android-sample/src/main/kotlin/de/halfbit/knot3/sample) the first place to look.
2. [Terminal events in Actions section](https://github.com/beworker/knot/wiki/Terminal-events-in-Actions-section)
3. [Composite ViewModel](https://www.halfbit.de/posts/composite-viewmodel/)
4. [Troubleshooting](https://github.com/beworker/knot/wiki/Troubleshooting)

# Other examples
- [Co2Monitor sample app](https://github.com/beworker/co2monitor/blob/master/android-client/main-dashboard/src/main/java/de/halfbit/co2monitor/main/dashboard/DashboardViewModel.kt)

# Composition

If your knot becomes complex and you want to improve its readability and maintainability, you may consider to write a composite knot. You start composition by grouping related functionality into, in a certain sense, indecomposable pieces called `Primes`. 

<img src="docs/diagrams/flowchart-composite-knot.png" width="625" />

Each `Prime` is isolated from the other `Primes`. It defines its own set of `Changes`, `Actions` and `Reducers`. It's only the `State`, what is shared between the `Primes`. In that respect each `Prime` can be seen as a separate `Knot` working on a shared `State`. Once all `Primes` are defined, they can be composed together and provided to `CompositeKnot` which implements standard `Knot` interface. For more information check out [PrimeTest](https://github.com/beworker/knot/blob/master/knot3/src/test/kotlin/de/halfbit/knot3/PrimeTest.kt) class.

# Why Knot?

* Predictable - state is the single source of truth.
* Side-effect free reducer - by design.
* Scalable - single knots can be combined together to build more complex application logic.
* Decomposable - complex knots can be decomposed into primes by related functionality.
* Structured - easy to read and write DSL for writing better structured and less buggy code.
* Concise - it has minimalistic API and compact implementation.
* Testable - reducers and transformers are easy to test.
* Production ready - Knot is used in production.
* Why not?

# RxJava3 Binaries [![Maven Central](http://img.shields.io/maven-central/v/de.halfbit/knot3.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.halfbit%22%20a%3A%22knot3%22)
```kotlin
allprojects {
    repositories {
        mavenCentral()
    }
}
dependencies {
    implementation "de.halfbit:knot3:<version>"
    
    // Becase Knot is not released for each and every RxJava version, 
    // it is recommended you also explicitly depend on RxJava's latest 
    // version for bug fixes and new features.
    implementation 'io.reactivex.rxjava3:rxjava:3.0.4'    
}
```

# RxJava2 Binaries [![Maven Central](http://img.shields.io/maven-central/v/de.halfbit/knot.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22de.halfbit%22%20a%3A%22knot%22)

```kotlin
allprojects {
    repositories {
        mavenCentral()
    }
}
dependencies {
    implementation "de.halfbit:knot:<version>"
    
    // Becase Knot is not released for each and every RxJava version, 
    // it is recommended you also explicitly depend on RxJava's latest 
    // version for bug fixes and new features.
    implementation 'io.reactivex.rxjava2:rxjava:2.2.19'
}
```

# Inspiration
Knot was inspired by two awesome projects
* Krate https://github.com/gustavkarlsson/krate
* Redux-loop https://github.com/redux-loop/redux-loop

# License
```
Copyright 2019, 2020 Sergej Shafarenka, www.halfbit.de

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
