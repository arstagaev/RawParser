package com.avtelma.backgroundparser.service_parsing_events

//import com.avtelma.backgroundparser.tools.VariablesAndConst.CURRENT_FILE_IN_PROGRESS

import XYZ
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.avtelma.backgroundparser.R

import com.avtelma.backgroundparser.tools.log
import com.avtelma.backgroundparser.service_parsing_events.enum.STATE_OF_PARSING
import com.avtelma.backgroundparser.service_parsing_events.enum.STYLE_OF_PARSING
import com.avtelma.backgroundparser.service_parsing_events.models.LtLn
import com.avtelma.backgroundparser.tools.toastShow
import com.avtelma.backgroundparser.tools.VariablesAndConst.PROGRESS_MAX
import com.avtelma.backgroundparser.tools.VariablesAndConst.PROGRESS_NOTIF
import com.avtelma.backgroundparser.tools.VariablesAndConst.RECORD_ACTIVITY_FOR_RAWPARSER
import com.avtelma.backgroundparser.tools.VariablesAndConst.TARGET_NAME_FILE_IMU_and_GPS
import com.avtelma.backgroundparser.tools.VariablesAndConst.currentStateOfParsing
import com.avtelma.backgroundparser.tools.VariablesAndConst.root1_raw
import com.avtelma.backgroundparser.tools.VariablesAndConst.root2_preproc
import com.avtelma.backgroundparser.tools.VariablesAndConst.styleOfParsing
import com.avtelma.backgroundparser.broadcastreceivers.CloseServiceReceiver
import com.avtelma.backgroundparser.core.algorithm.*
import com.avtelma.backgroundparser.tools.VariablesAndConst.THRESHOLD_GAS_BREAK_DURATION
import com.avtelma.backgroundparser.tools.VariablesAndConst.THRESHOLD_STOP_DURATION
import com.avtelma.backgroundparser.tools.VariablesAndConst.THRESHOLD_TURN_DURATION
import kotlinx.coroutines.*
import java.io.*


/**
 * Need check permission before run this Service
 *
 */
@SuppressLint("MissingPermission")
class ParsingEventService : Service() {

    private var PERCENTAGE_OF_COMPLETE = 0
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false



    var arrayOfXYZ = arrayListOf<XYZ>()
    var curProgress = 0

    private var isMultiLog = true
    /////////////

    override fun onBind(intent: Intent): IBinder? {
        log("Some component want to bind with the service")
        // We don't provide binding, so return null

        return null
    }

