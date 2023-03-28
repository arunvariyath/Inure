package app.simple.inure.adapters.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import app.simple.inure.R
import app.simple.inure.decorations.fastscroll.PopupTextProvider
import app.simple.inure.decorations.overscroll.VerticalListViewHolder
import app.simple.inure.decorations.ripple.DynamicRippleConstraintLayout
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.glide.modules.GlideApp
import app.simple.inure.glide.util.ImageLoader.loadAPKIcon
import app.simple.inure.interfaces.adapters.AdapterCallbacks
import app.simple.inure.popups.apps.PopupAppsCategory
import app.simple.inure.preferences.MainPreferences
import app.simple.inure.util.FileSizeHelper.toSize
import app.simple.inure.util.RecyclerViewUtils
import app.simple.inure.util.Sort
import java.util.*

class AdapterApks : RecyclerView.Adapter<VerticalListViewHolder>(), PopupTextProvider {

    var apps = arrayListOf<String>()
    private lateinit var adapterCallbacks: AdapterCallbacks

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalListViewHolder {
        return when (viewType) {
            RecyclerViewUtils.TYPE_HEADER -> {
                Header(LayoutInflater.from(parent.context)
                           .inflate(R.layout.adapter_header_apks, parent, false))
            }
            RecyclerViewUtils.TYPE_ITEM -> {
                Holder(LayoutInflater.from(parent.context)
                           .inflate(R.layout.adapter_all_apps_small_details, parent, false))
            }
            else -> {
                throw IllegalArgumentException("there is no type that matches the type $viewType, make sure your using types correctly")
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onBindViewHolder(holder: VerticalListViewHolder, position_: Int) {
        val position = position_ - 1
        if (holder is Holder) {

            holder.icon.loadAPKIcon(apps[position])
            holder.name.text = apps[position].substring(apps[position].lastIndexOf("/") + 1)
            holder.packageId.text = apps[position]
            holder.info.text = apps[position].toSize() + " | " + apps[position].substring(apps[position].lastIndexOf(".") + 1).uppercase(Locale.getDefault())

            holder.container.setOnClickListener {
                adapterCallbacks.onApkClicked(it, holder.bindingAdapterPosition.minus(1))
            }

            holder.container.setOnLongClickListener {
                adapterCallbacks.onApkLongClicked(it, holder.bindingAdapterPosition.minus(1))
                true
            }
        }

        if (holder is Header) {
            holder.total.text = String.format(holder.itemView.context.getString(R.string.total), apps.size)

            holder.category.text = when (MainPreferences.getAppsCategory()) {
                PopupAppsCategory.USER -> {
                    holder.getString(R.string.user)
                }
                PopupAppsCategory.SYSTEM -> {
                    holder.getString(R.string.system)
                }
                PopupAppsCategory.BOTH -> {
                    with(StringBuilder()) {
                        append(holder.getString(R.string.user))
                        append(" | ")
                        append(holder.getString(R.string.system))
                    }
                }
                else -> {
                    holder.getString(R.string.unknown)
                }
            }

            holder.sorting.text = when (MainPreferences.getSortStyle()) {
                Sort.NAME -> {
                    holder.getString(R.string.name)
                }
                Sort.PACKAGE_NAME -> {
                    holder.getString(R.string.package_name)
                }
                Sort.INSTALL_DATE -> {
                    holder.getString(R.string.install_date)
                }
                Sort.SIZE -> {
                    holder.getString(R.string.app_size)
                }
                else -> {
                    holder.getString(R.string.unknown)
                }
            }
        }
    }

    override fun onViewRecycled(holder: VerticalListViewHolder) {
        super.onViewRecycled(holder)
        if (holder is Holder) {
            GlideApp.with(holder.icon).clear(holder.icon)
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
            RecyclerViewUtils.TYPE_HEADER
        } else RecyclerViewUtils.TYPE_ITEM
    }

    fun setOnItemClickListener(adapterCallbacks: AdapterCallbacks) {
        this.adapterCallbacks = adapterCallbacks
    }

    inner class Holder(itemView: View) : VerticalListViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.adapter_all_app_icon)
        val name: TypeFaceTextView = itemView.findViewById(R.id.adapter_all_app_name)
        val packageId: TypeFaceTextView = itemView.findViewById(R.id.adapter_recently_app_package_id)
        val info: TypeFaceTextView = itemView.findViewById(R.id.adapter_all_app_info)
        val container: DynamicRippleConstraintLayout = itemView.findViewById(R.id.adapter_all_app_container)
    }

    inner class Header(itemView: View) : VerticalListViewHolder(itemView) {
        val total: TypeFaceTextView = itemView.findViewById(R.id.adapter_total_apps)
        val sorting: TypeFaceTextView = itemView.findViewById(R.id.adapter_header_sorting)
        val category: TypeFaceTextView = itemView.findViewById(R.id.adapter_header_category)
        val loader: View = itemView.findViewById(R.id.loader)
    }

    override fun getPopupText(position: Int): String {
        return apps[position].substring(apps[position]
                                            .lastIndexOf("/") + 1)[0].toString().uppercase(Locale.getDefault())
    }
}
