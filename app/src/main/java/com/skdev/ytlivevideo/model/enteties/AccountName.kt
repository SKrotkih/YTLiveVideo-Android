package com.skdev.ytlivevideo.model.enteties

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import com.skdev.ytlivevideo.util.Config

object AccountName {
    private var mChosenAccountName: String? = null
    private const val ACCOUNT_KEY = "AccountKey"

    fun getName(context: Context, bundle: Bundle? = null): String? {
        if (mChosenAccountName == null) {
            mChosenAccountName = if (bundle != null) {
                bundle.getString(ACCOUNT_KEY)
            } else {
                val sp = PreferenceManager.getDefaultSharedPreferences(context)
                sp.getString(ACCOUNT_KEY, null)
            }
        }
        return mChosenAccountName
    }

    fun saveName(context: Context, accountName: String?, bundle: Bundle? = null) {
        Log.d(Config.APP_NAME, "saveAccountName")
        if (accountName == null) {
            mChosenAccountName = null
            return
        }
        if (bundle == null) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            sp.edit().putString(ACCOUNT_KEY, accountName).apply()
        } else {
            bundle.putString(ACCOUNT_KEY, accountName)
        }
        mChosenAccountName = accountName
        Log.d(Config.APP_NAME, "accountName=$accountName")
    }
}
