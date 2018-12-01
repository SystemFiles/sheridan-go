package ca.sykesdev.sheridango.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class used to define a property
 * and its needed information and methods
 */
public class Property implements Parcelable {
    private String mID, mName, mPhotoID, mType;
    private double mCost, mInvestAmount, mLat, mLong;
    private double mPercentageOwned, mIncomeBenefits;

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

    /**
     * Constructor used for personal property listing and handling. We are using only information
     * that is needed for selling and investing further into existing properties.
     * @param mID The property ID
     * @param mName The name of the property
     * @param mPhotoID The photo ID used to retrieve photo from Google
     * @param mCost The cost of the property in total (for 100%)
     * @param mPercentageOwned The percentage owned by this user
     * @param mIncomeBenefits The income benefits received by this user.
     */
    public Property(String mID, String mName, String mPhotoID, double mCost, double mPercentageOwned, double mIncomeBenefits) {
        this.mID = mID;
        this.mName = mName;
        this.mPhotoID = mPhotoID;
        this.mCost = mCost;
        this.mPercentageOwned = mPercentageOwned;
        this.mIncomeBenefits = mIncomeBenefits;
    }

    protected Property(Parcel in) {
        mID = in.readString();
        mName = in.readString();
        mPhotoID = in.readString();
        mType = in.readString();
        mCost = in.readDouble();
        mInvestAmount = in.readDouble();
        mLat = in.readDouble();
        mLong = in.readDouble();
        mPercentageOwned = in.readDouble();
        mIncomeBenefits = in.readDouble();
    }

    public static final Creator<Property> CREATOR = new Creator<Property>() {
        @Override
        public Property createFromParcel(Parcel in) {
            return new Property(in);
        }

        @Override
        public Property[] newArray(int size) {
            return new Property[size];
        }
    };

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

    public double getmPercentageOwned() {
        return mPercentageOwned;
    }

    public double getmIncomeBenefits() {
        return mIncomeBenefits;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mID);
        dest.writeString(mName);
        dest.writeString(mPhotoID);
        dest.writeString(mType);
        dest.writeDouble(mCost);
        dest.writeDouble(mInvestAmount);
        dest.writeDouble(mLat);
        dest.writeDouble(mLong);
        dest.writeDouble(mPercentageOwned);
        dest.writeDouble(mIncomeBenefits);
    }
}
