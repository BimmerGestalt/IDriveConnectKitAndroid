package io.bimmergestalt.idriveconnectkit.android

import android.content.Context
import android.content.Intent
import com.bmwgroup.connected.car.app.BrandType
import java.lang.IllegalArgumentException

class CarAPIAppInfo(val id: String,
                    val title: String,
                    val category: String,   // should be Multimedia, Radio, or OnlineServices
                    val version: String,    // should be a version string, such as "v3"
                    val rhmiVersion: String?,
                    val brandType: BrandType,
                    val connectIntentName: String?,
                    val disconnectIntentName: String?,
                    val appIcon: ByteArray?) {

	companion object {
		const val INTENT_REGISTERING = "com.bmwgroup.connected.app.action.ACTION_CAR_APPLICATION_REGISTERING"

		/**
		 * Parse out the CarAPIAppInfo represented by this intent, which should have the action of `INTENT_REGISTERING`
		 */
		fun fromDiscoveryIntent(intent: Intent): CarAPIAppInfo {
			return CarAPIAppInfo(id = intent.getStringExtra("EXTRA_APPLICATION_ID") ?: throw IllegalArgumentException("Missing EXTRA_APPLICATION_ID"),
					title = intent.getStringExtra("EXTRA_APPLICATION_TITLE") ?: throw IllegalArgumentException("Missing EXTRA_APPLICATION_TITLE"),
					category = intent.getStringExtra("EXTRA_APPLICATION_CATEGORY") ?: throw IllegalArgumentException("Missing EXTRA_APPLICATION_CATEGORY"),
					version = intent.getStringExtra("EXTRA_APPLICATION_VERSION") ?: throw IllegalArgumentException("Missing EXTRA_APPLICATION_VERSION"),
					rhmiVersion = intent.getStringExtra("EXTRA_RHMI_VERSION"),
					brandType = intent.getSerializableExtra("EXTRA_APPLICATION_BRAND") as? BrandType ?: throw IllegalArgumentException("Missing EXTRA_APPLICATION_BRAND"),
					connectIntentName = intent.getStringExtra("EXTRA_APPLICATION_CONNECT_RECEIVER_ACTION"),
					disconnectIntentName = intent.getStringExtra("EXTRA_APPLICATION_DISCONNECT_RECEIVER_ACTION"),
					appIcon = intent.getByteArrayExtra("EXTRA_APPLICATION_APP_ICON"))
		}
	}

	fun toIntent(): Intent {
		return Intent()
			.setAction(INTENT_REGISTERING)
			.putExtra("EXTRA_APPLICATION_ID", id)
			.putExtra("EXTRA_APPLICATION_TITLE", title)
			.putExtra("EXTRA_APPLICATION_CATEGORY", category)
			.putExtra("EXTRA_APPLICATION_VERSION", version)
			.putExtra("EXTRA_RHMI_VERSION", rhmiVersion)
			.putExtra("EXTRA_APPLICATION_BRAND", brandType)
			.putExtra("EXTRA_APPLICATION_CONNECT_RECEIVER_ACTION", connectIntentName)
			.putExtra("EXTRA_APPLICATION_DISCONNECT_RECEIVER_ACTION", disconnectIntentName)
			.putExtra("EXTRA_APPLICATION_APP_ICON", appIcon)
	}

	/**
	 * Announce this app to any BMW/Mini Connected
	 */
	fun announceApp(context: Context) {
		val intent = this.toIntent()
		context.sendBroadcast(intent)

		val listeners = context.packageManager.queryBroadcastReceivers(intent, 0)
		listeners.forEach {
			val directedIntent = intent.setPackage(it.activityInfo.packageName)
			context.sendBroadcast(directedIntent)
		}
	}
}