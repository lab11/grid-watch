package com.umich.gridwatch.Tutorial.Pages;


import android.support.v4.app.Fragment;
import android.text.TextUtils;

import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.ReviewItem;

import java.util.ArrayList;


/**
 * A page asking for a name and an email.
 */
public class LoginPage extends Page {
    public static final String NAME_DATA_KEY = "name";
    public static final String PASSWORD_DATA_KEY = "key";


    public LoginPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return LoginInfoFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
        dest.add(new ReviewItem("Your name", mData.getString(NAME_DATA_KEY), getKey(), -1));
        dest.add(new ReviewItem("Your password", mData.getString(PASSWORD_DATA_KEY), getKey(), -1));
    }

    @Override
    public boolean isCompleted() {
        return !TextUtils.isEmpty(mData.getString(NAME_DATA_KEY)) &&
                !TextUtils.isEmpty(mData.getString(PASSWORD_DATA_KEY));
    }
}