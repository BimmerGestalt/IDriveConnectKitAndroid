package me.hufman.idriveconnectionkit.android.security

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert
import me.hufman.idriveconnectionkit.android.CertMangling
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class TestSecurityModule {
	@Test
	fun testManager() {
		val appContext = InstrumentationRegistry.getTargetContext()
		val securityAccess = SecurityAccess(appContext)
		securityAccess.discover()

		val manager = SecurityModuleManager(appContext, securityAccess.installedSecurityServices)
		manager.connect()
		assertTrue(0 < securityAccess.installedSecurityServices.size)
		assertEquals(securityAccess.installedSecurityServices, manager.connectedSecurityModules.keys)

		// test that it signs a challenge for us
		val challenge = byteArrayOf(0x6d, 0x58, 0x5f, 0x14,
				0x72, 0x72, 0x19, 0x75,
				0x4e, 0x73, 0x19, 0x38,
				0x61, 0x2f, 0x50, 0x78)
		manager.connectedSecurityModules.entries.forEach { entry ->
			val name = entry.key
			val securityModule = entry.value
			val handle = securityModule.createSecurityContext(appContext.packageName, "TestSecurityServiceJava")
			var response = securityModule.signChallenge(handle, challenge)
			Assert.assertEquals("$name - Received a challenge response", 512, response.size)
			Assert.assertEquals("$name - Received the correct challenge response", 0x15, response[0])
		}
	}


	@Test
	fun testBMWCertificate() {
		val appContext = InstrumentationRegistry.getTargetContext()
		val securityAccess = SecurityAccess(appContext)
		securityAccess.discover()

		val manager = SecurityModuleManager(appContext, securityAccess.installedSecurityServices)
		manager.connect()
		assertTrue(0 < securityAccess.installedSecurityServices.size)
		assertEquals(securityAccess.installedSecurityServices, manager.connectedSecurityModules.keys)

		manager.connectedSecurityModules.entries.forEach { entry ->
			val name = entry.key
			val securityModule = entry.value

			val handle = securityModule.createSecurityContext(appContext.packageName, "TestSecurityServiceJava")
			val bmwCert = securityModule.loadAppCert(handle)
			val parsedCerts = CertMangling.loadCerts(bmwCert)
			assertNotNull(parsedCerts)
			val certNames = parsedCerts?.mapNotNull {
				CertMangling.getCN(it)
			} ?: LinkedList()
			assertEquals(2, certNames.size)
			assertArrayEquals(arrayOf("a4a_app_Android_FeatureCertificate_PIA_KML_VOICE_00.00.01", "a4a_app_BMWTouchCommand_Connection_00.00.10"), certNames.toTypedArray().sortedArray())
		}
	}

}