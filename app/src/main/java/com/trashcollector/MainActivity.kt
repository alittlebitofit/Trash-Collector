package com.trashcollector

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL

const val TAG = "MainActivityTag"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        GlobalScope.launch(Dispatchers.IO) {
            // ...
            try {
                val url =
                    URL("https://docs.unity3d.com/Packages/com.unity.textmeshpro@3.2/manual/images/TMP_RichTextLineIndent.png")
                val bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                val image = InputImage.fromBitmap(bitmap, 0)


                val result = recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        // Task completed successfully
//                    Toast.makeText(baseContext, visionText.toString(), Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "visionText.text.toString(): ${visionText.text.toString()}")

                        val resultText = visionText.text
                        Toast.makeText(baseContext, resultText, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "resultText: $resultText")

                        for (block in visionText.textBlocks) {
                            val blockText = block.text
                            Log.d(TAG, "blockText: $blockText")
                            for (line in block.lines) {
                                val lineText = line.text
                                Log.d(TAG, "lineText: $lineText")
                                for (element in line.elements) {
                                    val elementText = element.text
                                    Log.d(TAG, "elementText: $elementText")
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // Task failed with an exception
                        Toast.makeText(baseContext, "on failure listener", Toast.LENGTH_SHORT)
                            .show()
                        e.printStackTrace()
                    }

                Log.d(TAG, "result: $result")
                Toast.makeText(baseContext, "result", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(baseContext, "failed", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

    }
}