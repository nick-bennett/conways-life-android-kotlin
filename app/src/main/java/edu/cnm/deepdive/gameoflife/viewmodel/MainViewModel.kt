/*
 *  Copyright 2021 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.gameoflife.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import edu.cnm.deepdive.gameoflife.model.Terrain
import androidx.lifecycle.LiveData
import edu.cnm.deepdive.gameoflife.viewmodel.MainViewModel
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Lifecycle
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application), LifecycleObserver {

    private val terrain: MutableLiveData<Terrain?>
    private val generation: MutableLiveData<Long>
    private val population: MutableLiveData<Int>
    private val rng: Random
    private var runner: Runner? = null

    private val _running: MutableLiveData<Boolean>
    val running: LiveData<Boolean>
        get() = _running
    val density: MutableLiveData<Int>

    init {
        terrain = MutableLiveData(null)
        generation = MutableLiveData(0L)
        population = MutableLiveData(0)
        _running = MutableLiveData(false)
        density = MutableLiveData(DEFAULT_DENSITY)
        rng = Random()
        reset()
    }

    fun getTerrain(): LiveData<Terrain?> {
        return terrain
    }

    fun getGeneration(): LiveData<Long> {
        return generation
    }

    fun getPopulation(): LiveData<Int> {
        return population
    }

    fun start() {
        stopRunner(false)
        _running.value = true
        startRunner()
    }

    fun stop() {
        stopRunner(true)
    }

    fun reset() {
        stop()
        val terrain = Terrain(DEFAULT_TERRAIN_SIZE, density.value!! / 100.0, rng).also {
            this.terrain.value = it
            generation.value = it.iterationCount
            population.value = it.population
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun pause() {
        stopRunner(!(_running.value ?: false))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resume() {
        if (_running.value ?: false)  {
            startRunner()
        }
    }

    private fun startRunner() {
        runner = Runner().also {
            it.start()
        }
    }

    private fun stopRunner(postOnStop: Boolean) {
        runner?.let {
            it.postOnStop = postOnStop
            it.running = false
        }
        runner = null
    }

    private inner class Runner : Thread() {

        var running = true
        var postOnStop = false

        override fun run() {
            while (running) {
                terrain.value?.let {
                    it.iterate()
                    generation.postValue(it.iterationCount)
                    population.postValue(it.population)
                }
            }
            if (postOnStop) {
                this@MainViewModel._running.postValue(false)
            }
        }

    }

    companion object {
        private const val DEFAULT_TERRAIN_SIZE = 500
        private const val DEFAULT_DENSITY = 20
    }

}