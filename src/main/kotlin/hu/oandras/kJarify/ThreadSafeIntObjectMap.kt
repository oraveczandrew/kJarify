/*
 * Copyright (C) 2025 András Oravecz and the contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

@file:Suppress("unused")

package hu.oandras.kJarify

import androidx.annotation.GuardedBy
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal class ThreadSafeIntObjectMap<T>(initialCapacity: Int = 10) {

    @PublishedApi
    @JvmField
    @GuardedBy("writeLock,readLock")
    internal val map: MutableIntObjectMap<T> = MutableIntObjectMap(initialCapacity)

    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    @JvmField
    val readLock: ReentrantReadWriteLock.ReadLock = lock.readLock()

    @JvmField
    val writeLock: ReentrantReadWriteLock.WriteLock = lock.writeLock()

    private fun ensureNotInReadLock() {
        val lock = lock
        check(!(!lock.isWriteLockedByCurrentThread && lock.readHoldCount > 0))
    }

    operator fun set(key: Int, value: T) {
        ensureNotInReadLock()
        writeLock.withLock {
            map.put(key, value)
        }
    }

    fun remove(key: Int) {
        ensureNotInReadLock()
        writeLock.withLock {
            map.remove(key)
        }
    }

    fun removeBy(key: Int) {
        remove(key)
    }

    fun clear() {
        ensureNotInReadLock()
        writeLock.withLock {
            map.clear()
        }
    }

    val size: Int
        get() = map.size

    fun isNotEmpty(): Boolean {
        return !isEmpty()
    }

    fun putAll(from: IntObjectMap<T>) {
        writeLock.withLock {
            map.putAll(from)
        }
    }

    operator fun get(key: Int): T? {
        readLock.withLock {
            return map[key]
        }
    }

    fun getOrDefault(key: Int, ifAbsent: T): T {
        readLock.withLock {
            return map.getOrDefault(key, ifAbsent)
        }
    }

    fun containsKey(key: Int): Boolean {
        return readLock.withLock {
            map.containsKey(key)
        }
    }

    fun containsValue(value: T): Boolean {
        return readLock.withLock {
            map.containsValue(value)
        }
    }

    fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    val values: List<T>
        get() = readLock.withLock {
            val map = map
            val size = map.size
            if (size == 0) {
                return emptyList()
            }

            val values = ArrayList<T>(size)
            map.forEachValue {
                values.add(it)
            }
            return@withLock values
        }

    fun put(key: Int, value: T) {
        set(key, value)
    }

    inline fun getOrPut(
        key: Int,
        isInterruptible: Boolean = false,
        atomicCreate: Boolean = true,
        defaultValue: () -> T
    ): T {
        val value = readLock.withLock(isInterruptible) {
            get(key)
        }

        if (value != null) {
            return value
        }

        return if (atomicCreate) {
            writeLock.withLock(isInterruptible) {
                val value2 = map[key]
                if (value2 != null) {
                    value2
                } else {
                    val newValue = defaultValue.invoke()
                    set(key, newValue)
                    newValue
                }
            }
        } else {
            val newValue = defaultValue.invoke()

            writeLock.withLock(isInterruptible) {
                set(key, newValue)
            }

            newValue
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T> Lock.withLock(
    isInterruptible: Boolean = false,
    action: () -> T
): T {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    if (isInterruptible) {
        lockInterruptibly()
    } else {
        lock()
    }

    try {
        return action()
    } finally {
        unlock()
    }
}