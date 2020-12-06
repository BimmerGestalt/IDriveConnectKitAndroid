package me.hufman.idriveconnectionkit.android.security

import android.content.Context
import android.content.pm.PackageManager
import java.lang.reflect.InvocationTargetException
import java.util.*

class SecurityAccess private constructor() {

	companion object {
		const val TAG = "IDriveSecurity"

		private val _listeners = Collections.synchronizedMap(WeakHashMap<SecurityAccess, () -> Unit>())
		fun getInstance(context: Context): SecurityAccess {
			SecurityAccess.context = context.applicationContext
			return SecurityAccess()
		}

		val installedSecurityServices = Collections.synchronizedSet(HashSet<KnownSecurityServices>())   // what services are detected as installed
		var customPackageName: String? = null  // an overridden packageName, instead of using the context packageName
		var bmwCerts: ByteArray? = null

		lateinit var context: Context
		val securityServiceManager by lazy { SecurityServiceManager(context, installedSecurityServices, Runnable {onUpdate()}) }
		val securityModuleManager by lazy { SecurityModuleManager(context, installedSecurityServices) }

		fun addListener(owner: SecurityAccess, listener: () -> Unit) {
			_listeners[owner] = listener
		}

		fun onUpdate() {
			_listeners.values.toList().forEach {
				it.invoke()
			}
		}
	}

	var callback: () -> Unit = {}
		set(value) {
			field = value
			addListener(this, value)
		}

	/** Discover any installed security services */
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

	/** Try to connect to any installed security services */
	fun connect() {
		discover()

		securityServiceManager.connect()
		securityModuleManager.connect()

		if (securityModuleManager.isConnected()) {
			onUpdate()
		}
	}

	fun disconnect() {
		securityServiceManager.disconnect()
		securityModuleManager.disconnect()
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
		return securityModuleManager.isConnected() ||
				securityServiceManager.connectedSecurityServices.keys.any { it.name.startsWith(brandHint, ignoreCase = true) }
	}

	/**
	 * Sign the given challenge from the car, returning a response to login with
	 * The challenge should be 16 bytes long
	 * The appName doesn't really matter
	 * It will throw IllegalArgumentException for invalid challenges
	 */
	@Throws(IllegalArgumentException::class)
	fun signChallenge(appName: String = "", challenge: ByteArray):ByteArray {
		synchronized(SecurityAccess) {
			val connection = securityServiceManager.connectedSecurityServices.values.firstOrNull() ?:
				securityModuleManager.getConnection()!!
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
		synchronized(SecurityAccess) {
			var bmwCerts = bmwCerts
			if (bmwCerts != null) return bmwCerts
			val connection = securityServiceManager.connectedSecurityServices.entries.firstOrNull { it.key.name.startsWith(brandHint, ignoreCase = true) }?.value ?:
				securityServiceManager.connectedSecurityServices.values.firstOrNull() ?:
				securityModuleManager.getConnection()!!
			val handle = connection.createSecurityContext(customPackageName ?: context.packageName, appName)
			bmwCerts = connection.loadAppCert(handle)
			SecurityAccess.bmwCerts = bmwCerts
			connection.releaseSecurityContext(handle)
			return bmwCerts
		}
	}
}