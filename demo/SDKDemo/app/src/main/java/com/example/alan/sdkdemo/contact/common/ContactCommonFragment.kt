package com.example.alan.sdkdemo.contact.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.alan.sdkdemo.R
import com.example.alan.sdkdemo.contact.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.set

class ContactCommonFragment : Fragment(), ItemClick {
    private val dataList = mutableListOf<ContactBean>()
    private val peopleList = mutableListOf<ContactBean>()
    private lateinit var recyclerView: RecyclerView
    private var adapter: MyItemRecyclerViewAdapter? = null
    private val url = "https://cs.zijingcloud.com:443/api/"
    private var departmentId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            departmentId = it.getInt("department_id", 0);
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_contact_common_list, container, false)
        initRecyclerView(view)
        view.findViewById<ConstraintLayout>(R.id.con_search).setOnClickListener {
            (activity as ContactActivity).showContactSearch()
        }
        when (SPUtil.instance(activity!!).getModel()) {
            "cloud_conference" -> {
                // 云会议室模型下通讯录
                handleCloudMeetContact()
            }
            "virtual_mcu" -> {
                // 虚拟MCU通讯录
                handleMCUContact()
            }
            else -> {

            }
        }
        return view
    }

    /**
     * 云会议室模型
     */
    private fun handleCloudMeetContact() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (departmentId == 0) {
                    val headquarters = fetchCloudHeadquartersAsync(0.toString()).await()
                    departmentId = headquarters
                }
                fetchPeopleAsync(departmentId).await()
                fetchDepartmentAsync(departmentId).await()
                refreshUI()
            } catch (e: Exception) {

            }
        }
    }

    /**
     * 虚拟MCU模型
     */
    private fun handleMCUContact() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                fetchPeopleAsync(departmentId).await()
                fetchDepartmentAsync(departmentId).await()
                refreshUI()
            } catch (e: Exception) {

            }
        }
    }


    /**
     * 获取总部门id（云会议室模型）
     */
    private suspend fun fetchCloudHeadquartersAsync(departId: String): Deferred<Int> {
        val requestUrl = url + "v3/app/department/search.do"
        val header = HashMap<String, String>()
        header["sessionId"] = SPUtil.instance(activity!!).getSessionId()
        val requestBody = JSONObject()
        requestBody.put("companyId", SPUtil.instance(activity!!).getCompanyId())
        requestBody.put("departmentId", departId)
        val responseBody = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), header).await()
        val json = JSONObject(responseBody)
        val data = json.optJSONArray("data")
        val id = (data[0] as JSONObject).optInt("id", -1)
        if (id != -1) {
            return CompletableDeferred(id)
        } else {
            throw Exception("获取失败")
        }
    }

    private suspend fun fetchPeopleAsync(departmentId: Int): Deferred<Boolean> {
        val requestUrl = url + "v3/app/user/search.do"
        val header = HashMap<String, String>()
        header["sessionId"] = SPUtil.instance(activity!!).getSessionId()
        val requestBody = JSONObject()
        requestBody.put("companyId", SPUtil.instance(activity!!).getCompanyId())
        requestBody.put("departmentId", departmentId)
        val responseBody = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), header).await()
        val jsonObject = JSONObject(responseBody)
        val data = jsonObject.optJSONArray("data");
        if (data != null && data.length() != 0) {
            setPeopleAll(data)
        }

        return CompletableDeferred(true)
    }

    /**
     * 获取该部门下所有部门
     */
    private suspend fun fetchDepartmentAsync(departmentId: Int): Deferred<Boolean> {
        val requestUrl = url + "v3/app/department/search.do"
        val header = HashMap<String, String>()
        header["sessionId"] = SPUtil.instance(activity!!).getSessionId()
        val requestBody = JSONObject()
        requestBody.put("companyId", SPUtil.instance(activity!!).getCompanyId())
        requestBody.put("departmentId", departmentId)
        val responseBody = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), header).await()
        val json = JSONObject(responseBody)
        val data = json.optJSONArray("data")
        if (data != null && data.length() != 0) {
            setDepartment(data)
        }
        return CompletableDeferred(true)
    }


    private suspend fun refreshUI() = withContext(Dispatchers.Main) {
        adapter?.notifyDataSetChanged()
    }

    private fun setDepartment(data: JSONArray) {
        for (i in 0 until data.length()) {
            val bean = ContactBean()
            try {
                val content = data.getJSONObject(i)
                bean.isPeople = false
                bean.departmentId = content.getInt("id").toString()
                bean.departmentName = content.getString("name")
                val nameList = content.optJSONArray("namePath")
                val tempList: MutableList<String> = ArrayList()
                for (j in 0 until nameList.length()) {
                    tempList.add(nameList.optString(j))
                }
                bean.departFatherList = tempList
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            dataList.add(bean)
        }
    }

    private fun setPeopleAll(data: JSONArray) {
        peopleList.clear()
        dataList.clear()
        for (i in 0 until data.length()) {
            val bean = ContactBean()
            try {
                val content = data.getJSONObject(i)
                bean.isPeople = true
                bean.usrId = content.getInt("subscriberId").toString()
                bean.usrCuid = content.getString("account")
                bean.usrIsEndpoint = content.getString("type")
                bean.usrNickName = content.getString("trueName")
                bean.sipKey = content.optString("sipKey")
                bean.duties = content.optString("position")
                bean.departmentId = content.optInt("departmentId").toString()
                val nameList = content.optJSONArray("departmentNamePath")
                val tempList: MutableList<String> = ArrayList()
                for (j in 0 until nameList.length()) {
                    tempList.add(nameList.optString(j))
                }
                bean.departFatherList = tempList
                if (!bean.usrIsEndpoint.equals("terminal")) {
                    peopleList.add(bean)
                } else {
                    dataList.add(bean)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        dataList.addAll(peopleList)
    }


    private fun initRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.list)
        adapter = MyItemRecyclerViewAdapter(dataList, this)
        recyclerView.adapter = adapter
        adapter?.notifyDataSetChanged()
    }

    companion object {
        @JvmStatic
        fun newInstance(departmentId: Int = 0) =
                ContactCommonFragment().apply {
                    arguments = Bundle().apply {
                        putInt("department_id", departmentId)
                    }
                }
    }

    override fun onItemClickListener(bean: ContactBean, isPeople: Boolean) {
        if (isPeople) {
            (activity as ContactActivity).showContactDetail(bean)
        } else {
            (activity as ContactActivity).showNextDepartment(bean.departmentId)
        }
    }
}