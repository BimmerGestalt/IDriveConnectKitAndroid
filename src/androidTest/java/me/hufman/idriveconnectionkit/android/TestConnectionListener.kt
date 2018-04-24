package me.hufman.idriveconnectionkit.android


import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TestConnectionListener {

	private val lock = Semaphore(0) // wait for the CarAPI to discover a specific app

	@Test
	fun testConnection() {
		val appContext = InstrumentationRegistry.getTargetContext()

		assertFalse(IDriveConnectionListener.isConnected)
		assertNull(IDriveConnectionListener.brand)
		assertNull(IDriveConnectionListener.host)
		assertNull(IDriveConnectionListener.port)
		assertNull(IDriveConnectionListener.instanceId)

		// prepare listener
		IDriveConnectionListener.callback = Runnable {
			lock.release()
		}

		// now the car connects
		val connectionIntent = Intent("com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_ATTACHED")
		connectionIntent.putExtra("EXTRA_BRAND", "mini")
		connectionIntent.putExtra("EXTRA_ACCESSORY_BRAND", "mini")
		connectionIntent.putExtra("EXTRA_HOST", "127.0.0.1")
		connectionIntent.putExtra("EXTRA_PORT", 1234)
		connectionIntent.putExtra("EXTRA_INSTANCE_ID", 13)
		appContext.sendBroadcast(connectionIntent)

		// wait for the app to notice
		lock.tryAcquire(10000, TimeUnit.MILLISECONDS)

		// verify the handling
		assertTrue(IDriveConnectionListener.isConnected)
		assertEquals("mini", IDriveConnectionListener.brand)
		assertEquals("127.0.0.1", IDriveConnectionListener.host)
		assertEquals(1234, IDriveConnectionListener.port)
		assertEquals(13, IDriveConnectionListener.instanceId)

		// now the car detaches
		val disconnectionIntent = Intent("com.bmwgroup.connected.accessory.ACTION_CAR_ACCESSORY_DETACHED")
		appContext.sendBroadcast(disconnectionIntent)

		// wait for the app to notice
		lock.tryAcquire(10000, TimeUnit.MILLISECONDS)

		// verify the handling
		assertFalse(IDriveConnectionListener.isConnected)
		assertEquals("mini", IDriveConnectionListener.brand)
		assertEquals("127.0.0.1", IDriveConnectionListener.host)
		assertEquals(1234, IDriveConnectionListener.port)
		assertEquals(13, IDriveConnectionListener.instanceId)
	}
}