package com.example.upgradeexampleapp;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.upgradeexampleapp.databinding.FragmentFirstBinding;

import java.io.File;

public class FirstFragment extends Fragment {

    public static final String TAG = "DOWNLOADER";
    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);

        try {
            Context ctx = requireContext();
            PackageInfo pInfo = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            String version = pInfo.versionName;
            appendToTextView(String.format("App version: %s", version));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        requireContext().registerReceiver(
                attachmentDownloadCompleteReceive,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        return binding.getRoot();
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(attachmentDownloadCompleteReceive);
    }

    public void appendToTextView(String text) {
        CharSequence currentText = binding.textviewFirst.getText();
        binding.textviewFirst.setText(String.format("%s\n%s", currentText, text));
    }

    BroadcastReceiver attachmentDownloadCompleteReceive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                appendToTextView(String.format("Download complete. ID is %d", downloadId));
                openDownloadedAttachment(context, downloadId);
            }
        }
    };


    private void openDownloadedAttachment(final Context context, final long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);
        if (cursor.moveToFirst()) {
            appendToTextView("Processing download " + downloadId);
            Log.i(TAG, "Hello from the log");
            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                String filePath = Uri.parse(downloadLocalUri).getPath();
                Log.i("DOWNLOADER", String.format("File local uri %s", downloadLocalUri));
                File file = new File(filePath);
                Log.i("DOWNLOADER", String.format("File details: %s", file));
                Log.i("DOWNLOADER", String.format("File exists? %s", file.exists()));
                String authority = BuildConfig.APPLICATION_ID + ".provider";
                Uri apkUri = FileProvider.getUriForFile(requireContext(), authority, file);
                Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                        .setData(apkUri)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Log.i(TAG, "Starting activity to install apk...");
                startActivity(intent);
            } else {
                Log.i(TAG, "Download didn't work. Doing nothing.");
            }
        }
        cursor.close();
    }


    public void downloadApk(String url, String title) {
        Uri Download_Uri = Uri.parse(url);
        DownloadManager.Request request = new DownloadManager.Request(Download_Uri);

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(false);
        request.setTitle("Update downloading...");
        request.setDescription("Downloading CHT Android app update.");
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "hacky-test.apk");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION);

        DownloadManager downloadManager = (DownloadManager) requireActivity().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Button clicked.");
                downloadApk("https://10-10-1-129.my.local-ip.co/app-debug.apk", "hacky-test");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}