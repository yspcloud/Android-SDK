package com.example.alan.sdkdemo.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.example.alan.sdkdemo.R
import com.example.alan.sdkdemo.ui.ZJConferenceActivity
import com.vcrtc.VCRTC
import com.vcrtc.callbacks.VCCallback
import com.vcrtc.entities.WhiteboardPayload
import com.vcrtc.widget.WhiteBoardView
import com.vcrtc.widget.WhiteBoardView.BitmapCallBack
import com.vcrtc.widget.WhiteBoardView.DataCallBack
import java.util.*

class WhiteBoardUtil(val vcrtc: VCRTC, val myContext: ZJConferenceActivity) {
    var isJoin = false
    var isMark = true
    private var isExpand = false
    private var whiteBoardView: WhiteBoardView? = null
    private var isSelect = false
    var currentMarkBitmap: Bitmap? = null

    // tools
    private lateinit var ivPen: ImageView
    private lateinit var ivEarse: ImageView
    private lateinit var ivClear: ImageView
    private lateinit var ivDown: ImageView

    // status
    // color
    private lateinit var ivBlack: ImageView
    private lateinit var ivGreen: ImageView
    private lateinit var ivYellow: ImageView
    private lateinit var ivOrange: ImageView
    private lateinit var ivBlue: ImageView

    // width
    private lateinit var ivThin: ImageView
    private lateinit var ivRegular: ImageView
    private lateinit var ivMedium: ImageView
    private lateinit var ivBold: ImageView

    private lateinit var ivMarkFloat: ImageView
    private lateinit var ivMarkBackground: ImageView

    private lateinit var llTools: LinearLayout
    private lateinit var llBaseTools: LinearLayout
    private lateinit var llBaseToolsBackground: LinearLayout
    private lateinit var floatParent: LinearLayout
    private lateinit var llClearTools: LinearLayout

    private lateinit var tvClearAll: TextView
    private lateinit var tvClearMine: TextView
    private lateinit var tvClearOther: TextView

    var frameBackground: FrameLayout? = null

    private var rlWhiteParent: FrameLayout? = null
    private var currentColor = Color.BLACK
    private var currentWidth = 10
    private val COLOR = 0
    private val WIDTH = 1
    private val TOOLS = 2

    fun initWhiteBoardView(rootView: View) {
        rootView.apply {
            rlWhiteParent = findViewById(R.id.fl_white_content)
            llTools = findViewById(R.id.ll_tools)
            ivPen = findViewById(R.id.iv_pen)
            ivEarse = findViewById(R.id.iv_earser)
            ivClear = findViewById(R.id.iv_clear)
            ivDown = findViewById(R.id.iv_down)
            ivBlack = findViewById(R.id.iv_black)
            ivYellow = findViewById(R.id.iv_yellow)
            ivOrange = findViewById(R.id.iv_orange)
            ivGreen = findViewById(R.id.iv_green)
            ivBlue = findViewById(R.id.iv_blue)
            ivThin = findViewById(R.id.iv_thin)
            ivRegular = findViewById(R.id.iv_regular)
            ivMedium = findViewById(R.id.iv_medium_bold)
            ivBold = findViewById(R.id.iv_bold)
            ivMarkFloat = findViewById(R.id.iv_mark_float)
            llBaseTools = findViewById(R.id.ll_base_tools)
            llBaseToolsBackground = findViewById(R.id.ll_base_tools_background)
            floatParent = findViewById(R.id.float_parent)
            ivMarkBackground = findViewById(R.id.iv_white_background)
            llClearTools = findViewById(R.id.ll_clear_tools)
            tvClearAll = findViewById(R.id.tv_white_clear_all)
            tvClearMine = findViewById(R.id.tv_white_clear_mine)
            tvClearOther = findViewById(R.id.tv_white_clear_other)
            frameBackground = rootView.findViewById(R.id.frame_background)
            ivPen.setOnClickListener(whiteToolsListener)
            ivEarse.setOnClickListener(whiteToolsListener)
            ivClear.setOnClickListener(whiteToolsListener)
            ivDown.setOnClickListener(whiteToolsListener)
            ivBlack.setOnClickListener(whiteToolsListener)
            ivYellow.setOnClickListener(whiteToolsListener)
            ivGreen.setOnClickListener(whiteToolsListener)
            ivBlue.setOnClickListener(whiteToolsListener)
            ivOrange.setOnClickListener(whiteToolsListener)
            ivThin.setOnClickListener(whiteToolsListener)
            ivRegular.setOnClickListener(whiteToolsListener)
            ivMedium.setOnClickListener(whiteToolsListener)
            ivBold.setOnClickListener(whiteToolsListener)
            floatParent.setOnClickListener(whiteToolsListener)
            tvClearOther.setOnClickListener(whiteToolsListener)
            tvClearMine.setOnClickListener(whiteToolsListener)
            tvClearAll.setOnClickListener(whiteToolsListener)
            ivPen.isSelected = true
            ivRegular.isSelected = true
            ivBlack.isSelected = true
        }
    }

