package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.IOException
import java.net.Socket

class IDriveConnectionListener : BroadcastReceiver() {
	private val TAG = "IDriveConnectionListen"

	/**
	 * The current connection status is available as static members
	 * isConnected indicates whether the car is currently connected
	 * brand is which type of car is connected: bmw bmwi mini
	 * host is what host to use to reach the Etch service, usually 127.0.0.1
	 * port is which port on that host to reach the Etch service
	 * instanceId is an internal identifier
	 * callback is called after the connection state changes
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

		private var connectThread: Thread? = null
		fun reset() {
			isConnected = false
		}

		fun tryConnect() {
			synchronized(IDriveConnectionListener::class.java) {
				if (!isConnected && connectThread?.isAlive != true) {
					connectThread = ConnectThread()
					connectThread?.start()
				}
			}
		}
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

	private class ConnectThread: Thread() {
		override fun run() {
			val PORTS = listOf(4004, 4005, 4006, 4007, 4008)
			for (port in PORTS) {
				try {
					val socket = Socket("127.0.0.1", port)
					if (socket.isConnected) {
						// we found a car proxy! probably
						IDriveConnectionListener.isConnected = true
						IDriveConnectionListener.brand = "bmw"  // a complete guess! but will crash if it's incorrect
						IDriveConnectionListener.host = "127.0.0.1"
						IDriveConnectionListener.port = port
						IDriveConnectionListener.callback?.run()
					}
				} catch (e: IOException) {
					// this port isn't open
				}
			}
		}
	}
}
