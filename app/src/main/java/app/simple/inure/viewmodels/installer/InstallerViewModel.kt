package app.simple.inure.viewmodels.installer

import android.app.Application
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.simple.inure.apk.installer.InstallerUtils
import app.simple.inure.apk.utils.PackageData
import app.simple.inure.apk.utils.PackageData.getInstallerDir
import app.simple.inure.apk.utils.PackageUtils.getPackageArchiveInfo
import app.simple.inure.extensions.viewmodels.RootShizukuViewModel
import app.simple.inure.helpers.ShizukuServiceHelper
import app.simple.inure.models.User
import app.simple.inure.preferences.ConfigurationPreferences
import app.simple.inure.shizuku.PackageInstaller
import app.simple.inure.util.ConditionUtils.invert
import app.simple.inure.util.FileUtils
import app.simple.inure.util.FileUtils.escapeSpecialCharactersForUnixPath
import app.simple.inure.util.FileUtils.getLength
import app.simple.inure.util.NullSafety.isNull
import app.simple.inure.util.StringUtils.endsWithAny
import com.anggrayudi.storage.file.baseName
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.lingala.zip4j.ZipFile
import java.io.File

class InstallerViewModel(application: Application, private val uri: Uri?, val file: File?) : RootShizukuViewModel(application) {

    private var files: ArrayList<File>? = null
    private var splitApkFiles: ArrayList<File>? = null
    private var baseApk: File? = null
    private var user: User? = null
    private val splitApkExtensions = arrayOf(".zip", ".apks", ".apkm", ".xapk")

    private val packageInfo: MutableLiveData<PackageInfo> by lazy {
        MutableLiveData<PackageInfo>().also {
            viewModelScope.launch(Dispatchers.Default) {
                prepare()
            }
        }
    }

    private val baseApkLiveData: MutableLiveData<File> by lazy {
        MutableLiveData<File>()
    }

    private val success: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    fun getPackageInfo(): LiveData<PackageInfo> {
        return packageInfo
    }

    fun getFile(): LiveData<File> {
        return baseApkLiveData
    }

    fun getSuccess(): LiveData<Int> {
        return success
    }

