package com.qt.media.test.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.qt.media.test.databinding.ActivityMainBinding


/**
 *
 * DESC：
 * Created on 2021/10/26
 *
 */
class MainActivity : AppCompatActivity() {

    //鉴权
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO
    )

    /**
     * 视图
     */
    private lateinit var mBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initData()
        bindListener()
    }

    private fun initData() {
        checkSelfPermission(permissions)
    }

    private fun bindListener() {
        mBinding.btnVideo.setOnClickListener {
            startActivity(Intent(this,VideoEncoderActivity::class.java))
        }

        mBinding.btnWebp.setOnClickListener {
            startActivity(Intent(this,WebpEncoderActivity::class.java))
        }
        mBinding.btnGif.setOnClickListener {
            startActivity(Intent(this,GifEncoderActivity::class.java))
        }
        mBinding.btnImage.setOnClickListener {
            startActivity(Intent(this,ImageEncoderActivity::class.java))
        }
    }


    /**
     * 检查是否有劝降没有通过 true均有权限 false 无权限
     * @return
     */
    fun checkSelfPermission(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions, 10001)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var hasPermissionDismiss = false //有权限没有通过
        for (element in grantResults) {
            if (element == -1) {
                hasPermissionDismiss = true
            }
        }
        //如果有权限没有被允许
        if (hasPermissionDismiss) {
            Toast.makeText(this, "缺少必要权限，可能导致应用功能无法使用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == 100) {
            mBinding.root.postDelayed({
            }, 500)
        }
    }
}