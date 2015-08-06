package com.umich.gridwatch.Intro.Pages;

import android.support.v4.app.Fragment;

import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.ReviewItem;

import java.util.ArrayList;

/**
 * Created by nklugman on 5/26/15.
 */

//  -> username
//  -> password

//	-> name
//  -> address
//  -> age
//  -> estimated number of outages in a week
//  -> utility name


public class NewAccountPage extends Page {
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "key";
    public static final String PASSWORD_CONFIRM_KEY = "confirm";
    public static final String EMAIL_TEXT_KEY = "email";
    public static final String EMAIL_COMPLETE_KEY = "email_complete";
    public static final String USERNAME_COMPLETE_KEY = "username_complete";
    public static final String PASSWORD_COMPLETE_KEY = "password_complete";



    public NewAccountPage(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return NewAccountFragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
        dest.add(new ReviewItem("Desired username", mData.getString(USERNAME_KEY), getKey(), -1));
        dest.add(new ReviewItem("Your password", mData.getString(PASSWORD_KEY), getKey(), -1));
        dest.add(new ReviewItem("Your email", mData.getString(EMAIL_TEXT_KEY), getKey(), -1));

    }

    @Override
    public boolean isCompleted() {
        if (!mData.getBoolean(EMAIL_COMPLETE_KEY))
            return false;
        if (!mData.getBoolean(USERNAME_COMPLETE_KEY))
            return false;
        if (!mData.getBoolean(PASSWORD_COMPLETE_KEY))
            return false;
        return true;
    }






}