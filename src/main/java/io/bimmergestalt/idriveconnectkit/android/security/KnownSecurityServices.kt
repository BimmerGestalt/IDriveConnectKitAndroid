package io.bimmergestalt.idriveconnectkit.android.security

// The list of known BMW/Mini apps that we can connect to
// We'll try to connect to any of them that are installed
enum class KnownSecurityServices(val className: String,
                                 val packageName: String = className.substring(0, className.lastIndexOf('.'))) {
	BMWClassicUSA("com.bmwgroup.connected.bmw.usa.SECURITY_SERVICE"),
	MiniClassicUSA("com.bmwgroup.connected.mini.usa.SECURITY_SERVICE"),
	BMWClassic("com.bmwgroup.connected.bmw.SECURITY_SERVICE"),
	MiniClassic("com.bmwgroup.connected.mini.SECURITY_SERVICE"),
	BMWConnectedNA("de.bmw.connected.na.SECURITY_SERVICE"),
	MiniConnectedNA("de.mini.connected.na.SECURITY_SERVICE"),
	BMWConnected("de.bmw.connected.SECURITY_SERVICE"),
	MiniConnected("de.mini.connected.SECURITY_SERVICE"),

	// My BMW has a different pattern
	BMWMine("com.bmwgroup.connected.core.services.security.CarSecurityService", "de.bmw.connected.mobile20.row"),
	MiniMine("com.bmwgroup.connected.core.services.security.CarSecurityService", "de.mini.connected.mobile20.row"),
	J29Mine("com.bmwgroup.connected.core.services.security.CarSecurityService", "de.j29.connected.mobile20.row"),
	BMWMineNA("com.bmwgroup.connected.core.services.security.CarSecurityService", "de.bmw.connected.mobile20.na"),
	MiniMineNA("com.bmwgroup.connected.core.services.security.CarSecurityService", "de.mini.connected.mobile20.na"),
	J29MineNA("com.bmwgroup.connected.core.services.security.CarSecurityService", "de.j29.connected.mobile20.na");

	override fun toString(): String {
		return this.name
	}
}