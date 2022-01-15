package com.avtelma.backgroundparser.broadcastreceivers

import android.content.Intent

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.util.Log
import com.avtelma.backgroundparser.service_parsing_events.ParsingActions
import com.avtelma.backgroundparser.service_parsing_events.ParsingEventService


class CloseServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        //if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Log.i("ccc","ccc  START!!!!!")
        //log("###############Start !!!!!!!!!!!!!!!!")

        //context.sendBroadcast(Intent("call"));

        Intent(context, ParsingEventService::class.java).also {
            it.action = ParsingActions.STOP.name

           // //Unbonding
           // EndlessService().bleManager?.notifyCharacteristic(true, UUID.fromString("74ab521e-060d-26df-aa64-cf4df2d0d643"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //log("Starting the service in >=26 Mode")
                context.startForegroundService(it)
                return
            }

            //log("Starting the service in < 26 Mode")
            context.startService(it)
        }
    }
}