package me.hufman.idriveconnectionkit.android


import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.awaitility.Awaitility.await
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestConnectionListener {

	@Test
	fun testListenerCallback() {
		var listenerCount = 0
		var listenerCalled = false
		run {
			val instance = IDriveConnectionObserver {
				listenerCalled = true
			}
			assertTrue(IDriveConnectionListener.listeners.isNotEmpty())
			listenerCount = IDriveConnectionListener.listeners.size
			IDriveConnectionStatus.reset()
			assertTrue(listenerCalled)
		}

		await().untilAsserted {
			// create garbage to collect, Android will skip otherwise
			val value = ByteArray(1024*1024)
			System.gc()
			assertEquals(listenerCount - 1, IDriveConnectionListener.listeners.size)
//			assertTrue(IDriveConnectionStatus.listeners.isEmpty())
		}
	}

	@Test
	fun testConnection() {
		val appContext = InstrumentationRegistry.getTargetContext()
		val instance = IDriveConnectionReceiver()
		instance.subscribe(appContext)

		assertFalse(IDriveConnectionStatus.isConnected)
		assertNull(IDriveConnectionStatus.brand)
		assertNull(IDriveConnectionStatus.host)
		assertNull(IDriveConnectionStatus.port)
		assertNull(IDriveConnectionStatus.instanceId)

		// prepare listener
		var listenerCalled = false
		instance.callback = {
			listenerCalled = true
		}

		// now the car connects
		val connectionIntent = Intent("com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_ATTACHED")
		connectionIntent.putExtra("EXTRA_BRAND", "mini")
		connectionIntent.putExtra("EXTRA_ACCESSORY_BRAND", "mini")
		connectionIntent.putExtra("EXTRA_HOST", "127.0.0.1")
		connectionIntent.putExtra("EXTRA_PORT", 1234)
		connectionIntent.putExtra("EXTRA_INSTANCE_ID", 13)
		appContext.sendBroadcast(connectionIntent)

		// verify the handling
		await().untilAsserted {assertTrue(IDriveConnectionStatus.isConnected)}
		assertTrue(listenerCalled)
		assertEquals("mini", IDriveConnectionStatus.brand)
		assertEquals("127.0.0.1", IDriveConnectionStatus.host)
		assertEquals(1234, IDriveConnectionStatus.port)
		assertEquals(13, IDriveConnectionStatus.instanceId)

		// now the car detaches
		listenerCalled = false
		val disconnectionIntent = Intent("com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_DETACHED")
		appContext.sendBroadcast(disconnectionIntent)

		// verify the handling
		await().untilAsserted {assertFalse(IDriveConnectionStatus.isConnected)}
		assertTrue(listenerCalled)
		assertEquals("mini", IDriveConnectionStatus.brand)
		assertEquals("127.0.0.1", IDriveConnectionStatus.host)
		assertEquals(1234, IDriveConnectionStatus.port)
		assertEquals(13, IDriveConnectionStatus.instanceId)

		instance.unsubscribe(appContext)
	}
}