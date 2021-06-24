package me.hufman.idriveconnectionkit.android

/**
 * The current connection status is available as static members of this object
 * isConnected indicates whether the car is currently connected
 * brand is which type of car is connected: bmw bmwi mini
 * host is what host to use to reach the Etch service, usually 127.0.0.1
 * port is which port on that host to reach the Etch service
 * instanceId is an internal identifier
 *
 * Any listeners are notified when the connection status changes
 *
 * This notification happens on whichever thread modified the connection status
 * And so some listeners may need to post to a handler to update their UI
 */
interface IDriveConnectionStatus {
	companion object {
		var isConnected: Boolean = false
			private set
		var brand: String? = null
			private set
		var host: String? = null
			private set
		var port: Int? = null
			private set
		var instanceId: Int? = null
			private set

		fun reset() {
			isConnected = false
			instanceId = -1
			IDriveConnectionListener.onUpdate()
		}

		fun setConnection(brand: String, host: String, port: Int, instanceId: Int? = null) {
			isConnected = true
			this.brand = brand
			this.host = host
			this.port = port
			this.instanceId = instanceId
			IDriveConnectionListener.onUpdate()
		}
	}

	// accessor methods to allow an object to be passed around
	val isConnected: Boolean
		get() = IDriveConnectionStatus.isConnected
	val brand: String?
		get() = IDriveConnectionStatus.brand
	val host: String?
		get() = IDriveConnectionStatus.host
	val port: Int?
		get() = IDriveConnectionStatus.port
	val instanceId: Int?
		get() = IDriveConnectionStatus.instanceId
}