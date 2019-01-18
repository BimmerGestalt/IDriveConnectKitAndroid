package com.bmwgroup.connected.core.services.security

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.bmwgroup.connected.core.audio.AudioModule.*
import com.bmwgroup.connected.internal.security.ICarSecurityService

const val TAG = "CarSecurityService"

object CarSecurityServiceImpl: ICarSecurityService.Stub() {
	fun init(brandName: String): CarSecurityServiceImpl {
		a(brandName)
		return this
	}
	override fun finalize() {
		super.finalize()
		synchronized(TAG) {
			b()
		}
	}

	override fun createSecurityContext(packageName: String?, appName: String?): Long {
		synchronized(TAG) {
			val handle = c(packageName, appName).toLong()
			Log.i(TAG, "createSecurityContext(\"$packageName\", \"$appName\") = $handle")
			return handle
		}
	}

	override fun releaseSecurityContext(contextHandle: Long) {
		synchronized(TAG) {
			Log.i(TAG, "releaseSecurityContext($contextHandle)")
			d(contextHandle.toInt())
		}
	}

	override fun loadAppCert(contextHandle: Long): ByteArray {
		synchronized(TAG) {
			Log.i(TAG, "loadAppCert($contextHandle)")
			return f(contextHandle.toInt())
		}
	}

	override fun signChallenge(contextHandle: Long, challenge: ByteArray?): ByteArray {
		synchronized(TAG) {
			Log.i(TAG, "signChallenge($contextHandle)")
			return g(contextHandle.toInt(), challenge)
		}
	}
}

class BMWSecurityService: Service() {
	override fun onBind(p0: Intent?): IBinder {
		Log.i(TAG, "Receiving connection as a BMW Security Service")
		synchronized(TAG) {
			return CarSecurityServiceImpl.init("bmw")
		}

	}
}
class MiniSecurityService: Service() {
	override fun onBind(p0: Intent?): IBinder {
		Log.i(TAG, "Receiving connection as a Mini Security Service")
		synchronized(TAG) {
			return CarSecurityServiceImpl.init("mini")
		}
	}
}