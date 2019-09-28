package me.hufman.idriveconnectionkit.android

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.bmwgroup.connected.car.app.BrandType
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TestCarAPIDiscovery {

	companion object {
		val TAG = "TestCarAPIDiscovery"
		fun loadInputStream(input: InputStream, maxSize: Long = 5000000): ByteArray {
			val writer = ByteArrayOutputStream()
			val data = ByteArray(4096)
			try {
				var read = input.read(data, 0, data.size)
				while (read > -1 && (maxSize == 0L || writer.size() < maxSize)) {
					writer.write(data, 0, read)
					read = input.read(data, 0, data.size)
				}
				return writer.toByteArray()
			} catch (e: IOException) {
				Log.e(TAG, "Failed to load resource")
				return ByteArray(0)
			}

		}
	}

	private val lock = Semaphore(0) // wait for the CarAPI to discover a specific app
	class WaitForCarAPI(val appId: String, val lock: Semaphore): CarAPIDiscovery.DiscoveryCallback {
		override fun discovered(app: CarAPIClient) {
			if (app.appInfo.id == appId)
				lock.release()
		}
	}

	@Test
	fun startDiscovery() {
		val appContext = InstrumentationRegistry.getTargetContext()

		CarAPIDiscovery.discoverApps(appContext, WaitForCarAPI("com.clearchannel.iheartradio.connect", lock))
		lock.tryAcquire(60000, TimeUnit.MILLISECONDS)    // wait up to 60s for the CarAPI app to respond
		CarAPIDiscovery.cancelDiscovery(appContext)
		Log.i(TAG, "Discovered " + CarAPIDiscovery.discoveredApps.size + " Car API apps")
		CarAPIDiscovery.discoveredApps.entries.forEach {
			Log.i(TAG, it.value.toString())
		}
		assertTrue(CarAPIDiscovery.discoveredApps.containsKey("com.clearchannel.iheartradio.connect"))

		val appClient = CarAPIDiscovery.discoveredApps["com.clearchannel.iheartradio.connect"] as CarAPIClient
		val app = appClient.appInfo
		assertEquals("com.clearchannel.iheartradio.connect", app.id)
		assertEquals("iHeartRadio", app.title)
		assertEquals("Radio", app.category)
		assertEquals("v3", app.version)
		assertEquals(BrandType.ALL, app.brandType)
		assertNotNull(app.appIcon)
		assertNotNull(app.connectIntentName)
		assertNotNull(app.disconnectIntentName)

		// try to fetch resources from it
		assertNotNull(appClient.getAppCertificate())
		assertNull(appClient.getUiDescription())
		assertNotNull(appClient.getTextsDB("bmw"))
		assertNotNull(appClient.getImagesDB("bmw"))

		val certIS = appClient.getAppCertificate()
		val cert = loadInputStream(certIS as InputStream)
		assertEquals(6299, cert.size)

		val parsedCerts = CertMangling.loadCerts(cert)
		assertNotNull(parsedCerts)
		val certNames = parsedCerts?.mapNotNull {
			CertMangling.getCN(it)
		} ?: LinkedList()
		assertEquals(3, certNames.size)
		Log.d(TAG, certNames.toString())
		assertTrue(certNames.containsAll(arrayListOf("a4a_android-ca", "a4a_root-ca", "a4a_app_iheartradioconnect___com_clearchannel_iheartradio_connect_00.00.18")))
	}
}