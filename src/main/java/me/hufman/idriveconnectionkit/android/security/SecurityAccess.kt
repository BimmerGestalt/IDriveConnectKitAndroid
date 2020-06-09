package me.hufman.idriveconnectionkit.android.security

import android.content.Context
import android.content.pm.PackageManager
import java.lang.reflect.InvocationTargetException
import java.util.*

class SecurityAccess(val context: Context, var listener: Runnable = Runnable {}) {

	companion object {
		const val TAG = "IDriveSecurity"

		private var instance: SecurityAccess? = null
		fun getInstance(context: Context): SecurityAccess {
			synchronized(SecurityAccess::javaClass) {
				val instance = instance
				return if (instance == null) {
					val newInstance = SecurityAccess(context)
					this.instance = newInstance
					newInstance
				} else {
					instance
				}
			}
		}
	}

	val installedSecurityServices = Collections.synchronizedSet(HashSet<KnownSecurityServices>())   // what services are detected as installed
	var customPackageName: String? = null  // an overridden packageName, instead of using the context packageName
	var bmwCerts: ByteArray? = null

	val securityServiceManager = SecurityServiceManager(context, installedSecurityServices, Runnable {listener.run()})
	val securityModuleManager = SecurityModuleManager(context, installedSecurityServices)

	fun discover() {
		val packageManager = context.packageManager
		KnownSecurityServices.values().forEach { securityService ->
			try {
				// check if we have the package installed
				val packageName = securityService.packageName
				packageManager.getPackageInfo(packageName, 0)
				installedSecurityServices.add(securityService)
			} catch (e: PackageManager.NameNotFoundException) {
				installedSecurityServices.remove(securityService)
			}
		}
	}

	fun connect() {
		discover()

		securityServiceManager.connect()
//		securityModuleManager.connect() // TODO this is buggy sometimes

		if (securityModuleManager.connectedSecurityModules.isNotEmpty()) {
			listener.run()
		}
	}

	fun disconnect() {
		securityServiceManager.disconnect()
	}

	/**
	 * Whether we are currently connecting to a security service
	 */
	fun isConnecting(brandHint: String = ""): Boolean {
		val connected = securityServiceManager.connectedSecurityServices.keys
		val pending = securityServiceManager.securityConnections.keys - connected
		return pending.isNotEmpty() && pending.any { it.name.startsWith(brandHint, ignoreCase = true) }
	}
	/**
	 * Whether any BMW/Mini security services are connected
	 */
	fun isConnected(brandHint: String = ""): Boolean {
		return securityServiceManager.connectedSecurityServices.keys.any { it.name.startsWith(brandHint, ignoreCase = true) } ||
				securityModuleManager.connectedSecurityModules.keys.any { it.name.startsWith(brandHint, ignoreCase = true) }
	}

	/**
	 * Sign the given challenge from the car, returning a response to login with
	 * The challenge should be 16 bytes long
	 * The appName doesn't really matter
	 * It will throw IllegalArgumentException for invalid challenges
	 */
	@Throws(IllegalArgumentException::class)
	fun signChallenge(appName: String = "", challenge: ByteArray):ByteArray {
		synchronized(this) {
			val connection = securityServiceManager.connectedSecurityServices.values.firstOrNull() ?:
					securityModuleManager.connectedSecurityModules.values.first()
			try {
				val handle = connection.createSecurityContext(customPackageName
						?: context.packageName, appName)
				val response = connection.signChallenge(handle, challenge)
				connection.releaseSecurityContext(handle)

				if (response?.size == 0) {
					// The Classic apps raise SecurityException for invalid challenges
					// The new apps return ByteArray(0), so we should raise ourselves
					throw IllegalArgumentException("Invalid challenge")
				}
				return response
			} catch (e: SecurityException) {
				// Sometimes the JNI exception gets converted to SecurityException
				throw java.lang.IllegalArgumentException("Invalid challenge")
			} catch (e: InvocationTargetException) {
				// Sometimes the JNI exception comes through
				throw java.lang.IllegalArgumentException("Invalid challenge")
			}
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
				securityModuleManager.connectedSecurityModules.entries.firstOrNull { it.key.name.startsWith(brandHint, ignoreCase = true) }?.value ?:
				securityServiceManager.connectedSecurityServices.values.firstOrNull() ?:
				securityModuleManager.connectedSecurityModules.values.first()
			val handle = connection.createSecurityContext(customPackageName ?: context.packageName, appName)
			bmwCerts = connection.loadAppCert(handle)
			this.bmwCerts = bmwCerts
			connection.releaseSecurityContext(handle)
			return bmwCerts
		}
	}
}