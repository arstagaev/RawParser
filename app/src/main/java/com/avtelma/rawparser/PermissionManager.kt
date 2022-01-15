package com.avtelma.rawparser

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

//////////
const val MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1
const val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 2
// AppOpsManager.OPSTR_MANAGE_EXTERNAL_STORAGE is a @SystemAPI at the moment
// We should remove the annotation for applications to avoid hardcoded value
const val MANAGE_EXTERNAL_STORAGE_PERMISSION = "android:manage_external_storage"

@RequiresApi(30)
fun checkStoragePermissionApi30(activity: ComponentActivity): Boolean {
    val appOps = activity.getSystemService(AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        MANAGE_EXTERNAL_STORAGE_PERMISSION,
        activity.applicationInfo.uid,
        activity.packageName
    )

    return mode == AppOpsManager.MODE_ALLOWED
}

@RequiresApi(30)
fun requestStoragePermissionApi30(activity: ComponentActivity) {
    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)

    activity.startActivityForResult(intent, MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST)
}

@RequiresApi(19)
fun checkStoragePermissionApi19(activity: MainActivity): Boolean {
    val status =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
    //AppCompatActivity
    return status == PackageManager.PERMISSION_GRANTED
}

@RequiresApi(19)
fun requestStoragePermissionApi19(activity: ComponentActivity) {
    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ActivityCompat.requestPermissions(
        activity,
        permissions,
        READ_EXTERNAL_STORAGE_PERMISSION_REQUEST
    )
}

//@ExperimentalPermissionsApi
//fun PermissionState.isPermanentlyDenied(): Boolean {
//    return !shouldShowRationale && !hasPermission
//}