    private val whiteToolsListener = View.OnClickListener {
        when (it.id) {
            R.id.iv_pen -> {
                if (ivPen.isSelected && llTools.visibility == View.GONE) {
                    if (llClearTools.visibility == View.VISIBLE) {
                        llClearTools.visibility = View.GONE
                    } else {
                        val lp = floatParent.layoutParams as RelativeLayout.LayoutParams
                        lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        lp.setMargins(floatParent.left, floatParent.top - DensityUtil.dip2px(myContext, 86f), 0, 0)
                        floatParent.layoutParams = lp
                    }
                    llTools.visibility = View.VISIBLE
                } else if (ivPen.isSelected && llTools.visibility == View.VISIBLE) {
                    updatePosition()
                    llTools.visibility = View.GONE
                } else {
                    refreshWhiteTools(TOOLS)
                    ivPen.isSelected = true
                    whiteBoardView?.setPenColor(currentColor)
                    whiteBoardView?.setPenWidth(currentWidth.toFloat())
                    if (llClearTools.visibility == View.VISIBLE) {
                        updatePosition()
                        llClearTools.visibility = View.GONE
                    }
                }
            }
            R.id.iv_earser -> {
                resetToolsPosition()
                refreshWhiteTools(TOOLS)
                ivEarse.isSelected = true
                whiteBoardView?.setPathEraser()
            }
            R.id.iv_clear -> {
                if (llClearTools.visibility == View.GONE) {
                    if (llTools.visibility == View.VISIBLE) {
                        llTools.visibility = View.GONE
                    } else {
                        val lp = floatParent.layoutParams as RelativeLayout.LayoutParams
                        lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        lp.setMargins(floatParent.left, floatParent.top - DensityUtil.dip2px(myContext, 86f), 0, 0)
                        floatParent.layoutParams = lp
                    }
                    if (isJoin) {
                        tvClearOther.visibility = View.GONE
                        tvClearAll.visibility = View.GONE
                    } else {
                        tvClearOther.visibility = View.VISIBLE
                        tvClearAll.visibility = View.VISIBLE
                    }
                    llClearTools.visibility = View.VISIBLE
                } else {
                    updatePosition()
                    llClearTools.visibility = View.GONE
                }
            }
            R.id.iv_down -> {
                resetToolsPosition()
                if (whiteBoardView != null) {
                    val bitmap = com.vcrtc.utils.BitmapUtil.createBitmapFromView(frameBackground)
                    BitmapUtil.saveBitmapInDCIM(myContext, bitmap, System.currentTimeMillis().toString() + "" + ".jpg")
                }
            }
            R.id.iv_black -> setColor(ivBlack, "#000000")
            R.id.iv_yellow -> setColor(ivYellow, "#FFD562")
            R.id.iv_orange -> setColor(ivOrange, "#FF605C")
            R.id.iv_green -> setColor(ivGreen, "#40E3AB")
            R.id.iv_blue -> setColor(ivBlue, "#408CFF")
            R.id.iv_thin -> setWidth(ivThin, 5)
            R.id.iv_regular -> setWidth(ivRegular, 10)
            R.id.iv_medium_bold -> setWidth(ivMedium, 15)
            R.id.iv_bold -> setWidth(ivBold, 20)
            R.id.tv_white_clear_all -> {
                resetToolsPosition()
                whiteBoardView?.clear()
                vcrtc.clearWhiteboardPayload()
//                sendWhiteBoardBitmap(vcrtc)
            }
            R.id.tv_white_clear_mine -> {
                resetToolsPosition()
                whiteBoardView?.clearLocal()
            }
            R.id.tv_white_clear_other -> {
                resetToolsPosition()
                whiteBoardView?.clearPayload()
            }
            R.id.iv_mark_float -> {
                isSelect = !isSelect
                if (isSelect) {
                    llBaseToolsBackground.background = myContext.getResources().getDrawable(R.drawable.background_float)
                    llBaseTools.visibility = View.VISIBLE
                    if (isJoin) {
//                        context.joinBroad()
                    }
                } else {
                    llBaseToolsBackground.background = myContext.getResources().getDrawable(R.drawable.background_float_none)
                    llBaseTools.visibility = View.GONE
                    llTools.visibility = View.GONE
                    if (isJoin || isMark) {
                        releaseWhiteView()
                    }
                }
                ivMarkFloat.isSelected = isSelect
            }
            R.id.float_parent -> {
                floatClick()
            }
        }
    }

