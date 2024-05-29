package com.example.mycardgame

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mycardgame.models.BoardSize
import com.example.mycardgame.utils.BitmapScaler
import com.example.mycardgame.utils.EXTRA_BOARD_SIZE
import com.example.mycardgame.utils.EXTRA_GAME_NAME
import com.example.mycardgame.utils.isPermissionGranted
import com.example.mycardgame.utils.requestPermission
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateActivity"
        private const val PICK_PHOTO_CODE = 655
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }

    private lateinit var rvImagepicker: RecyclerView
    private lateinit var etGamename: EditText
    private lateinit var btnsave: Button
    private lateinit var pbupload: ProgressBar

    private lateinit var adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImageRequired = -1
    private val chossenImageUris = mutableListOf<Uri>()
    private val storage = Firebase.storage
    private val db = Firebase.firestore


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        rvImagepicker = findViewById(R.id.rvImagepicker)
        etGamename = findViewById(R.id.etGamename)
        btnsave = findViewById(R.id.btnsave)
        pbupload = findViewById(R.id.progressBar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImageRequired = boardSize.getNumPair()
        supportActionBar?.title = "Choose pics (0 / ${numImageRequired})"

        btnsave.setOnClickListener{
            saveDataToFirebase()
        }


        etGamename.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGamename.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                TODO("Not yet implemented")
            }

            override fun afterTextChanged(p0: Editable?) {
                btnsave.isEnabled = shouldEnableSaveButton()
            }

        })

        adapter = ImagePickerAdapter(this,chossenImageUris,boardSize,object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceholderClicked() {
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                    launchForPhoto()
                } else {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION,
                        READ_EXTERNAL_PHOTOS_CODE)

                }
            }
        })
        rvImagepicker.adapter = adapter
        rvImagepicker.setHasFixedSize(true)
        rvImagepicker.layoutManager = GridLayoutManager(this,boardSize.getwidth())
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchForPhoto()
            }
            else {
                Toast.makeText(this,"In order to create a custom Game, you need to provide access to your photos...",Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTO_CODE || resultCode !=Activity.RESULT_OK || data == null) {
            Log.w(TAG,"Did not get data back from the launched activity, user likely canceled the flow")
            return
        }
        val selectedUri: Uri? = data.data
        val clipData: ClipData? = data.clipData
        if(clipData != null) {
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem: ClipData.Item = clipData.getItemAt(i)
                if (chossenImageUris.size < numImageRequired) {
                    chossenImageUris.add(clipItem.uri)
                }
            }
        }else if(selectedUri != null){
            Log.i(TAG, "data: $selectedUri")
            chossenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose Pics (${chossenImageUris.size} / $numImageRequired)"
        btnsave.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        if(chossenImageUris.size != numImageRequired) {
            Toast.makeText(this,"Need Images to Proceed...",Toast.LENGTH_SHORT).show()
            return false
        }
        if(etGamename.text.isBlank() || etGamename.text.length < MIN_GAME_NAME_LENGTH) {
            Toast.makeText(this,"Fill the Game Name in the Given Format..",Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun launchForPhoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)
        startActivityForResult(Intent.createChooser(intent,"Choose Pics.."),PICK_PHOTO_CODE)
    }


    private fun handleImageUpload(gamename: String) {
        pbupload.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrl = mutableListOf<String>()
        for((index:Int, photoUri:Uri) in chossenImageUris.withIndex()) {
            val imageByteArray: ByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gamename/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference: StorageReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                .continueWithTask {photoUploadTask ->
                    Log.i(TAG,"Uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                    photoReference.downloadUrl
                }.addOnCompleteListener{ downloadUrlTask ->
                    if(!downloadUrlTask.isSuccessful) {
                        Log.e(TAG,"Exception with Firebase storage", downloadUrlTask.exception)
                        Toast.makeText(this,"Failed to upload image",Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if(didEncounterError) {
                        pbupload.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrl.add(downloadUrl)
                    pbupload.progress = uploadedImageUrl.size * 100 / chossenImageUris.size
                    Log.i(TAG,"Finished uploading $photoUri, num uploaded ${uploadedImageUrl}")
                    if(uploadedImageUrl.size == chossenImageUris.size) {
                        handleAllImagesUploaded(gamename,uploadedImageUrl)
                    }
                }
        }
    }

    private fun saveDataToFirebase() {
        val customgamename: String = etGamename.text.toString()
        Log.i(TAG, "saveDataToFirebase")
        btnsave.isEnabled = false

        db.collection("games").document(customgamename).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name taken already...")
                    .setMessage("A game already exists with the same name '$customgamename'. Please try another name ")
                    .setPositiveButton("OK", null)
                    .show()
                btnsave.isEnabled = true
            } else {
                handleImageUpload(customgamename)
            }
        }.addOnFailureListener{exception ->
            Log.e(TAG,"Encountered error while saving memory game",exception)
            Toast.makeText(this,"Encountered error while saving memory game",Toast.LENGTH_SHORT).show()
            btnsave.isEnabled = true
        }
    }

    private fun handleAllImagesUploaded(gamename: String, imageUrl: MutableList<String>) {
        db.collection("games").document(gamename)
            .set(mapOf("images" to imageUrl))
            .addOnCompleteListener{ gameCreationTask ->
                pbupload.visibility = View.GONE
                if(!gameCreationTask.isSuccessful) {
                    Log.e(TAG,"Exception with Game Creation")
                    Toast.makeText(this,"Failed Game Creation",Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG,"Successfully Created Game $gamename")
                AlertDialog.Builder(this)
                    .setTitle("Upload Completed! Let's play your game '$gamename'")
                    .setPositiveButton("OK") { _,_ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME,gamename)
                        setResult(Activity.RESULT_OK,resultData)
                        finish()
                    }.show()
            }
    }


    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap: Bitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG,60,byteOutputStream)
        return byteOutputStream.toByteArray()
    }
}


