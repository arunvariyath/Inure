package app.simple.inure.widgets

import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.widget.RemoteViews
import android.widget.Toast
import app.simple.inure.R
import app.simple.inure.constants.ServiceConstants
import app.simple.inure.receivers.AdminReceiver

class LockScreenWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        /**
         * Loop for every App Widget instance that belongs to this provider.
         * Noting, that is, a user might have multiple instances of the same
         * widget on their home screen.
         */
        for (appWidgetID in appWidgetIds!!) {
            val remoteViews = RemoteViews(context!!.packageName, R.layout.widget_lock)
            remoteViews.setOnClickPendingIntent(R.id.lock_screen, getPendingSelfIntent(context, ServiceConstants.actionWidgetLockScreen))
            appWidgetManager!!.updateAppWidget(appWidgetID, remoteViews)
        }
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        try {
            if (intent!!.action == ServiceConstants.actionWidgetLockScreen) {
                lock(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getPendingSelfIntent(context: Context, action: String): PendingIntent? {
        val intent = Intent(context, this.javaClass)
        intent.action = action
        return PendingIntent.getBroadcast(context, 123, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * A general technique for calling the onUpdate method,
     * requiring only the context parameter.
     *
     * @author John Bentley, based on Android-er code.
     * @see [Android-er > 2010-10-19 > Update Widget in onReceive]
     *      (http://android-er.blogspot.com.au/2010/10/update-widget-in-onreceive-method.html)
     */
    private fun onUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        /**
         * Uses getClass().getName() rather than MyWidget.class.getName() for
         * portability into any App Widget Provider Class
         */
        val thisAppWidgetComponentName = ComponentName(context.packageName, javaClass.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun lock(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager?
        if (pm!!.isInteractive) {
            val policy = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager?
            try {
                policy!!.lockNow()
            } catch (ex: SecurityException) {
                ex.printStackTrace()
                try {
                    Toast.makeText(context, "must enable device administrator", Toast.LENGTH_LONG).show()
                    with(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)) {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, AdminReceiver::class.java))
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Additional text explaining why this needs to be added.")
                        context.startActivity(this)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}