    private fun floatClick() {
        isSelect = !isSelect
        if (!isExpand) {
            isExpand = true
            llBaseToolsBackground.background = myContext.resources.getDrawable(R.drawable.background_float)
            llBaseTools.visibility = View.VISIBLE
            if (isJoin) {
                if (isMark) {
                    myContext.joinMark()
                } else {
                    myContext.joinBoard()
                }
            } else {
                if (isMark) {
                    if (whiteBoardView == null) {
                        myContext.startMark()
                    } else {
                        whiteBoardView?.setTouchabled(true)
                        myContext.updateMark()
                    }
                }
            }
        } else {
            isExpand = false
            llBaseToolsBackground.background = myContext.resources.getDrawable(R.drawable.background_float_none)
            llBaseTools.visibility = View.GONE
            if (llTools.visibility == View.VISIBLE) {
                updatePosition()
                llTools.visibility = View.GONE
            }
            if (llClearTools.visibility == View.VISIBLE) {
                updatePosition()
                llClearTools.visibility = View.GONE
            }
            if (isJoin) {
                hideWhiteView()
            } else {
                if (isMark) {
                    // 收起工具栏
                    whiteBoardView?.apply {
                        whiteBoardView?.setTouchabled(false)
                        // 设置白板透明背景 && 收掉黑色背景
                        myContext.switchMark()
                    }

                }
            }
        }
        ivMarkFloat.isSelected = isSelect
    }

    private fun hideWhiteView() {
        rlWhiteParent!!.visibility = View.GONE
        myContext.handleDisplay(true)
    }


    private fun updatePosition() {
        val lp = floatParent.layoutParams as RelativeLayout.LayoutParams
        lp.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        lp.setMargins(floatParent.left, floatParent.top + DensityUtil.dip2px(myContext, 86f), 0, 0)
        floatParent.layoutParams = lp
    }

    private fun resetToolsPosition() {
        if (llTools.visibility == View.VISIBLE) {
            updatePosition()
            llTools.visibility = View.GONE
        }
        if (llClearTools.visibility == View.VISIBLE) {
            updatePosition()
            llClearTools.visibility = View.GONE
        }
    }

    private fun setColor(iv: ImageView, color: String) {
        refreshWhiteTools(COLOR)
        iv.isSelected = true
        currentColor = Color.parseColor(color)
        whiteBoardView?.setPenColor(currentColor)
    }

    private fun setWidth(iv: ImageView, width: Int) {
        refreshWhiteTools(WIDTH)
        iv.isSelected = true
        currentWidth = width
        whiteBoardView?.setPenWidth(width.toFloat())
    }

    private fun refreshWhiteTools(type: Int) {
        when (type) {
            COLOR -> {
                ivBlack.isSelected = false
                ivYellow.isSelected = false
                ivGreen.isSelected = false
                ivBlue.isSelected = false
                ivOrange.isSelected = false
            }
            WIDTH -> {
                ivThin.isSelected = false
                ivRegular.isSelected = false
                ivMedium.isSelected = false
                ivBold.isSelected = false
            }
            TOOLS -> {
                ivPen.isSelected = false
                ivEarse.isSelected = false
                ivClear.isSelected = false
            }
        }
    }

    fun releaseWhiteView() {
        rlWhiteParent!!.removeView(whiteBoardView)
        rlWhiteParent!!.visibility = View.GONE
        whiteBoardView = null
        resetPen()
//        context.hideOrShow(true)
    }

