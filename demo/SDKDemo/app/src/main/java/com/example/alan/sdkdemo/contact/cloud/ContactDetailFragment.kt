package com.example.alan.sdkdemo.contact.cloud

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.alan.sdkdemo.R
import com.example.alan.sdkdemo.contact.ContactActivity
import com.example.alan.sdkdemo.contact.HttpUtil
import com.example.alan.sdkdemo.contact.SPUtil
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class ContactDetailFragment : Fragment() {
    val ID = "id"
    private lateinit var tvName: TextView
    private lateinit var tvDepartment: TextView
    private lateinit var tvPosition: TextView
    private lateinit var tvNumber: TextView
    private var url: String? = null
    var contactId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            contactId = it.getString(ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        url = "https://" + (activity as ContactActivity).vcrp.apiServer + "/rest/v1/app1/manager/sessions/"
        val rootView = inflater.inflate(R.layout.fragment_contact_detail, container, false)
        rootView.apply {
            tvDepartment = findViewById(R.id.tv_depart)
            tvName = findViewById(R.id.tv_name)
            tvPosition = findViewById(R.id.tv_position)
            tvNumber = findViewById(R.id.tv_number)

        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                contactId?.let {
                    val responseBody = fetchContactInfoAsync(it).await()
                    if (!TextUtils.isEmpty(responseBody)) {
                        parseBody(responseBody)
                    }
                }
            } catch (e: IllegalStateException) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }




        return rootView
    }

    private suspend fun parseBody(responseBody: String) {
        val jsonObject = JSONObject(responseBody)
        val nick_name: JSONArray? = jsonObject.optJSONArray("nick_names")
        val dutie: JSONArray? = jsonObject.optJSONArray("duties")
        val departName: JSONArray? = jsonObject.optJSONArray("belong_to_dep_names")
        val meetNum: JSONArray? = jsonObject.optJSONArray("priv_room_cuids")
        var nickName = ""
        var depart = ""
        var duty = ""
        var meet = ""
        if (nick_name != null && nick_name.length() > 0) {
            nickName = nick_name[0].toString()
        }
        if (departName != null && departName.length() > 0) {
            depart = departName[0].toString()
        }
        if (dutie != null && dutie.length() > 0) {
            duty = dutie[0].toString()
        }
        if (meetNum != null && meetNum.length() > 0) {
            meet = meetNum[0].toString()
        }

        showUI(nickName, depart, duty, meet)
    }

    private suspend fun showUI(name: String, department: String, position: String, meetNum: String) = withContext(Dispatchers.Main) {
        tvName.text = name
        tvDepartment.text = department
        tvPosition.text = position
        tvNumber.text = meetNum

    }

    private suspend fun fetchContactInfoAsync(id: String): Deferred<String> {
        val requestUrl = url + SPUtil.instance(activity!!).getSessionId() + "/usrs/usr_ids/"
        val body = JSONObject()
        val typeArray = JSONArray()
        val valueArray = JSONArray()
        typeArray.put(Integer.valueOf(id))
        valueArray.put(0)
        body.put("cmdid", "subscribe_usr")
        body.put("usr_ids", typeArray)
        body.put("last_modify_dtms", valueArray)
        val responseBody = HttpUtil.doPostAsync(requestUrl, body.toString(), null).await()
        return CompletableDeferred(responseBody)
    }

    companion object {
        @JvmStatic
        fun newInstance(id: String) =
                ContactDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString(ID, id)
                    }
                }
    }
}