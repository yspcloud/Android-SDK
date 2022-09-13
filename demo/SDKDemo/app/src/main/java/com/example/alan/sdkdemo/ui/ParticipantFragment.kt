package com.example.alan.sdkdemo.ui

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.alan.sdkdemo.R
import com.vcrtc.VCRTC
import com.vcrtc.entities.Call
import com.vcrtc.entities.ConferenceStatus
import com.vcrtc.entities.Participant
import com.vcrtc.entities.Stage
import com.vcrtc.webrtc.RTCManager
import kotlinx.coroutines.*

class ParticipantFragment : Fragment(), ZJConferenceActivity.ConferenceCallBack, OnParticipantItemListener {
    private var vc: VCRTC? = null
    private var dataList: MutableList<Participant> = mutableListOf()
    private var participant: Map<String, Participant>? = null
    private var uuid: String? = null
    private var adapter: ParticipantAdapter? = null
    private lateinit var recycler: RecyclerView
    private var myCall: Call? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_participant, container, false)
        (activity as ZJConferenceActivity).apply {
            uuid = this.myUUID
            vc = this.vcrtc
            myCall = this.call
            setConferenceCallBack(this@ParticipantFragment)
        }
        rootView.findViewById<ImageView>(R.id.iv_back).setOnClickListener { activity?.supportFragmentManager?.popBackStack() }
        initRecycler(rootView)
        participant = vc?.participants
        GlobalScope.launch(Dispatchers.IO) {
            handleDataListAsync().await()
            refreshView()
        }

        return rootView
    }

    private fun initRecycler(rootView: View) {
        recycler = rootView.findViewById(R.id.recycler)
        adapter = ParticipantAdapter(mutableListOf(), activity!!, this)
        recycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        recycler.adapter = adapter
    }

    @Synchronized
    private fun handleDataListAsync(): Deferred<Boolean> {
        dataList.clear()
        val deferred = CompletableDeferred<Boolean>()
        try {
            participant?.filter { !it.value.display_name.startsWith("_Cloud_robot_") }?.forEach {
                if (uuid == it.value.uuid) {
                    dataList.add(0, it.value)
                } else {
                    dataList.add(it.value)
                }
            }
            deferred.complete(true)
        } catch (e: Exception) {
            deferred.complete(false)
        }
        return deferred
    }

    private suspend fun refreshView() = withContext(Dispatchers.Main) {
        adapter?.updateList(dataList)
    }

    companion object {

        @JvmStatic
        fun newInstance() = ParticipantFragment()
    }

    private val handleDataRunnable = Runnable {
        GlobalScope.launch(Dispatchers.IO) {
            val hasHandle = handleDataListAsync().await()
            if (hasHandle) {
                refreshView()
            }
        }
    }

    private val handler = Handler()


    override fun onAddParticipant(participant: Participant?) {
        handler.apply { removeCallbacks(handleDataRunnable) }.postDelayed(handleDataRunnable, 500)
    }

    override fun onUpdateParticipant(participant: Participant?) {
        handler.apply { removeCallbacks(handleDataRunnable) }.postDelayed(handleDataRunnable, 500)
    }

    override fun onRemoveParticipant(uuid: String?) {
        handler.apply { removeCallbacks(handleDataRunnable) }.postDelayed(handleDataRunnable, 500)
    }

    override fun onStageVoice(stages: MutableList<Stage>?) {
    }

    override fun onConferenceUpdate(status: ConferenceStatus?) {

    }

    override fun onRoleUpdate(role: String?) {
        myCall?.isHost = ("HOST" == role)
    }


    override fun onDestroy() {
        handler.removeCallbacks(handleDataRunnable)
        super.onDestroy()
    }

    override fun onItemListener(participant: Participant) {
        if (myCall!!.isHost) {
            if ("waiting_room" == participant.service_type || "connecting" == participant.service_type) {
                return
            }
            showOperationWindow(participant)
        } else {
            if (RTCManager.isIsShitongPlatform()) {
                showGuestWindow(participant)
            }
        }
    }

    private fun showGuestWindow(participant: Participant) {
        val popView = LayoutInflater.from(activity).inflate(R.layout.pop_participant_guest_layout, null)
        val popupWindow = PopupWindow(popView, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        popupWindow.apply {
            setBackgroundDrawable(resources.getDrawable(R.color.transparent))
            isFocusable = true
            isOutsideTouchable = false
            animationStyle = R.style.pop_animation
            showAtLocation(view, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0)
        }

        val tvPick = popView!!.findViewById<TextView>(R.id.tv_pick)
        tvPick.setOnClickListener {
            if ((activity as ZJConferenceActivity).mediaShiTongFragment != null)
                (activity as ZJConferenceActivity).mediaShiTongFragment.setStick(participant.uuid)
            popupWindow.dismiss()

        }
        popView?.apply {
            findViewById<View>(R.id.linear_release_mute).visibility = View.GONE
            findViewById<View>(R.id.tv_change_name).visibility = View.GONE
            findViewById<View>(R.id.linear_pick).visibility = View.VISIBLE
            findViewById<View>(R.id.ll_all_see).visibility = View.GONE
            val tvName = findViewById<TextView>(R.id.tv_name)
            tvName.text = participant.overlay_text
            val tvCancel = findViewById<TextView>(R.id.tv_cancel)
            tvCancel.setOnClickListener { popupWindow.dismiss() }
            setOnClickListener { popupWindow.dismiss() }
        }


    }

    private fun showOperationWindow(participant: Participant) {
        val view: View = if (RTCManager.isIsShitongPlatform()) {
            LayoutInflater.from(activity).inflate(R.layout.pop_participant_operation_shitong, null)
        } else {
            if (participant.uuid == uuid) {
                LayoutInflater.from(activity).inflate(R.layout.pop_participant_guest_layout, null)
            } else {
                LayoutInflater.from(activity).inflate(R.layout.pop_participant_operation, null)
            }
        }
        val popupWindow = PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        popupWindow.setBackgroundDrawable(resources.getDrawable(R.color.transparent))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = false
        popupWindow.animationStyle = R.style.pop_animation
        popupWindow.showAtLocation(getView(), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 0)
        view.findViewById<TextView>(R.id.tv_name).text = participant.overlay_text
        view.findViewById<TextView>(R.id.tv_change_name).setOnClickListener {
            showEditNameView(participant)
            popupWindow.dismiss()
        }
        val tvAllSee = view.findViewById<TextView>(R.id.tv_all_see)
        val tvPick = view.findViewById<TextView>(R.id.tv_pick)
        val tvRemove = view.findViewById<TextView>(R.id.tv_remove)
        val tvSetRole = view.findViewById<TextView>(R.id.tv_set_role)
        val llReleaseMute = view.findViewById<LinearLayout>(R.id.linear_release_mute)
        val tvReleaseMute = view.findViewById<TextView>(R.id.tv_release_mute)
        val tvCancel = view.findViewById<TextView>(R.id.tv_cancel)
        tvCancel.setOnClickListener { v: View? -> popupWindow.dismiss() }

        view.setOnClickListener { v: View? -> popupWindow.dismiss() }
        if ("YES" == participant.is_video_call) {
            tvAllSee.visibility = View.VISIBLE
            tvAllSee.text = if (participant.spotlight > 0) "取消设为焦点" else "设为焦点"
            tvAllSee.setOnClickListener {
                popupWindow.dismiss()
                vc?.setParticipantSpotlight(participant.uuid, participant.spotlight == 0.0)
            }
            // 双流状态下无效
            tvPick?.visibility = if (RTCManager.isIsShitongPlatform()) View.VISIBLE else View.GONE
            tvPick?.setOnClickListener {
                if ((activity as ZJConferenceActivity).mediaShiTongFragment != null)
                    (activity as ZJConferenceActivity).mediaShiTongFragment.setStick(participant.uuid)
                popupWindow.dismiss()
            }
        } else {
            tvPick?.visibility = View.GONE
            tvAllSee.visibility = View.GONE
        }

        if (participant.uuid != uuid) {
            tvRemove.setOnClickListener {
                vc?.disconnectParticipant(participant.uuid)
                popupWindow.dismiss()
            }
            tvSetRole.setText(if ("chair" == participant.role) R.string.participants_set_guest else R.string.participants_set_host)
            tvSetRole.setOnClickListener {
                if ("chair" == participant.role) {
                    vc?.setParticipantRole(participant.uuid, "guest")
                } else {
                    vc?.clearHand(participant.uuid)
                    vc?.setParticipantRole(participant.uuid, "chair")
                }
                popupWindow.dismiss()
            }
        } else {
            tvRemove?.visibility = View.GONE
            view.findViewById<View>(R.id.line1)?.visibility = View.GONE
            view.findViewById<View>(R.id.line2)?.visibility = View.GONE
            tvSetRole?.visibility = View.GONE
        }

        llReleaseMute.visibility = View.VISIBLE
        if ("YES" == participant.is_muted){
            tvReleaseMute.text = "解除静音"
            llReleaseMute.setOnClickListener {
                vc?.setParticipantMute(participant.uuid, false)
                popupWindow.dismiss()
            }
        }else{
            tvReleaseMute.text = "静音"
            llReleaseMute.setOnClickListener {
                vc?.setParticipantMute(participant.uuid, true)
                popupWindow.dismiss()
            }
        }


    }

    private fun showEditNameView(participant: Participant) {
        val view = LayoutInflater.from(activity).inflate(R.layout.pop_change_name, null)
        val popupWindow = PopupWindow(view, RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        popupWindow.setBackgroundDrawable(resources.getDrawable(R.color.transparent))
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = false
        popupWindow.showAtLocation(getView(), Gravity.CENTER, 0, 0)

        val etName = view.findViewById<EditText>(R.id.et_name)
        val tvCancel = view.findViewById<TextView>(R.id.tv_cancel)
        val tvSure = view.findViewById<TextView>(R.id.tv_sure)

        etName.hint = participant.overlay_text

        tvCancel.setOnClickListener { popupWindow.dismiss() }

        tvSure.setOnClickListener {
            vc?.setParticipantName(participant.uuid, etName.text.toString())
            popupWindow.dismiss()
        }
    }
}