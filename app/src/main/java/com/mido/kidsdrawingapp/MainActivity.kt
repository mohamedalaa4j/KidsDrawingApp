package com.mido.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint : ImageButton? = null
    var customProgressDialog : Dialog? = null
    var imageSaved = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        drawing_view.setSizeForBrush(10.toFloat())

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener {
            if (isReadStorageAllow()){

                val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                resultLauncher.launch(pickPhotoIntent)

            }else {
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }
        ib_save.setOnClickListener {
            if (isReadStorageAllow()){
                showProgressDialog()
                lifecycleScope.launch{

                    val flDrawingView : FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile(getBitmapFromView(flDrawingView))

                }
            }
        }

        ib_share.setOnClickListener {
            if (imageSaved == 0) {
                Toast.makeText(this, "Save the image first", Toast.LENGTH_SHORT).show()
            }
        }

    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            iv_background.visibility = View.VISIBLE
            if (data != null) {
                iv_background.setImageURI(data.data)
            }
            println("OK")
        }
    }

    private fun openActivityForResult() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }


    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size : ")

        // small brush button functionality
        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener {

            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }

        // medium brush button functionality
        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener {

            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }

        // large brush button functionality
        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener {

            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()

    }

    fun paintClicked(view : View){
        if (view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val tagColor = imageButton.tag.toString()
            drawing_view.setColor(tagColor)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            mImageButtonCurrentPaint = view

        }

    }

    fun requestStoragePermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this,"You already have the permission ", Toast.LENGTH_LONG).show()
        }else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission granted now you can read the storage files ", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"Oops you just denied the permission", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun isReadStorageAllow(): Boolean{
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) :Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return  returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){

            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File saved successfully : $result", Toast.LENGTH_SHORT ).show()
                            imageSaved = 1
                            ib_share.setOnClickListener {
                                if (imageSaved == 1) {
                                    shareImage(result)
                                    imageSaved = 0

                                }else{ Toast.makeText(this@MainActivity, "Save the image first", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_SHORT ).show()

                        }
                    }

                }
                catch (e : Exception){
                     result = ""
                    e.printStackTrace()
                }
            }
        }

        return result
    }



    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if (customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "share"))
        }

    }
}