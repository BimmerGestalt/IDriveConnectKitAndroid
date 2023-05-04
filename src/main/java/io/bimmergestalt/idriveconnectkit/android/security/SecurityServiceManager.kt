package io.bimmergestalt.idriveconnectkit.android.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.bmwgroup.connected.internal.security.ICarSecurityService
import io.bimmergestalt.idriveconnectkit.android.security.SecurityAccess.Companion.TAG
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

class SecurityServiceManager(val context: Context, val installedSecurityServices: Set<KnownSecurityServices>, var listener: Runnable = Runnable {}) {
	val securityConnections = ConcurrentHashMap<KnownSecurityServices, SecurityConnectionListener>() // listeners that are trying to connect
	val connectedSecurityServices = ConcurrentHashMap<KnownSecurityServices, ICarSecurityService>()  // connected proxy objects
	var success = false // we have successfully connected at least once, even if we have since disconnected


	fun connect() {
		verifyConnections()

		installedSecurityServices.filter {
			// Mobile20 does not allow Security Service connections, don't try
			!it.packageName.contains("mobile20")
		}.forEach { securityService ->
			if (!connectedSecurityServices.containsKey(securityService)) {
				securityConnections.remove(securityService)?.disconnect()
				val connection = SecurityConnectionListener(context, securityService)
				securityConnections[securityService] = connection
				connection.connect()
			}
		}
	}

	fun disconnect() {
		val connections = LinkedList(securityConnections.keys)
		connections.forEach { key ->
			securityConnections.remove(key)?.disconnect()
		}
	}

	/**
	 * Check all the active connections and remove any that aren't actually connected
	 */
	fun verifyConnections() {
		val connectedKeys = ArrayList(connectedSecurityServices.keys)
		connectedKeys.forEach { key ->
			val connection = connectedSecurityServices[key]
			if (connection == null) {
				connectedSecurityServices.remove(key)
				securityConnections.remove(key)?.disconnect()
			} else {
				try {
					val handle = connection.createSecurityContext(context.packageName, "test app")
					connection.releaseSecurityContext(handle)
				} catch (e: Exception) {
					Log.i(TAG, "Removing dead connection to $key")
					connectedSecurityServices.remove(key)
					securityConnections.remove(key)?.disconnect()
				}
			}
		}
	}

	inner class SecurityConnectionListener(val context: Context, val securityService: KnownSecurityServices) : ServiceConnection {
		fun connect() {
			// Try to connect to a given BMW/Mini app
			Log.i(TAG, "Connecting to $securityService")
			val intent = Intent(securityService.className)
			intent.setPackage(securityService.packageName)
			try {
				val exists = context.bindService(intent, this, Context.BIND_AUTO_CREATE)
				if (!exists) {
					disconnect()
				}
			} catch (e: SecurityException) {
				// new versions of BMW Connected don't let us connect
				disconnect()
			}
		}

		override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
			// Remember that we connected
			Log.i(TAG, "Connected to security service $securityService")
			val previousConnectionCount = connectedSecurityServices.size
			connectedSecurityServices[securityService] = ICarSecurityService.Stub.asInterface(service)
			success = true
			if (previousConnectionCount == 0) {
				listener.run()
			}
		}

		override fun onServiceDisconnected(p0: ComponentName?) {
			// Remove ourselves from the list of active connections
			Log.i(TAG, "Disconnected from security service $securityService")
			this.disconnect()
		}

		fun disconnect() {
			try {
				context.unbindService(this)
			} catch (e: Exception) {
				// ignore
			}
			connectedSecurityServices.remove(securityService)
			securityConnections.remove(securityService)
			if (securityConnections.size == 0) {
				// no longer connecting
				listener.run()
			}
			if (connectedSecurityServices.size == 0) {
				// not connected
				listener.run()
			}
		}
	}
}

