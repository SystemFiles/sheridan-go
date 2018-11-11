package model;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import ca.sykesdev.sheridango.MainActivity;
import ca.sykesdev.sheridango.R;

/**
 * Places Recycler Adapter used for Recycler view on places...
 */
public class PlacesRecyclerAdapter extends RecyclerView.Adapter<PlacesRecyclerAdapter.PlacesViewHolder> {

    ArrayList<Property> mProperties;

    /**
     * Constructor for adapter
     * @param places The list of places
     */
    public PlacesRecyclerAdapter (ArrayList<Property> places) {
        mProperties = places;
    }

    public static class PlacesViewHolder extends RecyclerView.ViewHolder {

        private TextView mTxtPlaceName;
        private TextView mTxtPlaceType;
        private TextView mTxtPlaceValue;
        private TextView mTxtPlaceInvestAmount;

        public PlacesViewHolder(@NonNull LinearLayout layProperties) {
            super(layProperties);
            mTxtPlaceName = layProperties.findViewById(R.id.txtPropertyName);
            mTxtPlaceType = layProperties.findViewById(R.id.txtPropertyType);
            mTxtPlaceValue = layProperties.findViewById(R.id.txtPropertyValue);
            mTxtPlaceInvestAmount = layProperties.findViewById(R.id.txtPropertyInvestAmount);
        }
    }

    @NonNull
    @Override
    public PlacesRecyclerAdapter.PlacesViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                     int view) {
        // Create and inflate view for text data
        LinearLayout layProperties = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.property_item_layout_view, parent, false);

        // Create and return the Viewholder for txtPlace using the passed layout
        return new PlacesViewHolder(layProperties);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull PlacesRecyclerAdapter.PlacesViewHolder placeViewHolder, int position) {
        String format = "%s";
        String nFormat = "%.2f";

        // TODO: Fix error where values in Recycler view only represent initialized place values for cost and InvestAmt...
        // Set the text for each place...
        placeViewHolder.mTxtPlaceName.setText(String.format(format,
                mProperties.get(position).getmName()));
        placeViewHolder.mTxtPlaceType.setText(String.format("Property Type: " + format,
                mProperties.get(position).getmType()));
        placeViewHolder.mTxtPlaceValue.setText(String.format("Buy Out for: $" + nFormat,
                mProperties.get(position).getmCost()));
        placeViewHolder.mTxtPlaceInvestAmount.setText(String.format("Total amount owned: " + nFormat
                        + "%%",
                (mProperties.get(position).getmInvestAmount()) * 100));
    }

    @Override
    public int getItemCount() {
        return mProperties.size();
    }
}
