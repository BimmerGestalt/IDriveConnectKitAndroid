package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.util.*

/** Interface for IDriveConnectionStatus to update listeners */
interface IDriveConnectionListener: IDriveConnectionStatus {
	companion object {
		private val _listeners = Collections.synchronizedMap(WeakHashMap<IDriveConnectionListener, Boolean>())
		val listeners: Map<IDriveConnectionListener, Boolean> = _listeners

		fun addListener(listener: IDriveConnectionListener) {
			_listeners[listener] = true
		}

		fun onUpdate() {
			listeners.keys.toList().forEach {
				it.onUpdate()
			}
		}
	}

	fun onUpdate()
}

/** Observe and be notified on connection changes */
class IDriveConnectionObserver(var callback: () -> Unit = {}): IDriveConnectionListener {
	init {
		IDriveConnectionListener.addListener(this)
	}

	override fun onUpdate() {
		callback()
	}
}

/** Subscribes to announcements from Connected about car connection
 * and then updates IDriveConnectionStatus appropriately
 */
class IDriveConnectionReceiver: IDriveConnectionListener, BroadcastReceiver() {
	private val TAG = "IDriveConnectionListen"

	companion object {
		const val INTENT_ATTACHED = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_ATTACHED"
		const val INTENT_DETACHED = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_DETACHED"
		const val INTENT_BCL_REPORT = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_INFO"
	}

	// only listen for callbacks if we have a callback
	// don't use it as a constructor parameter because we need this property setter logic
	var callback: () -> Unit = {}
		set(value) {
			field = value
			IDriveConnectionListener.addListener(this)
		}

	/**
	 * Listen for status updates about whether the car is connected
	 */
	override fun onReceive(context: Context?, intent: Intent?) {
		if (intent == null) return
		if (intent.action == INTENT_ATTACHED) {
			Log.i(TAG, "Received car announcement: " + intent.action)
			val brand = intent.getStringExtra("EXTRA_BRAND")
			val host = intent.getStringExtra("EXTRA_HOST")
			val port = intent.getIntExtra("EXTRA_PORT", -1)
			val instanceId = intent.getIntExtra("EXTRA_INSTANCE_ID", -1)
			IDriveConnectionStatus.setConnection(brand, host, port, instanceId)
		}
		if (intent.action == INTENT_DETACHED) {
			Log.i(TAG, "Received car announcement: " + intent.action)
			IDriveConnectionStatus.reset()
		}
		if (intent.action == INTENT_BCL_REPORT) {
			if (this.instanceId ?: -1 <= 0) {  // don't know InstanceId yet
				// try to get InstanceId from the BCL Report
				val brand = this.brand ?: return
				val host = this.host ?: return
				val port = this.port ?: return
				val state = intent.getStringExtra("EXTRA_STATE")
				val instanceId = intent.getShortExtra("EXTRA_INSTANCE_ID", 0)
				if (state == "WORKING" && instanceId > 0) {
					Log.i(TAG, "Recovered instance ID from BCL report: $instanceId")
					IDriveConnectionStatus.setConnection(brand, host, port, instanceId.toInt())
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

	override fun onUpdate() {
		callback()
	}
}
