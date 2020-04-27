package com.anzen.android.examenandroid.helpers

import android.content.Context
import android.util.Log
import com.anzen.android.examenandroid.R
import com.anzen.android.examenandroid.models.Station
import com.anzen.android.examenandroid.utils.ResponseListener
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class JsonReaderHelper(private val context: Context) {

    fun getInfoBikes(responseListener : ResponseListener<Any>) {
        ///
        ///Read json file from raw resources
        ///
        val jsonBikes = context.resources.openRawResource(R.raw.bikes).bufferedReader().use { it.readText() }

        ///
        ///Try parse json string to list of Station models
        ///
        try {
            val stationList: List<Station> = Gson().fromJson(jsonBikes, Array<Station>::class.java).toList()
            if (stationList.isEmpty()) {
                responseListener.onError(context.getString(R.string.empty_list))
            } else {
                responseListener.onSuccess(stationList)
            }
        } catch (e: Exception) {
            Log.e("JsonReaderHelperError", e.message.toString())
            responseListener.onError(context.getString(R.string.error_message))
        }
    }
}