package ani.dantotsu.util

import android.app.AlertDialog
import android.content.Context
import android.view.View
import ani.dantotsu.R

class AlertDialogBuilder(private val context: Context) {
    private var title: String? = null
    private var message: String? = null
    private var posButtonTitle: String? = null
    private var negButtonTitle: String? = null
    private var neutralButtonTitle: String? = null
    private var onPositiveButtonClick: (() -> Unit)? = null
    private var onNegativeButtonClick: (() -> Unit)? = null
    private var onNeutralButtonClick: (() -> Unit)? = null
    private var items: Array<String>? = null
    private var checkedItems: BooleanArray? = null
    private var onItemsSelected: ((BooleanArray) -> Unit)? = null
    private var selectedItemIndex: Int = -1
    private var onItemSelected: ((Int) -> Unit)? = null
    private var customView: View? = null
    private var attach: ((dialog: AlertDialog) -> Unit)? = null
    fun setTitle(title: String?): AlertDialogBuilder {
        this.title = title
        return this
    }

    fun setTitle(int: Int, formatArgs: Int? = null): AlertDialogBuilder {
        this.title = context.getString(int, formatArgs)
        return this
    }

    fun setMessage(message: String?): AlertDialogBuilder {
        this.message = message
        return this
    }

    fun setMessage(stringInt: Int, vararg formatArgs: Any): AlertDialogBuilder {
        this.message = context.getString(stringInt, *formatArgs)
        return this
    }

    fun setCustomView(view: View): AlertDialogBuilder {
        this.customView = view
        return this
    }

    fun setPosButton(title: String?, onClick: (() -> Unit)? = null): AlertDialogBuilder {
        this.posButtonTitle = title
        this.onPositiveButtonClick = onClick
        return this
    }

    fun setPosButton(
        int: Int,
        formatArgs: Int? = null,
        onClick: (() -> Unit)? = null
    ): AlertDialogBuilder {
        this.posButtonTitle = context.getString(int, formatArgs)
        this.onPositiveButtonClick = onClick
        return this
    }

    fun setNegButton(title: String?, onClick: (() -> Unit)? = null): AlertDialogBuilder {
        this.negButtonTitle = title
        this.onNegativeButtonClick = onClick
        return this
    }

    fun setNegButton(
        int: Int,
        formatArgs: Int? = null,
        onClick: (() -> Unit)? = null
    ): AlertDialogBuilder {
        this.negButtonTitle = context.getString(int, formatArgs)
        this.onNegativeButtonClick = onClick
        return this
    }

    fun setNeutralButton(title: String?, onClick: (() -> Unit)? = null): AlertDialogBuilder {
        this.neutralButtonTitle = title
        this.onNeutralButtonClick = onClick
        return this
    }

    fun setNeutralButton(
        int: Int,
        formatArgs: Int? = null,
        onClick: (() -> Unit)? = null
    ): AlertDialogBuilder {
        this.neutralButtonTitle = context.getString(int, formatArgs)
        this.onNeutralButtonClick = onClick
        return this
    }

    fun attach(attach: ((dialog: AlertDialog) -> Unit)?): AlertDialogBuilder {
        this.attach = attach
        return this
    }

    fun singleChoiceItems(
        items: Array<String>,
        selectedItemIndex: Int = -1,
        onItemSelected: (Int) -> Unit
    ): AlertDialogBuilder {
        this.items = items
        this.selectedItemIndex = selectedItemIndex
        this.onItemSelected = onItemSelected
        return this
    }

    fun multiChoiceItems(
        items: Array<String>,
        checkedItems: BooleanArray? = null,
        onItemsSelected: (BooleanArray) -> Unit
    ): AlertDialogBuilder {
        this.items = items
        this.checkedItems = checkedItems ?: BooleanArray(items.size) { false }
        this.onItemsSelected = onItemsSelected
        return this
    }

    fun show() {
        val builder = AlertDialog.Builder(context, R.style.MyPopup)
        if (title != null) builder.setTitle(title)
        if (message != null) builder.setMessage(message)
        if (customView != null) builder.setView(customView)
        if (items != null) {
            if (onItemSelected != null) {
                builder.setSingleChoiceItems(items, selectedItemIndex) { _, which ->
                    selectedItemIndex = which
                    onItemSelected?.invoke(which)
                }
            } else if (checkedItems != null && onItemsSelected != null) {
                builder.setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                    checkedItems?.set(which, isChecked)
                }
            }
        }
        if (posButtonTitle != null) {
            builder.setPositiveButton(posButtonTitle) { dialog, _ ->
                onPositiveButtonClick?.invoke()
                dialog.dismiss()
            }
        }
        if (negButtonTitle != null) {
            builder.setNegativeButton(negButtonTitle) { dialog, _ ->
                onNegativeButtonClick?.invoke()
                dialog.dismiss()
            }
        }
        if (neutralButtonTitle != null) {
            builder.setNeutralButton(neutralButtonTitle) { dialog, _ ->
                onNeutralButtonClick?.invoke()
                dialog.dismiss()
            }
        }
        builder.setCancelable(false)
        val dialog = builder.create()
        attach?.invoke(dialog)
        dialog.window?.setDimAmount(0.8f)
        dialog.show()
    }

}

fun Context.customAlertDialog(): AlertDialogBuilder {
    return AlertDialogBuilder(this)
}