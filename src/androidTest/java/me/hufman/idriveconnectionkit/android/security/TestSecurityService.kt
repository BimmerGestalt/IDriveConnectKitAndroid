package me.hufman.idriveconnectionkit.android.security

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert.*
import me.hufman.idriveconnectionkit.android.CertMangling
import org.awaitility.Awaitility.await
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Not expected to succeed if Connected Classic is not installed
 */
@RunWith(AndroidJUnit4::class)
class TestSecurityService {

	@Test
	fun testCarChallengeResponse() {
		var callbackTriggered = false
		val appContext = InstrumentationRegistry.getTargetContext()
		val securityAccess = SecurityAccess.getInstance(appContext)
		val manager = SecurityAccess.securityServiceManager

		// test that we can connect to a security service
		callbackTriggered = false
		securityAccess.callback = { callbackTriggered = true }
		securityAccess.connect()
		await("Connecting to Connected Classic").until { manager.connectedSecurityServices.isNotEmpty() }
		await("Waiting for pending connections").until { (manager.securityConnections.keys - manager.connectedSecurityServices.keys).isEmpty() }
		println("Connected to: ${manager.connectedSecurityServices.keys}")
		Thread.sleep(1000)
		assertEquals("Callback successfully triggered", true, callbackTriggered)

		// test that it signs a challenge for us
		val challenge = byteArrayOf(0x6d, 0x58, 0x5f, 0x14,
		                            0x72, 0x72, 0x19, 0x75,
		                            0x4e, 0x73, 0x19, 0x38,
		                            0x61, 0x2f, 0x50, 0x78)

		manager.connectedSecurityServices.entries.forEach { entry ->
			val name = entry.key
			val securityService = entry.value
			val handle = securityService.createSecurityContext(appContext.packageName, "TestSecurityServiceJava")
			var response = securityService.signChallenge(handle, challenge)

			assertEquals("$name - Received a challenge response", 512, response.size)
			assertEquals("$name - Received the correct challenge response", 0x15, response[0])

			// test that an invalid challenge is not accepted
			try {
				response = securityAccess.signChallenge("TestSecurityServiceJava", ByteArray(0))
				fail("$name - Invalid challenge was signed, returned a response of size " + response.size)
			} catch (e: IllegalArgumentException) {
				assert(e.message!!.contains("Error while calling native function signChallenge"))
			}
		}

		// test that the callback is triggered when disconnecting
		callbackTriggered = false
		var callbackTriggered2 = false
		securityAccess.callback = { callbackTriggered2 = true}
		securityAccess.disconnect()
		assertFalse("Callback successfully not triggered", callbackTriggered)
		assertTrue("Callback successfully triggered", callbackTriggered2)
	}

	@Test
	fun testBMWCertificate() {
		val appContext = InstrumentationRegistry.getTargetContext()
		val securityAccess = SecurityAccess.getInstance(appContext)
		val manager = SecurityAccess.securityServiceManager

		securityAccess.connect()
		await("Connecting to Connected Classic").until { manager.connectedSecurityServices.isNotEmpty() }
		await("Waiting for pending connections").until { (manager.securityConnections.keys - manager.connectedSecurityServices.keys).isEmpty() }
		println("Connected to: ${manager.connectedSecurityServices.keys}")
		manager.connectedSecurityServices.entries.forEach { entry ->
			val name = entry.key
			val securityService = entry.value

			val handle = securityService.createSecurityContext(appContext.packageName, "TestSecurityServiceJava")
			val bmwCert = securityService.loadAppCert(handle)
			val parsedCerts = CertMangling.loadCerts(bmwCert)
			assertNotNull(parsedCerts)
			val certNames = parsedCerts?.mapNotNull {
				CertMangling.getCN(it)
			} ?: LinkedList()
			assertEquals(2, certNames.size)
			assertTrue(certNames.containsAll(arrayListOf("a4a_app_Android_FeatureCertificate_PIA_KML_VOICE_00.00.01", "a4a_app_BMWTouchCommand_Connection_00.00.10")))
		}
	}
}