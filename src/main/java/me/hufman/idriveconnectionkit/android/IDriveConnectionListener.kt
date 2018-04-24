package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class IDriveConnectionListener : BroadcastReceiver() {
	private val TAG = "IDriveConnectionListen"

	/**
	 * The current connection status is available as static members
	 * @param isConnected indicates whether the car is currently connected
	 * @param brand which type of car is connected: bmw bmwi mini
	 * @param host what host to use to reach the Etch service, usually 127.0.0.1
	 * @param port which port on that host to reach the Etch service
	 * @param instanceId is an internal identifier
	 * @param callback is called after the connection state changes
	 */
	companion object {
		var isConnected: Boolean = false
			private set
		var brand: String? = null
			private set
		var host: String? = null
			private set
		var port: Int? = null
			private set
		var instanceId: Int? = null
			private set
		var callback: Runnable? = null
	}

	/**
	 * Listen for status updates about whether the car is connected
	 */
	override fun onReceive(context: Context?, intent: Intent?) {
		if (intent == null) return
		Log.i(TAG, "Received car announcement: " + intent.action)
		if (intent.action == "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_ATTACHED") {
			isConnected = true
			brand = intent.getStringExtra("EXTRA_BRAND")
			host = intent.getStringExtra("EXTRA_HOST")
			port = intent.getIntExtra("EXTRA_PORT", -1)
			instanceId = intent.getIntExtra("EXTRA_INSTANCE_ID", -1)
			if (callback != null) callback?.run()
		}
		if (intent.action == "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_DETACHED") {
			isConnected = false
			if (callback != null) callback?.run()
		}
	}
}
