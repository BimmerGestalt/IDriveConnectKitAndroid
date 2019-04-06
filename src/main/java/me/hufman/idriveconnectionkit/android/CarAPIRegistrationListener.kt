package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bmwgroup.connected.car.app.BrandType

/**
 * Listens for the Connected app to search for CarAPI apps
 * and registers self as a CarAPI app to be notified about car connections
 */
class CarAPIRegistrationListener: BroadcastReceiver() {
	companion object {
		const val TAG = "CarAPIRegistration"
		const val REQUEST_INTENT_ACTION = "com.bmwgroup.connected.car.app.action.CONNECTED_APP_INSTALLED"
		const val RESPONSE_INTENT_ACTION = "com.bmwgroup.connected.app.action.ACTION_CAR_APPLICATION_REGISTERING"
	}
	override fun onReceive(context: Context?, intent: Intent?) {
		context ?: return
		intent ?: return

		val CONNECTED_ACTION = "${context.packageName}.CARAPI_CONNECTED"
		val DISCONNECTED_ACTION = "${context.packageName}.CARAPI_DISCONNECTED"
		if (intent.action == REQUEST_INTENT_ACTION) {
			Log.d(TAG, "Received CarAPI Discovery request!")
			// the Connected app is searching for CarApi apps to start specifically
			val response = Intent(RESPONSE_INTENT_ACTION)
					.putExtra("EXTRA_APPLICATION_CATEGORY", "OnlineServices")
					.putExtra("EXTRA_APPLICATION_BRAND", BrandType.ALL)
					.putExtra("EXTRA_APPLICATION_ID", context.packageName)
					.putExtra("EXTRA_APPLICATION_TITLE", context.packageName)
					.putExtra("EXTRA_APPLICATION_VERSION", "v3")
					.putExtra("EXTRA_APPLICATION_CONNECT_RECEIVER_ACTION", CONNECTED_ACTION)
					.putExtra("EXTRA_APPLICATION_DISCONNECT_RECEIVER_ACTION", DISCONNECTED_ACTION)
			context.sendBroadcast(response)
		} else if (intent.action == CONNECTED_ACTION) {
			Log.i(TAG, "Received CarAPI Connection announcement")
			val brand = intent.getStringArrayExtra("com.bmwgroup.connected.extras.vehicle.brand")
			// TODO start probing for the car connection
		} else if (intent.action == DISCONNECTED_ACTION) {
			Log.i(TAG, "Received CarAPI Disconnection announcement")
			IDriveConnectionListener.reset()
		}
	}
}