package ca.sykesdev.sheridango.adapter;

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
import ca.sykesdev.sheridango.interfaces.ClickListener;
import ca.sykesdev.sheridango.model.Property;

public class MyPropertysRecyclerAdapter extends RecyclerView.Adapter<MyPropertysRecyclerAdapter.PropertyViewHolder> {
    private final ClickListener listener;
    private ArrayList<Property> mProperties;

    /**
     * Constructor for adapter
     * @param properties List of properties to place in the list
     */
    public MyPropertysRecyclerAdapter (ArrayList<Property> properties, ClickListener listener) {
        this.mProperties = properties;
        this.listener = listener;
    }

    /**
     * View holder for my property
     */
    public static class PropertyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        // LOGING CONSTANT
        private final String TAG = "PLACES_RECYCLER_ADAPT";

        // Declare controls inside recycler
        private TextView mTxtPropName, mTxtPropOwned, mTxtPropIncome;

        private Button mBtnShowDetails;
        private WeakReference<ClickListener> listenerRef;

        public PropertyViewHolder(@NonNull LinearLayout layProperties, ClickListener listener) {
            super(layProperties);

            // Get all controls init
            mTxtPropName = layProperties.findViewById(R.id.txtMyPropName);
            mTxtPropOwned = layProperties.findViewById(R.id.txtMyPropPercentageOwned);
            mTxtPropIncome = layProperties.findViewById(R.id.txtMyPropIncomeBenefits);
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
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int view) {
        // Create and inflate view for text data
        LinearLayout layProperties = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_property_item_layout_view, parent, false);

        // Create and return the Viewholder for txtPlace using the passed layout
        return new MyPropertysRecyclerAdapter.PropertyViewHolder(layProperties, listener);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder propertyViewHolder, int position) {
        String format = "%s";
        String nFormat = "%.2f";

        // Set the text for each place...
        propertyViewHolder.mTxtPropName.setText(String.format(format,
                mProperties.get(position).getmName()));
        propertyViewHolder.mTxtPropOwned.setText(String.format("Percentage Owned: " + nFormat + "%%",
                mProperties.get(position).getmPercentageOwned() * 100));
        propertyViewHolder.mTxtPropIncome.setText(String.format("Income (per hour): $" + nFormat,
                mProperties.get(position).getmIncomeBenefits()));
    }

    @Override
    public int getItemCount() {
        return mProperties.size();
    }
}
