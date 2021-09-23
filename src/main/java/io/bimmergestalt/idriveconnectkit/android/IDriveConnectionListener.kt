package io.bimmergestalt.idriveconnectkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import java.lang.IllegalArgumentException
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
			val listeners = ArrayList(listeners.keys)
			listeners.forEach {
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

	companion object {
		val TAG = "IDriveConnectionListen"
		const val INTENT_ATTACHED = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_ATTACHED"
		const val INTENT_DETACHED = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_DETACHED"
		const val INTENT_BCL_REPORT = "com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_INFO"
	}

	var subscribed = false

	// only listen for callbacks if we have a callback
	// don't use it as a constructor parameter because we need this property setter logic
	var callback: () -> Unit = {}
		set(value) {
			field = value
			IDriveConnectionListener.addListener(this)
		}

	/** Little helper receiver to temporarily watch for InstanceID updates */
	private class IDriveBclReceiver(val idrive: IDriveConnectionListener): BroadcastReceiver() {
		var subscribed = false

		override fun onReceive(context: Context?, intent: Intent?) {
			context ?: return
			intent ?: return
			if (intent.action == INTENT_BCL_REPORT) {
				if (idrive.instanceId ?: -1 <= 0) {  // don't know InstanceId yet
					// try to get InstanceId from the BCL Report
					val brand = idrive.brand ?: return
					val host = idrive.host ?: return
					val port = idrive.port ?: return
					val state = intent.getStringExtra("EXTRA_STATE")
					val instanceId = intent.getShortExtra("EXTRA_INSTANCE_ID", 0)
					if (state == "WORKING" && instanceId > 0) {
						Log.i(TAG, "Recovered instance ID from BCL report: $instanceId")
						IDriveConnectionStatus.setConnection(brand, host, port, instanceId.toInt())
					}
				}
			}
			if ((idrive.instanceId ?: -1) > 0 && subscribed) {
				Log.i(TAG, "Unsubscribing from BCL updates after receiving instanceId ${idrive.instanceId}")
				// stop listening for BCL reports
				unsubscribe(context)
			}
		}

		fun subscribe(context: Context) {
			if (!subscribed) {
				context.registerReceiver(this, IntentFilter(INTENT_BCL_REPORT))
				subscribed = true
			}
		}
		fun unsubscribe(context: Context) {
			try {
				subscribed = false
				context.unregisterReceiver(this)
			} catch (e: IllegalArgumentException) {}
		}
	}
	private val bclListener = IDriveBclReceiver(this)

	/**
	 * Listen for status updates about whether the car is connected
	 */
	override fun onReceive(context: Context?, intent: Intent?) {
		context ?: return
		intent ?: return
		if (intent.action == INTENT_ATTACHED) {
			val brand = intent.getStringExtra("EXTRA_BRAND")
			val host = intent.getStringExtra("EXTRA_HOST")
			val port = intent.getIntExtra("EXTRA_PORT", -1)
			val instanceId = intent.getIntExtra("EXTRA_INSTANCE_ID", -1)
			Log.i(TAG, "Received car announcement: ${intent.action} $brand $instanceId@$host:$port")
			brand ?: return
			host ?: return
			if (port == -1 || instanceId == -1) {
				return
			}
			IDriveConnectionStatus.setConnection(brand, host, port, instanceId)
		}
		if (intent.action == INTENT_DETACHED) {
			Log.i(TAG, "Received car announcement: " + intent.action)
			IDriveConnectionStatus.reset()
		}

		// subscribe to listen for InstanceId
		if ((this.instanceId ?: -1) <= 0 && !bclListener.subscribed) {
			Log.i(TAG, "Re-subscribing to BCL updates to recover instanceId")
			bclListener.subscribe(context.applicationContext)
		}
	}

	fun subscribe(context: Context) {
		if (!subscribed) {
			context.registerReceiver(this, IntentFilter(INTENT_ATTACHED))
			context.registerReceiver(this, IntentFilter(INTENT_DETACHED))
			subscribed = true
		}
		bclListener.subscribe(context)
	}

	fun unsubscribe(context: Context) {
		try {
			subscribed = false
			context.unregisterReceiver(this)
		} catch (e: IllegalArgumentException) {}
		bclListener.unsubscribe(context)
	}

	override fun onUpdate() {
		callback()
	}
}
