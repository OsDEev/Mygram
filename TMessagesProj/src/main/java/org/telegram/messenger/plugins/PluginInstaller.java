package org.telegram.messenger.plugins;

import android.app.Activity;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

import java.io.File;

public class PluginInstaller {

    public static void requestInstall(MessageObject message, BaseFragment fragment, Activity activity) {
        if (message == null || activity == null || fragment == null || message.getDocument() == null) {
            return;
        }
        final TLRPC.Document document = message.getDocument();
        if (document == null || !message.getDocumentName().toLowerCase().endsWith(".myp")) {
            return;
        }
        final int account = message.currentAccount;
        final String attachName = FileLoader.getAttachFileName(document);

        File file = resolveFile(message, account);
        if (file != null && file.exists()) {
            showConfirm(fragment, activity, file);
            return;
        }

        final NotificationCenter notificationCenter = NotificationCenter.getInstance(account);
        final boolean[] done = {false};
        final NotificationCenter.NotificationCenterDelegate observer = new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int a, Object... args) {
                if (done[0] || args == null || args.length == 0) {
                    return;
                }
                if (!attachName.equals(args[0])) {
                    return;
                }
                if (id != NotificationCenter.fileLoaded && id != NotificationCenter.fileLoadFailed) {
                    return;
                }
                done[0] = true;
                notificationCenter.removeObserver(this, NotificationCenter.fileLoaded);
                notificationCenter.removeObserver(this, NotificationCenter.fileLoadFailed);
                AndroidUtilities.runOnUIThread(() -> {
                    if (activity == null || fragment == null) {
                        return;
                    }
                    File cached = resolveFile(message, account);
                    if (cached != null && cached.exists()) {
                        showConfirm(fragment, activity, cached);
                    } else {
                        toast(activity, R.string.MYgramPluginInstallFailed);
                    }
                });
            }
        };
        notificationCenter.addObserver(observer, NotificationCenter.fileLoaded);
        notificationCenter.addObserver(observer, NotificationCenter.fileLoadFailed);
        FileLoader.getInstance(account).loadFile(document, message, FileLoader.PRIORITY_HIGH, 0);
        toast(activity, R.string.MYgramPluginDownloading);
    }

    private static File resolveFile(MessageObject message, int account) {
        File file = null;
        if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
            file = new File(message.messageOwner.attachPath);
        }
        if (file == null || !file.exists()) {
            file = FileLoader.getInstance(account).getPathToMessage(message.messageOwner);
        }
        return file;
    }

    private static void showConfirm(BaseFragment fragment, Activity activity, File file) {
        if (activity == null || fragment == null) {
            return;
        }
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(LocaleController.getString(R.string.MYgramPlugins));
            builder.setMessage(LocaleController.formatString(R.string.MYgramInstallPluginPrompt, file.getName()));
            builder.setPositiveButton(LocaleController.getString(R.string.MYgramInstallYes), (dialog, which) -> install(fragment, activity, file));
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            fragment.showDialog(builder.create());
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static void install(BaseFragment fragment, Activity activity, File file) {
        PluginRuntime.EXTRACT_EXECUTOR.submit(() -> {
            boolean ok;
            try {
                ok = PluginManager.getInstance().installPlugin(activity.getApplicationContext(), file);
            } catch (Exception e) {
                FileLog.e(e);
                ok = false;
            }
            final boolean success = ok;
            AndroidUtilities.runOnUIThread(() ->
                    toast(activity, success ? R.string.MYgramPluginInstalled : R.string.MYgramPluginInstallFailed));
        });
    }

    private static void toast(Activity activity, int resId) {
        try {
            android.widget.Toast.makeText(activity, LocaleController.getString(resId), android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}