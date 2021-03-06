package com.avtelma.backgroundparser.core.algorithm

import AngleBetweenQ_P
import Axang2Q_P
import Deviation_P
import EventLine
import EventPreFinal
import Find_max_index_P
import Mean_P
import NormVector_P
import Quatrotate_P
import RotVecCalc_P
import VectorNorm_P
import XYZ
import android.util.Log
import com.avtelma.backgroundparser.service_parsing_events.models.LtLn
import com.avtelma.backgroundparser.service_parsing_events.checkZeroOrNot
import com.avtelma.backgroundparser.service_parsing_events.generateNameOfLogEvents
import com.avtelma.backgroundparser.tools.VariablesAndConst.GENERATE_SPECIAL_ID_FOR_EVENTS_2
import com.avtelma.backgroundparser.tools.VariablesAndConst.THRESHOLD_GAS_BREAK_DURATION
import com.avtelma.backgroundparser.tools.VariablesAndConst.THRESHOLD_JUMP_DURATION
import com.avtelma.backgroundparser.tools.VariablesAndConst.THRESHOLD_STOP_DURATION
import com.avtelma.backgroundparser.tools.VariablesAndConst.THRESHOLD_TURN_DURATION
import com.avtelma.backgroundparser.tools.VariablesAndConst.root2_preproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import kotlin.math.abs
import kotlin.math.sign


var SIZE_OF_EVENTS = 0
var stop_duration      : Int = 0
var gas_break_duration : Int = 0
var turn_duration      : Int = 0
var jump_duration      : Int = 0


/**
 *  Below - main code base of Algorithm
 */
//var Events = arrayListOf<EventLine>()



var isHorisonted = 0
var isAzimuted = 0

//var STOP_THRESHOLD=0.0002; //Do adaptive
var GAS_THRESHHOLD = 0.06;   //Do adaptive

//var GAS_BREAKS_THRESHHOLD = 0 // really?
val Chunk_size = 25       // Constant?

var first_chunk_completed = -1;
var pos = 0;
var isLocatedVert = false;
var isLocatedForw = false;



//var Forw: IntArray = intArrayOf(0, 0, 0);
var Forw = 0
var Hor_counter = 0;
var Forw_counter = 0;
var Q_Hor =     arrayListOf<Double>(1.0, 0.0, 0.0, 0.0)
var Q_Hor_old = arrayListOf<Double>(1.0, 0.0, 0.0, 0.0)
var Q_Forw =    arrayListOf<Double>(1.0, 0.0, 0.0, 0.0)
var GB_s = 0;
var GB_f = 0;
var GB_event = false;
var isReverse = 1;
var Side = -1
var test=0.0;

//Preallocating arrays
//    condition=zeros(size(T,1),1);
//    MeanA=zeros(size(T,1),3);
//    Chunk_A=zeros(Chunk_size,3);
//    Chunk_F=zeros(Chunk_size,3);
//    Events_M=zeros(size(T,1),5);
var MeanA : ArrayList<Double> = ArrayList()
var MeanA_temp     = arrayListOf<Double>()
var MeanA_mod :Double = 0.0
var RMS_amplitude = doubleArrayOf()
var RMS_Mean = doubleArrayOf()
var DevA  :ArrayList<Double> = ArrayList()
var DevA_mod: Double = 0.0

var NorMean  :ArrayList<Double> = ArrayList()
var Vert: Int = 0
var Axang_Hor :ArrayList<Double> = ArrayList()
var ANGLE_CRITERIA = 5.0
var IS_HORISONTED = 0
//var TURN_THRESHHOLD = 0

//1
var Suspect :ArrayList<Double> = arrayListOf(0.0, 0.0, 0.0)

var Axang_forw = arrayListOf<Double>()
var Q_Forw_old = arrayListOf<Double>(1.0, 0.0, 0.0, 0.0)
var IS_AZIMUTED = 0

var Grav = arrayListOf<Double>(0.0, 0.0, 0.0) //
var A1 = arrayListOf<Double>(0.0, 0.0, 0.0)

val STOP_THRESHOLD = 0.05
val GAS_BREAKS_THRESHHOLD = 0.06

val TURN_THRESHHOLD = 0.08
val BUMP_THRESHHOLD = 0.2
var testt = 0.0
var CURRENT_POS = 1
var CONDITION = 0

//var event = EventLine(0,0,0,0,CONDITION)

