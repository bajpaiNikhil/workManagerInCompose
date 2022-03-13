package com.example.workmanagerincompose

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.*
import com.example.workmanagerincompose.api.FileApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
class DownloadWorker(
    private val context: Context ,
    private var workerParameters: WorkerParameters
): CoroutineWorker(context , workerParameters) {


    override suspend fun doWork(): Result {
        startForegroundService()
        delay(5000L)
        val response = FileApi.instance.downloadImage()
        response.body()?.let { body ->
            return withContext(Dispatchers.IO){
                val file = File(context.cacheDir, "image.jpg")
                val outputStream = FileOutputStream(file)
                outputStream.use { stream->
                    try{
                        stream.write(body.bytes())
                    }catch (e : IOException){
                        return@withContext Result.failure(
                            workDataOf(
                                WorkerKeys.ERROR_MESSAGE to e.localizedMessage //towrite here
                            )
                        )
                    }
                }
                Result.success(
                    workDataOf(
                        WorkerKeys.IMAGE_URI to file.toUri().toString()
                    )
                )
            }
        }
        if(!response.isSuccessful){
            if(response.code().toString().startsWith("5")){
                return Result.retry()
            }
            return Result.failure(
                workDataOf(
                    WorkerKeys.ERROR_MESSAGE to "Network error"
                )
            )
        }
        return Result.failure(
            workDataOf(
                WorkerKeys.ERROR_MESSAGE to "Unknown Error"
            )
        )

    }
    private suspend fun startForegroundService(){
        setForeground(
            ForegroundInfo(
                Random.nextInt() ,
                NotificationCompat.Builder(context ,"download_channel")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentText("Downloading.....")
                    .setContentTitle("Download in progress")
                    .build()
            )
        )
    }
}