/*
//Date: 2018-06-27
//Title: Learn Intent
//Reference: https://www.raywenderlich.com/171071/android-intents-tutorial-kotlin
//
//Descripton:
//An intent is an abstract concept of work or functionality that can be performed by your app sometime in the future.
//In short, it’s something your app needs to do.
//1. Actions: This is what the intent needs to accomplish, such as dialing a telephone number, opening a URL,
//             or editing some data. An action is simply a string constant describing what is being accomplished.
//2. Data: This is the resource the intent operates on. It is expressed as a Uniform Resource Identifier or Uri object in Android
//
//Extra
//Extras are a form of key-value pairs that give your intent additional information to complete its action.
//The types of extras an intent can acknowledge and use change depending on the action
//
//Implicit Intents
//Implicit intents let Android developers give users the power of choice.
//An implicit Intent informs Android that it needs an app to handle the intent’s action when it starts.
//The Android system then compares the given intent against all apps installed on the device to see which
//ones can handle that action, and therefore process that intent. If more than one can handle the intent,
//the user is prompted to choose one:
//If only one app responds, the intent automatically takes the user to that app to perform the action.
If there are no apps to perform that action, then Android will return nothing, leaving you with a null
value that will cause your app to crash!
//
//Explicit Intents
//
//

 */

package com.raywenderlich.memeify

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.content.FileProvider.getUriForFile
import android.view.View
import kotlinx.android.synthetic.main.activity_take_picture.*
import java.io.File

class TakePictureActivity : Activity(), View.OnClickListener {

  private var pictureTaken: Boolean = false
  
  private var selectedPhotoPath: Uri? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_take_picture)

    pictureImageview.setOnClickListener(this)
    enterTextButton.setOnClickListener(this)

    checkReceivedIntent()
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.pictureImageview -> takePictureWithCamera()
      R.id.enterTextButton -> moveToNextScreen()
      else -> println("No case satisfied")
    }
  }

  //CLY
  private fun takePictureWithCamera() {
    // 1 declares an Intent object
    val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    // 2 - gettting temp file to store image
    val imagePath = File(filesDir, "images")
    val newFile = File(imagePath, "default_image.jpg")
    if (newFile.exists()) {
      newFile.delete()
    } else {
      newFile.parentFile.mkdirs()
    }

    //.fileprovider string. File Providers are a special way of providing files to your App and ensure
    // it is done in a safe and secure way.
    selectedPhotoPath = getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", newFile)

    // 3 adds an Extra to your newly created intent.
    //EXTRA_OUTPUT specifies where you should save the photo from the camera —
    // in this case, the Uri location of the empty file you created earlier.
    captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, selectedPhotoPath)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    } else {
      val clip = ClipData.newUri(contentResolver, "A photo", selectedPhotoPath)
      captureIntent.clipData = clip
      captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    //Start activity that can perform the action captureIntent
    startActivityForResult(captureIntent, TAKE_PHOTO_REQUEST_CODE)

  }

  //only executes when an activity started by startActivityForResult() in takePictureWithCamera()
  // has finished and returns to your app.
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    //RESULT_Ok  this is simply an Android constant that indicates successful execution.
    if (requestCode == TAKE_PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
      setImageViewWithImage()
    }
  }

//BitmapResizer is a helper class bundled with the starter project to make sure the Bitmap you
// retrieve from the camera is scaled to the correct size for your device’s screen.
//Although the device can scale the image for you, resizing it in this way is more memory efficient.
  private fun setImageViewWithImage() {
    val photoPath: Uri = selectedPhotoPath ?: return
    pictureImageview.post {
      val pictureBitmap = BitmapResizer.shrinkBitmap(
              this@TakePictureActivity,
              photoPath,
              pictureImageview.width,
              pictureImageview.height
      )
      pictureImageview.setImageBitmap(pictureBitmap)
    }
    lookingGoodTextView.visibility = View.VISIBLE
    pictureTaken = true
  }


  companion object {
    const private val MIME_TYPE_IMAGE = "image/"

    // identify your intent when it returns
    const private val TAKE_PHOTO_REQUEST_CODE = 1
  }

  private fun moveToNextScreen() {
    if (pictureTaken) {

      //create an intent for the next activity, and set up the necessary extras, using the constants
      val nextScreenIntent = Intent(this, EnterTextActivity::class.java).apply {
        putExtra(IMAGE_URI_KEY, selectedPhotoPath)
        putExtra(BITMAP_WIDTH, pictureImageview.width)
        putExtra(BITMAP_HEIGHT, pictureImageview.height)
      }

      startActivity(nextScreenIntent)
    } else {
      Toaster.show(this, R.string.select_a_picture)
    }
  }

  private fun checkReceivedIntent() {
    val imageReceivedIntent = intent
    val intentAction = imageReceivedIntent.action
    val intentType = imageReceivedIntent.type

    if (Intent.ACTION_SEND == intentAction && intentType != null) {
      if (intentType.startsWith(MIME_TYPE_IMAGE)) {
        selectedPhotoPath = imageReceivedIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        setImageViewWithImage()
      }
    }
  }

}
