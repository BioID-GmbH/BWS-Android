package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.bioid.authenticator.base.network.NoConnectionException;
import com.bioid.authenticator.base.network.ServerErrorException;
import com.bioid.authenticator.base.network.TechnicalException;
import com.bioid.authenticator.base.network.bioid.webservice.WrongCredentialsException;

/**
 * Provider for BWS enrollment tokens.
 * <p>
 * Implementations are allowed to perform any long running operations without offloading work to a separate thread.
 */
public interface EnrollmentTokenProvider extends Parcelable {

    /**
     * Requests a new enrollment token.
     * The token can be used to perform a user enrollment with the BioID Webservice (BWS).
     *
     * @param ctx the Android application context
     * @throws WrongCredentialsException if the supplied user credentials are invalid
     * @throws NoConnectionException     if no connection could be established
     * @throws ServerErrorException      if the server failed to process the request
     * @throws TechnicalException        if any other technical error occurred
     */
    EnrollmentToken requestEnrollmentToken(@NonNull Context ctx);
}
