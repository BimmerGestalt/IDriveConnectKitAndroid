package me.hufman.idriveconnectionkit.android.security

import android.content.Context
import android.os.IBinder
import android.util.Log
import com.bmwgroup.connected.internal.security.ICarSecurityService
import me.hufman.idriveconnectionkit.android.security.SecurityAccess.Companion.TAG
import java.lang.IllegalArgumentException
import java.util.concurrent.ConcurrentHashMap

/**
 * Directly reaches into Connected to query the Security Module
 */
class SecurityModuleManager(val context: Context, val installedSecurityServices: Set<KnownSecurityServices>) {
	val connectedSecurityModules = ConcurrentHashMap<KnownSecurityServices, SecurityModuleClient>()

	fun connect() {
		connectedSecurityModules.keys.retainAll(installedSecurityServices)
		installedSecurityServices.forEach {
			try {
				connectedSecurityModules[it] = SecurityModuleClient(context, it.packageName)
			} catch (e: Exception) {
				Log.w(TAG, "Could not inspect security module in $it", e)
			}
		}
	}
}

/**
 * Given a package name of a Connected app, establish a connection into the SecurityModule
 * Raise ValueException if it was unsuccessful
 */
class SecurityModuleClient(context: Context, val packageName: String): ICarSecurityService {
	val moduleContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE)
	val proxy: SecurityModuleProxy

	init {
		// try to locate the proper proxy
		proxy = try {
			SecurityModuleClass(moduleContext.classLoader, "com.bmwgroup.connected.core.security.SecurityModule")
		} catch (e: Exception) {
			SecurityModuleClass(moduleContext.classLoader, "com.bmwgroup.connected.core.audio.AudioModule")
		}

		proxy.init("")
	}

	override fun asBinder(): IBinder {
		throw NotImplementedError()
	}

	override fun createSecurityContext(packageName: String, appName: String): Long {
		return proxy.createSecurityContext(packageName, appName).toLong()
	}

	override fun loadAppCert(contextHandle: Long): ByteArray {
		return proxy.getCertificates(contextHandle.toInt())
	}
	override fun signChallenge(contextHandle: Long, challenge: ByteArray?): ByteArray {
		return proxy.signChallenge(contextHandle.toInt(), challenge ?: ByteArray(0))
	}
	override fun releaseSecurityContext(contextHandle: Long) {
		proxy.destroySecurityContext(contextHandle.toInt())
	}
}

/**
 * This pattern of abstract runnables is meant to enable subclasses to
 * locate functions at the time of construction
 * while still providing a proper type interface for the wrapper class to call, in case
 * the concrete runnables have a different parameter order or something
 */
abstract class SecurityModuleProxy {
	protected abstract val _init: (String) -> Unit
	fun init(brandName: String) = _init(brandName)

	protected abstract val _createSecurityContext: (String, String) -> Int
	fun createSecurityContext(packageName: String, appName: String): Int = _createSecurityContext(packageName, appName)

	protected abstract val _getCertificates: (Int) -> ByteArray
	fun getCertificates(handle: Int): ByteArray = _getCertificates(handle)

	protected abstract val _signChallenge: (Int, ByteArray) -> ByteArray
	fun signChallenge(handle: Int, challenge: ByteArray): ByteArray = _signChallenge(handle, challenge)

	protected abstract val _destroySecurityContext: (Int) -> Unit
	fun destroySecurityContext(handle: Int) = _destroySecurityContext(handle)

	protected abstract val _deInit: () -> Unit
	fun deInit() = _deInit()
}

/**
 * Throws
 */
class SecurityModuleClass(classLoader: ClassLoader, className: String): SecurityModuleProxy() {
	override val _init: (String) -> Unit
	override val _createSecurityContext: (String, String) -> Int
	override val _getCertificates: (Int) -> ByteArray
	override val _signChallenge: (Int, ByteArray) -> ByteArray
	override val _destroySecurityContext: (Int) -> Unit
	override val _deInit: () -> Unit

	init {
		// find the matching functions from the SecurityModule to fit our SecurityModuleProxy
		val inspected = classLoader.loadClass(className)

		try {
			val init = inspected.declaredMethods.filter {
				it.returnType == Void::class.javaPrimitiveType &&
				it.parameterTypes.contentEquals(arrayOf(String::class.java))
			}.ensureLength(1).first()
			_init = { name -> init(null, name) }

			val createSecurityContext = inspected.declaredMethods.filter {
				it.returnType == Int::class.javaPrimitiveType &&
				it.parameterTypes.contentEquals(arrayOf(String::class.java, String::class.java))
			}.ensureLength(1).first()
			_createSecurityContext = { packageName, appName -> createSecurityContext(null, packageName, appName) as Int }

			val getCertificates = inspected.declaredMethods.filter {
				it.returnType == ByteArray::class.java &&
				it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
			}.ensureLength(1).first()
			_getCertificates = { handle -> getCertificates(null, handle) as ByteArray }

			val signChallenge = inspected.declaredMethods.filter {
				it.returnType == ByteArray::class.java &&
				it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType, ByteArray::class.java))
			}.ensureLength(1).first()
			_signChallenge = { handle, challenge -> signChallenge(null, handle, challenge) as ByteArray }

			val destroySecurityContext = inspected.declaredMethods.filter {
				it.returnType == Void::class.javaPrimitiveType &&
				it.parameterTypes.contentEquals(arrayOf(Int::class.javaPrimitiveType))
			}.ensureLength(1).first()
			_destroySecurityContext = { handle -> destroySecurityContext(null, handle) }

			val deInit = inspected.declaredMethods.filter {
				it.returnType == Void::class.javaPrimitiveType &&
				it.parameterTypes.contentEquals(arrayOf())
			}.ensureLength(1).first()
			_deInit = { deInit(null) }
		} catch (e: Exception) {
			// could not find a method in this class
			Log.w(TAG, "Unable to parse $className into a SecurityModule:")
			_dumpMethods(classLoader, className)
			throw e
		}
	}

	companion object {
		fun _dumpMethods(classLoader: ClassLoader, className: String) {
			val inspected = classLoader.loadClass(className)

			inspected.declaredMethods.forEach {
				Log.i(TAG, "Found SecurityModule method: $it with return type ${it.returnType}")
			}
		}

	}
}

fun <E> Collection<E>.ensureLength(length: Int): Collection<E> {
	if (this.size != length) {
		throw IllegalArgumentException("Unxpected length ${this.size}, needed $length")
	}
	return this
}