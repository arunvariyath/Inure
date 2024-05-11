package app.simple.inure.popups.viewers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import app.simple.inure.R
import app.simple.inure.constants.Misc
import app.simple.inure.decorations.ripple.DynamicRippleTextView
import app.simple.inure.extensions.popup.BasePopupWindow
import app.simple.inure.extensions.popup.PopupLinearLayout

class PopupInformation(anchor: View, string: String, showAsDropDown: Boolean = true) : BasePopupWindow() {
    init {
        val contentView = LayoutInflater.from(anchor.context).inflate(R.layout.popup_information_menu, PopupLinearLayout(anchor.context))

        contentView.findViewById<DynamicRippleTextView>(R.id.popup_copy).setOnClickListener {
            val clipBoard = contentView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Info", string)
            clipBoard.setPrimaryClip(clipData)
            dismiss()
        }

        if (showAsDropDown) {
            init(contentView, anchor, Misc.xOffset, Misc.yOffset)
        } else {
            initAtLocation(contentView, anchor, Misc.xOffset, Misc.yOffset)
        }
    }
}
