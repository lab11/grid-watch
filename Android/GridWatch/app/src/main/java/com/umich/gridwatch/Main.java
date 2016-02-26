package com.umich.gridwatch;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.support.multidex.MultiDex;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.umich.gridwatch.Chat.helper.ChatPreferenceManager;
import com.umich.gridwatch.Utils.Private;

import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

/**
 * Created by nklugman on 5/29/15.
 */
@ReportsCrashes(
        formUri = Private.acra_form_uri,
        reportType = HttpSender.Type.JSON,
        httpMethod = HttpSender.Method.POST,
        formUriBasicAuthLogin = Private.acra_login_name,
        formUriBasicAuthPassword = Private.acra_login_key,
        formKey = "", // This is required for backward compatibility but not used
        mode = ReportingInteractionMode.DIALOG,
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast, // optional. displays a Toast message when the user accepts to send a report.
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PACKAGE_NAME,
                ReportField.REPORT_ID,
                ReportField.BUILD,
                ReportField.STACK_TRACE,
                ReportField.USER_COMMENT,
                ReportField.CUSTOM_DATA,
                ReportField.LOGCAT
        },
        logcatArguments = { "-t", "100", "-v", "long", "ActivityManager:I", "GridWatch:D", "*:S" }
)


public class Main extends Application {

    private final static String onCreateTag = "Main:onCreate";
    private final static String mainTag = "Main";


    private RequestQueue mRequestQueue;
    private static Main mInstance;
    private ChatPreferenceManager pref;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(onCreateTag, "STARTING GRIDWATCH");
        //if (!SensorConfig.debug) {
        //    ACRA.init(this); //TODO disabled for testing... enable before release. This captures all crashes to an cloudant instance and launches a UI allowing for the user to add comments.
        //}
        mInstance = this;

    }

    /*
    Overriding this method is key for the FFT library. MultiDex allows for more libraries to be included. Unfortunatly, this seems to slow down the gradle build fairly significantly.
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized Main getInstance() {
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public ChatPreferenceManager getPrefManager() {
        if (pref == null) {
            pref = new ChatPreferenceManager(this);
        }

        return pref;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        req.setTag(TextUtils.isEmpty(tag) ? mainTag : tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(mainTag);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }

    public void logout() {
        pref.logout();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