    /**
     * ENTRY-COMMAND POINT OF SERVICE
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")

        if (intent != null) {
            val action = intent.action
            log("using an intent with action $action")
            when (action) {
                ParsingActions.START.name ->  startService() // Engine Start
                ParsingActions.STOP.name  ->  stopService()  // Engine Stop!
                ParsingActions.FULL_PARSING.name -> {
                    styleOfParsing = STYLE_OF_PARSING.WATER_FALL_PARSING
                    parseAllFiles()

                }
                ParsingActions.TARGET_PARSING.name -> {
                    styleOfParsing = STYLE_OF_PARSING.TARGET_PARSING
                    CoroutineScope(Dispatchers.IO).launch {
                        parseTargetFile(File(root1_raw, TARGET_NAME_FILE_IMU_and_GPS))
                    }

                }
//                ParsingActions.CLEAR_DESKTOP_LOG.name -> {
//
//                    CoroutineScope(Dispatchers.IO).launch {
//                        clearJustOldLogs(File(root1_raw, TARGET_NAME_FILE_IMU_and_GPS))
//                    }
//
//                }

                else -> log("This should never happen. No action in the received intent")
            }
        } else {
            log("with a null intent. It has been probably restarted by the system.")
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }



    // Send an Intent with an action named "my-event".
//    private fun sendMessage(msg : String) {
//        val intent = Intent("my-event")
//        // add data
//        intent.putExtra("message", msg)
//        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
//    }

    override fun onCreate() {
        super.onCreate()
        log("The service has been created".toUpperCase())
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("The service has been destroyed".toUpperCase())

        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, ParsingEventService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
    }




    @SuppressLint("CheckResult")
    private fun startService() {


        if (isServiceStarted) return

        log("Starting the foreground service task")

        isServiceStarted = true


        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ParsingEventService::lock").apply {
                    acquire()
                }
            }


        toastShow("Service has been started",Color.YELLOW,this@ParsingEventService)

        Log.w("ccc","<><><>ALL DONE<><><><><><><>")
        Log.w("ccc","<><><>ALL DONE<><><><><><><>")
        Log.w("ccc","<><><>ALL DONE<><><><><><><>")
        /**
         * MAIN LOOPER
         */
        GlobalScope.launch(Dispatchers.Main) {
            while (isServiceStarted) {
                launch(Dispatchers.Main) {
//                    val builder = NotificationCompat.Builder(this@ParsingEventService)
//                        .setProgress(100, PROGRESS_NOTIF,false)
//
//                    val notification = builder.build()
//
//                    val notificationManager =
//                        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//                    notificationManager.notify(1, builder.build())

                    if (styleOfParsing == STYLE_OF_PARSING.TARGET_PARSING) {
                        // show notification with current progress
                        curProgress = ((PROGRESS_NOTIF.toFloat()/ PROGRESS_MAX.toFloat() )*100f).toInt()

                        if (currentStateOfParsing == STATE_OF_PARSING.PARSING) {
                            builder.setProgress(100,curProgress,false)
                                .setContentText("???:${TARGET_NAME_FILE_IMU_and_GPS}");
                            notificationManager.notify(1, builder.build())
                            //PROGRESS_NOTIF++
                        } else {
                            curProgress = 0
                            builder.setContentText("ready to work");
                            notificationManager.notify(1, builder.build())
                            delay(2000)
                            stopService()
                        }
                    }else if (styleOfParsing == STYLE_OF_PARSING.WATER_FALL_PARSING) {
                        curProgress = PROGRESS_NOTIF

                        if (currentStateOfParsing == STATE_OF_PARSING.PARSING) {

                            builder.setProgress(PROGRESS_MAX,curProgress,false)
                                .setContentText("${PROGRESS_NOTIF}/${PROGRESS_MAX}???:${TARGET_NAME_FILE_IMU_and_GPS}");
                            notificationManager.notify(1, builder.build())
                            //PROGRESS_NOTIF++
                        } else {
                            curProgress = 0
                            builder.setContentText("ready to work");
                            notificationManager.notify(1, builder.build())
                            delay(2000)
                            stopService()
                        }
                    }
                }
                delay(500)
                // for lifecycle state of parsing:
                if (styleOfParsing == STYLE_OF_PARSING.TARGET_PARSING && PROGRESS_NOTIF.toFloat() >= PROGRESS_MAX.toFloat()*0.97f) {
                    currentStateOfParsing = STATE_OF_PARSING.END
                } else if (styleOfParsing == STYLE_OF_PARSING.WATER_FALL_PARSING && PROGRESS_NOTIF == PROGRESS_MAX) {
                    currentStateOfParsing = STATE_OF_PARSING.END
                }
                Log.w("sss","<><><><><><><> LOAD SERVICE AGAIN <><><><><> $TIME ${PROGRESS_NOTIF} / ${PROGRESS_MAX}  || ${curProgress} || ${currentStateOfParsing.name}")
            }
            log("End of the loop for the service")
        }
    }

    fun appendText(sFileName: String, sBody: String){
        try {
            val root = File(Environment.getExternalStorageDirectory(), "ItelmaBLE_Background/Jsons")
            if (!root.exists()) {
                root.mkdirs()
            }
            val file = File(root, sFileName)

            val fileOutputStream = FileOutputStream(file,true)
            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
            outputStreamWriter.append("\n "+sBody)

            outputStreamWriter.close()
            fileOutputStream.close()
            //findAndReplacePartOfText(file)

        } catch (e: IOException) {
            Log.e("ccc","ERROR "+ e.message)
            e.printStackTrace()
        }

    }

    fun parseAllFiles() { // both dont launch!
        currentStateOfParsing = STATE_OF_PARSING.PARSING
        var needToInspect = getFilesFromFolderRaw(root1_raw)//noAlreadyParsedFromRawString(getFilesFromFolderRaw(root1_raw),getFilesFromFolderPreProc(root2_preproc))
        //Log.i("lll","lll ${noAlreadyParsedFromRawString(arrayListOf("312","0312","2341"), arrayListOf("312","0312","23445"))}")
        Log.i("lll","lll SIZE NEED TO INSPECT:::::::::::::::::::::::${needToInspect.size}   :::::::::::::::::")

        Toast.makeText(applicationContext,"Need to inspect ${needToInspect.size} trips",Toast.LENGTH_LONG).show()

        Log.i("lll","lll ${needToInspect.toString()}")
        // delete garbage logs
        runBlocking(Dispatchers.IO) {
            launch {
                for (i in needToInspect) {

                    try {
                        clearJustOldLogs(File("${root1_raw}/${i}_imu_gps.txt"),i.pieceFileName)
                    } catch (e: Exception) {
                        Log.e("ERROR ","eeerror with ${"${root1_raw}/${i}_imu_gps.txt"} //code:${e.message}")
                    }
                    Log.i("well","well done <|> ${root1_raw}/${i}_imu_gps.txt")


                }
            }
        }

        var needToParse = noAlreadyParsedFromRawString(
            getFilesFromFolderRaw(root1_raw),
            getFilesFromFolderPreProc(root2_preproc)
        )
        Log.i("lll","lll SIZE NEED TO PARSE:::::::::::::::::::::::${needToParse.size}   :::::::::::::::::")
        PROGRESS_MAX = needToParse.size
        PROGRESS_NOTIF = 0
        // parse needed logs
        CoroutineScope(Dispatchers.IO).launch {

            for (i in needToParse) {
                last_LtLn = LtLn(0.0,0.0)

                //parseTargetFile(File("${root1_raw}/${i}_imu_gps.txt"))
                parseTargetFile(i.filex)

                //Log.i("well","well done >|parse|< ${root1_raw}/${i}_imu_gps.txt")
                Log.i("well","well done >|parse|< ${i.filex}")
                PROGRESS_NOTIF++

            }

        }
        if (styleOfParsing == STYLE_OF_PARSING.WATER_FALL_PARSING && PROGRESS_NOTIF == PROGRESS_MAX) {
            currentStateOfParsing = STATE_OF_PARSING.END
        }
    }

    var CURRENT_POS = 1
    suspend fun parseTargetFile(file: File) = withContext(Dispatchers.IO){
        var isExist = file.exists()
        Log.i("go for PARSE . is Exist: ${isExist}", " " + file.name)
        if (!isExist) {
            return@withContext
        }
        //Read text from file
        TARGET_NAME_FILE_IMU_and_GPS = file.name

        TIME = 0
        //val text = StringBuilder()

        val reader = BufferedReader(FileReader(file))
        if (styleOfParsing == STYLE_OF_PARSING.TARGET_PARSING) {
            PROGRESS_MAX = 0

            while (reader.readLine() != null) PROGRESS_MAX++
            reader.close()
        }



        var NumberOfLine = 1
        try {
            // read lines in txt by Bufferreader

            val br = BufferedReader(FileReader(file))
            var line: String?

            while (br.readLine().also { line = it } != null) {
                //Log.i("vvv","l "+line)
                if (line != "" || line != " ") {

                    val items = line?.split(";"," ")?.toTypedArray()
                    //Log.i("vvv","leght "+items?.get(2).toString()+"  "+items?.get(1).toString())

                    //if (NumberOfLine >=25) {
                    // we create pool of xyz array in 500 elements  // 2907_212603_imu_gps.txt has been error
                    if (items != null) {

                        arrayOfXYZ.add(
                            XYZ(
                                items.get(0).toDouble(),
                                items.get(1).toDouble(),
                                items.get(2).toDouble()
                            )
                        )

                    }

                    if (arrayOfXYZ.size == 25) {

                        //Log.i("vvv","vvv ${AlgorithmDefineEvents().calc(arrayOfXYZ)}")
                        //Log.i("aloradance","stromae ${arrayOfXYZ.joinToString()}")
                        var eventX = calc(arrayOfXYZ)
                        //Location.distanceBetween()
                        var ltLn = LtLn(0.0,0.0)
                        //addLogsEvents("${GENERATE_SPECIAL_ID_FOR_EVENTS_2}","${eventX.stop_1},${eventX.gas_break_2},${eventX.turn_3},${eventX.jump_4},${eventX.condition_debug}")
                        if (items != null && items?.size!! >= 6) {
                            ltLn = getNormalCoordinates(items?.get(3)!!,items.get(4))
                        }

                        //var lat = //.toDouble()
                        //var lon = .toDouble()
                        Log.i("ccc","ccc eventX ${eventX.toString()}")
                        compareLogs2( eventX, ltLn.lat,ltLn.lon ) // <<<

                        CURRENT_POS++
                        if (styleOfParsing == STYLE_OF_PARSING.TARGET_PARSING) {
                            PROGRESS_NOTIF++
                        }
                        //delay(20)

                        arrayOfXYZ.removeAt(0)

                        //delay(10)
                    }
                    NumberOfLine++
                }
            }
            br.close()

            /** End up short tail:
             * If only one Event defined, need him write to file
             * For define end-tail of events?
             */
            if (SAVER_Event_Container!!.stop_duration         > THRESHOLD_STOP_DURATION
                || SAVER_Event_Container!!.gas_break_duration > THRESHOLD_GAS_BREAK_DURATION
                || SAVER_Event_Container!!.turn_duration      > THRESHOLD_TURN_DURATION
            )
            writePreProcLog(SAVER_Event_Container!!)

            if (styleOfParsing == STYLE_OF_PARSING.TARGET_PARSING && PROGRESS_NOTIF.toFloat() == PROGRESS_MAX.toFloat()*0.98f) {
                currentStateOfParsing = STATE_OF_PARSING.END
            }
        } catch (e: IOException) {
            Log.e("ccc", "error +${e.message}")
            Log.e("ccc", "error +${e.message}")
            Log.e("ccc", "error +${e.message}")
            //arrayListOfSpeed.clear()
            //You'll need to add proper error handling here
        }
    }



    fun addLogsEvents(sFileName: String, sBody: String){
        //SIZE_OF_EVETNS++

        try {
            //val root2 = File(Environment.getExternalStorageDirectory(), "PreProcessing") // and folder
            if (!root2_preproc.exists()) {
                root2_preproc.mkdirs()
            }
            val file = File(root2_preproc, sFileName)

            val fileOutputStream = FileOutputStream(file,true)
            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
            outputStreamWriter.append("\n"+sBody)

            outputStreamWriter.close()
            fileOutputStream.close()
            //findAndReplacePartOfText(file)

        } catch (e: IOException) {
            Log.e("ccc","ERROR "+ e.message)
            e.printStackTrace()
        }

    }




    /////////////


    private fun stopService() {

        log("Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }

            stopForeground(true)
            stopSelf()


        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)

    }

    private lateinit var notificationManager : NotificationManager
    private val notificationChannelMajor = "avtelmaMainRawParser"
    private val notificationChannelId2 = "avtelma2"

    private var builder = NotificationCompat.Builder(this, notificationChannelMajor)
        .setContentTitle("AVTelma")
        .setContentText("\uD83D\uDD34 Working..")
       // .setContentIntent(pendingIntent)
        .setSmallIcon(R.drawable.all_out)
        .setProgress(100, PROGRESS_NOTIF,false)
        //.setTicker("Ticker text")
        .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
        .setOnlyAlertOnce(true) // ATTENTION!!!
        //.addAction(actionX)

