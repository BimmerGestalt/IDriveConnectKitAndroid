package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * On Android Oreo, we aren't allowed to have a static broadcast-receiver for
 * the custom events that the Connected app announces
 *
 * So, this broadcast-receiver waits for the USB or BT connection
 */
class PlugConnectionListener: BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		// A USB or BT connection is created
		// Start listening for the Connected app to announce
		Log.i(IDriveListenerService.TAG, "Heard a device connection announcement ${intent?.action}")
		context?.startService(Intent(IDriveListenerService.INTENT_ACTION).setPackage(context.packageName))
	}
}