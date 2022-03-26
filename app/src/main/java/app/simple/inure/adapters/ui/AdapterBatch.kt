package app.simple.inure.adapters.ui

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.decorations.checkbox.CheckBox
import app.simple.inure.decorations.overscroll.RecyclerViewConstants
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.ripple.DynamicRippleConstraintLayout
import app.simple.inure.decorations.ripple.DynamicRippleImageButton
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.glide.modules.GlideApp
import app.simple.inure.glide.util.ImageLoader.loadAppIcon
import app.simple.inure.interfaces.adapters.AppsAdapterCallbacks

class AdapterBatch(var apps: ArrayList<PackageInfo> = arrayListOf()) : RecyclerView.Adapter<VerticalListViewHolder>() {

    private var appsAdapterCallbacks: AppsAdapterCallbacks? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalListViewHolder {
        return when (viewType) {
            RecyclerViewConstants.TYPE_HEADER -> {
                Header(LayoutInflater.from(parent.context)
                           .inflate(R.layout.adapter_header_batch, parent, false))
            }
            RecyclerViewConstants.TYPE_ITEM -> {
                Holder(LayoutInflater.from(parent.context)
                           .inflate(R.layout.adapter_batch, parent, false))
            }
            else -> {
                throw IllegalArgumentException("there is no type that matches the type $viewType, make sure your using types correctly")
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: VerticalListViewHolder, position_: Int) {
        val position = position_ - 1
        if (holder is Holder) {

            holder.icon.transitionName = "app_$position"
            holder.icon.loadAppIcon(apps[position].packageName)
            holder.name.text = apps[position].applicationInfo.name
            holder.packageId.text = apps[position].packageName

            if (apps[position].applicationInfo.enabled) {
                holder.name.paintFlags = holder.name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            } else {
                holder.name.paintFlags = holder.name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }

            holder.checkBox.setCheckedWithoutAnimations(false)

            holder.container.setOnClickListener {
                appsAdapterCallbacks?.onAppClicked(apps[position], holder.icon)
            }

            holder.container.setOnLongClickListener {
                appsAdapterCallbacks?.onAppLongPressed(apps[position], holder.icon)
                true
            }
        }

        if (holder is Header) {
            holder.search.setOnClickListener {
                appsAdapterCallbacks?.onSearchPressed(it)
            }

            holder.filter.setOnClickListener {
                appsAdapterCallbacks?.onFilterPressed(it)
            }

            holder.sort.setOnClickListener {
                appsAdapterCallbacks?.onSortPressed(it)
            }

            holder.settings.setOnClickListener {
                appsAdapterCallbacks?.onSettingsPressed(it)
            }

            holder.total.text = String.format(holder.itemView.context.getString(R.string.total_apps), apps.size)
        }
    }

    override fun onViewRecycled(holder: VerticalListViewHolder) {
        super.onViewRecycled(holder)
        if (holder is Holder) {
            GlideApp.with(holder.icon).clear(holder.icon)
        }
        if (holder is Header) {
            Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)
        }
    }

    override fun getItemCount(): Int {
        return apps.size + 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            RecyclerViewConstants.TYPE_HEADER
        } else RecyclerViewConstants.TYPE_ITEM
    }

    fun setOnItemClickListener(appsAdapterCallbacks: AppsAdapterCallbacks) {
        this.appsAdapterCallbacks = appsAdapterCallbacks
    }

    fun getCurrentAppsList(): ArrayList<PackageInfo> {
        return apps
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.adapter_batch_app_icon)
        val name: TextView = itemView.findViewById(R.id.adapter_batch_app_name)
        val packageId: TextView = itemView.findViewById(R.id.adapter_batch_app_package_id)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val container: DynamicRippleConstraintLayout = itemView.findViewById(R.id.adapter_batch_app_container)
    }

    inner class Header(itemView: View) : VerticalListViewHolder(itemView) {
        val total: TypeFaceTextView = itemView.findViewById(R.id.adapter_total_apps)
        val sort: DynamicRippleImageButton = itemView.findViewById(R.id.adapter_header_sort_button)
        val filter: DynamicRippleImageButton = itemView.findViewById(R.id.adapter_header_filter_button)
        val search: DynamicRippleImageButton = itemView.findViewById(R.id.adapter_header_search_button)
        val settings: DynamicRippleImageButton = itemView.findViewById(R.id.adapter_header_configuration_button)
    }
}