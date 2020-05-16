package me.hufman.idriveconnectionkit.android.security

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.util.*

class SecurityAccess(val context: Context, var listener: Runnable = Runnable {}) {

	companion object {
		const val TAG = "IDriveSecurity"
	}

	val installedSecurityServices = Collections.synchronizedSet(HashSet<KnownSecurityServices>())   // what services are detected as installed
	var customPackageName: String? = null  // an overridden packageName, instead of using the context packageName
	var bmwCerts: ByteArray? = null

	val securityServiceManager = SecurityServiceManager(context, installedSecurityServices, listener)

	init {
		discover()
	}

	fun discover() {
		val packageManager = context.packageManager
		KnownSecurityServices.values().forEach { securityService ->
			try {
				// check if we have the package installed
				val packageName = securityService.packageName
				packageManager.getPackageInfo(packageName, 0)
				installedSecurityServices.add(securityService)
			} catch (e: PackageManager.NameNotFoundException) {
				Log.i(TAG, "$securityService not installed")
				installedSecurityServices.remove(securityService)
			}
		}
	}

	fun connect() {
		discover()

		securityServiceManager.connect()
	}

	fun disconnect() {
		securityServiceManager.disconnect()
	}

	/**
	 * Whether we are currently connecting to a security service
	 */
	fun isConnecting(brandHint: String = ""): Boolean {
		return securityServiceManager.securityConnections.keys.any { it.name.startsWith(brandHint, ignoreCase = true) }
	}
	/**
	 * Whether any BMW/Mini security services are connected
	 */
	fun isConnected(brandHint: String = ""): Boolean {
		return securityServiceManager.connectedSecurityServices.keys.any { it.name.startsWith(brandHint, ignoreCase = true) }
	}

	/**
	 * Sign the given challenge from the car, returning a response to login with
	 * The challenge should be 16 bytes long
	 * The appName doesn't really matter
	 * It will throw SecurityException for invalid challenges
	 */
	@Throws(SecurityException::class)
	fun signChallenge(appName: String = "", challenge: ByteArray):ByteArray {
		synchronized(this) {
			val connection = securityServiceManager.connectedSecurityServices.values.first()
			val handle = connection.createSecurityContext(customPackageName ?: context.packageName, appName)
			val response = connection.signChallenge(handle, challenge)
			connection.releaseSecurityContext(handle)
			if (response?.size == 0) {
				// The Classic apps raise SecurityException for invalid challenges
				// The new apps return ByteArray(0), so we should raise ourselves
				throw SecurityException("Invalid challenge")
			}
			return response
		}
	}

	/**
	 * Loads the BMW cert from the security service
	 * This is helpful for logging into the car
	 */
	fun fetchBMWCerts(appName: String = "SecurityService", brandHint: String = ""):ByteArray {
		synchronized(this) {
			var bmwCerts = this.bmwCerts
			if (bmwCerts != null) return bmwCerts
			val connection = securityServiceManager.connectedSecurityServices.entries.firstOrNull { it.key.name.startsWith(brandHint, ignoreCase = true) }?.value ?:
				securityServiceManager.connectedSecurityServices.values.first()
			val handle = connection.createSecurityContext(customPackageName ?: context.packageName, appName)
			bmwCerts = connection.loadAppCert(handle)
			this.bmwCerts = bmwCerts
			connection.releaseSecurityContext(handle)
			return bmwCerts
		}
	}
}