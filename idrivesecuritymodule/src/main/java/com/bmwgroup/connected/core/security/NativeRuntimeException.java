/*
 * Decompiled with CFR 0_123.
 */
package com.bmwgroup.connected.core.security;

public class NativeRuntimeException
        extends Exception {
    private static final long serialVersionUID = -4225320785804121110L;
    private final int nativeErrorCode;
    private final String nativeErrorMsg;

    public NativeRuntimeException(int n, String string) {
        this.nativeErrorCode = n;
        this.nativeErrorMsg = string;
    }

    @Override
    public String getMessage() {
        return this.nativeErrorMsg;
    }

    public int getNativeErrorCode() {
        return this.nativeErrorCode;
    }
}