var event = EventLine(0,0,0,0, CONDITION,0,0,0,0)
//var CONDITION = -1
var BIG_BUFFER : String = ""
fun calc(arrayOfXYZinner: ArrayList<XYZ>) : EventLine {
    var chunk_A = arrayOfXYZinner // vvariables 25
    logMB("start of cycle >>>>>>>>>>>>>>>> ${chunk_A.joinToString()}")

    CONDITION = isHorisonted + isAzimuted
    event.stop_1     = 0
    event.gas_break_2= 0
    event.turn_3     = 0
    event.jump_4     = 0
    event.condition_debug = CONDITION
    //event = EventLine(0,0,0,0,CONDITION)

    //Log.i("ccc","uuu isHoris $isHorisonted isAzim $isAzimuted")
    var GB_int = 0
    var GB_fin = 0
    logMB("${arrayOfXYZinner[0].x}  ${arrayOfXYZinner[0].y}  ${arrayOfXYZinner[0].z}  ${arrayOfXYZinner.size}")
    // here we starting define conditions
    when (CONDITION) {
        -1 -> {

            logMB("###################### CONDITION = -1 ##############################")
        }
        0 -> {
            MeanA     = Mean_P(chunk_A)
            MeanA_mod = VectorNorm_P(MeanA)

            DevA     = Deviation_P(chunk_A, MeanA)
            DevA_mod = VectorNorm_P(DevA)

            if (DevA_mod < STOP_THRESHOLD) {
                //Events
                event.stop_1 = 1  // EVENT
                //isLocatedVert
                NorMean = NormVector_P(MeanA) // ?


                if (isLocatedVert == false) {

                    Vert = Find_max_index_P(NorMean)
                    Grav[Vert] = 1.0
                    isLocatedVert = true
                }

                Axang_Hor = RotVecCalc_P(Grav, NorMean)
                Q_Hor_old = Q_Hor
                Q_Hor = Axang2Q_P(Axang_Hor)

                if (AngleBetweenQ_P(Q_Hor, Q_Hor_old) < ANGLE_CRITERIA) {
                    Hor_counter++
                }
                if (Hor_counter > 10) { // ?????? ????????????????????, ???????? ???? 12 ???????????? - ???? ???? ?????? ?????????????? ???????????? ????????????????????, ?? ???????? ???? 10 ???? ????????
                    isHorisonted = 1
                }
            }

            logMB("devA mod:$DevA_mod Axang_Hor:$Axang_Hor Q_Hor:$Q_Hor Q_Hor_old:$Q_Hor_old")
        }
        1 -> {
            logMB("###################### CONDITION = 1 ############################## ")
            logMB("Q_Hor:$Q_Hor ")

            MeanA     = Mean_P(chunk_A)
            DevA      = Deviation_P(chunk_A, MeanA)

            DevA_mod  = VectorNorm_P(DevA)
            MeanA_mod = VectorNorm_P(MeanA)

            MeanA     =  Quatrotate_P(Q_Hor, MeanA)
            DevA      =  Quatrotate_P(Q_Hor, DevA)

            MeanA[Vert] = 0.0

            //test        = VectorNorm(MeanA);

            if (VectorNorm_P(MeanA) > GAS_BREAKS_THRESHHOLD) { // change vector norm - to meanmod

                Suspect = MeanA//NormVector(MeanA)

                if (isLocatedForw == false){
                    Forw = Find_max_index_P(Suspect)

                    A1[Forw] = 1.0              //
                    Side = 3 - (Vert + Forw)    //
                    isLocatedForw = true
                }
            }

            if (DevA_mod < STOP_THRESHOLD)   {
                event.stop_1 = 1      // EVENT

                if(isLocatedForw){
                    Axang_forw = RotVecCalc_P(A1, NormVector_P(Suspect)) // Nan
                    Q_Forw_old = Q_Forw
                    Q_Forw = Axang2Q_P(Axang_forw)

                    if (AngleBetweenQ_P(Q_Forw, Q_Forw_old) < ANGLE_CRITERIA) { // ANGLE_CRITERIA = 5
                        Forw_counter++

                        if (Forw_counter > Chunk_size / 5) {
                            isAzimuted = 1

                            isReverse = -1
                        }


                    }else{
                        Forw_counter = 0
                    }
                }
            }else{
            }
        }
        2 -> {
            logMB("###################### CONDITION = 2 ############################## $SIZE_OF_EVENTS")
            MeanA    = Mean_P(chunk_A)
            //MeanA_mod = VectorNorm(MeanA) //

            DevA     = Deviation_P(chunk_A, MeanA)
            DevA_mod = VectorNorm_P(DevA)

            MeanA =  Quatrotate_P( Q_Hor,  MeanA )
            DevA =  Quatrotate_P( Q_Hor,  DevA  )

            MeanA =  Quatrotate_P( Q_Forw, MeanA )
            DevA =  Quatrotate_P( Q_Forw, DevA  )





            if (abs(MeanA[Forw]) > GAS_BREAKS_THRESHHOLD) {

                event.gas_break_2 = ((MeanA[Forw]).sign * isReverse).toInt()

            }

            if (DevA_mod < STOP_THRESHOLD) {

                event.stop_1      = 1

            }
            //!!! i force reinit Side below coz he is equal = -1
            //Side = 2
            if (abs(MeanA[Side]) > TURN_THRESHHOLD) {

                event.turn_3  = ((MeanA[Side]).sign * isReverse).toInt()

            }

            if (DevA[Vert] > BUMP_THRESHHOLD) {

                event.jump_4 = 1

            }


            // for (g in 0 until Events.size ){
            //     Log.i("event","size:${Events.size} events: ${Events[g].stop} ${Events[g].gas_break} ${Events[g].turn} ${Events[g].jump}")
            // }
        }
    }
    logMB("|||${event.stop_1},${event.gas_break_2},${event.turn_3},${event.jump_4},${event.condition_debug}||| Cond: $CONDITION : $CURRENT_POS  Q_Hor:${Q_Hor[0]} ${Q_Hor[1]} ${Q_Hor[2]} ${Q_Hor[3]}; Q_Forw:${Q_Forw[0]} ${Q_Forw[1]} ${Q_Forw[2]} ${Q_Forw[3]}; MeanA:[${MeanA[0]} ${MeanA[1]} ${MeanA[2]}] MeanA_mod:$MeanA_mod* DevA:[${DevA[0]} ${DevA[1]} ${DevA[2]}]; *${DevA_mod.toString()}* ")
    return event


//        if (arrayListOf<Int>(
//                event.stop_1,
//                event.gas_break_2,
//                event.turn_3,
//                event.jump_4      ) == lastChapter && howManyRepeat < 5) {
//            addLogEve("${GENERATE_SPECIAL_ID_FOR_EVENTS_2}","${event.stop_1},${event.gas_break_2},${event.turn_3},${event.jump_4},${event.condition_debug}")
//
//            lastChapter = arrayListOf<Int>(0,0,0,0)
//            howManyRepeat++
//        }else if (arrayListOf<Int>(
//                event.stop_1,
//                event.gas_break_2,
//                event.turn_3,
//                event.jump_4) != lastChapter) {
//            addLogEve("${GENERATE_SPECIAL_ID_FOR_EVENTS_2}","${event.stop_1},${event.gas_break_2},${event.turn_3},${event.jump_4},${event.condition_debug}")
//
//            howManyRepeat = 0
//        }
    //addLogsEvents("${GENERATE_SPECIAL_ID_FOR_EVENTS_2}","${event.stop_1},${event.gas_break_2},${event.turn_3},${event.jump_4},${event.condition_debug}")

    //bw.append("\n${event.stop_1},${event.gas_break_2},${event.turn_3},${event.jump_4},${event.condition_debug}")


}

