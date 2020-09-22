package com.skdev.ytlivevideo.model.enteties

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import com.skdev.ytlivevideo.ui.MainActivity

object AccountName {
    private var mChosenAccountName: String? = null

    fun getName(context: Context, bundle: Bundle? = null): String? {
        if (mChosenAccountName == null) {
            mChosenAccountName = if (bundle != null) {
                bundle.getString(MainActivity.ACCOUNT_KEY)
            } else {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                sp.getString(MainActivity.ACCOUNT_KEY, null)
            }
        }
        return mChosenAccountName
    }

    fun saveName(context: Context, accountName: String?, bundle: Bundle? = null) {
        Log.i(MainActivity.APP_NAME, "saveAccountName")
        if (accountName == null) {
            mChosenAccountName = null
            return
        }
        if (bundle == null) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            sp.edit().putString(MainActivity.ACCOUNT_KEY, accountName).apply()
        } else {
            bundle.putString(MainActivity.ACCOUNT_KEY, accountName)
        }
        mChosenAccountName = accountName
        Log.i(MainActivity.APP_NAME, "accountName=$accountName")
    }
}