    private fun resetPen() {
        refreshWhiteTools(TOOLS)
        refreshWhiteTools(COLOR)
        refreshWhiteTools(WIDTH)
        ivPen.isSelected = true
        ivRegular.isSelected = true
        ivBlack.isSelected = true
        currentColor = Color.BLACK
        currentWidth = 10
    }

    fun getWhiteBoardview(): WhiteBoardView? {
        return whiteBoardView
    }

    fun makeFloatVisible(isVisible: Boolean) {
        if (isVisible) {
            if (floatParent.visibility != View.VISIBLE) {
                ivMarkFloat.visibility = View.VISIBLE
                floatParent.visibility = View.VISIBLE
                isSelect = false
                ivMarkFloat.isSelected = false
                isExpand = false
            }
        } else {
            ivMarkFloat.visibility = View.GONE
            llBaseTools.visibility = View.GONE
            floatParent.visibility = View.GONE
            llTools.visibility = View.GONE
            llBaseToolsBackground.background = myContext.resources.getDrawable(R.drawable.background_float_none)
        }
    }

    fun setCurrentStatus(isJoin: Boolean) {
        this.isJoin = isJoin
    }


    fun convertViewToBitmap(view: View): Bitmap? {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun sendWhiteBoardBitmap(vcrtc: VCRTC) {
        val bitmap = convertViewToBitmap(frameBackground!!)
        if (bitmap != null) {
            vcrtc.sendPresentation(BitmapUtil.formatBitmap16_9(bitmap, 1920, 1080))
        }
    }

    fun initStartBroad() {
        isSelect = true
        ivMarkFloat.isSelected = true
        llBaseToolsBackground.background = myContext.resources.getDrawable(R.drawable.background_float)
        llBaseTools.visibility = View.VISIBLE
        isExpand = true
    }

    fun initStartMark() {
        isSelect = false
        ivMarkFloat.isSelected = false
        llBaseToolsBackground.background = myContext.resources.getDrawable(R.drawable.background_float_none)
        llBaseTools.visibility = View.GONE
        whiteBoardView?.setTouchabled(false)
        isExpand = false
    }

    private fun sendBitmap() {
        if (!isJoin) {
            val bitmap = convertViewToBitmap(frameBackground!!)
            vcrtc.sendPresentation(BitmapUtil.formatBitmap16_9(bitmap, 1920, 1080))
        }
    }


    fun resetPosition() {
        val lp = floatParent.layoutParams as RelativeLayout.LayoutParams
        lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP)
        lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lp.setMargins(DensityUtil.dpToPx(myContext, 20), 0, 0, DensityUtil.dpToPx(myContext, 80))
        floatParent.layoutParams = lp
    }

    fun setWhiteBoardView(view: WhiteBoardView) {
        Log.d("white_join", "setWhiteBoardView: $view")

        whiteBoardView = view
        whiteBoardView?.apply {
            setBackgroundColor(Color.WHITE)
            setPenWidth(currentWidth.toFloat())
            setPenColor(currentColor)
            setDataCallBack(object : DataCallBack {
                override fun onPayload(uuid: UUID, whiteboardPayload: WhiteboardPayload, vcCallback: VCCallback) {
                    vcrtc.addWhiteboardPayload(whiteboardPayload, vcCallback)
                }

                override fun onDelete(i: Int) {
                    vcrtc.deleteWhiteboardPayload(i)
                }
            })
            setBitmapCallBack(BitmapCallBack { bitmap ->
                vcrtc.sendPresentation(bitmap)
            })
        }

    }

    fun addWhiteBoardView(whiteWidth: Float, whiteHeight: Float) {

        whiteBoardView?.apply {
            val params = FrameLayout.LayoutParams(
                    whiteWidth.toInt(), whiteHeight.toInt()
            )
            params.gravity = Gravity.CENTER
            setBackgroundResource(R.color.white)
            rlWhiteParent!!.addView(this, rlWhiteParent!!.childCount, params)
            rlWhiteParent!!.visibility = View.VISIBLE
            setActionListener(object : WhiteBoardView.ActionListener {
                override fun onActionDown() {}
                override fun onActionUp() {
                    if (llTools.visibility == View.VISIBLE) {
                        updatePosition()
                        llTools.visibility = View.GONE
                    }
                }
            })
        }
    }

