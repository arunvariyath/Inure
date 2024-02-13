package app.simple.inure.ui.preferences.mainscreens

import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import app.simple.inure.BuildConfig
import app.simple.inure.R
import app.simple.inure.decorations.ripple.DynamicRippleConstraintLayout
import app.simple.inure.decorations.ripple.DynamicRippleRelativeLayout
import app.simple.inure.decorations.toggles.Switch
import app.simple.inure.dialogs.configuration.AppPath.Companion.showAppPathDialog
import app.simple.inure.dialogs.miscellaneous.StoragePermission
import app.simple.inure.dialogs.miscellaneous.StoragePermission.Companion.showStoragePermissionDialog
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.preferences.ConfigurationPreferences
import app.simple.inure.ui.preferences.subscreens.ComponentManager
import app.simple.inure.ui.preferences.subscreens.Language
import app.simple.inure.ui.preferences.subscreens.Shortcuts
import app.simple.inure.util.PermissionUtils.checkStoragePermission
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

class ConfigurationScreen : ScopedFragment() {

    private lateinit var keepScreenOnSwitchView: Switch
    private lateinit var shortcuts: DynamicRippleRelativeLayout
    private lateinit var components: DynamicRippleRelativeLayout
    private lateinit var language: DynamicRippleRelativeLayout
    private lateinit var path: DynamicRippleConstraintLayout
    private lateinit var showUsersSwitch: Switch
    private lateinit var rootSwitchView: Switch
    private lateinit var shizukuSwitchView: Switch

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.preferences_configuration, container, false)

        keepScreenOnSwitchView = view.findViewById(R.id.configuration_switch_keep_screen_on)
        shortcuts = view.findViewById(R.id.configuration_shortcuts)
        components = view.findViewById(R.id.configuration_component_manager)
        language = view.findViewById(R.id.configuration_language)
        path = view.findViewById(R.id.configuration_path)
        showUsersSwitch = view.findViewById(R.id.configuration_show_user_list_switch)
        rootSwitchView = view.findViewById(R.id.configuration_root_switch_view)
        shizukuSwitchView = view.findViewById(R.id.configuration_shizuku_switch_view)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.forEach {
                when (it.key) {
                    ShizukuProvider.PERMISSION -> {
                        if (it.value) {
                            ConfigurationPreferences.setUsingShizuku(true)
                            shizukuSwitchView.isChecked = true
                        } else {
                            ConfigurationPreferences.setUsingShizuku(false)
                            shizukuSwitchView.isChecked = false
                        }
                    }
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startPostponedEnterTransition()

        keepScreenOnSwitchView.isChecked = ConfigurationPreferences.isKeepScreenOn()
        showUsersSwitch.isChecked = ConfigurationPreferences.isShowUsersList()
        rootSwitchView.isChecked = ConfigurationPreferences.isUsingRoot()
        shizukuSwitchView.isChecked = ConfigurationPreferences.isUsingShizuku()

        keepScreenOnSwitchView.setOnSwitchCheckedChangeListener { isChecked ->
            ConfigurationPreferences.setKeepScreenOn(isChecked)

            if (isChecked) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        shortcuts.setOnClickListener {
            openFragmentSlide(Shortcuts.newInstance(), "shortcuts")
        }

        components.setOnClickListener {
            openFragmentSlide(ComponentManager.newInstance(), "components")
        }

        language.setOnClickListener {
            openFragmentSlide(Language.newInstance(), "language")
        }

        path.setOnClickListener {
            if (requireContext().checkStoragePermission()) {
                childFragmentManager.showAppPathDialog()
            } else {
                childFragmentManager.showStoragePermissionDialog().setStoragePermissionCallbacks(object : StoragePermission.Companion.StoragePermissionCallbacks {
                    override fun onStoragePermissionGranted() {
                        childFragmentManager.showAppPathDialog()
                    }
                })
            }
        }

        showUsersSwitch.setOnSwitchCheckedChangeListener {
            ConfigurationPreferences.setShowUsersList(it)
        }

        rootSwitchView.setOnSwitchCheckedChangeListener { it ->
            if (it) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    kotlin.runCatching {
                        Shell.enableVerboseLogging = BuildConfig.DEBUG
                        Shell.setDefaultBuilder(
                                Shell.Builder
                                    .create()
                                    .setFlags(Shell.FLAG_REDIRECT_STDERR or Shell.FLAG_MOUNT_MASTER)
                                    .setTimeout(10))
                    }.getOrElse {
                        it.printStackTrace()
                    }

                    Shell.getShell() // Request root access

                    if (Shell.isAppGrantedRoot() == true) {
                        withContext(Dispatchers.Main) {
                            ConfigurationPreferences.setUsingRoot(true)
                            rootSwitchView.isChecked = true
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            ConfigurationPreferences.setUsingRoot(false)
                            rootSwitchView.isChecked = false
                        }
                    }
                }
            } else {
                ConfigurationPreferences.setUsingRoot(false)
                rootSwitchView.isChecked = false
            }
        }

        shizukuSwitchView.setOnSwitchCheckedChangeListener {
            if (it) {
                requestPermissionLauncher.launch(arrayOf(ShizukuProvider.PERMISSION))
            } else {
                ConfigurationPreferences.setUsingShizuku(false)
            }
        }
    }

    @Suppress("unused")
    private fun isShizukuPermissionGranted(): Boolean {
        return if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            requireActivity().checkSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            ConfigurationPreferences.isUsingRoot -> {
                // rootSwitchView.setChecked(ConfigurationPreferences.isUsingRoot())
            }
        }
    }

    companion object {
        fun newInstance(): ConfigurationScreen {
            val args = Bundle()
            val fragment = ConfigurationScreen()
            fragment.arguments = args
            return fragment
        }
    }
}