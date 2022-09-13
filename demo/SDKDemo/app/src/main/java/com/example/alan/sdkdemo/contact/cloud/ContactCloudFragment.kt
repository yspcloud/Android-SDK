package com.example.alan.sdkdemo.contact.cloud

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alan.sdkdemo.R
import com.example.alan.sdkdemo.contact.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.lang.Runnable
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ContactCloudFragment : Fragment(), ItemClick {

    final val DEPARTMENT_ID = "departmentId"

    private var url: String? = null
    private var mContactList = mutableListOf<ContactBean>()
    private var peopleList = mutableListOf<ContactBean>()
    private lateinit var recyclerView: RecyclerView
    private var adapter: ContactAdapter? = null
    private var departmentId: String? = null
    private var scheduledExecutor: ScheduledExecutorService? = null
    private lateinit var viewStub: ViewStub
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            departmentId = it.getString(DEPARTMENT_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        url = "https://" + (activity as ContactActivity).vcrp.apiServer + "/rest/v1/app1/manager/sessions/"
        scheduledExecutor = Executors.newScheduledThreadPool(2)
        val rootView = inflater.inflate(R.layout.fragment_contact_cloud, container, false)
        rootView.findViewById<TextView>(R.id.search_edit).setOnClickListener {
            (activity as ContactActivity).showContactSearch()
        }
        viewStub = rootView.findViewById(R.id.view_stub)
        initRecycleView(rootView)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (TextUtils.isEmpty(departmentId)) {
                    departmentId = fetchHeadquartersIdAsync().await()
                }
                if (!TextUtils.isEmpty(departmentId)) {
                    val currentBody = fetchCurrentResourceAsync(departmentId!!).await()
                    parseBody(currentBody)
                    handleView()
                }
            } catch (e: Exception) {
                GlobalScope.launch(Dispatchers.Main) {
                    Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                }
            }

        }

        return rootView
    }

    private fun initRecycleView(rootView: View) {
        recyclerView = rootView.findViewById(R.id.recyclerView)
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        adapter = ContactAdapter(mContactList, activity, this)
        recyclerView.adapter = adapter

    }

    private suspend fun handleView() = withContext(Dispatchers.Main) {
        adapter?.notifyDataSetChanged()
        viewStub.visibility = if (mContactList.isEmpty()) {View.VISIBLE}else{View.GONE}
        scheduledExecutor?.scheduleAtFixedRate(runnable, 0, 10000, TimeUnit.MILLISECONDS)
    }

    /**
     * 获取总部门ID
     */
    private suspend fun fetchHeadquartersIdAsync(): Deferred<String> {
        val requestUrl = url + SPUtil.instance(activity!!).getSessionId() + "/departments/department_ids/search/"
        val requestBody = JSONObject()
        val arrayName = JSONArray()
        val arrayValue = JSONArray()
        arrayName.put("senior_id")
        arrayValue.put(-1)
        requestBody.put("filter_type", arrayName)
        requestBody.put("filter_value", arrayValue)
        val response = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), null).await()
        var headquartersId = ""
        if (!TextUtils.isEmpty(response)) {
            headquartersId = JSONObject(response).getJSONArray("department_ids")[0].toString()
        }
        return CompletableDeferred(headquartersId)
    }

    /**
     * 间隔5s刷新一次在线状态
     */
    private val runnable = Runnable {
        GlobalScope.launch(Dispatchers.IO) {
            getOnlineStatus()
        }

    }

    private suspend fun getOnlineStatus() {

        val requestUrl = url + SPUtil.instance(activity!!).getSessionId() + "/usrs/get_usr_online_status/"
        val requestBody = JSONObject();
        requestBody.put("cmdid", "get_usr_online_status")
        requestBody.put("usr_ids", setDataToIdList())
        try {
            val responseBody = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), null).await()
            parseStatus(responseBody)
        } catch (e: Exception) {
            Log.d("getOnlineStatus", ": " + e.message)
//            GlobalScope.launch(Dispatchers.Main) {
//                Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
//            }
        }

    }

    private suspend fun parseStatus(responseBody: String) = withContext(Dispatchers.Main){
        val body = JSONObject(responseBody)
        val usrIds: JSONArray = body.getJSONArray("usr_ids")
        val onLineStatus: JSONArray = body.getJSONArray("online_status")
        val map: MutableMap<String, Int> = HashMap()
        for (i in 0 until usrIds.length()) {
            map["" + usrIds.getInt(i)] = onLineStatus.getInt(i)
        }
        for (i in mContactList.indices) {
            if (mContactList[i].usrId != null && "" != mContactList[i].usrId && map.containsKey(mContactList[i].usrId)) {
                map[mContactList[i].usrId]?.let { mContactList[i].onLineStatus = it }
            }
        }
        adapter?.notifyDataSetChanged()
    }

    /**
     * 获取idlist
     */
    private fun setDataToIdList(): JSONArray? {
        val jsonArray = JSONArray()
        val list = mutableListOf<ContactBean>()
        list.addAll(mContactList)
        for (i in list.indices) {
            if (list[i].usrId != null && "" != list[i].usrId) {
                jsonArray.put(Integer.valueOf(list[i].usrId))
            }
        }
        return jsonArray
    }

    /**
     * 获取当前部门下的所有子部门与人员
     */
    private suspend fun fetchCurrentResourceAsync(currentId: String): Deferred<String> {
        val requestUrl = url + SPUtil.instance(activity!!).getSessionId() + "/departments/children_ids/"
        val requestBody = JSONObject()
        requestBody.put("cmdid", "get_department")
        requestBody.put("department_id", Integer.valueOf(currentId))
        requestBody.put("option", "account")
        val response = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), null).await()
        return CompletableDeferred(response)
    }

    /**
     * 处理当前组织数据
     */
    @Synchronized
    private suspend fun parseBody(body: String) {
        Log.d("response_body", "parseBody: $body")
        if (!TextUtils.isEmpty(body)) {
            val jsonObject = JSONObject(body)
            val departmentNames: JSONArray = jsonObject.optJSONArray("department_names")
            val departmentIds: JSONArray = jsonObject.optJSONArray("department_ids")
            val usrOnlineStatus: JSONArray = jsonObject.optJSONArray("usr_online_status")
            val usrIds: JSONArray = jsonObject.optJSONArray("usr_ids")
            val usrNickNames: JSONArray = jsonObject.optJSONArray("usr_nick_names")
            val usrLoginIds: JSONArray = jsonObject.optJSONArray("usr_login_ids")
            val usrIsEndpoints: JSONArray = jsonObject.optJSONArray("usr_is_endpoints")
            val usrCuid: JSONArray = jsonObject.optJSONArray("usr_cuid")
            val usrStatus: JSONArray = jsonObject.optJSONArray("usr_online_status")

            if (usrIds.length() > 0) {
                setPeople(usrOnlineStatus, usrIds, usrNickNames, usrLoginIds, usrIsEndpoints, usrCuid, usrStatus)
            }
            if (departmentIds.length() > 0) {
                setDepartment(departmentNames, departmentIds)
            }
        }
    }


    private fun setPeople(usrOnlineStatus: JSONArray, usrIds: JSONArray, usrNickNames: JSONArray, usrLoginIds: JSONArray,
                          usrIsEndpoints: JSONArray, usrCuid: JSONArray, usrStatus: JSONArray) {
        mContactList.clear()
        peopleList.clear()
        for (i in 0 until usrIds.length()) {
            val bean = ContactBean()
            bean.usrOnlineStatu = usrOnlineStatus[i].toString()
            bean.usrId = usrIds[i].toString()
            bean.usrNickName = usrNickNames[i].toString()
            bean.usrLoginId = usrLoginIds[i].toString()
            bean.usrIsEndpoint = usrIsEndpoints[i].toString()
            bean.usrCuid = usrCuid[i].toString()
            bean.onLineStatus = usrStatus.getInt(i)
            bean.isPeople = true
            if ("0" == bean.usrIsEndpoint) {
                peopleList.add(bean)
            } else {
                if ("2" != bean.usrIsEndpoint) {
                    mContactList.add(bean)
                }
            }
        }
        mContactList.addAll(peopleList)
    }

    private fun setDepartment(departmentNames: JSONArray, departmentIds: JSONArray) {
        for (i in 0 until departmentNames.length()) {
            val bean = ContactBean()
            try {
                bean.departmentId = departmentIds[i].toString()
                bean.departmentName = departmentNames[i].toString()
                bean.isPeople = false
                mContactList.add(bean)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }


    companion object {
        @JvmStatic
        fun newInstance(params: String) =
                ContactCloudFragment().apply {
                    arguments = Bundle().apply {
                        putString(DEPARTMENT_ID, params)
                    }
                }
    }


    override fun onItemClickListener(bean: ContactBean, isPeople: Boolean) {
        if (!isPeople) {
            (activity as ContactActivity).showNextDepartment(bean.departmentId)
        } else {
            (activity as ContactActivity).showContactDetail(bean)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scheduledExecutor?.shutdownNow()
    }
}