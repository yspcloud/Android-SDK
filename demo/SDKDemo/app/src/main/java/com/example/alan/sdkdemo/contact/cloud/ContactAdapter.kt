package com.example.alan.sdkdemo.contact.cloud

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.alan.sdkdemo.R
import com.example.alan.sdkdemo.contact.ContactBean
import com.example.alan.sdkdemo.contact.ItemClick

/**
 * Created by ricardo
 * 8/30/21.
 */
class ContactAdapter : RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private var dataList = mutableListOf<ContactBean>()
    private var context: Context? = null
    private var itemClick: ItemClick? = null
    constructor(context: Context?)

    constructor(dataList: MutableList<ContactBean>, context: Context?, itemClick: ItemClick) : super() {
        this.dataList = dataList
        this.context = context
        this.itemClick = itemClick
    }


    override fun onCreateViewHolder(viewGroup: ViewGroup, position: Int): ViewHolder {

        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_layout, viewGroup, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactBean = dataList[position]
        holder.item.setOnClickListener {
            itemClick?.onItemClickListener(contactBean, contactBean.isPeople)
        }
        if (contactBean.isPeople) {
            handlePeople(holder, position, contactBean)
        }else{
            handleDep(holder, position, contactBean)
        }

    }

    private fun handleDep(holder: ViewHolder, position: Int, contactBean: ContactBean){
        holder.ivHead.setImageResource(R.drawable.box)
        holder.ivGuide.visibility = View.VISIBLE
        holder.tvStatus.visibility = View.GONE
        holder.tvName.visibility = View.GONE
        holder.tvDpName.visibility = View.VISIBLE
        holder.tvDpName.text = contactBean.departmentName
    }

    private fun handlePeople(holder: ViewHolder, position: Int, contactBean: ContactBean){
        holder.tvName.visibility = View.VISIBLE
        holder.tvStatus.visibility = View.VISIBLE
        holder.ivHead.visibility = View.VISIBLE
        holder.tvDpName.visibility = View.GONE
        holder.ivGuide.visibility = View.GONE
        holder.tvName.text = contactBean.usrNickName
        if ("0" == contactBean.usrIsEndpoint) {
            holder.ivHead.setImageResource(if (contactBean.onLineStatus != 0) {
                R.drawable.icon_user_online
            } else {
                R.drawable.icon_user_offline
            })
        } else {
            holder.ivHead.setImageResource(if (contactBean.onLineStatus != 0) {
                R.drawable.icon_terminal_online
            } else {
                R.drawable.icon_terminal_offline
            })
        }
        holder.tvStatus.text = when (contactBean.onLineStatus) {
            0 -> {
                String.format("[%s]", "离线")
            }
            1 -> {
                String.format("[%s]", "在线")
            }
            else -> {
                String.format("[%s]", "会议中")
            }
        }
    }

    override fun getItemCount(): Int = dataList.size


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivHead: ImageView
        var ivGuide: ImageView
        var tvName: TextView
        var tvStatus: TextView
        var tvDpName: TextView
        var item: ConstraintLayout

        init {
            itemView.apply {
                ivHead = findViewById(R.id.iv_head)
                ivGuide = findViewById(R.id.iv_guide)
                tvName = findViewById(R.id.tv_name)
                tvStatus = findViewById(R.id.tv_status)
                tvDpName = findViewById(R.id.tv_dp_name)
                item = findViewById(R.id.item_view)
            }
        }

    }
}