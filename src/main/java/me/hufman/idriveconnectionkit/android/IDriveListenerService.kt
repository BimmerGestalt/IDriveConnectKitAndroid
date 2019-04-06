package me.hufman.idriveconnectionkit.android

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log

/**
 * When the phone is connected to the car, this service starts up to listen for the ATTACHED announcement
 */
class IDriveListenerService: Service() {
	companion object {
		const val INTENT_ACTION = "me.hufman.idriveconnectionkit.android.IDriveListenerService.START"
		const val SHUTDOWN_DELAY: Long = 5 * 60 * 1000    // wait 5 minutes before shutting down listener
		const val TAG = "IDriveListenerService"
	}

	val idriveConnectionListener = IDriveConnectionListener()
	val handler = Handler()
	val ShutdownTask = Runnable {
		this@IDriveListenerService.stopSelf()
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Log.i(TAG, "Starting to listen for Connected announcement")
		idriveConnectionListener.subscribe(this)
		handler.removeCallbacks(ShutdownTask)
		handler.postDelayed(ShutdownTask, SHUTDOWN_DELAY)
		return Service.START_STICKY
	}

	override fun onDestroy() {
		idriveConnectionListener.unsubscribe(this)
		super.onDestroy()
	}
}