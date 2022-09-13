package com.example.alan.sdkdemo.contact.common

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

class MyItemRecyclerViewAdapter(
        private val values: List<ContactBean>, private val itemClick: ItemClick)
    : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_contact_common, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contactBean = values[position]
        holder.itemView.setOnClickListener {
            itemClick.onItemClickListener(contactBean, contactBean.isPeople)
        }
        if (contactBean.isPeople){
            holder.tvName.text = contactBean.usrNickName
            holder.ivGuide.visibility = View.GONE
            if ("0" == contactBean.usrIsEndpoint) {
                holder.ivHead.setImageResource(R.drawable.icon_user_online)
            } else {
                holder.ivHead.setImageResource(R.drawable.icon_terminal_online)
            }
        }else{
            holder.ivHead.setImageResource(R.drawable.box)
            holder.tvName.text = contactBean.departmentName
            holder.ivGuide.visibility = View.VISIBLE
        }

    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivHead: ImageView = view.findViewById(R.id.iv_head)
        val ivGuide: ImageView = view.findViewById(R.id.iv_guide)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val itemView: ConstraintLayout = view.findViewById(R.id.item_view)

        override fun toString(): String {
            return ""
        }
    }
}