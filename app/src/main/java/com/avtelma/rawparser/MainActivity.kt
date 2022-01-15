package com.avtelma.rawparser

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.avtelma.backgroundparser.service_parsing_events.ParsingActions
import com.avtelma.backgroundparser.service_parsing_events.ParsingEventService
import com.avtelma.backgroundparser.tools.VariablesAndConst.RECORD_ACTIVITY_FOR_RAWPARSER
import com.avtelma.backgroundparser.tools.log
import com.avtelma.rawparser.ui.theme.RawParserTheme

class MainActivity : ComponentActivity() {
    // After API 23 permission request is asked at runtime
    private val EXTERNAL_STORAGE_PERMISSION_CODE = 23

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runOnUiThread {
            requestPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                2
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if (!checkStoragePermissionApi30(this@MainActivity) ){
                requestStoragePermissionApi30(this@MainActivity)
            }

        }else {

            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                EXTERNAL_STORAGE_PERMISSION_CODE)

        }
        setContent {
            RawParserTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column() {
                        Button(onClick = {
                            launchService()
                            launchService(ParsingActions.FULL_PARSING)
                        }) {
                            Text(text = "Launch Service")
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {

                            //GoogleMaps()
                            //}
                        }
                    }
                }
            }
        }
    }

    //
    fun launchService() {
        Intent(this, ParsingEventService::class.java).also {
            it.action = ParsingActions.START.name

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
        }
    }

    fun launchService(actions: ParsingActions) {
        Intent(this, ParsingEventService::class.java).also {
            it.action = actions.name

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
        }
    }
}

