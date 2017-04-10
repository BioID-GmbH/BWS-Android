package com.bioid.authenticator.base.network.bioid.webservice.token;

import android.content.Context;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.bioid.authenticator.base.network.bioid.webservice.BioIdWebserviceClientExtended;

/**
 * Can be used to request BWS tokens from the BWS API.
 */
public class BwsTokenProvider implements VerificationTokenProvider, EnrollmentTokenProvider {

    private final String bcid;

    /**
     * Creates a new BwsTokenProvider.
     *
     * @param bcid Biometric Class ID (BCID) of the user for whom the token shall be issued.
     */
    @SuppressWarnings("SameParameterValue")
    public BwsTokenProvider(String bcid) {
        this.bcid = bcid;
    }

    @Override
    public VerificationToken requestVerificationToken(@NonNull Context ctx) {
        return createBwsClient(ctx).requestVerificationToken(bcid);
    }

    @Override
    public EnrollmentToken requestEnrollmentToken(@NonNull Context ctx) {
        return createBwsClient(ctx).requestEnrollmentToken(bcid);
    }

    @VisibleForTesting
    BioIdWebserviceClientExtended createBwsClient(@NonNull Context ctx) {
        return new BioIdWebserviceClientExtended();
    }

    //region Parcelable implementation
    public static final Creator<BwsTokenProvider> CREATOR = new Creator<BwsTokenProvider>() {
        @Override
        public BwsTokenProvider createFromParcel(Parcel in) {
            return new BwsTokenProvider(in);
        }

        @Override
        public BwsTokenProvider[] newArray(int size) {
            return new BwsTokenProvider[size];
        }
    };

    private BwsTokenProvider(Parcel in) {
        bcid = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bcid);
    }

    @Override
    public int describeContents() {
        return 0;
    }
    //endregion
}