var TIME = 0
var last_time        = 0
var last_stop_1      = 0
var last_gas_break_2 = 0
var last_turn_3      = 0
var last_jump_4      = 0
var last_LtLn : LtLn = LtLn(0.0,0.0)


var needPublishLog = false

var SAVER_Event_Container : EventPreFinal? = null
//fun compareLogs (eventLine: EventLine, lat: Float, lon : Float) {
//    TIME++
//    needPublishLog = false
//    //stop_1
//    //gas_break_2
//    //turn_3
//    //jump_4
//    if (last_stop_1 == eventLine.stop_1) {
//        event.stop_duration++
//    } else if (eventLine.stop_duration >= 75) {
//        needPublishLog = true
//    }
//
//    if (last_gas_break_2 == eventLine.gas_break_2 && !needPublishLog) {
//        event.gas_break_duration++
//    } else if (eventLine.gas_break_duration >= 25) {
//        needPublishLog = true
//    }
//
//    if (last_turn_3 == eventLine.turn_3 && !needPublishLog) {
//        event.turn_duration++
//    } else if (eventLine.turn_duration >= 25) {
//        needPublishLog = true
//    }
//
//    if (last_jump_4 == eventLine.jump_4 && !needPublishLog) {
//        event.jump_duration++
//    } else if (eventLine.stop_duration >= 12) {
//        needPublishLog = true
//    }
//
//
//    ////
//    if (needPublishLog) {
//        var maxDuration = arrayListOf<Int>(event.stop_duration, event.gas_break_duration, event.turn_duration, event.jump_duration).maxOrNull()
//        if (maxDuration == null) { maxDuration = 1000000 } // just for test\\\
//
//
//        var asd = EventPreFinal(
//            last_stop_1,
//            last_gas_break_2,
//            last_turn_3,
//            last_jump_4,eventLine.condition_debug,
//            TIME -maxDuration,
//            event.stop_duration,
//            event.gas_break_duration,
//            event.turn_duration,
//            event.jump_duration,
//            last_LtLn
//        )
//
//
//        writePreProcLog(asd)
//        //writePreProcLog("${last_stop_1},${last_gas_break_2},${last_turn_3},${last_jump_4} ${eventLine.condition_debug} ${TIME-maxDuration} ${event.stop_duration},${event.gas_break_duration},${event.turn_duration},${event.jump_duration}")
//
//        //BUFFER_FILTER.add(EventPreFinal(last_stop_1, last_gas_break_2, last_turn_3, last_jump_4,condition_debug = eventLine.condition_debug,TIME-maxDuration,eventLine.stop_duration,eventLine.gas_break_duration,eventLine.turn_duration,eventLine.jump_duration))
//
//
////        if (last_stop_1      != eventLine.stop_1 ) {
////            event.stop_duration      = 1
////        }
////        if (last_gas_break_2 != eventLine.gas_break_2 ) {
////            event.gas_break_duration = 1
////        }
////        if (last_turn_3      != eventLine.turn_3 ) {
////            event.turn_duration      = 1
////        }
////        if (last_jump_4      == eventLine.jump_4 ) { // coz in fly auto cant be in 3+ sec
////            event.jump_duration      = 1
////        }
//        event.stop_duration      = 1
//        event.gas_break_duration = 1
//        event.turn_duration      = 1
//        event.jump_duration      = 1
//    }
//    last_stop_1      = eventLine.stop_1
//    last_gas_break_2 = eventLine.gas_break_2
//    last_turn_3      = eventLine.turn_3
//    last_jump_4      = eventLine.jump_4
//}

