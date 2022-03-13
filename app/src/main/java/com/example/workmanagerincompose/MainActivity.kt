package com.example.workmanagerincompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.*
import coil.compose.rememberImagePainter
import com.example.workmanagerincompose.ui.theme.WorkManagerInComposeTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        val downloadRequest  = PeriodicWorkRequestBuilder<DownloadWorker>(
//            Duration.ofHours(5)
//        To get the work done in every 5 hour
//        )

        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()
            )
            .build()
        val colorFilterRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .build()
        val workManager =  WorkManager.getInstance(applicationContext)

        setContent {
            WorkManagerInComposeTheme {
                // A surface container using the 'background' color from the theme
                val workInfo = workManager
                    .getWorkInfosForUniqueWorkLiveData("download")
                    .observeAsState()
                    .value
                val downloadInfo = remember(key1 = workInfo) {
                    workInfo?.find { it.id == downloadRequest.id}
                }
                val filterInfo = remember(key1 = workInfo) {
                    workInfo?.find { it.id == colorFilterRequest.id}
                }

                val imageUri by derivedStateOf {
                    val downloadUri = downloadInfo?.outputData?.getString(WorkerKeys.IMAGE_URI)?.toUri()
                    val filterUri = filterInfo?.outputData?.getString(WorkerKeys.FILTER_URI)?.toUri()
                    filterUri ?: downloadUri
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    imageUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(
                                data = uri
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    Button(
                        onClick = {
                            workManager
                                .beginUniqueWork(
                                    "download",
                                    ExistingWorkPolicy.KEEP,
                                    downloadRequest
                                )
                                .then(colorFilterRequest)
                                .enqueue()
                        },
                        enabled = downloadInfo?.state != WorkInfo.State.RUNNING
                    ) {
                        Text(text = "Start download")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    when(downloadInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Downloading...")
                        WorkInfo.State.SUCCEEDED -> Text("Download succeeded")
                        WorkInfo.State.FAILED -> Text("Download failed")
                        WorkInfo.State.CANCELLED -> Text("Download cancelled")
                        WorkInfo.State.ENQUEUED -> Text("Download enqueued")
                        WorkInfo.State.BLOCKED -> Text("Download blocked")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    when(filterInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Applying filter...")
                        WorkInfo.State.SUCCEEDED -> Text("Filter succeeded")
                        WorkInfo.State.FAILED -> Text("Filter failed")
                        WorkInfo.State.CANCELLED -> Text("Filter cancelled")
                        WorkInfo.State.ENQUEUED -> Text("Filter enqueued")
                        WorkInfo.State.BLOCKED -> Text("Filter blocked")
                    }
                }

            }
        }
    }
}