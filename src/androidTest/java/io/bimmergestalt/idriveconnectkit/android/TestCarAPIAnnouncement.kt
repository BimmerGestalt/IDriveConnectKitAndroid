package io.bimmergestalt.idriveconnectkit.android

import android.support.test.InstrumentationRegistry
import com.bmwgroup.connected.car.app.BrandType
import org.junit.Assert.assertEquals
import org.junit.Test

class TestCarAPIAnnouncement {
	@Test
	fun testAppInfo() {
		val appContext = InstrumentationRegistry.getTargetContext()
		val appInfo = CarAPIAppInfo(appContext.packageName, "Test App", "OnlineServices", "2", null, BrandType.ALL, null, null, null)
		val intent = appInfo.toIntent()

		val receivedAppInfo = CarAPIAppInfo.fromDiscoveryIntent(intent)
		assertEquals(appContext.packageName, receivedAppInfo.id)
		assertEquals("Test App", receivedAppInfo.title)
		assertEquals("OnlineServices", receivedAppInfo.category)
		assertEquals("2", receivedAppInfo.version)
		assertEquals(null, receivedAppInfo.rhmiVersion)
		assertEquals(BrandType.ALL, receivedAppInfo.brandType)
		assertEquals(null, receivedAppInfo.connectIntentName)
		assertEquals(null, receivedAppInfo.disconnectIntentName)
		assertEquals(null, receivedAppInfo.appIcon)
	}
}