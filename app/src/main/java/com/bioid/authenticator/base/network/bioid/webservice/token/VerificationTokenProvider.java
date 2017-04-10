package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
import com.bioid.authenticator.base.network.bioid.webservice.DeviceNotRegisteredException;

/**
 * Provider for BWS verification tokens.
 * <p>
 * Implementations are allowed to perform any long running operations without offloading work to a separate thread.
 */
public interface VerificationTokenProvider extends Parcelable {

    /**
     * Requests a new verification token.
     * The token can be used to perform a user verification with the BioID Webservice (BWS).
     *
     * @param ctx the Android application context
     * @throws DeviceNotRegisteredException if the device is not registered anymore
     * @throws NoConnectionException        if no connection could be established
     * @throws ServerErrorException         if the server failed to process the request
     * @throws TechnicalException           if any other technical error occurred
     */
    VerificationToken requestVerificationToken(@NonNull Context ctx);
}
