package com.hendraanggrian.defaults

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.WeakHashMap

interface Defaults<E : Defaults.Editor> {

    companion object {
        const val TAG = "Defaults"

        internal var DEBUGGER: DefaultsDebugger? = null
        private lateinit var BINDINGS: MutableMap<Class<*>, Constructor<Saver>>
        internal val EMPTY_SAVER: Saver = object : Saver {
            override fun save() {}
            override fun saveAsync() {}
        }

        fun setDebug(debug: DefaultsDebugger?) {
            DEBUGGER = debug
        }

        @Suppress("UNCHECKED_CAST")
        internal fun findBindingConstructor(cls: Class<*>): Constructor<Saver>? {
            if (!Companion::BINDINGS.isInitialized) BINDINGS = WeakHashMap()
            var binding = BINDINGS[cls]
            if (binding != null) {
                DEBUGGER?.invoke("HIT: Cache found in binding weak map.")
                return binding
            }
            if (cls.name.startsWith("android.") || cls.name.startsWith("java.")) {
                DEBUGGER?.invoke("MISS: Reached framework class. Abandoning search.")
                return null
            }
            try {
                binding = cls.classLoader!!
                    .loadClass(cls.name + Default.SUFFIX)
                    .getConstructor(cls, Defaults::class.java) as Constructor<Saver>
                DEBUGGER?.invoke("HIT: Loaded binding class, caching in weak map.")
            } catch (e: ClassNotFoundException) {
                val superclass = cls.superclass
                DEBUGGER?.invoke("Not found. Trying superclass ${superclass!!.name}")
                binding =
                    findBindingConstructor(
                        superclass
                    )
            } catch (e: NoSuchMethodException) {
                throw RuntimeException("Unable to find binding constructor for \$name", e)
            }
            BINDINGS[cls] = binding!!
            return binding
        }
    }

    operator fun contains(key: String): Boolean

    operator fun get(key: String): String = getString(key)!!

    fun getString(key: String): String?

    fun getString(key: String, defaultValue: String?): String?

    fun getInt(key: String): Int

    fun getInt(key: String, defaultValue: Int): Int

    fun getLong(key: String): Long

    fun getLong(key: String, defaultValue: Long): Long

    fun getFloat(key: String): Float

    fun getFloat(key: String, defaultValue: Float): Float

    fun getBoolean(key: String): Boolean

    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun getEditor(): E

    infix operator fun invoke(edit: (E.() -> Unit)): Defaults<E> =
        apply { getEditor().apply { edit() }.save() }

    interface Editor : Saver {

        operator fun minusAssign(key: String)

        fun reset()

        operator fun set(key: String, value: String?)

        operator fun set(key: String, value: Int)

        operator fun set(key: String, value: Long)

        operator fun set(key: String, value: Float)

        operator fun set(key: String, value: Boolean)
    }

    interface Saver {

        fun save()

        fun saveAsync()
    }
}

infix fun Any.bindDefaults(source: Defaults<*>): Defaults.Saver {
    val targetClass = javaClass
    Defaults.DEBUGGER?.invoke("Looking up binding for ${targetClass.name}")
    val constructor =
        Defaults.findBindingConstructor(targetClass)
    if (constructor == null) {
        Defaults.DEBUGGER?.invoke("${targetClass.name} binding not found, returning empty Committer.")
        return Defaults.EMPTY_SAVER
    }
    try {
        return constructor.newInstance(this, source)
    } catch (e: IllegalAccessException) {
        throw RuntimeException("Unable to invoke \$constructor", e)
    } catch (e: InstantiationException) {
        throw RuntimeException("Unable to invoke \$constructor", e)
    } catch (e: InvocationTargetException) {
        val cause = e.cause
        if (cause is RuntimeException) {
            throw cause
        }
        if (cause is Error) {
            throw cause
        }
        throw RuntimeException("Unable to create binding instance.", cause)
    }
}