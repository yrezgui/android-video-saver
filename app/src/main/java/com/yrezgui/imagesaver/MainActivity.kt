package com.yrezgui.imagesaver

import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val photoPicker = registerForActivityResult(PickVisualMedia()) { uri ->
        Log.d("PhotoPicker", "uri selected: $uri")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.save_video_dcim_folder).setOnClickListener {
            saveVideoInDcimFolder()
        }

        findViewById<Button>(R.id.save_video_movies_subfolder).setOnClickListener {
            saveVideoInMoviesSubFolder()
        }

        findViewById<Button>(R.id.open_photo_picker).setOnClickListener {
            openPhotoPicker()
        }
    }


    /**
     * Some apps save videos in the DCIM folder, but users are expecting only pictures & videos from
     * their device camera to be saved there
     */
    private fun saveVideoInDcimFolder() {
        lifecycleScope.launch(Dispatchers.IO) {
            assets.open(EXAMPLE_VIDEO_FILENAME).use { inputStream ->
                val targetFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val copiedFile = File(targetFolder, "${UUID.randomUUID()}.mp4")
                copiedFile.createNewFile()

                copiedFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                updateMediaStore(copiedFile.path, "video/mp4") {
                    val toast = Toast.makeText(
                        this@MainActivity,
                        "video saved in DCIM folder",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                }
            }
        }
    }

    /**
     * By saving videos in a dedicated subfolder, it makes it easier to discover them when going
     * through the device's library in the photo picker albums view
     */
    private fun saveVideoInMoviesSubFolder() {
        lifecycleScope.launch(Dispatchers.IO) {
            assets.open(EXAMPLE_VIDEO_FILENAME).use { inputStream ->
                val moviesFolder =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val targetFolder = File(moviesFolder, SUBFOLDER_NAME)
                // We create the subfolder if it doesn't already exist
                targetFolder.mkdir()

                val copiedFile = File(targetFolder, "${UUID.randomUUID()}.mp4")
                copiedFile.createNewFile()

                copiedFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                updateMediaStore(copiedFile.path, "video/mp4") {
                    val toast = Toast.makeText(
                        this@MainActivity,
                        "video saved in Movies subfolder",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()
                }
            }
        }
    }

    /**
     * Trigger a scan of a file to make sure its metadata are indexed inside MediaStore
     */
    private fun updateMediaStore(filepath: String, mimeType: String, callback: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            MediaScannerConnection.scanFile(
                this@MainActivity,
                arrayOf(filepath),
                arrayOf(mimeType)
            ) { _, scannedUri ->
                if (scannedUri == null) {
                    Log.e("MediaStore", "File $filepath could not be scanned")
                }
                callback()
            }
        }
    }

    private fun openPhotoPicker() {
        photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
    }

    companion object {
        const val EXAMPLE_VIDEO_FILENAME = "example.mp4"
        const val SUBFOLDER_NAME = "VideoSaver"
    }
}