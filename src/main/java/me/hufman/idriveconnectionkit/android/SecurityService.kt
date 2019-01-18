package me.hufman.idriveconnectionkit.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.bmwgroup.connected.internal.security.ICarSecurityService
import java.lang.Exception
import java.util.*


object SecurityService {
	const val TAG = "IDriveSecurityService"

	val knownSecurityServices = HashMap<String, String>()
	val securityConnections = HashMap<String, SecurityConnectionListener>()
	val activeSecurityConnections = HashMap<String, ICarSecurityService>()
	var sourcePackageName: String = ""  // the default packageName
	var listener = Runnable {}
	var bmwCerts: ByteArray? = null

	init {
		// The list of known BMW/Mini apps that we can connect to
		// We'll try to connect to each one
		knownSecurityServices.put("BMWClassicUSA", "com.bmwgroup.connected.bmw.usa.SECURITY_SERVICE")
		knownSecurityServices.put("MiniClassicUSA", "com.bmwgroup.connected.mini.usa.SECURITY_SERVICE")
		knownSecurityServices.put("BMWClassic", "com.bmwgroup.connected.bmw.SECURITY_SERVICE")
		knownSecurityServices.put("MiniClassic", "com.bmwgroup.connected.mini.SECURITY_SERVICE")
		knownSecurityServices.put("BMWConnectedNA", "de.bmw.connected.na.SECURITY_SERVICE")
		knownSecurityServices.put("MiniConnectedNA", "de.mini.connected.na.SECURITY_SERVICE")
		knownSecurityServices.put("BMWConnected", "de.bmw.connected.SECURITY_SERVICE")
		knownSecurityServices.put("MiniConnected", "de.mini.connected.SECURITY_SERVICE")
	}

	class SecurityConnectionListener(val name: String, val intentName: String) : ServiceConnection {
		fun connect(context: Context) {
			// Try to connect to a given BMW/Mini app
			Log.i(TAG, "Connecting to " + name)
			val intent = Intent(intentName)
			intent.setPackage(intentName.substring(0, intentName.lastIndexOf('.')))
			try {
				val exists = context.bindService(intent, this, Context.BIND_AUTO_CREATE)
				if (!exists) {
					onServiceDisconnected(null)
					context.unbindService(this)
				}
			} catch (e: SecurityException) {
				// new versions of BMW Connected don't let us connect
				onServiceDisconnected(null)
			}
		}


		override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
			// Remember that we connected
			Log.i(TAG, "Connected to security service " + name)
			val previousConnectionCount = activeSecurityConnections.size
			activeSecurityConnections.put(name, ICarSecurityService.Stub.asInterface(service))
			if (previousConnectionCount == 0) {
				listener.run()
			}
		}

		override fun onServiceDisconnected(p0: ComponentName?) {
			// Remove ourselves from the list of active connections
			Log.i(TAG, "Disconnected from security service " + name)
			activeSecurityConnections.remove(name)
			if (activeSecurityConnections.size == 0) {
				listener.run()
			}
		}
	}

	/**
	 * Wait for a BMW/Mini security service to be connected, because the
	 * connection attempts are asynchronous
	 */
	fun subscribe(listener: Runnable) {
		this.listener = listener
		if (activeSecurityConnections.size > 0) {
			this.listener.run()
		}
	}

	/**
	 * Try to connect to any available BMW/Mini security services
	 */
	fun connect(context: Context) {
		sourcePackageName = context.packageName
		verifyConnections() // clean out any dead connections that have been uninstalled
		knownSecurityServices.forEach { (key, value) ->
			if (!activeSecurityConnections.containsKey(key)) {
				securityConnections.remove(key)
				val connection = SecurityConnectionListener(key, value)
				securityConnections.put(key, connection)
				connection.connect(context)
			} else {
				Log.i(TAG, "Already connected to $key")
			}
		}
	}

	/**
	 * Check all the active connections and remove any that aren't actually connected
	 */
	fun verifyConnections() {
		val activeKeys = ArrayList(activeSecurityConnections.keys)
		activeKeys.forEach { key ->
			val connection = activeSecurityConnections[key]
			if (connection == null) {
				activeSecurityConnections.remove(key)
			} else {
				try {
					val handle = connection.createSecurityContext(sourcePackageName, "test app")
					connection.releaseSecurityContext(handle)
				} catch (e: Exception) {
					Log.i(TAG, "Removing dead connection to $key")
					activeSecurityConnections.remove(key)
				}
			}
		}
	}

	/**
	 * Whether any BMW/Mini security services are connected
	 */
	fun isConnected(brandHint: String = ""): Boolean {
		return activeSecurityConnections.keys.filter { it.startsWith(brandHint, ignoreCase = true) }.isNotEmpty()
	}

	/**
	 * Sign the given challenge from the car, returning a response to login with
	 * The packageName and appName don't really matter
	 */
	fun signChallenge(packageName: String? = null, appName: String = "", challenge: ByteArray):ByteArray {
		synchronized(SecurityService) {
			val connection = activeSecurityConnections.values.first()
			val handle = connection.createSecurityContext(packageName ?: sourcePackageName, appName)
			val response = connection.signChallenge(handle, challenge)
			connection.releaseSecurityContext(handle)
			return response
		}
	}

	/**
	 * Loads the BMW cert from the security service
	 * This is helpful for logging into the car
	 */
	fun fetchBMWCerts(packageName: String? = null, appName: String = "SecurityService", brandHint: String = ""):ByteArray {
		synchronized(SecurityService) {
			var bmwCerts = this.bmwCerts
			if (bmwCerts != null) return bmwCerts
			val connection = activeSecurityConnections.entries.firstOrNull { it.key.startsWith(brandHint, ignoreCase = true) }?.value ?:
			                activeSecurityConnections.values.first()
			val handle = connection.createSecurityContext(packageName ?: sourcePackageName, appName)
			bmwCerts = connection.loadAppCert(handle)
			this.bmwCerts = bmwCerts
			connection.releaseSecurityContext(handle)
			return bmwCerts
		}
	}
}