//    private fun refreshMainNotification(msg : String){
//
//        builder.setContentText("\uD83D\uDD34 Working.. | $msg")
//        notificationManager.notify(notificationChannelId,0, builder.build());
//    }


    private fun createNotification(): Notification {

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        // ?????? ??????
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val nc =
//                NotificationChannel("Channel Player", "Player", NotificationManager.IMPORTANCE_HIGH)
//            notificationManager.createNotificationChannel(nc)
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // coz notif channels added in android 8.0
            val channel = NotificationChannel(
                notificationChannelMajor,
                "AVTelma_RawParserMain",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Main Indicator of Service"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 500, 100, 100, 100, 100, 100, 100, 300, 400, 500)
                it
            }
            //notificationManager.createNotificationChannel(channel)

//            val channel2 = NotificationChannel(
//                notificationChannelId2,
//                "AVTelma_xyz",
//                NotificationManager.IMPORTANCE_DEFAULT
//            ).let {
//                it.description = "no main Indicator of Service"
//                it.enableLights(true)
//                it.lightColor = Color.RED
//                it
//            }
            notificationManager.createNotificationChannel(channel)
            //notificationManager.createNotificationChannel(channel2)
        }

        val pendingIntent: PendingIntent = Intent(this,RECORD_ACTIVITY_FOR_RAWPARSER).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        //////////
        val broadcastIntent = Intent(this, CloseServiceReceiver::class.java)

        val actionIntent = PendingIntent.getBroadcast(
            this,
            0, Intent(this, CloseServiceReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT
        )
//        val actionIntent2 = PendingIntent.getBroadcast(
//            this,
//            0, Intent(this, UnBondingReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT
//        )
        var actionX: NotificationCompat.Action =
            NotificationCompat.Action.Builder(R.drawable.all_out, "Stop", actionIntent).build()
//        val remoteViews = RemoteViews(packageName, android.R.layout.activity_list_item)
//        remoteViews.setTextViewText(R.id.text,"d")


        return builder
            .setContentTitle("AVTelma")
            .setContentText("\uD83D\uDD34 Working..")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.all_out)
            //.setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .setOnlyAlertOnce(true) // ATTENTION!!!
            .addAction(R.drawable.all_out,"stop",actionIntent)
            //.addAction(com.avtelma.avtsiplatform.R.drawable.ic_stop_rec,"unbonding",actionIntent2)

            .build()
    }




}
