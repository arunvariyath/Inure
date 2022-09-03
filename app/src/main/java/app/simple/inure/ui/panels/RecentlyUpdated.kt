package app.simple.inure.ui.panels

import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.viewModels
import app.simple.inure.R
import app.simple.inure.adapters.home.AdapterRecentlyUpdated
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.dialogs.menus.AppsMenu
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.interfaces.adapters.AppsAdapterCallbacks
import app.simple.inure.viewmodels.panels.HomeViewModel

class RecentlyUpdated : ScopedFragment() {

    private lateinit var recyclerView: CustomVerticalRecyclerView
    private lateinit var adapterRecentlyUpdated: AdapterRecentlyUpdated

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_recently_updated, container, false)

        recyclerView = view.findViewById(R.id.recently_updated_recycler_view)
        adapterRecentlyUpdated = AdapterRecentlyUpdated()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showLoader()

        homeViewModel.getUpdatedApps().observe(viewLifecycleOwner) {
            postponeEnterTransition()
            hideLoader()

            adapterRecentlyUpdated.apps = it
            recyclerView.adapter = adapterRecentlyUpdated

            (view.parent as? ViewGroup)?.doOnPreDraw {
                startPostponedEnterTransition()
            }

            adapterRecentlyUpdated.setOnItemClickListener(object : AppsAdapterCallbacks {
                override fun onAppClicked(packageInfo: PackageInfo, icon: ImageView) {
                    openAppInfo(packageInfo, icon)
                }

                override fun onAppLongPressed(packageInfo: PackageInfo, icon: ImageView) {
                    AppsMenu.newInstance(packageInfo)
                        .show(childFragmentManager, "apps_menu")
                }

                override fun onSearchPressed(view: View) {
                    openFragmentSlide(Search.newInstance(true), "search")
                }

                override fun onSettingsPressed(view: View) {
                    openFragmentSlide(Preferences.newInstance(), "prefs_screen")
                }
            })
        }
    }

    companion object {
        fun newInstance(loading: Boolean = false): RecentlyUpdated {
            val args = Bundle()
            val fragment = RecentlyUpdated()
            args.putBoolean(BundleConstants.loading, loading)
            fragment.arguments = args
            return fragment
        }
    }
}