    private fun prepare() {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                extractFiles()
                createPackageInfoAndFilterFiles()
            }.getOrElse {
                it.printStackTrace()
                postWarning(it.message ?: "Unknown error")
            }
        }
    }

    private fun extractFiles() {
        clearInstallerCache()
        PackageData.makePackageFolder(applicationContext())

        if (file != null && file.exists()) {
            if (file.name.endsWithAny(*splitApkExtensions)) {
                ZipFile(file.path).extractAll(file.path.substringBeforeLast("."))
                files = File(file.path.substringBeforeLast("."))
                    .listFiles()!!.toList() as ArrayList<File> /* = java.util.ArrayList<java.io.File> */
            } else if (file.name.endsWith(".apk")) {
                files = arrayListOf(file)
            }
        } else {
            uri?.let { it ->
                val documentFile = DocumentFile.fromSingleUri(applicationContext(), it)!!
                val sourceFile = if (documentFile.name!!.endsWith(".apk")) {
                    applicationContext().getInstallerDir(documentFile.name!!)
                } else {
                    applicationContext().getInstallerDir(documentFile.baseName + ".zip")
                }

                if (!sourceFile.exists()) {
                    contentResolver.openInputStream(it).use {
                        FileUtils.copyStreamToFile(it!!, sourceFile)
                    }
                }

                if (documentFile.name!!.endsWithAny(*splitApkExtensions)) {
                    ZipFile(sourceFile.path).extractAll(sourceFile.path.substringBeforeLast("."))
                    files = File(sourceFile.path.substringBeforeLast("."))
                        .listFiles()!!.toList() as ArrayList<File> /* = java.util.ArrayList<java.io.File> */
                } else if (documentFile.name!!.endsWith(".apk")) {
                    files = arrayListOf(sourceFile)
                }
            }
        }
    }

    private fun createPackageInfoAndFilterFiles() {
        files!!.filter { it.absolutePath.endsWith(".apk") }

        if (files!!.size > 1) {
            @Suppress("UNCHECKED_CAST")
            splitApkFiles = files!!.clone() as ArrayList<File>
        } else {
            splitApkFiles = arrayListOf()
        }

        var packageInfo: PackageInfo? = null

        /**
         * Find base/master apk
         */
        for (file in files!!) {
            packageInfo = packageManager.getPackageArchiveInfo(file) ?: continue
            packageInfo.applicationInfo.sourceDir = file.absolutePath
            packageInfo.applicationInfo.publicSourceDir = file.absolutePath
            packageInfo.applicationInfo.name = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
            this.packageInfo.postValue(packageInfo)
            baseApkLiveData.postValue(file)
            baseApk = file

            try {
                splitApkFiles!!.remove(file)
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            break
        }

        if (packageInfo.isNull()) {
            throw Exception("ERR: unable to get package info")
        }
    }

    private fun packageManagerInstall() {
        viewModelScope.launch(Dispatchers.Default) {
            val sessionParams = InstallerUtils.makeInstallParams(files!!.getLength())
            val sessionCode = InstallerUtils.createSession(sessionParams, applicationContext())

            for (file in files!!) {
                if (file.exists() && file.name.endsWith(".apk")) {
                    InstallerUtils.installWriteSessions(sessionCode, file, applicationContext())
                }
            }

            InstallerUtils.commitSession(sessionCode, applicationContext())
        }
    }

    private fun rootInstall() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Shell.cmd("run-as ${application.packageName}").exec()

                val totalSizeOfAllApks = files!!.getLength()
                Log.d(TAG, "Total size of all apks: $totalSizeOfAllApks")
                val sessionId = with(Shell.cmd("${installCommand()} $totalSizeOfAllApks").exec()) {
                    Log.d(TAG, "Output: $out")
                    with(out[0]) {
                        substringAfter("[").substringBefore("]").toInt()
                    }
                }

                Log.d(TAG, "Session id: $sessionId")
                for (file in files!!) {
                    if (file.exists() && file.name.endsWith(".apk")) {
                        val size = file.length()
                        Log.d(TAG, "Size of ${file.name}: $size")
                        val splitName = file.name.substringBeforeLast(".")
                        Log.d(TAG, "Split name: $splitName")
                        val idx = files?.indexOf(file)
                        Log.d(TAG, "Index: $idx")

                        val path = file.absolutePath.escapeSpecialCharactersForUnixPath()
                        Log.d(TAG, "Path: $path")

                        Shell.cmd("pm install-write -S $size $sessionId $idx $path").exec().let {
                            Log.d(TAG, "Output: ${it.out}")
                            Log.d(TAG, "Error: ${it.err}")
                        }
                    }
                }

                Shell.cmd("pm install-commit $sessionId").exec().let { result ->
                    if (result.isSuccess) {
                        Log.d(TAG, "Output: ${result.out}")
                        Log.d(TAG, "Error: ${result.err}")
                        success.postValue((0..50).random())

                        Log.d(TAG, "Setting installer to ${application.packageName} for ${packageInfo.value!!.packageName}")
                        Shell.cmd("pm set-installer ${packageInfo.value!!.packageName} ${application.packageName}").exec().let {
                            if (it.isSuccess) {
                                Log.d(TAG, "Installer set to ${application.packageName} for ${packageInfo.value!!.packageName}")
                            } else {
                                Log.d(TAG, "Unable to set installer to ${application.packageName} for ${packageInfo.value!!.packageName}")
                                Log.e(TAG, "Output: ${it.out}")
                            }
                        }
                    } else {
                        Log.d(TAG, "Output: ${result.out}")
                        Log.d(TAG, "Error: ${result.err}")
                        postWarning(result.out.joinToString())
                    }
                }
            } catch (e: java.lang.NullPointerException) {
                if (e.message.isNullOrEmpty().invert()) {
                    postWarning(e.message!!)
                } else {
                    postError(e)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                postError(e)
            }
        }
    }

    fun install(user: User?) {
        this.user = user

        if (ConfigurationPreferences.isUsingShizuku() || ConfigurationPreferences.isUsingRoot()) {
            initializeCoreFramework()
        } else {
            packageManagerInstall()
        }
    }

    override fun onShellCreated(shell: Shell?) {
        rootInstall()
    }

    override fun onShellDenied() {
        packageManagerInstall()
    }

    override fun onShizukuCreated(shizukuServiceHelper: ShizukuServiceHelper) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Shizuku install")

            try {
                val uris = files!!.map { file ->
                    FileProvider.getUriForFile(applicationContext(), "${applicationContext().packageName}.provider", file)
                }

                val packageInstaller = PackageInstaller()
                val shizukuInstall = packageInstaller.install(uris, applicationContext())

                if (shizukuInstall.status == android.content.pm.PackageInstaller.STATUS_SUCCESS) {
                    success.postValue((0..50).random())
                } else {
                    postWarning("ERR: ${shizukuInstall.status} : ${shizukuInstall.message}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                postWarning(e.message ?: "Unknown error")
            }
        }
    }

    override fun onShizukuDenied() {
        /**
         * We don't want to show warning here.
         */
        // super.onShizukuDenied()
        packageManagerInstall()
    }

    private fun clearInstallerCache() {
        kotlin.runCatching {
            if (File(applicationContext().cacheDir.path + "/installer_cache/").deleteRecursively()) {
                Log.d(javaClass.name, "Installer cache cleared")
            }
        }
    }

    private fun installCommand(): String {
        /**
         * Users feature is only available after Nougat
         */
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            "pm install-create -i ${application.packageName} --user ${user?.id ?: getCurrentUser()} -S"
        } else {
            "pm install-create -i ${application.packageName} -S"
        }
    }

    fun installAnyway() {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val path = packageInfo.value!!.applicationInfo?.sourceDir?.escapeSpecialCharactersForUnixPath()

                Shell.cmd("run-as ${application.packageName}").exec()
                Shell.cmd("pm install --bypass-low-target-sdk-block $path").exec().let {
                    if (it.isSuccess) {
                        success.postValue((0..50).random())
                    } else {
                        postWarning(it.err.joinToString())
                        Log.e(TAG, "Error: ${it.err}")
                    }

                    Log.d(TAG, "Output: ${it.out}")
                }
            }.onFailure {
                postWarning(it.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private const val TAG = "InstallerViewModel"
    }
}
