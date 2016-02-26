package com.umich.gridwatch.Tutorial.Pages;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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
public class NewAccountFragment extends Fragment {
    private static final String ARG_KEY = "key";

    private PageFragmentCallbacks mCallbacks;
    private String mKey;
    private NewAccountPage mPage;
    private TextView mUserNameView;
    private TextView mPasswordView;
    private TextView mPasswordConfirmView;

    private TextView mEmailView;

    private TextView mEmailReportView;
    private TextView mUsernameReportView;
    private TextView mPasswordReportView;

    private static final int USERNAME_TAKEN = -1;
    private static final int USERNAME_NOT_VALID = 0;
    private static final int USERNAME_ALL_GOOD = 1;
    private static final int USERNAME_MIN_LENGTH = 3;

    private static final int EMAIL_TAKEN = -1; //TODO add this query in
    private static final int EMAIL_NOT_VALID = 0;
    private static final int EMAIL_ALL_GOOD = 1;

    public static NewAccountFragment create(String key) {
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);

        NewAccountFragment fragment = new NewAccountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public NewAccountFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mKey = args.getString(ARG_KEY);
        mPage = (NewAccountPage) mCallbacks.onGetPage(mKey);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page_new_account, container, false);
        ((TextView) rootView.findViewById(android.R.id.title)).setText(mPage.getTitle());

        mUserNameView = ((TextView) rootView.findViewById(R.id.your_username));
        mUserNameView.setText(mPage.getData().getString(NewAccountPage.USERNAME_KEY));

        mEmailView = ((TextView) rootView.findViewById(R.id.your_email));
        mEmailView.setText(mPage.getData().getString(NewAccountPage.EMAIL_TEXT_KEY));

        mPasswordView = ((TextView) rootView.findViewById(R.id.your_password));
        mPasswordView.setText(mPage.getData().getString(NewAccountPage.PASSWORD_KEY));

        mPasswordConfirmView = ((TextView) rootView.findViewById(R.id.your_password_confirm));
        mPasswordConfirmView.setText(mPage.getData().getString(NewAccountPage.PASSWORD_CONFIRM_KEY));

        mEmailReportView = ((TextView) rootView.findViewById(R.id.new_account_email));
        mEmailReportView.setTextColor(Color.RED);
        mEmailReportView.setText("Email Address Not Valid");

        mPasswordReportView = ((TextView) rootView.findViewById(R.id.new_account_password));
        mPasswordReportView.setTextColor(Color.RED);
        mPasswordReportView.setText("Passwords Do Not Match");

        mUsernameReportView = ((TextView) rootView.findViewById(R.id.new_account_username));
        mUsernameReportView.setTextColor(Color.RED);
        mUsernameReportView.setText("Username Not Valid");


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

        mUserNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int valid = isValidUsername(mUsernameReportView.getText().toString());
                mUserNameView.setTextColor(Color.RED);
                mUsernameReportView.setTextColor(Color.RED);
                if (valid == USERNAME_TAKEN) {
                    mUsernameReportView.setText("Username Taken");
                    mPage.getData().putBoolean(NewAccountPage.USERNAME_COMPLETE_KEY, false);
                } else if (valid == USERNAME_NOT_VALID) {
                    mUsernameReportView.setText("Username Not Valid");
                    mPage.getData().putBoolean(NewAccountPage.USERNAME_COMPLETE_KEY, false);
                } else {
                    mUserNameView.setTextColor(Color.GREEN);
                    mUsernameReportView.setTextColor(Color.GREEN);
                    mUsernameReportView.setText("Username Valid");
                    mPage.getData().putBoolean(NewAccountPage.USERNAME_COMPLETE_KEY, true);
                }

                mPage.getData().putString(NewAccountPage.USERNAME_KEY,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mEmailView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!isValidEmail(mEmailView.getText().toString())) {
                    mEmailView.setTextColor(Color.RED);
                    mEmailReportView.setTextColor(Color.RED);
                    mEmailReportView.setText("Email Address Not Valid");
                    mPage.getData().putBoolean(NewAccountPage.EMAIL_COMPLETE_KEY, false);

                } else {
                    mEmailView.setTextColor(Color.GREEN);
                    mEmailReportView.setTextColor(Color.GREEN);
                    mEmailReportView.setText("Email Address Valid");
                    mPage.getData().putBoolean(NewAccountPage.EMAIL_COMPLETE_KEY, true);
                }
                mPage.getData().putString(NewAccountPage.EMAIL_TEXT_KEY,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mPasswordView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mPage.getData().putString(NewAccountPage.PASSWORD_KEY,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });

        mPasswordConfirmView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1,
                                          int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (!mPasswordConfirmView.getText().toString().equals(mPasswordView.getText().toString())) {
                    mPasswordReportView.setTextColor(Color.RED);
                    mPasswordReportView.setText("Passwords Do Not Match");
                    mPage.getData().putBoolean(NewAccountPage.PASSWORD_COMPLETE_KEY, false);

                } else {
                    mPasswordReportView.setTextColor(Color.GREEN);
                    mPasswordReportView.setText("Passwords Match!");
                    mPage.getData().putBoolean(NewAccountPage.PASSWORD_COMPLETE_KEY, true);
                }
                mPage.getData().putString(NewAccountPage.PASSWORD_CONFIRM_KEY,
                        (editable != null) ? editable.toString() : null);
                mPage.notifyDataChanged();
            }
        });


    }

    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public final static int isValidUsername(CharSequence target) {

        if (target.length() <= USERNAME_MIN_LENGTH) {
            return USERNAME_NOT_VALID;
        }
        if (username_taken(target)) {
            return USERNAME_TAKEN;
        }
        return USERNAME_ALL_GOOD;
    }

    public final static boolean username_taken(CharSequence target) {
        return false;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);

        // In a future update to the support library, this should override setUserVisibleHint
        // instead of setMenuVisibility.
        if (mUserNameView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mPasswordView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mPasswordConfirmView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
        if (mEmailView != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (!menuVisible) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }

}