package me.hufman.idriveconnectionkit.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.bmwgroup.connected.internal.security.ICarSecurityService
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentHashMap


object SecurityService {
	const val TAG = "IDriveSecurityService"

	// The list of known BMW/Mini apps that we can connect to
	// We'll try to connect to any of them that are installed
	val knownSecurityServices = mapOf(
			"BMWClassicUSA" to "com.bmwgroup.connected.bmw.usa.SECURITY_SERVICE",
			"MiniClassicUSA" to "com.bmwgroup.connected.mini.usa.SECURITY_SERVICE",
			"BMWClassic" to "com.bmwgroup.connected.bmw.SECURITY_SERVICE",
			"MiniClassic" to "com.bmwgroup.connected.mini.SECURITY_SERVICE",
			"BMWConnectedNA" to "de.bmw.connected.na.SECURITY_SERVICE",
			"MiniConnectedNA" to "de.mini.connected.na.SECURITY_SERVICE",
			"BMWConnected" to "de.bmw.connected.SECURITY_SERVICE",
			"MiniConnected" to "de.mini.connected.SECURITY_SERVICE"
	)
	val installedSecurityServices = Collections.synchronizedSet(HashSet<String>())   // what services are detected as installed
	val securityConnections = ConcurrentHashMap<String, SecurityConnectionListener>() // listeners that are trying to connect
	val activeSecurityConnections = ConcurrentHashMap<String, ICarSecurityService>()  // active proxy objects
	var success = false // we have successfully connected at least once, even if we have since disconnected
	var sourcePackageName: String = ""  // the default packageName
	var listener = Runnable {}
	var bmwCerts: ByteArray? = null


	class SecurityConnectionListener(val context: Context, val name: String, val intentName: String) : ServiceConnection {
		fun connect() {
			// Try to connect to a given BMW/Mini app
			Log.i(TAG, "Connecting to $name")
			val intent = Intent(intentName)
			intent.setPackage(intentName.substring(0, intentName.lastIndexOf('.')))
			try {
				val exists = context.bindService(intent, this, Context.BIND_AUTO_CREATE)
				if (exists) {
					installedSecurityServices.add(name)
				} else {
					disconnect()
				}
			} catch (e: SecurityException) {
				// new versions of BMW Connected don't let us connect
				installedSecurityServices.add(name)
				disconnect()
			}
		}


		override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
			// Remember that we connected
			Log.i(TAG, "Connected to security service $name")
			val previousConnectionCount = activeSecurityConnections.size
			activeSecurityConnections[name] = ICarSecurityService.Stub.asInterface(service)
			success = true
			if (previousConnectionCount == 0) {
				listener.run()
			}
		}

		override fun onServiceDisconnected(p0: ComponentName?) {
			// Remove ourselves from the list of active connections
			Log.i(TAG, "Disconnected from security service $name")
			this.disconnect()
		}

		fun disconnect() {
			try {
				context.unbindService(this)
			} catch (e: Exception) {
				// ignore
			}
			activeSecurityConnections.remove(name)
			securityConnections.remove(name)
			if (securityConnections.size == 0) {
				// no longer connecting
				listener.run()
			}
			if (activeSecurityConnections.size == 0) {
				// not connected
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
		val packageManager = context.packageManager
		verifyConnections() // clean out any dead connections that have been uninstalled
		knownSecurityServices.forEach { (key, value) ->
			val packageName = value.substring(0, value.lastIndexOf('.'))
			try {
				// check if we have the package installed
				packageManager.getPackageInfo(packageName, 0)

				if (!activeSecurityConnections.containsKey(key)) {
					securityConnections.remove(key)?.disconnect()
					val connection = SecurityConnectionListener(context, key, value)
					securityConnections[key] = connection
					connection.connect()
				} else {
					Log.i(TAG, "Already connected to $key")
				}
			} catch (e: PackageManager.NameNotFoundException) {
				Log.i(TAG, "$key not installed")
			}
		}
	}

	fun disconnect() {
		activeSecurityConnections.keys.forEach {key ->
			securityConnections.remove(key)?.disconnect()
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
				securityConnections.remove(key)?.disconnect()
			} else {
				try {
					val handle = connection.createSecurityContext(sourcePackageName, "test app")
					connection.releaseSecurityContext(handle)
				} catch (e: Exception) {
					Log.i(TAG, "Removing dead connection to $key")
					activeSecurityConnections.remove(key)
					securityConnections.remove(key)?.disconnect()
				}
			}
		}
	}

	/**
	 * Whether we are currently connecting to a security service
	 */
	fun isConnecting(brandHint: String = ""): Boolean {
		return securityConnections.keys.any { it.startsWith(brandHint, ignoreCase = true) }
	}
	/**
	 * Whether any BMW/Mini security services are connected
	 */
	fun isConnected(brandHint: String = ""): Boolean {
		return activeSecurityConnections.keys.any { it.startsWith(brandHint, ignoreCase = true) }
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