    fun joinWhiteBoardView(view: WhiteBoardView) {
        Log.d("white_join", "joinWhiteBoardView: $view")
        whiteBoardView = view
        whiteBoardView?.apply {
            setBackgroundColor(Color.WHITE)
            setPenColor(currentColor)
            setPenWidth(currentWidth.toFloat())
            setDataCallBack(object : DataCallBack {
                override fun onPayload(uuid: UUID, whiteboardPayload: WhiteboardPayload, vcCallback: VCCallback) {
                    vcrtc.addWhiteboardPayload(whiteboardPayload, vcCallback)
                }

                override fun onDelete(i: Int) {
                    Log.d("white_join", "joinWhiteBoardView onDelete: $i")
                    vcrtc.deleteWhiteboardPayload(i)
                }
            })
        }

    }


    fun releaseView() {
        if (currentMarkBitmap != null && !currentMarkBitmap!!.isRecycled) {
            currentMarkBitmap!!.recycle()
            currentMarkBitmap = null
        }
        if (llClearTools.visibility == View.VISIBLE) {
            llClearTools.visibility = View.GONE
        }
        if (llTools.visibility == View.VISIBLE) {
            llTools.visibility = View.GONE
        }
        isExpand = false
        ivMarkBackground.setImageDrawable(null)
        sendHandler.removeCallbacks(sendRunnable)
    }

    fun showWhiteView() {
        rlWhiteParent!!.visibility = View.VISIBLE
        frameBackground!!.visibility = View.VISIBLE
    }

    private val sendHandler = Handler(Looper.getMainLooper())
    private val sendRunnable = Runnable { sendBitmap() }

    /**
     * 为标注设置背景
     * @param isVisible
     * @param bitmap
     */
    fun setMarkBackground(isVisible: Boolean, bitmap: Bitmap?) {
        if (isVisible) {
            if (isMark) {
                ivMarkBackground.visibility = View.VISIBLE
                if (bitmap != null && !bitmap.isRecycled) {
                    Glide.with(myContext).asBitmap().load(bitmap).into(ivMarkBackground)
                }
            }
        } else {
            ivMarkBackground.visibility = View.GONE
        }
        System.gc()
    }
    fun setCurrentMarkBitmapD(currentMarkBitmap: Bitmap) {
        if (this.currentMarkBitmap != null && !currentMarkBitmap.isRecycled) {
            this.currentMarkBitmap!!.recycle()
            this.currentMarkBitmap = null
        }
        this.currentMarkBitmap = currentMarkBitmap
    }


    fun joinMarkView(view: WhiteBoardView) {
        whiteBoardView = view
        whiteBoardView?.apply {
            setPenColor(currentColor)
            setPenWidth(currentWidth.toFloat())
            setBackgroundColor(Color.parseColor("#00000000"))
            setDataCallBack(object : DataCallBack {
                override fun onPayload(uuid: UUID, whiteboardPayload: WhiteboardPayload, vcCallback: VCCallback) {
                    vcrtc.addWhiteboardPayload(whiteboardPayload, vcCallback)
                }

                override fun onDelete(i: Int) {
                    vcrtc.deleteWhiteboardPayload(i)
                    deleteCapture()
                }
            })
        }

    }

    fun addMarkView(whiteWidth: Int, whiteHeight: Int, isVertical: Boolean){
        whiteBoardView?.apply {
            val params = FrameLayout.LayoutParams(
                    whiteWidth, whiteHeight
            )
            params.gravity = Gravity.CENTER
            rlWhiteParent!!.addView(this, rlWhiteParent!!.childCount, params)
            rlWhiteParent?.visibility = View.VISIBLE
            frameBackground?.visibility = View.VISIBLE
            setActionListener(object : WhiteBoardView.ActionListener {
                override fun onActionDown() {
                    sendHandler.removeCallbacks(sendRunnable)
                }

                override fun onActionUp() {
                    if (llTools.visibility == View.VISIBLE) {
                        updatePosition()
                        llTools.visibility = View.GONE
                    }
                    if (!isJoin){
                        sendHandler.postDelayed(sendRunnable, 300)
                    }
                }

            })
        }
    }

    private fun deleteCapture() {
        deleteHandler.removeCallbacks(runnable)
        deleteHandler.postDelayed(runnable, 500)
    }

    var deleteHandler = Handler(Looper.getMainLooper())
    var runnable = Runnable {
        if (!isJoin) {
            sendWhiteBoardBitmap(vcrtc)
        }
    }

}