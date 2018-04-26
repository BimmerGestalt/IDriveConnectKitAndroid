package me.hufman.idriveconnectionkit.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_INCLUDE_STOPPED_PACKAGES
import android.content.IntentFilter
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.util.Log
import com.bmwgroup.connected.car.app.BrandType



object CarAPIDiscovery {
	const val TAG = "IDriveSecurityService"

	var broadcastReceiver: DiscoveryReceiver? = null
	val discoveredApps: MutableMap<String, CarAPIClient> = HashMap()
	class DiscoveryReceiver(val callback: DiscoveryCallback?): BroadcastReceiver() {
		val INTENT_NAME = "com.bmwgroup.connected.app.action.ACTION_CAR_APPLICATION_REGISTERING"
		fun intentFilter(): IntentFilter {
			return IntentFilter(INTENT_NAME)
		}
		override fun onReceive(context: Context?, intent: Intent?) {
			val id = intent?.getStringExtra("EXTRA_APPLICATION_ID")
			if (id != null) {
				Log.i(TAG, "Heard CarAPI announcement of: " + intent.getStringExtra("EXTRA_APPLICATION_ID"))
				try {
					val app = CarAPIClient.fromDiscoveryIntent(intent)
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
		if (broadcastReceiver == null) {
			broadcastReceiver = DiscoveryReceiver(callback)
			context.registerReceiver(broadcastReceiver, broadcastReceiver!!.intentFilter())
		}

		// trigger installed BMW Connected Ready apps to announce their presence
		val discoveryIntent = Intent()
		discoveryIntent.addFlags(FLAG_INCLUDE_STOPPED_PACKAGES)
		discoveryIntent.action = "com.bmwgroup.connected.car.app.action.CONNECTED_APP_INSTALLED"
		context.sendBroadcast(discoveryIntent)
	}

	/**
	 * Cancels app discovery
	 */
	fun cancelDiscovery(context: Context? = null) {
		if (broadcastReceiver != null) {
			context?.unregisterReceiver(broadcastReceiver)
			broadcastReceiver = null
		}
	}
}

class CarAPIClient(val id: String,
                   val title: String,
                   val category: String,
                   val version: String,
                   val brandType: BrandType,
                   val connectIntentName: String,
                   val disconnectIntentName: String,
                   val appIcon: ByteArray?) {

	companion object {
		fun fromDiscoveryIntent(intent: Intent): CarAPIClient {
			return CarAPIClient(id = intent.getStringExtra("EXTRA_APPLICATION_ID"),
			                    title = intent.getStringExtra("EXTRA_APPLICATION_TITLE"),
			                    category = intent.getStringExtra("EXTRA_APPLICATION_CATEGORY"),
			                    version = intent.getStringExtra("EXTRA_APPLICATION_VERSION"),
			                    brandType = intent.getSerializableExtra("EXTRA_APPLICATION_BRAND") as BrandType,
			                    connectIntentName = intent.getStringExtra("EXTRA_APPLICATION_CONNECT_RECEIVER_ACTION"),
			                    disconnectIntentName = intent.getStringExtra("EXTRA_APPLICATION_DISCONNECT_RECEIVER_ACTION"),
			                    appIcon = intent.getByteArrayExtra("EXTRA_APPLICATION_APP_ICON"))
		}
	}

	/**
	 * Opens the requested file from the CarAPI content provider
	 */
	fun openContent(context: Context, path: String): AssetFileDescriptor? {
		val providerModule = "content://" + id + ".provider"
		val uri = providerModule + "/" + path.trimStart('/')
		return context.contentResolver.openAssetFileDescriptor(Uri.parse(uri), "r")
	}

	/**
	 * Gets the app-specific cert to sign into the car with SAS
	 */
	fun getAppCertificate(context: Context): AssetFileDescriptor? {
		return openContent(context, "carapplications/" + id + "/" + id + ".p7b")
	}

	/**
	 * Gets the UI Description layout file
	 * May be missing, if the app uses the default CarAPI layouts
	 */
	fun getUiDescription(context: Context): AssetFileDescriptor? {
		return openContent(context, "carapplications/" + id + "/rhmi/ui_description.xml")
	}

	/**
	 * Get the images.zip db from the app
	 * @param brand should be {bmw,mini,common}
	 */
	fun getImagesDB(context: Context, brand: String): AssetFileDescriptor? {
		return openContent(context, "carapplications/" + id + "/rhmi/" + brand + "/images.zip")
	}

	/**
	 * Get the texts.zip db from the app
	 * @param brand should be {bmw,mini,common}
	 */
	fun getTextsDB(context: Context, brand: String): AssetFileDescriptor? {
		return openContent(context, "carapplications/" + id + "/rhmi/" + brand + "/texts.zip")
	}

	override fun toString(): String {
		return "$id: $title - $category $version"
	}
}