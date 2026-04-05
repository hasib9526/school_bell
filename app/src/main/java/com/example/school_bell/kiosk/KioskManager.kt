package com.example.school_bell.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.school_bell.receiver.SchoolDeviceAdminReceiver

class KioskManager(private val context: Context) {

    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    private val adminComponent = ComponentName(context, SchoolDeviceAdminReceiver::class.java)

    val isAdminActive: Boolean
        get() = devicePolicyManager.isAdminActive(adminComponent)

    val isDeviceOwner: Boolean
        get() = devicePolicyManager.isDeviceOwnerApp(context.packageName)

    fun isInLockTaskMode(): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun isLockTaskPermitted(): Boolean {
        return try {
            devicePolicyManager.isLockTaskPermitted(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun startKioskMode(activity: Activity) {
        try {
            if (isDeviceOwner) {
                devicePolicyManager.setLockTaskPackages(adminComponent, arrayOf(context.packageName))
            }
            activity.startLockTask()
        } catch (e: Exception) {
            // Lock task mode not available or not permitted
        }
    }

    fun stopKioskMode(activity: Activity) {
        try {
            activity.stopLockTask()
        } catch (e: Exception) {
            // Not in lock task mode
        }
    }

    fun requestAdminPermission(activity: Activity, requestCode: Int) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "School Bell requires device admin access to enable kiosk mode."
            )
        }
        activity.startActivityForResult(intent, requestCode)
    }

    fun removeAdminPermission() {
        try {
            devicePolicyManager.removeActiveAdmin(adminComponent)
        } catch (e: Exception) {
            // Not admin
        }
    }

    fun lockScreen() {
        if (isAdminActive) {
            devicePolicyManager.lockNow()
        }
    }

    fun setKioskPackages(packages: Array<String>) {
        if (isDeviceOwner) {
            try {
                devicePolicyManager.setLockTaskPackages(adminComponent, packages)
            } catch (e: Exception) {
                // Not device owner
            }
        }
    }
}
