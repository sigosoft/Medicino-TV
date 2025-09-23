package com.medicinoclinic.utils

import android.content.Context
import android.preference.PreferenceManager

class BaseClass {
    fun setSharedPreferance(con: Context?, key: String?, value: String?) {
        val sp = PreferenceManager.getDefaultSharedPreferences(con)
        val editor = sp.edit()
        editor.putString(key, value)
        editor.commit()
    }

    fun getSharedPreferance(
        con: Context?,
        key: String?,
        value: String?
    ): String? {
        val sp = PreferenceManager.getDefaultSharedPreferences(con)
        return sp.getString(key, value)
    }
}