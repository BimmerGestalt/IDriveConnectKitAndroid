package me.hufman.idriveconnectionkit.android

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.awaitility.Awaitility.await
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Callable

@RunWith(AndroidJUnit4::class)
class TestSecurityService {
	var callbackTriggered = false

	@Test
	fun testCarChallengeResponse() {
		val appContext = InstrumentationRegistry.getTargetContext()

		// test that we can connect to a security service
		callbackTriggered = false
		SecurityService.subscribe(Runnable { callbackTriggered = true })
		SecurityService.connect(appContext)
		await().until({ SecurityService.isConnected() })
		assertEquals("Callback successfully triggered", true, callbackTriggered)

		// test that the callback triggers again after already being connected
		callbackTriggered = false
		SecurityService.subscribe(Runnable { callbackTriggered = true })
		assertEquals("Callback successfully triggered again", true, callbackTriggered)

		// test that it signs a challenge for us
		val challenge = byteArrayOf(0x6d, 0x58, 0x5f, 0x14,
		                            0x72, 0x72, 0x19, 0x75,
		                            0x4e, 0x73, 0x19, 0x38,
		                            0x61, 0x2f, 0x50, 0x78)
		var response = SecurityService.signChallenge(appContext.packageName, "TestSecurityServiceJava", challenge)
		assertEquals("Received a challenge response", 512, response.size)
		assertEquals("Received the correct challenge response", 0x15, response[0])

		// test that an invalid challenge is not accepted
		try {
			response = SecurityService.signChallenge(appContext.packageName, "TestSecurityServiceJava", ByteArray(0))
			fail("Invalid challenge was signed, returned a response of size " + response.size)
		} catch (e: SecurityException) {
			assert(e.message!!.contains("Error while calling native function signChallenge"))
		}
	}
}