package com.example.alan.sdkdemo.contact

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.alan.sdkdemo.R
import com.example.alan.sdkdemo.contact.cloud.ContactCloudFragment
import com.example.alan.sdkdemo.contact.cloud.ContactDetailFragment
import com.example.alan.sdkdemo.contact.cloud.SearchCloudFragment
import com.example.alan.sdkdemo.contact.common.ContactCommonFragment
import com.example.alan.sdkdemo.contact.common.ContactDetailCloudFragment
import com.example.alan.sdkdemo.contact.common.SearchCommonFragment
import com.vcrtc.VCRTCPreferences

class ContactActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var flContainer: FrameLayout
    lateinit var vcrp: VCRTCPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cantact)
        flContainer = findViewById(R.id.fl_content)
        vcrp = VCRTCPreferences(this)
        val pref = VCRTCPreferences(this)
        val homeFragment = if (pref.isShiTongPlatform) {
            ContactCloudFragment.newInstance("")
        } else {
            ContactCommonFragment.newInstance()
        }
        supportFragmentManager.beginTransaction()
                .add(R.id.fl_content, homeFragment, "first")
                .addToBackStack(null)
                .commitAllowingStateLoss()
        findViewById<ImageView>(R.id.iv_back).setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.iv_back -> {
                finish()
            }
        }
    }

    fun showNextDepartment(id: String) {
        val fragment: Fragment = if (vcrp.isShiTongPlatform){
            ContactCloudFragment.newInstance(id)
        }else {
            ContactCommonFragment.newInstance(id.toInt())
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.fl_content, fragment, "")
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    fun showContactDetail(contactBean: ContactBean) {
        val fragment: Fragment = if (vcrp.isShiTongPlatform){
            ContactDetailFragment.newInstance(contactBean.usrId)
        }else {
            ContactDetailCloudFragment.newInstance(contactBean)
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.fl_content, fragment, "")
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    fun showContactSearch() {
        val fragment: Fragment = if (vcrp.isShiTongPlatform){
            SearchCloudFragment.newInstance()
        }else {
            SearchCommonFragment.newInstance()
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.fl_content, fragment, "")
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }


    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1){
            finish()
        }else{
            super.onBackPressed()
        }
        Log.d("onBackPressed", "size: ${supportFragmentManager.backStackEntryCount}: ")
    }

}