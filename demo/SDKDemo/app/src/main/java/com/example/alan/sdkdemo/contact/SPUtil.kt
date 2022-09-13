package com.example.alan.sdkdemo.contact

import android.content.Context
import android.content.SharedPreferences

class SPUtil {

    companion object{
        private var sp: SharedPreferences? = null
        fun instance(context: Context): SPUtil {
            if (sp == null) {
                sp = context.getSharedPreferences("data", Context.MODE_PRIVATE)
            }
            return SPUtil()
        }
    }

    fun getSessionId(): String {
        return sp?.getString("sessionId", "")!!
    }

    fun setSessionId(sessionId: String){
        val edit = sp!!.edit()
        edit.putString("sessionId", sessionId)
        edit.apply()
    }

    fun isLogin():Boolean {
        return sp?.getBoolean("login", false)!!
    }

    fun setLogin(isLogin: Boolean){
        val edit = sp!!.edit()
        edit.putBoolean("login", isLogin)
        edit.apply()
    }

    fun setModel(model: String){
        val edit = sp!!.edit()
        edit.putString("appMode", model)
        edit.apply()
    }

    fun getModel(): String = sp?.getString("appMode", "")!!

    fun setCompanyId(companyID: String){
        sp!!.edit().putString("companyId", companyID).apply()
    }
    fun getCompanyId(): String = sp?.getString("companyId", "")!!
}