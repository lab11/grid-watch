package com.umich.gridwatch.Tutorial.Pages;

import android.support.v4.app.Fragment;

import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.ReviewItem;

import java.util.ArrayList;

/**
 * Created by nklugman on 5/26/15.
 */
public class OptionalAccountPage extends Page {
    public static final String NAME_KEY = "real_name";
    public static final String ADDRESS_KEY = "address";
    public static final String AGE_KEY = "age";
    public static final String ESTIMATE_NUM = "estimate_num";
    public static final String ESTIMATE_LENGTH = "estimate_length";
    public static final String UTILITY_NAME = "utility_name";

    public OptionalAccountPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return OptionalAccountFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
        dest.add(new ReviewItem("Your name", mData.getString(NAME_KEY), getKey(), -1));
        dest.add(new ReviewItem("Your address", mData.getString(ADDRESS_KEY), getKey(), -1));
        dest.add(new ReviewItem("Your age", mData.getString(AGE_KEY), getKey(), -1));
        dest.add(new ReviewItem("Your estimated number of outages in a week", mData.getString(ESTIMATE_NUM), getKey(), -1));
        dest.add(new ReviewItem("Your estimated average length of outages in hours", mData.getString(ESTIMATE_LENGTH), getKey(), -1));
        dest.add(new ReviewItem("Your utility name", mData.getString(UTILITY_NAME), getKey(), -1));
    }

    @Override
    public boolean isCompleted() {
        return true;
    }
}