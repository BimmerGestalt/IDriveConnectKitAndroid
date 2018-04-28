package me.hufman.idriveconnectionkit.android

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal
import android.util.Base64
import java.util.*


/**
 * Helper functions to rearrange P7B files
 */
object CertMangling {

	/**
	 * Loads a list of all the certs in this p7b PEM file
	 */
	fun loadCerts(p7b: ByteArray): Collection<X509Certificate>? {
		val cf = CertificateFactory.getInstance("X.509")
		return cf.generateCertificates(ByteArrayInputStream(p7b)).filterIsInstance<X509Certificate>()
	}

	/**
	 * Helpfully parses out the X500 name components from a Principal
	 */
	fun getDNPieces(principal: X500Principal): List<String> {
		return principal.name.split(Regex("(?<!\\\\),"))
	}

	/**
	 * Converts an array of X500 rDNs to a useful map
	 */
	fun parseDNPieces(rdns: List<String>): Map<String, String> {
		return rdns.associate {rdn ->
			Pair(rdn.substringBefore('=').trim(), rdn.substringAfter('=').trim())
		}
	}
	/**
	 * Given an x509 cert, return only the CN part of the subject name
	 */
	fun getCN(cert: X509Certificate?): String? {
		if (cert == null) return null
		return parseDNPieces(getDNPieces(cert.subjectX500Principal))["CN"]
	}

	/**
	 * Outputs the given list of certs as a p7b PEM string
	 */
	fun outputCert(certs: List<X509Certificate>): ByteArray {
		val cf = CertificateFactory.getInstance("X.509")
		val pem = ByteArrayOutputStream()
		val certPath = cf.generateCertPath(certs)
		for (encoding in certPath.getEncodings()) {
			System.out.println("Found encoding: " + encoding);
		}
		pem.write("-----BEGIN PKCS7-----\n".toByteArray())
		pem.write(Base64.encode(certPath.getEncoded("PKCS7"), Base64.DEFAULT or Base64.CRLF))
		pem.write("-----END PKCS7-----\n".toByteArray())
		pem.close()
		return pem.toByteArray()
	}

	/**
	 * Given a p7b PEM cert obtained from a CarAPI app, and also
	 * given the BMW p7b PEM cert loaded from a SecurityService
	 * combine them into a new p7b PEM cert that is acceptable by the car
	 */
	fun mergeBMWCert(appCert: ByteArray, bmwCert: ByteArray): ByteArray {
		val appCerts = loadCerts(appCert)
		val bmwCerts = loadCerts(bmwCert)
		val filteredCerts = appCerts?.filter {
			getCN(it) != "a4a_root-ca"
		} ?: LinkedList()
		val bmwTouchCert = bmwCerts?.filter {
			getCN(it)?.startsWith("a4a_app_BMWTouchCommand_Connection") ?: false
		} ?: LinkedList()
		val combinedCerts = filteredCerts + bmwTouchCert

		return outputCert(combinedCerts)
	}

}