fun compareLogs2 (eventLine: EventLine, lat: Double, lon : Double) {
    TIME++
    needPublishLog = false
    var needGetOff = false
    //stop_1
    //gas_break_2
    //turn_3
    //jump_4
    if (last_stop_1 == eventLine.stop_1) {
        event.stop_duration++
    } else if (event.stop_duration < THRESHOLD_STOP_DURATION) {
        needGetOff = true
    } else {
        needPublishLog = true
    }

    if (last_gas_break_2 == eventLine.gas_break_2 && !needPublishLog) {
        event.gas_break_duration++
    } else if (event.gas_break_duration < THRESHOLD_GAS_BREAK_DURATION) {
        needGetOff = true
    } else {
        needPublishLog = true
    }

    if (last_turn_3 == eventLine.turn_3 && !needPublishLog) {
        event.turn_duration++
    } else if (event.turn_duration < THRESHOLD_TURN_DURATION) {
        needGetOff = true
    }  else {
        needPublishLog = true
    }

    if (last_jump_4 == eventLine.jump_4 && !needPublishLog) {
        event.jump_duration++
    } else if (event.jump_duration < THRESHOLD_JUMP_DURATION) {
        needGetOff = true
    } else {
        needPublishLog = true
    }
    // for somewhat refresh position ?
    if (event.stop_duration in 6..9) {
        last_LtLn = LtLn(lat, lon)
    }

    Log.i("ccc","ccc needPublishLog:${needPublishLog}|| event:st${eventLine.stop_duration}gs${eventLine.gas_break_duration}tr${eventLine.turn_duration}jm${eventLine.jump_duration}")
    ////
    if (needPublishLog) {
        if (needGetOff)
            return

        var maxDuration = arrayListOf<Int>(event.stop_duration, event.gas_break_duration, event.turn_duration, event.jump_duration).maxOrNull()
        if (maxDuration == null) { maxDuration = 1000000 } // just for test\\\


        var asd = EventPreFinal(
            last_stop_1, last_gas_break_2, last_turn_3, last_jump_4,eventLine.condition_debug,
            TIME -maxDuration,
            event.stop_duration, event.gas_break_duration, event.turn_duration, event.jump_duration,
            last_LtLn!!
        )


        writePreProcLog(asd)

        event.stop_duration      = 1
        event.gas_break_duration = 1
        event.turn_duration      = 1
        event.jump_duration      = 1
    }
    last_stop_1      =  eventLine.stop_1
    last_gas_break_2 =  eventLine.gas_break_2
    last_turn_3      =  eventLine.turn_3
    last_jump_4      =  eventLine.jump_4

    ///////////////
    var maxDurationSAVER = arrayListOf<Int>(event.stop_duration, event.gas_break_duration, event.turn_duration, event.jump_duration).maxOrNull()

    SAVER_Event_Container =  EventPreFinal(last_stop_1, last_gas_break_2, last_turn_3, last_jump_4,eventLine.condition_debug,
        TIME -maxDurationSAVER!!,
        event.stop_duration, event.gas_break_duration, event.turn_duration, event.jump_duration,
        last_LtLn!!
    )
}

