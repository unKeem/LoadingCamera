package com.example.loadingcamera

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.loadingcamera.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var filePath: String

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //카메라 인텐트 요청했을때 값을 돌려줄 콜백함수
        val requestCameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    try {
                        val calculateRatio = calculateInSampleSize(
                            Uri.fromFile(File(filePath)),
                            resources.getDimensionPixelSize(R.dimen.imgSize),
                            resources.getDimensionPixelSize(R.dimen.imgSize)
                        )

                        //옵션을 기준으로 비트맵 생성하기
                        val options = BitmapFactory.Options()
                        options.inSampleSize = calculateRatio
                        val bitamp = BitmapFactory.decodeFile(filePath, options)

                        //사진 회전정보 가져오기
                        val orientation =
                            getOrientationOfImage(Uri.fromFile(File(filePath))).toFloat()
                        //회전된 사진을 원위치로 비트맵돌리기
                        val newBitmap = getRotatedBitmap(bitamp, orientation)

                        newBitmap?.let {
                            binding.ivPicture.setImageBitmap(newBitmap)
                        } ?: let {
                            Log.d("loadingcamera", "사진캡쳐성공")
                        }
                    } catch (e: Exception) {
                        Log.d("loadingcamera", "사진캡쳐실패")
                    }
                } else {
                    Log.d("loadingcamera", "카메라앱에서 사진캡쳐 실패함")
                }
            }


        binding.btnCamera.setOnClickListener {
            //외장메모리 앱폴더에 파일명생성
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            //외장메모리 앱폴더 위치를 가져옴
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            //외장메모리 앱폴더 위치 파일경로까지 설정된 파일생성
            val file = File.createTempFile("__jpg_${timeStamp}_", ".jpg", storageDir)
            filePath = file.absolutePath

            //내가 지정한 파일을 파일프로바이더에 uri경로 만들어줌
            val photoUri =
                FileProvider.getUriForFile(this, "com.example.loadingcamera.fileprovider", file)

            //카메라에게 인텐트 요청함. (내가 파일프로바이더를 통해서 공유된 위치 파일에 사진을 캡쳐해서 저장해달라고)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            requestCameraLauncher.launch(intent)

        }
    }

    //이미지 비율계산
    fun calculateInSampleSize(fileUri: Uri, reqWidth: Int, reqHeight: Int): Int {
        //이미지 옵션(외부 이미지 사이즈 축소)
        val options = BitmapFactory.Options()
        //이미지 정보만을 가져와서 실제사이즈와 요청사이즈를 계산해서 비율조정하기
        options.inJustDecodeBounds = true

        try {
            var inputStream = contentResolver.openInputStream(fileUri)
            //실제이미지 정보를 options에 저장함
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()
            inputStream = null

        } catch (e: Exception) {
            Log.d("pictureprovide", "${e.printStackTrace()}")
        }
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // 이미지 회전 정보 가져오기
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getOrientationOfImage(uri: Uri): Int {
        // uri -> inputStream
        val inputStream = contentResolver.openInputStream(uri)
        val exif: ExifInterface? = try {
            ExifInterface(inputStream!!)
        } catch (e: IOException) {
            e.printStackTrace()
            return -1
        }
        inputStream.close()

        // 회전된 각도 알아내기
        val orientation =
            exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation != -1) {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> return 90
                ExifInterface.ORIENTATION_ROTATE_180 -> return 180
                ExifInterface.ORIENTATION_ROTATE_270 -> return 270
            }
        }
        return 0
    }

    // 이미지 회전하기
    @Throws(Exception::class)
    private fun getRotatedBitmap(bitmap: Bitmap?, degrees: Float): Bitmap? {
        if (bitmap == null) return null
        if (degrees == 0F) return bitmap
        val m = Matrix()
        m.setRotate(degrees, bitmap.width.toFloat() / 2, bitmap.height.toFloat() / 2)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

}