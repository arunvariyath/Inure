package app.simple.inure.ui.installer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import app.simple.inure.R
import app.simple.inure.adapters.details.AdapterInformation
import app.simple.inure.constants.BundleConstants
import app.simple.inure.decorations.overscroll.CustomVerticalRecyclerView
import app.simple.inure.extensions.fragments.ScopedFragment
import app.simple.inure.factories.installer.InstallerViewModelFactory
import app.simple.inure.viewmodels.installer.InstallerInformationViewModel
import java.io.File

class Information : ScopedFragment() {

    private lateinit var installerInformationViewModel: InstallerInformationViewModel
    private lateinit var recyclerView: CustomVerticalRecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.installer_fragment_information, container, false)

        recyclerView = view.findViewById(R.id.information_recycler_view)

        val file = requireArguments().getSerializable(BundleConstants.file)!! as File
        val installerViewModelFactory = InstallerViewModelFactory(null, file)
        installerInformationViewModel = ViewModelProvider(requireActivity(), installerViewModelFactory)[InstallerInformationViewModel::class.java]

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startPostponedEnterTransition()

        installerInformationViewModel.getInformation().observe(viewLifecycleOwner) {
            recyclerView.adapter = AdapterInformation(it)
        }
    }

    companion object {
        fun newInstance(file: File): Information {
            val args = Bundle()
            args.putSerializable(BundleConstants.file, file)
            val fragment = Information()
            fragment.arguments = args
            return fragment
        }
    }
}