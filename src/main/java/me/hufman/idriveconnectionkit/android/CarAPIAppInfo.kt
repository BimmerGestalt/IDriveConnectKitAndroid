package me.hufman.idriveconnectionkit.android

import android.content.Intent
import com.bmwgroup.connected.car.app.BrandType

class CarAPIAppInfo(val id: String,
                    val title: String,
                    val category: String,   // should be Multimedia, Radio, or OnlineServices
                    val version: String,    // should be a version string, such as "v3"
                    val rhmiVersion: String?,
                    val brandType: BrandType,
                    val connectIntentName: String,
                    val disconnectIntentName: String,
                    val appIcon: ByteArray?) {

	companion object {
		fun fromDiscoveryIntent(intent: Intent): CarAPIAppInfo {
			return CarAPIAppInfo(id = intent.getStringExtra("EXTRA_APPLICATION_ID"),
					title = intent.getStringExtra("EXTRA_APPLICATION_TITLE"),
					category = intent.getStringExtra("EXTRA_APPLICATION_CATEGORY"),
					version = intent.getStringExtra("EXTRA_APPLICATION_VERSION"),
					rhmiVersion = intent.getStringExtra("EXTRA_RHMI_VERSION"),
					brandType = intent.getSerializableExtra("EXTRA_APPLICATION_BRAND") as BrandType,
					connectIntentName = intent.getStringExtra("EXTRA_APPLICATION_CONNECT_RECEIVER_ACTION"),
					disconnectIntentName = intent.getStringExtra("EXTRA_APPLICATION_DISCONNECT_RECEIVER_ACTION"),
					appIcon = intent.getByteArrayExtra("EXTRA_APPLICATION_APP_ICON"))
		}
	}

	fun toIntent(): Intent {
		val intent = Intent()
				.setAction(CarAPIDiscovery.INTENT_REGISTERING)
				.putExtra("EXTRA_APPLICATION_ID", id)
				.putExtra("EXTRA_APPLICATION_TITLE", title)
				.putExtra("EXTRA_APPLICATION_CATEGORY", category)
				.putExtra("EXTRA_APPLICATION_VERSION", version)
				.putExtra("EXTRA_RHMI_VERSION", rhmiVersion)
				.putExtra("EXTRA_APPLICATION_BRAND", brandType)
				.putExtra("EXTRA_APPLICATION_CONNECT_RECEIVER_ACTION", connectIntentName)
				.putExtra("EXTRA_APPLICATION_DISCONNECT_RECEIVER_ACTION", disconnectIntentName)
				.putExtra("EXTRA_APPLICATION_APP_ICON", appIcon)
		return intent
	}
}