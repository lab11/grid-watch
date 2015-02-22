package com.umich.gridwatch;

import android.app.Application;
import android.util.Log;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

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
    @Override
    public void onCreate() {
        super.onCreate();
        Log.w("STARTING", "STARTING GRIDWATCH");
        ACRA.init(this);
    }
}
