package com.example.cameraandimages

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.content.res.Resources
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.widget.ImageView
import java.io.IOException
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val READ_EXTERNAL_STORAGE_REQ = 101
        private const val WRITE_EXTERNAL_STORAGE_REQ = 102
        private const val PICTURE_REQ = 103

        private const val SELECT_PICTURE = 104
        private const val TAKE_PICTURE = 105
    }

    private lateinit var imageView: ImageView
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)

        if(Build.VERSION.SDK_INT >= 23)
            checkForPermissions()

        findViewById<Button>(R.id.selectImageBtn).setOnClickListener { _ ->
            onSelectButtonClicked()
        }

        findViewById<Button>(R.id.captureImageBtn).setOnClickListener { _ ->
            onCaptureButtonClicked()
        }
    }

    private fun onSelectButtonClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_REQ)
        else
            startSelectPhotoIntent()
    }

    private fun onCaptureButtonClicked() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PICTURE_REQ)
        else
            startCameraIntent()
    }

    /**
     * This should only be called if permission has been granted.
     */
    private fun startCameraIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }

                val pathToFile = photoFile?.absolutePath

                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.cameraandimages.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, TAKE_PICTURE)
                }
            }
        }
    }

    private fun startSelectPhotoIntent() {
        val galleryIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, SELECT_PICTURE)
    }

    @TargetApi(23)
    private fun checkForPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQ)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            READ_EXTERNAL_STORAGE_REQ -> startSelectPhotoIntent()
            WRITE_EXTERNAL_STORAGE_REQ -> {}
            PICTURE_REQ -> startCameraIntent()
            else -> { /* Do Nothing */ }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            return
        }
        if (requestCode == SELECT_PICTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val contentURI = data.data
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                    val path = saveImage(bitmap)
                    Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
                    imageView.setImageBitmap(bitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Failed!", Toast.LENGTH_SHORT).show()
                }

            }

        } else if (requestCode == TAKE_PICTURE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val file = File(currentPhotoPath)
                val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.fromFile(file))
                if(bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    Toast.makeText(this, "Killt it bro.", Toast.LENGTH_SHORT).show()
                }

                // this will get the image THUMBNAIL.
                //val thumbnail = data.extras!!.get("data") as Bitmap
                //imageView.setImageBitmap(thumbnail)
                //Toast.makeText(this, "THUMBNAIL set.", Toast.LENGTH_SHORT).show()


                // val path = saveImage(thumbnail)
                // setSavedImageToImageView(path)
                // Toast.makeText(this@MainActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setSavedImageToImageView(path: String) {
        val targetWidth = imageView.width
        val targetHeight = imageView.height

//        val options = BitmapFactory.Options().apply {
//            // get the dimens of the bitmap.
//            inJustDecodeBounds = true
//            BitmapFactory.decodeFile(path, this)
//            val photoW: Int = outWidth
//            val photoH: Int = outHeight
//
//            // determine how much to scale down the image
//            //val scaleFactor: Int = Math.min(photoW / targetWidth, photoH / targetHeight)
//
//            // decode the image file into a bitmap sized to fill the view.
//            inJustDecodeBounds = false
//            //inSampleSize = scaleFactor
//        }
//
//        BitmapFactory.decodeFile(path, options)?.also { bitmap ->
//            imageView.setImageBitmap(bitmap)
//        }
        BitmapFactory.decodeFile(path).also { bitmap ->
            imageView.setImageBitmap(bitmap)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun saveImage(myBitmap: Bitmap): String {
        val bytes = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val wallpaperDirectory = File(
            Environment.getExternalStorageDirectory().toString() + getString(R.string.photo_file_path)
        )
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs()
        }

        try {
            val f = File(wallpaperDirectory, Calendar.getInstance().timeInMillis.toString() + ".jpg")
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(
                this,
                arrayOf(f.getPath()),
                arrayOf("image/jpeg"), null
            )
            fo.close()
            Log.d("TAG", "File Saved::---&gt;" + f.absolutePath)

            return f.absolutePath
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        return ""
    }

}
