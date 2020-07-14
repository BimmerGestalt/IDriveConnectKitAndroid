package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

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
		const val INTENT_ATTACHED = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_ATTACHED"
		const val INTENT_DETACHED = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_DETACHED"
		const val INTENT_BCL_REPORT = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_INFO"

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

		fun reset() {
			isConnected = false
			callback?.run()
		}

		fun setConnection(brand: String, host: String, port: Int, instanceId: Int? = null) {
			if (isConnected) return
			isConnected = true
			this.brand = brand
			this.host = host
			this.port = port
			this.instanceId = instanceId
			callback?.run()
		}
	}

	// accessor methods to allow an object to be passed around
	val isConnected: Boolean
		get() = IDriveConnectionListener.isConnected
	val brand: String?
		get() = IDriveConnectionListener.brand
	val host: String?
		get() = IDriveConnectionListener.host
	val port: Int?
		get() = IDriveConnectionListener.port
	val instanceId: Int?
		get() = IDriveConnectionListener.instanceId

	/**
	 * Listen for status updates about whether the car is connected
	 */
	override fun onReceive(context: Context?, intent: Intent?) {
		if (intent == null) return
		if (intent.action == INTENT_ATTACHED) {
			Log.i(TAG, "Received car announcement: " + intent.action)
			IDriveConnectionListener.isConnected = true
			IDriveConnectionListener.brand = intent.getStringExtra("EXTRA_BRAND")
			IDriveConnectionListener.host = intent.getStringExtra("EXTRA_HOST")
			IDriveConnectionListener.port = intent.getIntExtra("EXTRA_PORT", -1)
			IDriveConnectionListener.instanceId = intent.getIntExtra("EXTRA_INSTANCE_ID", -1)
			if (callback != null) callback?.run()
		}
		if (intent.action == INTENT_DETACHED) {
			Log.i(TAG, "Received car announcement: " + intent.action)
			IDriveConnectionListener.isConnected = false
			if (callback != null) callback?.run()
		}
		if (intent.action == INTENT_BCL_REPORT) {
			if (IDriveConnectionListener.instanceId == null) {  // try to get InstanceId from the BCL Report
				val state = intent.getStringExtra("EXTRA_STATE")
				val instanceId = intent.getShortExtra("EXTRA_INSTANCE_ID", 0)
				if (state == "WORKING" && instanceId > 0) {
					Log.i(TAG, "Recovered instance ID from BCL report: $instanceId")
					IDriveConnectionListener.instanceId = instanceId.toInt()
				}
			}
		}
	}

	fun subscribe(context: Context) {
		context.registerReceiver(this, IntentFilter(INTENT_ATTACHED))
		context.registerReceiver(this, IntentFilter(INTENT_DETACHED))
		context.registerReceiver(this, IntentFilter(INTENT_BCL_REPORT))
	}
	fun unsubscribe(context: Context) {
		context.unregisterReceiver(this)
	}
}
