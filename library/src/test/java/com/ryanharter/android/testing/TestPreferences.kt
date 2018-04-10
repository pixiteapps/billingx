package com.ryanharter.android.testing

import android.content.SharedPreferences

class TestPreferences : SharedPreferences {

  private val values = HashMap<String, Any>()

  override fun contains(key: String?): Boolean {
    return values.contains(key)
  }

  override fun getBoolean(key: String?, defValue: Boolean): Boolean {
    return values[key] as Boolean? ?: defValue
  }

  override fun getInt(key: String?, defValue: Int): Int {
    return values[key] as Int? ?: defValue
  }

  override fun getAll(): MutableMap<String, *> {
    return values
  }

  override fun getLong(key: String?, defValue: Long): Long {
    return values[key] as Long? ?: defValue
  }

  override fun getFloat(key: String?, defValue: Float): Float {
    return values[key] as Float? ?: defValue
  }

  override fun getString(key: String?, defValue: String?): String {
    return values[key] as String? ?: defValue ?: ""
  }

  override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String> {
    @Suppress("UNCHECKED_CAST")
    return values[key] as MutableSet<String>? ?: defValues ?: mutableSetOf()
  }

  override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    throw NotImplementedError()
  }

  override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
    throw NotImplementedError()
  }

  override fun edit(): SharedPreferences.Editor {
    return TestEditor(this)
  }

  class TestEditor(private val prefs: TestPreferences) : SharedPreferences.Editor {

    private var clear = false
    private val values = HashMap<String, Any>()
    private val removals = HashSet<String>()

    override fun clear(): SharedPreferences.Editor {
      clear = true
      values.clear()
      return this
    }

    override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
      key?.let { values[it] = value }
      return this
    }

    override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
      key?.let { values[it] = value }
      return this
    }

    override fun remove(key: String?): SharedPreferences.Editor {
      key?.let {
        values.remove(it)
        removals.add(it)
      }
      return this
    }

    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
      key?.let { values[it] = value }
      return this
    }

    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
      if (key != null && values != null) {
        this.values[key] = values
      }
      return this
    }

    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
      key?.let { values[it] = value }
      return this
    }

    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
      if (key != null && value != null) {
        values[key] = value
      }
      return this
    }

    override fun commit(): Boolean {
      if (clear) {
        prefs.values.clear()
        clear = false
      }
      prefs.values.putAll(values)
      removals.forEach { prefs.values.remove(it) }
      values.clear()
      return true
    }

    override fun apply() {
      commit()
    }

  }
}