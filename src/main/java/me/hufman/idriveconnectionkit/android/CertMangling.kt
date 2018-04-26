package me.hufman.idriveconnectionkit.android

import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x509.CertificateList
import org.bouncycastle.cert.X509CRLHolder
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.openssl.PEMReader
import org.bouncycastle.openssl.PEMWriter
import org.bouncycastle.util.CollectionStore
import org.bouncycastle.util.Selector
import org.bouncycastle.util.io.pem.PemObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.*

/**
 * Helper functions to rearrange P7B files
 */
object CertMangling {

	/**
	 * BouncyCastle's Store object only provides access to a cert by iterating with this Selector
	 * So this simple Selector emits all certs from the Store
	 */
	class SelectAll: Selector {
		override fun clone(): Any {
			return this
		}
		override fun match(obj: Any): Boolean {
			return true
		}
	}

	/**
	 * Loads the given p7b PEM file into a CMSSignedData object, or null if it failed to parse
	 */
	fun loadCertCms(p7b: ByteArray): CMSSignedData? {
		try {
			val pem = PEMReader(InputStreamReader(ByteArrayInputStream(p7b))).readPemObject()
			return CMSSignedData(pem.content)
		} catch (e: Exception) {
			return null
		}
	}

	/**
	 * Loads a list of all the certs in this p7b PEM file
	 */
	fun loadCerts(p7b: ByteArray): Collection<X509CertificateHolder>? {
		val cms = loadCertCms(p7b) ?: return null

		return cms.certificates.getMatches(SelectAll()).filterIsInstance<X509CertificateHolder>()
	}

	/**
	 * Given an x509 cert, return only the CN part of the subject name
	 */
	fun getCN(cert: X509CertificateHolder?): String? {
		val cnId = ASN1ObjectIdentifier("2.5.4.3")
		if (cert == null) return null
		return cert.subject.rdNs.firstOrNull { it.first.type == cnId }?.first?.value?.toString()
	}

	/**
	 * Outputs the given CMSSignedData as a p7b PEM string
	 */
	fun outputCert(cert: CMSSignedData): ByteArray {
		val pem = ByteArrayOutputStream()
		val pemwriter = PEMWriter(OutputStreamWriter(pem))
		pemwriter.writeObject(PemObject("PKCS7", cert.encoded))
		pemwriter.close()
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
		val combinedCerts = CollectionStore(filteredCerts + bmwTouchCert)

		// reconstruct a new p7b file
		val oldCertCMS = loadCertCms(appCert) ?: return ByteArray(0)
		val crls = CollectionStore(oldCertCMS.crLs.getMatches(SelectAll()).mapNotNull {
			when (it) {
				is X509CRLHolder -> it
				is CertificateList -> X509CRLHolder(it)
				else -> null
			}
		})
		val newCertCMS = CMSSignedData.replaceCertificatesAndCRLs(oldCertCMS, combinedCerts, oldCertCMS.attributeCertificates, crls)
		return outputCert(newCertCMS)
	}

}
