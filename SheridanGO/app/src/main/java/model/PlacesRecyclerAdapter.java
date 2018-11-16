package model;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import ca.sykesdev.sheridango.R;

/**
 * Places Recycler Adapter used for Recycler view on places...
 */
public class PlacesRecyclerAdapter extends RecyclerView.Adapter<PlacesRecyclerAdapter.PlacesViewHolder> {
    private final ClickListener listener;
    private ArrayList<Property> mProperties;

    /**
     * Constructor for adapter
     * @param places The list of places
     */
    public PlacesRecyclerAdapter (ArrayList<Property> places, ClickListener listener) {
        this.listener = listener;
        mProperties = places;
    }

    public static class PlacesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // LOGING CONSTANT
        private final String TAG = "PLACES_RECYCLER_ADAPT";

        // Declare controls inside recycler
        private TextView mTxtPlaceName;
        private TextView mTxtPlaceType;
        private TextView mTxtPlaceValue;
        private TextView mTxtPlaceInvestAmount;
        private Button mBtnShowDetails;
        private WeakReference<ClickListener> listenerRef;

        public PlacesViewHolder(@NonNull LinearLayout layProperties, ClickListener listener) {
            super(layProperties);

            // Get all controls init
            mTxtPlaceName = layProperties.findViewById(R.id.txtPropertyName);
            mTxtPlaceType = layProperties.findViewById(R.id.txtPropertyType);
            mTxtPlaceValue = layProperties.findViewById(R.id.txtPropertyValue);
            mTxtPlaceInvestAmount = layProperties.findViewById(R.id.txtPropertyInvestAmount);
            mBtnShowDetails = layProperties.findViewById(R.id.btnShowDetails);

            // Create weak reference to click listener
            listenerRef = new WeakReference<>(listener);
            mBtnShowDetails.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Log.i(TAG, "onClick: Show details clicked by user...");
            listenerRef.get().onPositionClicked(getAdapterPosition());
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
        return new PlacesViewHolder(layProperties, listener);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull PlacesRecyclerAdapter.PlacesViewHolder placeViewHolder, int position) {
        String format = "%s";
        String nFormat = "%.2f";

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
