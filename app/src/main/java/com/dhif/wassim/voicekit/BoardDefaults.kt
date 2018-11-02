package com.dhif.wassim.voicekit

import android.os.Build

object BoardDefaults {

    const val DEVICE_RPI3 = "rpi3"
    private const val DEVICE_IMX6UL_PICO = "imx6ul_pico"
    private const val DEVICE_IMX7D_PICO = "imx7d_pico"

    /**
     * Return the GPIO pin that the LED is connected on.
     * For example, on Intel Edison Arduino breakout, pin "IO13" is connected to an onboard LED
     * that turns on when the GPIO pin is HIGH, and off when low.
     */
    fun getGPIOForLED(): String {
        return when (Build.DEVICE) {
            DEVICE_RPI3 -> "BCM25"
            DEVICE_IMX6UL_PICO -> "GPIO4_IO22"
            DEVICE_IMX7D_PICO -> "GPIO2_IO02"
            else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
        }
    }

    /**
     * Return the GPIO pin that the Button is connected on.
     */
    fun getGPIOForButton(): String {
        return when (Build.DEVICE) {
            DEVICE_RPI3 -> "BCM23"
            DEVICE_IMX6UL_PICO -> "GPIO2_IO03"
            DEVICE_IMX7D_PICO -> "GPIO6_IO14"
            else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
        }
    }

    /**
     * Return the GPIO pin for the Voice Hat DAC trigger.
     */
    fun getGPIOForDacTrigger(): String {
        return when (Build.DEVICE) {
            DEVICE_RPI3 -> "BCM16"
            else -> throw IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE)
        }
    }
}
