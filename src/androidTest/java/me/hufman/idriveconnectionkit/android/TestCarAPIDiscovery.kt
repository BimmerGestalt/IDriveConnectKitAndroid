package me.hufman.idriveconnectionkit.android

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.bmwgroup.connected.car.app.BrandType
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TestCarAPIDiscovery {

	val TAG = "TestCarAPIDiscovery"

	private val lock = Semaphore(0) // wait for the CarAPI to discover a specific app
	inner class WaitForCarAPI(val appId: String): CarAPIDiscovery.DiscoveryCallback {
		override fun discovered(app: CarAPIClient) {
			if (app.id == appId)
				lock.release()
		}
	}

	@Test
	fun startDiscovery() {
		val appContext = InstrumentationRegistry.getTargetContext()

		CarAPIDiscovery.discoverApps(appContext, WaitForCarAPI("com.clearchannel.iheartradio.connect"))
		lock.tryAcquire(60000, TimeUnit.MILLISECONDS)    // wait up to 60s for the CarAPI app to respond
		CarAPIDiscovery.cancelDiscovery()
		Log.i(TAG, "Discovered " + CarAPIDiscovery.discoveredApps.size + " Car API apps")
		CarAPIDiscovery.discoveredApps.entries.forEach {
			Log.i(TAG, it.value.toString())
		}
		assertTrue(CarAPIDiscovery.discoveredApps.containsKey("com.clearchannel.iheartradio.connect"))

		val app = CarAPIDiscovery.discoveredApps["com.clearchannel.iheartradio.connect"] as CarAPIClient
		assertEquals("com.clearchannel.iheartradio.connect", app.id)
		assertEquals("iHeartRadio", app.title)
		assertEquals("Radio", app.category)
		assertEquals("v3", app.version)
		assertEquals(BrandType.ALL, app.brandType)
		assertNotNull(app.appIcon)
		assertNotNull(app.connectIntentName)
		assertNotNull(app.disconnectIntentName)

		// try to fetch resources from it
		assertNotNull(app.getAppCertificate(appContext))
		assertNull(app.getUiDescription(appContext))
		assertNotNull(app.getTextsDB(appContext, "bmw"))
		assertNotNull(app.getImagesDB(appContext, "bmw"))
	}
}