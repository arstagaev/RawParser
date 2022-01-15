package com.avtelma.backgroundparser.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.avtelma.backgroundparser.service_parsing_events.ParsingActions
import com.avtelma.backgroundparser.service_parsing_events.ParsingEventService
import com.avtelma.backgroundparser.service_parsing_events.ServiceState
import com.avtelma.backgroundparser.service_parsing_events.getServiceState
import com.avtelma.backgroundparser.tools.log


class StartReceiver : BroadcastReceiver() {
    // for notifications
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && getServiceState(context) == ServiceState.STARTED) {
            Intent(context, ParsingEventService::class.java).also {
                it.action = ParsingActions.START.name
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    log("Starting the service in >=26 Mode from a BroadcastReceiver")
                    context.startForegroundService(it)
                    return
                }
                log("Starting the service in < 26 Mode from a BroadcastReceiver")
                context.startService(it)
            }
        }
    }
}
