package model;

import android.content.Context;

import java.util.ArrayList;

import ca.sykesdev.sheridango.MainActivity;

/**
 * Class used to define a property
 * and its needed information and methods
 */
public class Property {
    private String mID, mName, mPhotoID, mType;
    private double mCost, mInvestAmount, mLat, mLong;

    /**
     * Creates a property object used in the rest of the app...
     * @param mID The property ID in the database
     * @param mName The name of the property
     * @param mPhotoID The PhotoID used later to look up a photo using a PhotoRequest
     * @param mType The Type of property used later to define price.
     * @param mCost The Total value of the property
     * @param mInvestAmount The Total invest percentage in the property globally
     * @param mLat The lat coordinate of the property
     * @param mLong The longitude coordinate of the property
     */
    public Property(String mID, String mName, String mPhotoID, String mType, double mCost, double mInvestAmount, double mLat, double mLong) {
        this.mID = mID;
        this.mName = mName;
        this.mPhotoID = mPhotoID;
        this.mType = mType;
        this.mCost = mCost;
        this.mInvestAmount = mInvestAmount;
        this.mLat = mLat;
        this.mLong = mLong;
    }

    public String getmID() {
        return mID;
    }

    public String getmName() {
        return mName;
    }

    public String getmPhotoID() {
        return mPhotoID;
    }

    public String getmType() {
        return mType;
    }

    public double getmCost() {
        return mCost;
    }

    public void setmCost(double mCost) {
        this.mCost = mCost;
    }

    public double getmInvestAmount() {
        return mInvestAmount;
    }

    public void setmInvestAmount(double mInvestAmount) {
        this.mInvestAmount = mInvestAmount;
    }

    public double getmLat() {
        return mLat;
    }

    public double getmLong() {
        return mLong;
    }
}
