package com.example.alan.sdkdemo.contact.common

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class SearchCommonFragment : Fragment() , ItemClick{
    private lateinit var recyclerView: RecyclerView
    private lateinit var editSearch: EditText
    private val contactBeanList = mutableListOf<ContactBean>()
    private val peopleList = mutableListOf<ContactBean>()
    private var adapter: MyItemRecyclerViewAdapter? = null
    private val url = "https://cs.zijingcloud.com:443/api/"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_search_cloud, container, false)

        recyclerView = rootView.findViewById(R.id.recyclerView)
        editSearch = rootView.findViewById(R.id.search_edit)
        initRecycler()
        SoftKeyboardUtil.forceOpenSoftKeyboard(activity, editSearch)
        editSearch.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_GO && !TextUtils.isEmpty(editSearch.text.toString())) {
                SoftKeyboardUtil.hideSoftKeyboard(activity)
                handled = true
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        fetchUserAsync(editSearch.text.toString()).await()
                        refreshUI()

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main){
                            Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            handled
        }
        return rootView
    }




    private suspend fun fetchUserAsync(name: String): Deferred<Boolean>{
        val requestUrl = url + "v3/app/user/search.do"
        val requestBody = JSONObject()
        requestBody.put("companyId", SPUtil.instance(activity!!).getCompanyId())
        requestBody.put("departmentId", 0)
        requestBody.put("trueName", name)
        val response = HttpUtil.doPostAsync(requestUrl, requestBody.toString(), null).await()
        setPeopleAll(JSONObject(response).optJSONArray("data"))
        return CompletableDeferred(true)
    }


    private suspend fun refreshUI() = withContext(Dispatchers.Main){
        adapter?.notifyDataSetChanged()
    }

    private fun setPeopleAll(data: JSONArray) {
        contactBeanList.clear()
        peopleList.clear()
        for (i in 0 until data.length()) {
            val bean = ContactBean()
            try {
                val content = data.getJSONObject(i)
                bean.isPeople = true
                bean.usrId = content.getInt("subscriberId").toString()
                bean.usrCuid = content.getString("account")
                bean.usrIsEndpoint = content.getString("type")
                bean.usrNickName = content.getString("trueName")
                bean.departmentId = content.optInt("departmentId").toString()
                bean.duties = content.optString("position")
                bean.sipKey = content.optString("sipKey")
                val nameList = content.optJSONArray("departmentNamePath")
                val tempList: MutableList<String> = ArrayList()
                for (j in 0 until nameList.length()) {
                    tempList.add(nameList.optString(j))
                }
                bean.departFatherList = tempList
                if (!bean.usrIsEndpoint.equals("terminal")) {
                    peopleList.add(bean)
                } else {
                    contactBeanList.add(bean)
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


    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(activity)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = layoutManager
        adapter = MyItemRecyclerViewAdapter(contactBeanList, this)
        recyclerView.adapter = adapter
    }



    companion object {

        @JvmStatic
        fun newInstance() =
                SearchCommonFragment().apply {
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