package com.dhif.wassim.voicekit

import android.content.Context
import com.google.auth.oauth2.UserCredentials
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

object Credentials {

    @Throws(IOException::class, JSONException::class)
    fun fromResource(context: Context, resourceId: Int): UserCredentials {
        val `is` = context.resources.openRawResource(resourceId)
        val bytes = ByteArray(`is`.available())
        `is`.read(bytes)
        val json = JSONObject(String(bytes, charset("UTF-8")))
        return UserCredentials(
            json.getString("client_id"),
            json.getString("client_secret"),
            json.getString("refresh_token")
        )
    }
}
