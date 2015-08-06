package com.umich.gridwatch.Intro.Pages;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.tech.freak.wizardpager.ui.PageFragmentCallbacks;
import com.umich.gridwatch.R;

/**
 * Created by nklugman on 5/26/15.
 */
public class OptionalAccountFragment extends Fragment {
    private static final String ARG_KEY = "address";

    private PageFragmentCallbacks mCallbacks;
    private String mKey;
    private OptionalAccountPage mPage;
    private TextView mNameView;
    private TextView mAddressView;
    private TextView mAgeView;
    private TextView mEstimateNumView;
    private TextView mEstimateLengthView;
    private TextView mUtilityNameView;

    public static OptionalAccountFragment create(String key) {
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);

        OptionalAccountFragment fragment = new OptionalAccountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public OptionalAccountFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mKey = args.getString(ARG_KEY);
        mPage = (OptionalAccountPage) mCallbacks.onGetPage(mKey);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page_optional_account, container, false);
        ((TextView) rootView.findViewById(android.R.id.title)).setText(mPage.getTitle());

        mNameView = ((TextView) rootView.findViewById(R.id.your_real_name));
        mNameView.setText(mPage.getData().getString(OptionalAccountPage.NAME_KEY));

        mAddressView = ((TextView) rootView.findViewById(R.id.your_address));
        mAddressView.setText(mPage.getData().getString(OptionalAccountPage.ADDRESS_KEY));

        mAgeView = ((TextView) rootView.findViewById(R.id.your_age));
        mAgeView.setText(mPage.getData().getString(OptionalAccountPage.AGE_KEY));

        mEstimateNumView = ((TextView) rootView.findViewById(R.id.your_number_estimated_outages));
        mEstimateNumView.setText(mPage.getData().getString(OptionalAccountPage.ESTIMATE_NUM));

        mEstimateLengthView = ((TextView) rootView.findViewById(R.id.your_estimated_outage_length));
        mEstimateLengthView.setText(mPage.getData().getString(OptionalAccountPage.ESTIMATE_LENGTH));

        mUtilityNameView = ((TextView) rootView.findViewById(R.id.your_utility_name));
        mUtilityNameView.setText(mPage.getData().getString(OptionalAccountPage.UTILITY_NAME));


        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof PageFragmentCallbacks)) {
            throw new ClassCastException("Activity must implement PageFragmentCallbacks");
        }

        mCallbacks = (PageFragmentCallbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAddressView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPage.getData().putString(OptionalAccountPage.ADDRESS_KEY,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mAgeView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPage.getData().putString(OptionalAccountPage.AGE_KEY,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mEstimateLengthView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPage.getData().putString(OptionalAccountPage.ESTIMATE_LENGTH,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mEstimateNumView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPage.getData().putString(OptionalAccountPage.ESTIMATE_NUM,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPage.getData().putString(OptionalAccountPage.NAME_KEY,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mUtilityNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPage.getData().putString(OptionalAccountPage.UTILITY_NAME,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });


    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        // In a future update to the support library, this should override setUserVisibleHint
        // instead of setMenuVisibility.
        if (mUtilityNameView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mNameView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mEstimateNumView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mEstimateLengthView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mAddressView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mAgeView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }

    }
}