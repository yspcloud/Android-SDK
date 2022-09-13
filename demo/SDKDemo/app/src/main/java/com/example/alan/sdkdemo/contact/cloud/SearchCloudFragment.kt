package com.example.alan.sdkdemo.contact.cloud

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.alan.sdkdemo.R
import com.example.alan.sdkdemo.contact.*
import com.example.alan.sdkdemo.util.SoftKeyboardUtil
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class SearchCloudFragment : Fragment(), ItemClick {
    private lateinit var recyclerView: RecyclerView
    private lateinit var editSearch: EditText
    private var url: String? = null
    private var userIdList = mutableListOf<Int>()
    private val contactBeanList = mutableListOf<ContactBean>()
    private val peopleList = mutableListOf<ContactBean>()
    private var adapter: ContactAdapter? = null
    private lateinit var viewStub: ViewStub

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        url = "https://" + (activity as ContactActivity).vcrp.apiServer + "/rest/v1/app1/manager/sessions/"
        val rootView = inflater.inflate(R.layout.fragment_search_cloud, container, false)

        recyclerView = rootView.findViewById(R.id.recyclerView)
        editSearch = rootView.findViewById(R.id.search_edit)
        viewStub = rootView.findViewById(R.id.view_stub)
        initRecycler()
        SoftKeyboardUtil.forceOpenSoftKeyboard(activity, editSearch)
        editSearch.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_GO && !TextUtils.isEmpty(editSearch.text.toString())) {
                SoftKeyboardUtil.hideSoftKeyboard(activity)
                handled = true
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val responseBody = fetchSearchPeopleAsync(editSearch.text.toString()).await()
                        val hasPeople = parseBodyAsync(responseBody).await()
                        if (hasPeople) {
                            val responseDetail = fetchSearchDetailAsync().await()
                            parseDetail(responseDetail)
                        }
                        refreshUI()


                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            handled
        }

        return rootView
    }

    private suspend fun refreshUI() = withContext(Dispatchers.Main) {
        adapter?.notifyDataSetChanged()
        viewStub.visibility = if (contactBeanList.isEmpty()) {View.VISIBLE}else{View.GONE}


    }

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        adapter = ContactAdapter(contactBeanList, activity, this)
        recyclerView.adapter = adapter
    }

    private fun parseDetail(responseDetail: String) {
        val jsonObject = JSONObject(responseDetail)
        val nick_names: JSONArray = jsonObject.getJSONArray("nick_names")
        val usr_ids: JSONArray = jsonObject.getJSONArray("usr_ids")
        val is_endpoints: JSONArray = jsonObject.getJSONArray("is_endpoints")
        val login_ids: JSONArray = jsonObject.getJSONArray("login_ids")
        val current_department: JSONArray = jsonObject.getJSONArray("current_department")
        val duties: JSONArray = jsonObject.getJSONArray("duties")
        val departNames: JSONArray = jsonObject.getJSONArray("belong_to_dep_names")
        val onLineStatus: JSONArray = jsonObject.optJSONArray("online_status")
        getContactListP(nick_names, usr_ids, is_endpoints, login_ids, current_department, departNames, duties, onLineStatus)

    }

    private fun parseBodyAsync(responseBody: String): Deferred<Boolean> {
        val json = JSONObject(responseBody)
        val array = json.optJSONArray("usr_ids")
        userIdList = getIdList(array)
        return if (userIdList.size == 0) {
            GlobalScope.launch(Dispatchers.Main) {
//                Toast.makeText(activity, "查无此人", Toast.LENGTH_SHORT).show()
            }
            CompletableDeferred(false)
        } else {
            CompletableDeferred(true)
        }


    }

    /**
     * 查询人的id
     */
    private suspend fun fetchSearchPeopleAsync(name: String): Deferred<String> {
        val requestUrl = url + SPUtil.instance(activity!!).getSessionId() + "/usrs/usr_id/search/"
        val requestBody = JSONObject()
        val typeArray = JSONArray()
        val valueArray = JSONArray()
        typeArray.put("nick_name")
        valueArray.put(name)
        requestBody.put("cmdid", "search_usr")
        requestBody.put("filter_type", typeArray)
        requestBody.put("filter_value", valueArray)

        val responseBody = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), null).await()

        return CompletableDeferred(responseBody)
    }

    private suspend fun fetchSearchDetailAsync(): Deferred<String> {
        val requestUrl = url + SPUtil.instance(activity!!).getSessionId() + "/usrs/usr_ids/"
        val body = JSONObject()
        val typeArray = JSONArray()
        val valueArray = JSONArray()
        putIdParams(userIdList, typeArray, valueArray)
        body.put("cmdid", "subscribe_usr")
        body.put("usr_ids", typeArray)
        body.put("last_modify_dtms", valueArray)
        val responseBody = HttpUtil.doPostAsync(requestUrl, body.toString(), null).await()
        return CompletableDeferred(responseBody)
    }

    private fun putIdParams(list: List<Int>, arrayId: JSONArray, time: JSONArray) {
        if (list.isNotEmpty()) {
            for (i in list.indices) {
                arrayId.put(list[i])
                time.put(0)
            }
        }
    }

    private fun getIdList(array: JSONArray?): MutableList<Int> {
        val list: MutableList<Int> = ArrayList()
        if (array != null && array.length() > 0) {
            for (i in 0 until array.length()) {
                try {
                    list.add(array.getInt(i))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
        return list
    }

    /**
     * 获取人的列表信息
     *
     * @param nick_names
     * @param usr_ids
     */
    private fun getContactListP(nick_names: JSONArray, usr_ids: JSONArray, is_endpoints: JSONArray, login_ids: JSONArray, current_department: JSONArray, departNames: JSONArray, duties: JSONArray, onLineStatus: JSONArray) {
        peopleList.clear()
        contactBeanList.clear()
        for (i in 0 until nick_names.length()) {
            val bean = ContactBean()
            bean.isPeople = true
            try {
                bean.usrNickName = nick_names.getString(i)
                bean.usrId = usr_ids.getInt(i).toString()
                bean.usrIsEndpoint = is_endpoints[i].toString()
                bean.usrLoginId = login_ids.getString(i)
                bean.duties = duties.getString(i)
                bean.onLineStatus = onLineStatus.getInt(i)
                val list: MutableList<Int> = ArrayList()
                for (j in 0 until current_department.getJSONArray(i).length()) {
                    list.add(current_department.getJSONArray(i).getInt(j))
                }
                bean.departmentIds = list
                val jsonArrayAnother = departNames.getJSONArray(i)
                val departListAnother: MutableList<List<String>> = ArrayList()
                for (k in 0 until jsonArrayAnother.length()) {
                    val content = jsonArrayAnother.getJSONArray(k)
                    val departs: MutableList<String> = ArrayList()
                    for (q in 0 until content.length()) {
                        departs.add(content.getString(q))
                    }
                    departListAnother.add(departs)
                }
                bean.departListAnother = departListAnother
                //                }
                if ("0" == bean.usrIsEndpoint) {
                    peopleList.add(bean)
                } else {
                    if ("2" != bean.usrIsEndpoint) {
                        contactBeanList.add(bean)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (contactBeanList.size > 0) {
            contactBeanList[0].isShowHead = true
        }
        contactBeanList.addAll(peopleList)
    }


    companion object {

        @JvmStatic
        fun newInstance() =
                SearchCloudFragment().apply {
                    arguments = Bundle().apply {

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
}