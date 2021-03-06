/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trashcollector

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.work.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.trashcollector.workers.BlurWorker
import com.trashcollector.workers.CleanupWorker
import com.trashcollector.workers.SaveImageToFileWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL

const val TAG_BlurViewModel = "BlurViewModelActivity1"
class BlurViewModel(application: Application) : AndroidViewModel(application) {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null
    private val workManager = WorkManager.getInstance(application)
    internal val outputWorkInfos: LiveData<List<WorkInfo>>
    internal val progressWorkInfoItems: LiveData<List<WorkInfo>>


    init {
        // This transformation makes sure that whenever the current work Id changes the WorkInfo
        // the UI is listening to changes
        outputWorkInfos = workManager.getWorkInfosByTagLiveData(TAG_OUTPUT)
        progressWorkInfoItems = workManager.getWorkInfosByTagLiveData(TAG_PROGRESS)
    }

    internal fun cancelWork() {
        workManager.cancelUniqueWork(IMAGE_MANIPULATION_WORK_NAME)
    }


    fun textExtractor(context: Context, resourceUri: String?): Text? {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        var returnValue: Text? = null
        GlobalScope.launch(Dispatchers.IO) {
            // ...
            try {
                val url =
                    URL("https://docs.unity3d.com/Packages/com.unity.textmeshpro@3.2/manual/images/TMP_RichTextLineIndent.png")
//                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                val resolver = context.contentResolver
                val bitmap = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(resourceUri)))
                val image = InputImage.fromBitmap(bitmap, 0)


                val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->

                        returnValue = visionText
                        // Task completed successfully
//                    Toast.makeText(baseContext, visionText.toString(), Toast.LENGTH_SHORT).show()
                        Log.d(TAG_BlurViewModel, "visionText.text.toString(): ${visionText.text.toString()}")

                        val resultText = visionText.text
                        Toast.makeText(context, resultText, Toast.LENGTH_SHORT).show()
//                        Log.d(TAG_BlurViewModel, "resultText: $resultText")

                        for (block in visionText.textBlocks) {
                            val blockText = block.text
//                            Log.d(TAG_BlurViewModel, "blockText: $blockText")
                            for (line in block.lines) {
                                val lineText = line.text
//                                Log.d(TAG_BlurViewModel, "lineText: $lineText")
                                for (element in line.elements) {
                                    val elementText = element.text
//                                    Log.d(TAG_BlurViewModel, "elementText: $elementText")
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
//                        Toast.makeText(context, "on failure listener", Toast.LENGTH_SHORT)
//                            .show()
                        e.printStackTrace()
                    }

                Log.d(TAG_BlurViewModel, "result: $result")
//                Toast.makeText(context, "result", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
//                Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        return returnValue
    }


    /**
     * Creates the input data bundle which includes the Uri to operate on
     * @return Data which contains the Image Uri as a String
     */
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        imageUri?.let {
            builder.putString(KEY_IMAGE_URI, imageUri.toString())
        }
        return builder.build()
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        // Add WorkRequest to Cleanup temporary images
        var continuation = workManager
            .beginUniqueWork(
                IMAGE_MANIPULATION_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequest.from(CleanupWorker::class.java)
            )

        // Add WorkRequests to blur the image the number of times requested
        for (i in 0 until blurLevel) {
            val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()

            // Input the Uri if this is the first blur operation
            // After the first blur operation the input will be the output of previous
            // blur operations.
            if (i == 0) {
                blurBuilder.setInputData(createInputDataForUri())
            }

            blurBuilder.addTag(TAG_PROGRESS)
            continuation = continuation.then(blurBuilder.build())
        }

        // Create charging constraint
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()

        // Add WorkRequest to save the image to the filesystem
        val save = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .setConstraints(constraints)
            .addTag(TAG_OUTPUT)
            .build()
        continuation = continuation.then(save)

        // Actually start the work
        continuation.enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    /**
     * Setters
     */
    internal fun setImageUri(uri: String?) {
        imageUri = uriOrNull(uri)
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }
}
