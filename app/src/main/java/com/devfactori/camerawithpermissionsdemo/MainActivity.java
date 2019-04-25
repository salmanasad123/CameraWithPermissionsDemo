package com.devfactori.camerawithpermissionsdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MTAG";

    public static final int STORAGE_AND_CAMERA_PERMISSION_REQUEST_CODE = 102;
    public static final int STORAGE_PERMISSION_REQUEST_CODE = 103;
    public static final int REQUEST_IMAGE_CAPTURE = 104;
    public static final String CAMERA_WRITE_EXTERNAL = "Camera And Write External";
    public static final String READ_EXTERNAL = "Read External";


    Button btnOpenCamera;
    private String currentImagePath = null;
    private boolean appSettingsClickedForCamera = false, appSettingsClickedForStorage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        setClickListeners();
    }

    private void initViews() {
        btnOpenCamera = findViewById(R.id.btn_camera);
    }

    private void setClickListeners() {
        btnOpenCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_camera:
                openCamera();
                break;
        }
    }


    private void openCamera() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            openCameraIntent();
        } else {
            requestStorageAndCameraPermissions();
        }
    }

    private void requestStorageAndCameraPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, STORAGE_AND_CAMERA_PERMISSION_REQUEST_CODE);
        }
    }


    private void openCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File imageFile = null;
        try {
            imageFile = createImageFile();
        } catch (IOException e) {
            // Error occurred while creating the File
            Log.i(TAG, "IOException");
            e.printStackTrace();
        }
        // Continue only if the File was successfully created
        if (imageFile != null) {
            //Uri mImageCaptureUri = Uri.fromFile(mFileTemp); // deprecated => app crash above 24
            Uri imageUri = FileProvider.getUriForFile(this, "com.devfactori.camerawithpermissionsdemo.fileprovider", imageFile);
            //attaching uri to camera intent
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name use the timestamp so the image doesn't have the same name timestamp is different for every second
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // create image file
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentImagePath = image.getAbsolutePath();
        return image;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_AND_CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraIntent();
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                        || shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    permissionRationaleDialog(CAMERA_WRITE_EXTERNAL);
                } else if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                        || !shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showAppPermissionSettingsDialog(CAMERA_WRITE_EXTERNAL);
                } else {
                    // do nothing both permissions denied
                }
            }
        }
    }


    private void permissionRationaleDialog(final String permission) {
        androidx.appcompat.app.AlertDialog.Builder alertBuilder = new androidx.appcompat.app.AlertDialog.Builder(this);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        if (permission.equals(CAMERA_WRITE_EXTERNAL)) {
            alertBuilder.setMessage(getString(R.string.camera_permission));
        } else {
            alertBuilder.setMessage(getString(R.string.storage_permission));
        }
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            public void onClick(DialogInterface dialog, int which) {
                if (permission.equals(CAMERA_WRITE_EXTERNAL)) {
                    requestStorageAndCameraPermissions();
                } else if (permission.equals(READ_EXTERNAL)) {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
                }

            }
        });
        alertBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        androidx.appcompat.app.AlertDialog alert = alertBuilder.create();
        alert.show();
    }


    private void showAppPermissionSettingsDialog(final String permission) {
        String title = "", message = "";
        if (permission.equals(CAMERA_WRITE_EXTERNAL)) {
            title = "Camera & Storage Permission Required";
            message = "In order to access your camera app needs camera & storage permission.";
        } else if (permission.equals(READ_EXTERNAL)) {
            title = "Storage Permission Required";
            message = "In order to access your gallery app needs read storage permission.";
        }
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (permission.equals(CAMERA_WRITE_EXTERNAL)) {
                            appSettingsClickedForCamera = true;
                        } else {
                            appSettingsClickedForStorage = true;
                        }
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                        intent.setData(uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setCancelable(false).create().show();
    }
}



