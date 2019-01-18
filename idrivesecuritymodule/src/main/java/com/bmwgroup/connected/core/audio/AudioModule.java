package com.bmwgroup.connected.core.audio;

import com.bmwgroup.connected.core.security.NativeRuntimeException;

public class AudioModule {
    static {
        System.loadLibrary("audio");
    }

    /**
     * Initialize based on brand name
     * @param brandName - should be bmw or mini
     */
    public static native void a(String brandName);

    /**
     * Deinitialize
     */
    public static native void b();

    /**
     * Prepare the security context, returning a handle
     * @param packageName
     * @param applicationName
     * @return
     * @throws NativeRuntimeException
     */
    public static native int c(String packageName, String applicationName) throws NativeRuntimeException;

    /**
     * Release the context
     * @param handle
     * @throws NativeRuntimeException
     */
    public static native void d(int handle) throws NativeRuntimeException;

    /**
     * Load a custom cert into the context
     * @param handle
     * @param appCert
     * @throws NativeRuntimeException
     */
    public static native void e(int handle, byte[] appCert) throws NativeRuntimeException;

    /**
     * Retrieve the cert from the context
     * @param handle
     * @return
     * @throws NativeRuntimeException
     */
    public static native byte[] f(int handle) throws NativeRuntimeException;

    /**
     * Signs the challenge from the car
     * @param handle
     * @param challenge
     * @return
     * @throws NativeRuntimeException
     */
    public static native byte[] g(int handle, byte[] challenge) throws NativeRuntimeException;
}
