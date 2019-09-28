package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES
import android.content.IntentFilter
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.lang.IllegalArgumentException


object CarAPIDiscovery {
	const val TAG = "IDriveCarApiDiscovery"

	const val INTENT_DISCOVERY = "com.bmwgroup.connected.car.app.action.CONNECTED_APP_INSTALLED"
	const val INTENT_REGISTERING = "com.bmwgroup.connected.app.action.ACTION_CAR_APPLICATION_REGISTERING"

	var broadcastReceiver: DiscoveryReceiver? = null
	val discoveredApps: MutableMap<String, CarAppResources> = HashMap()
	var callback: DiscoveryCallback? = null

	class DiscoveryReceiver: BroadcastReceiver() {
		fun intentFilter(): IntentFilter {
			return IntentFilter(INTENT_REGISTERING)
		}
		override fun onReceive(context: Context?, intent: Intent?) {
			val id = intent?.getStringExtra("EXTRA_APPLICATION_ID")
			if (context != null && id != null) {
				Log.i(TAG, "Heard CarAPI announcement of: " + intent.getStringExtra("EXTRA_APPLICATION_ID"))
				try {
					val app = CarAPIClient(context, CarAPIAppInfo.fromDiscoveryIntent(intent))
					discoveredApps[id] = app
					callback?.discovered(app)
				} catch (e: Exception) {
					Log.i(TAG, "Exception while handling CarAPI announcement from $id: $e")
				}
			}
		}
	}
	interface DiscoveryCallback {
		fun discovered(app: CarAPIClient)
	}

	/**
	 * Begins searching for CarAPI apps
	 */
	fun discoverApps(context: Context, callback: DiscoveryCallback?) {
		// register to listen for BMW Connected Ready announcements
		this.callback = callback
		if (broadcastReceiver == null) {
			broadcastReceiver = DiscoveryReceiver()
			context.registerReceiver(broadcastReceiver, broadcastReceiver!!.intentFilter())
		}

		// trigger installed BMW Connected Ready apps to announce their presence
		val discoveryIntent = Intent(INTENT_DISCOVERY)
		discoveryIntent.addFlags(FLAG_INCLUDE_STOPPED_PACKAGES)
		context.sendBroadcast(discoveryIntent)
		Log.i(TAG, "Soliciting CarAPI")

		// Samsung phones can't send broadcasts?
		val listeners = context.packageManager.queryBroadcastReceivers(discoveryIntent, 0)
		listeners.forEach {
			val directedIntent = discoveryIntent.setPackage(it.activityInfo.packageName)
			context.sendBroadcast(directedIntent)
		}
	}

	/**
	 * Announce this app
	 */
	fun announceApp(context: Context, appInfo: CarAPIAppInfo) {
		context.sendBroadcast(appInfo.toIntent())
	}

	/**
	 * Cancels app discovery
	 */
	fun cancelDiscovery(context: Context) {
		if (broadcastReceiver != null) {
			try {
				context.unregisterReceiver(broadcastReceiver)
			} catch (e: IllegalArgumentException) {}
			broadcastReceiver = null
		}
	}
}

class CarAPIClient(val context: Context, val appInfo: CarAPIAppInfo): CarAppResources {

	/**
	 * Opens the requested file from the CarAPI content provider
	 */
	fun openContent(path: String): InputStream? {
		val providerModule = "content://${appInfo.id}.provider"
		val uri = providerModule + "/" + path.trimStart('/')
		return context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")?.createInputStream()
	}

	/**
	 * Gets the app-specific cert to sign into the car with SAS
	 */
	override fun getAppCertificate(brand: String): InputStream? {
		return openContent("carapplications/${appInfo.id}/${appInfo.id}.p7b")
	}

	/**
	 * Gets the UI Description layout file
	 * May be missing, if the app uses the default CarAPI layouts
	 */
	override fun getUiDescription(brand: String): InputStream? {
		return openContent("carapplications/${appInfo.id}/rhmi/ui_description.xml")
	}

	/**
	 * Get the images.zip db from the app
	 * @param brand should be {bmw,mini,common}
	 */
	override fun getImagesDB(brand: String): InputStream? {
		return openContent("carapplications/${appInfo.id}/rhmi/" + brand + "/images.zip")
	}

	/**
	 * Get the texts.zip db from the app
	 * @param brand should be {bmw,mini,common}
	 */
	override fun getTextsDB(brand: String): InputStream? {
		return openContent("carapplications${appInfo.id}/rhmi/" + brand + "/texts.zip")
	}

	override fun toString(): String {
		return "${appInfo.id}: ${appInfo.title} - ${appInfo.category} ${appInfo.version}"
	}
}