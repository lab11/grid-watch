package com.umich.gridwatch.Tutorial.Pages;

import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.ReviewItem;

import java.util.ArrayList;

/**
 * Created by nklugman on 5/26/15.
 */
public class BetaPage extends Page {
    public static final String BETA_NAME_KEY = "beta_name";
    public static final String BETA_PASSWORD_KEY = "key";


    public BetaPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return BetaFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
        dest.add(new ReviewItem("Beta name", mData.getString(BETA_NAME_KEY), getKey(), -1));
        dest.add(new ReviewItem("Beta password", mData.getString(BETA_PASSWORD_KEY), getKey(), -1));
    }

    @Override
    public boolean isCompleted() {
        return !TextUtils.isEmpty(mData.getString(BETA_NAME_KEY)) &&
                !TextUtils.isEmpty(mData.getString(BETA_PASSWORD_KEY));
    }
}