//fun compareLogs3 (eventLine: EventLine, lat: Float, lon : Float) { // without any compress ,like raw may be present
//    TIME++
//    needPublishLog = false
//
//    //stop_1
//    //gas_break_2
//    //turn_3
//    //jump_4
//    if (last_stop_1 == eventLine.stop_1) {
//        event.stop_duration++
//    } else {
//        needPublishLog = true
//    }
//
//    if (last_gas_break_2 == eventLine.gas_break_2 && !needPublishLog) {
//        event.gas_break_duration++
//    }  else {
//        needPublishLog = true
//    }
//
//    if (last_turn_3 == eventLine.turn_3 && !needPublishLog) {
//        event.turn_duration++
//    }  else {
//        needPublishLog = true
//    }
//
//    if (last_jump_4 == eventLine.jump_4 && !needPublishLog) {
//        event.jump_duration++
//    } else {
//        needPublishLog = true
//    }
//
//
//    ////
//    if (needPublishLog) {
//
//
//        var maxDuration = arrayListOf<Int>(event.stop_duration, event.gas_break_duration, event.turn_duration, event.jump_duration).maxOrNull()
//        if (maxDuration == null) { maxDuration = 1000000 } // just for test\\\
//
//
//        var asd = EventPreFinal(
//            last_stop_1,
//            last_gas_break_2,
//            last_turn_3,
//            last_jump_4,eventLine.condition_debug,
//
//            if ( TIME == 0) 0 else TIME -maxDuration,
//
//            event.stop_duration,
//            event.gas_break_duration,
//            event.turn_duration,
//            event.jump_duration,
//            last_LtLn
//        )
//
//
//        writePreProcLog(asd)
//
//        event.stop_duration      = 1
//        event.gas_break_duration = 1
//        event.turn_duration      = 1
//        event.jump_duration      = 1
//    }
//    last_stop_1      =  eventLine.stop_1
//    last_gas_break_2 =  eventLine.gas_break_2
//    last_turn_3      =  eventLine.turn_3
//    last_jump_4      =  eventLine.jump_4
//}

fun logMB (msg: String) {
    //Log.i("algorithm","algorithm: ${msg}")
}




fun writePreProcLog(s: String) {
    Log.i("ccc","ccc already writed")
    try {
        //val root2 = File(Environment.getExternalStorageDirectory(), "PreProcessing") // and folder
        if (!root2_preproc.exists()) {
            root2_preproc.mkdirs()
        }
        val file = File(root2_preproc, GENERATE_SPECIAL_ID_FOR_EVENTS_2)

        val fileOutputStream = FileOutputStream(file,true)
        val outputStreamWriter = OutputStreamWriter(fileOutputStream)
        outputStreamWriter.appendLine(s)

        outputStreamWriter.close()
        fileOutputStream.close()
        //findAndReplacePartOfText(file)

    } catch (e: IOException) {
        Log.e("ccc","ERROR "+ e.message)
        e.printStackTrace()
    }
}

fun writePreProcLog(s: EventPreFinal) {
    Log.i("ccc","ccc already writed:= ${s}")
    try {
        if (!root2_preproc.exists()) {
            root2_preproc.mkdirs()
        }
        GENERATE_SPECIAL_ID_FOR_EVENTS_2 = generateNameOfLogEvents()

        val file = File(root2_preproc, GENERATE_SPECIAL_ID_FOR_EVENTS_2)

        val fileOutputStream = FileOutputStream(file,true)
        val outputStreamWriter = OutputStreamWriter(fileOutputStream)
        outputStreamWriter.appendLine("${s.stop_1},${s.gas_break_2},${s.turn_3},${s.jump_4} ${s.condition_debug} ${s.time} ${s.stop_duration},${s.gas_break_duration},${s.turn_duration},${s.jump_duration} ${checkZeroOrNot(s.ltln.lat)},${checkZeroOrNot(s.ltln.lon)}")

        outputStreamWriter.close()
        fileOutputStream.close()
        //findAndReplacePartOfText(file)
    } catch (e: IOException) {
        Log.e("ccc","ERROR "+ e.message)
        e.printStackTrace()
    }
}