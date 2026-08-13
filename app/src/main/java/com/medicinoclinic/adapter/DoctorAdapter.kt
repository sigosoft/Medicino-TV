package com.medicinoclinic.adapter

import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.SettingsActivity
import com.medicinoclinic.model.DoctorListingModel
import kotlin.math.abs

class DoctorAdapter(private var mList: List<DoctorListingModel>) : RecyclerView.Adapter<DoctorAdapter.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return if (mList[position].scroll_up.trim() == "2") VIEW_TYPE_STATIC else VIEW_TYPE_SCROLL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View = when (viewType) {
            VIEW_TYPE_STATIC -> inflater.inflate(R.layout.item_doctor_static, parent, false)
            else -> if (mList.size <= 2) {
                inflater.inflate(R.layout.item_doctor_room_new, parent, false)
            } else {
                inflater.inflate(R.layout.item_doctor_room, parent, false)
            }
        }

        val initialHeight = equalHeightForParent(parent)
        view.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            initialHeight
        )

        view.isFocusable = true
        view.isFocusableInTouchMode = true
        val holder = ViewHolder(view)
        if (viewType == VIEW_TYPE_SCROLL) {
            setMarqueeSpeed(holder.tvDoctorName, 20f)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_TOKEN)) {
            bindTokenAndRoom(holder, mList[position])
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doctor = mList[position]
        val displayName = "Dr. ${doctor.name}"
        val disableScroll = doctor.scroll_up.trim() == "2"

        bindTokenAndRoom(holder, doctor)

        applyEqualItemHeight(holder)
        if (disableScroll) {
            bindStaticDoctorName(holder, displayName)
        } else {
            bindScrollingDoctorName(holder, displayName, doctor.name)
        }
        fitLabeledValues(holder)
        holder.itemView.post {
            if (holder.bindingAdapterPosition == RecyclerView.NO_POSITION) return@post
            applyEqualItemHeight(holder)
            fitLabeledValues(holder)
        }

        holder.itemView.setOnFocusChangeListener { _, _ -> }
        holder.itemView.setOnClickListener { view ->
            if (view.isFocusable) {
                view.context.startActivity(Intent(view.context, SettingsActivity::class.java))
            }
        }
    }

    private fun bindTokenAndRoom(holder: ViewHolder, doctor: DoctorListingModel) {
        holder.tv_room.text = if (doctor.room == "null") "NA" else doctor.room
        val tokenText = doctor.token.trim()
        holder.tv_token.text = if (tokenText.isEmpty() || tokenText.equals("null", ignoreCase = true)) {
            "0"
        } else {
            tokenText
        }
        fitLabeledValues(holder)
    }

    private fun bindScrollingDoctorName(holder: ViewHolder, displayName: String, rawName: String) {
        val nameView = holder.tvDoctorName
        TextViewCompat.setAutoSizeTextTypeWithDefaults(nameView, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
        if (holder.defaultNameSizePx > 0f) {
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.defaultNameSizePx)
        }
        nameView.isSingleLine = true
        nameView.maxLines = 1
        nameView.ellipsize = TextUtils.TruncateAt.MARQUEE
        nameView.marqueeRepeatLimit = -1
        nameView.setHorizontallyScrolling(true)
        nameView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        nameView.text = displayName
        nameView.post {
            nameView.isSelected = rawName.length > 10
        }
    }

    private fun bindStaticDoctorName(holder: ViewHolder, displayName: String) {
        val nameView = holder.tvDoctorName
        nameView.isSelected = false
        nameView.isSingleLine = false
        nameView.maxLines = 2
        nameView.ellipsize = null
        nameView.setHorizontallyScrolling(false)
        nameView.gravity = Gravity.CENTER
        nameView.includeFontPadding = false
        nameView.text = formatTwoLineName(displayName)
        autoSizeText(nameView, holder.defaultNameSizePx, 12)
    }

    private fun fitLabeledValues(holder: ViewHolder) {
        autoSizeText(holder.tv_room, holder.defaultRoomSizePx, 16)
        autoSizeText(holder.tv_token, holder.defaultTokenSizePx, 16)
        holder.tvRoomLabel?.let { autoSizeText(it, holder.defaultLabelSizePx, 12) }
        holder.tvTokenLabel?.let { autoSizeText(it, holder.defaultLabelSizePx, 12) }
    }

    private fun autoSizeText(view: TextView, defaultSizePx: Float, minSp: Int) {
        view.post {
            if (view.width <= 0 || view.height <= 0) return@post
            val maxSp = pxToSp(view, defaultSizePx).coerceAtLeast(minSp + 2)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                view,
                minSp,
                maxSp,
                1,
                TypedValue.COMPLEX_UNIT_SP
            )
        }
    }

    private fun equalHeightForParent(parent: ViewGroup): Int {
        val count = mList.size.coerceAtLeast(1)
        return if (parent.height > 0) parent.height / count else ViewGroup.LayoutParams.MATCH_PARENT
    }

    private fun applyEqualItemHeight(holder: ViewHolder) {
        val parent = holder.itemView.parent as? RecyclerView ?: run {
            holder.itemView.post { applyEqualItemHeight(holder) }
            return
        }
        if (parent.height > 0) {
            setEqualHeight(holder, parent)
            return
        }
        parent.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (parent.height <= 0) return
                parent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                setEqualHeight(holder, parent)
            }
        })
    }

    private fun setEqualHeight(holder: ViewHolder, parent: RecyclerView) {
        val count = itemCount
        if (count <= 0 || parent.height <= 0) return
        val position = holder.bindingAdapterPosition
        if (position == RecyclerView.NO_POSITION) return

        val baseHeight = parent.height / count
        val remainder = parent.height % count
        val itemHeight = baseHeight + if (position < remainder) 1 else 0

        val lp = holder.itemView.layoutParams as? RecyclerView.LayoutParams
            ?: RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, itemHeight)
        if (lp.height != itemHeight || lp.width != ViewGroup.LayoutParams.MATCH_PARENT) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            lp.height = itemHeight
            holder.itemView.layoutParams = lp
        }
    }

    private fun formatTwoLineName(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return trimmed

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size <= 1) return trimmed

        var bestSplit = 1
        var bestDiff = Int.MAX_VALUE
        for (i in 1 until words.size) {
            val line1 = words.take(i).joinToString(" ")
            val line2 = words.drop(i).joinToString(" ")
            val diff = abs(line1.length - line2.length)
            if (diff < bestDiff) {
                bestDiff = diff
                bestSplit = i
            }
        }
        return words.take(bestSplit).joinToString(" ") + "\n" + words.drop(bestSplit).joinToString(" ")
    }

    private fun pxToSp(view: View, px: Float): Int {
        val scaledDensity = view.resources.displayMetrics.scaledDensity
        return (px / scaledDensity).toInt().coerceAtLeast(12)
    }

    override fun getItemCount(): Int = mList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDoctorName: TextView = itemView.findViewById(R.id.tv_name)
        val tv_room: TextView = itemView.findViewById(R.id.tv_room)
        val tv_token: TextView = itemView.findViewById(R.id.tv_token)
        val tvRoomLabel: TextView? = itemView.findViewById(R.id.tv_room_label)
        val tvTokenLabel: TextView? = itemView.findViewById(R.id.tv_token_label)
        val defaultNameSizePx: Float = tvDoctorName.textSize
        val defaultRoomSizePx: Float = tv_room.textSize
        val defaultTokenSizePx: Float = tv_token.textSize
        val defaultLabelSizePx: Float = tvRoomLabel?.textSize ?: tvDoctorName.textSize
    }

    fun setDoctors(newList: List<DoctorListingModel>) {
        mList = newList
        notifyDataSetChanged()
    }

    fun updateTokenForDoctor(doctorId: String, newToken: String, newRoom: String? = null) {
        val index = mList.indexOfFirst { it.doctor_id == doctorId }
        if (index < 0) return

        val current = mList[index]
        val tokenUnchanged = current.token.trim() == newToken.trim()
        val roomUnchanged = newRoom == null || current.room.trim() == newRoom.trim()
        if (tokenUnchanged && roomUnchanged) return

        val updatedList = mList.toMutableList()
        updatedList[index] = current.copy(
            token = newToken,
            room = newRoom?.takeIf { it.isNotBlank() && it != "null" } ?: current.room
        )
        mList = updatedList
        notifyItemChanged(index, PAYLOAD_TOKEN)
    }

    fun setMarqueeSpeed(tv: TextView?, speed: Float) {
        if (tv != null) {
            try {
                val field = if (tv is AppCompatTextView) {
                    tv.javaClass.superclass.getDeclaredField("mMarquee")
                } else {
                    tv.javaClass.getDeclaredField("mMarquee")
                }
                field?.let {
                    it.isAccessible = true
                    val marquee = it[tv]
                    if (marquee != null) {
                        val scrollSpeedFieldName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            "mPixelsPerSecond"
                        } else {
                            "mScrollUnit"
                        }
                        val speedField = marquee.javaClass.getDeclaredField(scrollSpeedFieldName)
                        speedField.isAccessible = true
                        speedField.setFloat(marquee, speed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_SCROLL = 1
        private const val VIEW_TYPE_STATIC = 2
        private const val PAYLOAD_TOKEN = "payload_token"
    }
}
