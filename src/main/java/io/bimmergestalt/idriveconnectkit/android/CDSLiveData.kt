package io.bimmergestalt.idriveconnectkit.android

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import androidx.lifecycle.LiveData
import org.json.JSONObject


class CDSLiveDataProvider(
	private val contentResolver: ContentResolver
) {
	operator fun get(propertyId: Int): CDSLiveData {
		return CDSLiveData(contentResolver, propertyId)
	}
}

/**
 * A LiveData that loads data from a Content Provider
 * Based on https://medium.com/@jmcassis/android-livedata-and-content-provider-updates-5f8fd3b2b3a4
 */
class CDSLiveData(
	private val contentResolver: ContentResolver,
	propertyId: Int
): LiveData<Map<String, Any>>() {
	private val uri = Uri.parse("content://io.bimmergestalt.cardata.provider/cds/$propertyId")
	private val observer = GenericContentObserver(contentResolver, uri) {
		// Notify LiveData listeners an event has happened
		this.postValue(getContentProviderValue())
	}

	@SuppressLint("Recycle")
	fun getContentProviderValue(): Map<String, Any>? {
		// running on a background thread, use cursor synchronously
		val cursor = try {
			contentResolver.query(uri, null, null, null, null)
		} catch (e: SecurityException) {
			// not allowed to view this value
			// or doesn't exist yet
			null
		} catch (e: IllegalArgumentException) {
			// unknown parameter?
			null
		}
		return try {
			cursor?.moveToFirst()
			val data = cursor?.getString(2)
			cursor?.close()
			data?.let {
				parseJsonObject(JSONObject(it))
			}
		} catch (e: Exception) {
			null
		}
	}

	private fun parseJsonObject(data: JSONObject): Map<String, Any> {
		val result = HashMap<String, Any>()
		for (key in data.keys()) {
			val objectValue = data.optJSONObject(key)
			if (objectValue != null) {
				result.putAll(parseJsonObject(objectValue))
			} else {
				result[key] = data.get(key)
			}
		}
		return result
	}

	override fun onActive() {
		super.onActive()
		observer.register()
	}

	override fun onInactive() {
		super.onInactive()
		observer.unregister()
	}
}

private class GenericContentObserver(private val contentResolver: ContentResolver,
                                     private val uri: Uri,
                                     private val callback: () -> Unit): ContentObserver(null) {
	fun register() {
		try {
			contentResolver.registerContentObserver(uri, true, this)

			// fire a notification to load initial data async
			// it will trigger the callback, which will fetch data in the background
			contentResolver.notifyChange(uri, null)
		} catch (e: SecurityException) {
			// not allowed to view this value
			// or doesn't exist yet
		}
	}

	fun unregister() {
		try {
			contentResolver.unregisterContentObserver(this)
		} catch (e: SecurityException) {
			// not allowed to view this value
			// or doesn't exist yet
		}
	}

	override fun onChange(selfChange: Boolean) {
		callback()
	}
}