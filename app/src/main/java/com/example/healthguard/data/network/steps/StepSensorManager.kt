package com.example.healthguard.data.network.steps



import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepSensorManager(
    context: Context,
    private val onStepCountChanged: (Int) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun startListening() {
        stepCounter?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    fun hasStepCounter(): Boolean = stepCounter != null


    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceReboot = event.values[0].toInt()
            onStepCountChanged(totalStepsSinceReboot)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}