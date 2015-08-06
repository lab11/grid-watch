package com.umich.gridwatch.Intro.Pages.Tutorial;

import android.support.v4.app.Fragment;

import com.tech.freak.wizardpager.model.ModelCallbacks;
import com.tech.freak.wizardpager.model.Page;
import com.tech.freak.wizardpager.model.ReviewItem;

import java.util.ArrayList;

/**
 * Created by nklugman on 5/26/15.
 */
public class Page4 extends Page {


    public Page4(ModelCallbacks callbacks, String title) {
        super(callbacks, title);
    }

    @Override
    public Fragment createFragment() {
        return Page4Fragment.create(getKey());
    }

    @Override
    public void getReviewItems(ArrayList<ReviewItem> dest) {
    }

    @Override
    public boolean isCompleted() {
        return true;
    }
}