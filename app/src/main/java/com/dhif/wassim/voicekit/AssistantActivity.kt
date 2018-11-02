package com.dhif.wassim.voicekit

import android.app.Activity
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ListView
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.voicehat.Max98357A
import com.google.android.things.contrib.driver.voicehat.VoiceHat
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.auth.oauth2.UserCredentials
import kotlinx.android.synthetic.main.activity_assistant.*
import org.json.JSONException
import java.io.IOException
import java.util.ArrayList

class AssistantActivity : Activity(), Button.OnButtonEventListener {
    private val TAG = AssistantActivity::class.java.simpleName

    // Peripheral and drivers constants.
    private val BUTTON_DEBOUNCE_DELAY_MS = 20
    // Default on using the Voice Hat on Raspberry Pi 3.
    private val USE_VOICEHAT_I2S_DAC = Build.DEVICE == BoardDefaults.DEVICE_RPI3

    // Audio constants.
    private val PREF_CURRENT_VOLUME = "current_volume"
    private val SAMPLE_RATE = 16000
    private val DEFAULT_VOLUME = 100

    // Assistant SDK constants.
    private val DEVICE_MODEL_ID = "@string/device_model_id"
    private val DEVICE_INSTANCE_ID = "@string/device_instance_id"
    private val LANGUAGE_CODE = "@string/language_code"

    // Hardware peripherals.
    private var mButton: Button? = null
    private var mButtonWidget: android.widget.Button? = null
    private var mLed: Gpio? = null
    private var mDac: Max98357A? = null

    private var mMainHandler: Handler? = null

    // List & adapter to store and display the history of Assistant Requests.
    private var mEmbeddedAssistant: EmbeddedAssistant? = null
    private val mAssistantRequests = ArrayList<String>()
    private var mAssistantRequestsAdapter: ArrayAdapter<String>? = null
    private var mHtmlOutputCheckbox: CheckBox? = null
    private var mWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "starting assistant")

        setContentView(R.layout.activity_assistant)

        val assistantRequestsListView = findViewById<ListView>(R.id.assistantRequestsListView)
        mAssistantRequestsAdapter = ArrayAdapter(
            this, android.R.layout.simple_list_item_1,
            mAssistantRequests
        )
        assistantRequestsListView.adapter = mAssistantRequestsAdapter
        mHtmlOutputCheckbox = findViewById(R.id.htmlOutput)
        mHtmlOutputCheckbox?.setOnCheckedChangeListener{ compoundButton, useHtml ->
            mWebView?.visibility = if (useHtml) View.VISIBLE else View.GONE
            assistantRequestsListView.visibility = if (useHtml) View.GONE else View.VISIBLE
            /* TODO mEmbeddedAssistant.setResponseFormat(
                if (useHtml)
                    TODO EmbeddedAssistant.HTML
                else
                    TODO EmbeddedAssistant.TEXT
            )*/
        }

        mWebView = findViewById(R.id.webview)
        mWebView?.settings?.javaScriptEnabled = true

        mMainHandler = Handler(mainLooper)
        mButtonWidget = findViewById(R.id.assistantQueryButton)
        // TODO Remove all findViewById, use assistantQueryButton directly
        // TODO assistantQueryButton.setOnClickListener{ mEmbeddedAssistant.startConversation() }


        // Audio routing configuration: use default routing.
        var audioInputDevice: AudioDeviceInfo? = null
        var audioOutputDevice: AudioDeviceInfo? = null
        if (USE_VOICEHAT_I2S_DAC) {
            audioInputDevice = findAudioDevice(AudioManager.GET_DEVICES_INPUTS, AudioDeviceInfo.TYPE_BUS)
            if (audioInputDevice == null) {
                Log.e(TAG, "failed to find I2S audio input device, using default")
            }
            audioOutputDevice = findAudioDevice(AudioManager.GET_DEVICES_OUTPUTS, AudioDeviceInfo.TYPE_BUS)
            if (audioOutputDevice == null) {
                Log.e(TAG, "failed to found I2S audio output device, using default")
            }
        }

        try {
            if (USE_VOICEHAT_I2S_DAC) {
                Log.i(TAG, "initializing DAC trigger")
                mDac = VoiceHat.openDac()
                mDac?.setSdMode(Max98357A.SD_MODE_SHUTDOWN)

                mButton = VoiceHat.openButton()
                mLed = VoiceHat.openLed()
            } else {
                val pioManager = PeripheralManager.getInstance()
                mButton = Button(
                    BoardDefaults.getGPIOForButton(),
                    Button.LogicState.PRESSED_WHEN_LOW
                )
                mLed = pioManager.openGpio(BoardDefaults.getGPIOForLED())
            }

            mButton?.setDebounceDelay(BUTTON_DEBOUNCE_DELAY_MS.toLong())
            mButton?.setOnButtonEventListener(this)

            mLed?.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            mLed?.setActiveType(Gpio.ACTIVE_HIGH)
        } catch (e: IOException) {
            Log.e(TAG, "error configuring peripherals:", e)
            return
        }

        // Set volume from preferences
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val initVolume = preferences.getInt(PREF_CURRENT_VOLUME, DEFAULT_VOLUME)
        Log.i(TAG, "setting audio track volume to: $initVolume")

        var userCredentials: UserCredentials? = null
        try {
            // TODO userCredentials = EmbeddedAssistant.generateCredentials(this, R.raw.google_services)
        } catch (e: IOException) {
            Log.e(TAG, "error getting user google_services", e)
        } catch (e: JSONException) {
            Log.e(TAG, "error getting user google_services", e)
        }


    }

    private fun findAudioDevice(deviceFlag: Int, deviceType: Int): AudioDeviceInfo? {
        val manager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val adis = manager.getDevices(deviceFlag)
        for (adi in adis) {
            if (adi.type == deviceType) {
                return adi
            }
        }
        return null
    }

    override fun onButtonEvent(button: Button, pressed: Boolean) {
        try {
            if (mLed != null) {
                mLed?.value = pressed
            }
        } catch (e: IOException) {
            Log.d(TAG, "error toggling LED:", e)
        }

        if (pressed) {
            // TODO mEmbeddedAssistant.startConversation()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "stopping assistant")
        if (mLed != null) {
            try {
                mLed?.close()
            } catch (e: IOException) {
                Log.w(TAG, "error closing LED", e)
            }

            mLed = null
        }
        if (mButton != null) {
            try {
                mButton?.close()
            } catch (e: IOException) {
                Log.w(TAG, "error closing button", e)
            }

            mButton = null
        }
        if (mDac != null) {
            try {
                mDac?.close()
            } catch (e: IOException) {
                Log.w(TAG, "error closing voice hat trigger", e)
            }

            mDac = null
        }
        // TODO mEmbeddedAssistant.destroy()
    }
}
