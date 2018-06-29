package me.hufman.idriveconnectionkit.android

import android.content.Context
import java.io.InputStream

interface CarAppResources {
	/**
	 * Gets the app-specific cert to sign into the car with SAS
	 */
	fun getAppCertificate(): InputStream?

	/**
	 * Gets the UI Description layout file
	 * May be missing, if the app uses the default CarAPI layouts
	 */
	fun getUiDescription(): InputStream?

	/**
	 * Get the images.zip db from the app
	 * @param brand should be {bmw,mini,common}
	 */
	fun getImagesDB(brand: String): InputStream?

	/**
	 * Get the texts.zip db from the app
	 * @param brand should be {bmw,mini,common}
	 */
	fun getTextsDB(brand: String): InputStream?
}