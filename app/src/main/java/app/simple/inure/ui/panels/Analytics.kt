package app.simple.inure.ui.panels

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import app.simple.inure.R
import app.simple.inure.decorations.ripple.DynamicRippleImageButton
import app.simple.inure.decorations.typeface.TypeFaceTextView
import app.simple.inure.dialogs.analytics.AnalyticsMenu
import app.simple.inure.extension.fragments.ScopedFragment
import app.simple.inure.preferences.AnalyticsPreferences
import app.simple.inure.preferences.AppearancePreferences
import app.simple.inure.themes.interfaces.ThemeChangedListener
import app.simple.inure.themes.manager.Theme
import app.simple.inure.themes.manager.ThemeManager
import app.simple.inure.util.FragmentHelper
import app.simple.inure.util.TypeFace
import app.simple.inure.util.ViewUtils.gone
import app.simple.inure.viewmodels.panels.AnalyticsViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.utils.ColorTemplate

class Analytics : ScopedFragment(), ThemeChangedListener {

    private lateinit var settings: DynamicRippleImageButton
    private lateinit var search: DynamicRippleImageButton
    private lateinit var minimumOsPie: PieChart
    private lateinit var targetOsPie: PieChart
    private lateinit var installLocationPie: PieChart
    private lateinit var minSdkHeading: TypeFaceTextView

    private val analyticsViewModel: AnalyticsViewModel by viewModels()

    private val chartOffset = 20F

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_analytics, container, false)

        settings = view.findViewById(R.id.configuration_button)
        search = view.findViewById(R.id.search_button)
        minimumOsPie = view.findViewById(R.id.minimum_os_pie)
        targetOsPie = view.findViewById(R.id.target_os_pie)
        installLocationPie = view.findViewById(R.id.install_location_pie)
        minSdkHeading = view.findViewById(R.id.min_sdk_heading)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            minimumOsPie.gone()
            minSdkHeading.gone()
        }

        ThemeManager.addListener(this)
        startPostponedEnterTransition()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        analyticsViewModel.getMinimumOsData().observe(viewLifecycleOwner) {
            minimumOsPie.apply {
                PieDataSet(it.first, "").apply {
                    data = PieData(this)
                    colors = it.second
                    valueTextColor = Color.TRANSPARENT
                    setEntryLabelColor(Color.TRANSPARENT)
                }

                legend.apply {
                    isEnabled = true
                    formSize = 10F
                    formToTextSpace = 5F
                    form = Legend.LegendForm.DEFAULT
                    textColor = ThemeManager.theme.textViewTheme.secondaryTextColor
                    this.xEntrySpace = 20F
                    this.yEntrySpace = 5F
                    this.typeface = TypeFace.getTypeFace(AppearancePreferences.getAppFont(), TypeFace.TypefaceStyle.MEDIUM.style, requireContext())
                    isWordWrapEnabled = true
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                }

                holeRadius = AnalyticsPreferences.getPieHoleRadiusValue()
                setHoleColor(Color.TRANSPARENT)
                setUsePercentValues(false)
                dragDecelerationFrictionCoef = 0.95f
                isHighlightPerTapEnabled = true
                description.isEnabled = false
                setExtraOffsets(chartOffset, chartOffset, chartOffset, chartOffset)
                setDrawCenterText(false)
                animateXY(1000, 500, Easing.EaseOutCubic)
            }

            /**
             * It's workaround for the bug that messes up the layout
             * of the legend data and solved by call
             * [PieChart.notifyDataSetChanged] two times.
             *
             * TODO - Find a solution
             */
            minimumOsPie.notifyDataSetChanged()
            minimumOsPie.notifyDataSetChanged()
            minimumOsPie.invalidate()
        }

        analyticsViewModel.getTargetOsData().observe(viewLifecycleOwner) {
            targetOsPie.apply {
                PieDataSet(it.first, "").apply {
                    data = PieData(this)
                    colors = it.second
                    valueTextColor = Color.TRANSPARENT
                    setEntryLabelColor(Color.TRANSPARENT)
                }

                legend.apply {
                    isEnabled = true
                    formSize = 10F
                    formToTextSpace = 5F
                    form = Legend.LegendForm.DEFAULT
                    textColor = ThemeManager.theme.textViewTheme.secondaryTextColor
                    this.xEntrySpace = 20F
                    this.yEntrySpace = 5F
                    this.typeface = TypeFace.getTypeFace(AppearancePreferences.getAppFont(), TypeFace.TypefaceStyle.MEDIUM.style, requireContext())
                    isWordWrapEnabled = true
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                }

                holeRadius = AnalyticsPreferences.getPieHoleRadiusValue()
                setHoleColor(Color.TRANSPARENT)
                setUsePercentValues(false)
                dragDecelerationFrictionCoef = 0.95f
                isHighlightPerTapEnabled = true
                description.isEnabled = false
                setExtraOffsets(chartOffset, chartOffset, chartOffset, chartOffset)
                setDrawCenterText(false)

                /**
                 * Won't be visible so we can save some rendering strength here
                 * Let the only above one animate
                 */
                // animateXY(1000, 500, Easing.EaseOutCubic)
            }

            targetOsPie.notifyDataSetChanged()
            targetOsPie.notifyDataSetChanged()
            targetOsPie.invalidate()
        }

        analyticsViewModel.getInstallLocationData().observe(viewLifecycleOwner) {
            installLocationPie.apply {
                PieDataSet(it.first, "").apply {
                    data = PieData(this)
                    colors = ColorTemplate.PASTEL_COLORS.toMutableList()
                    valueTextColor = Color.TRANSPARENT
                    setEntryLabelColor(Color.TRANSPARENT)
                }

                legend.apply {
                    isEnabled = true
                    formSize = 10F
                    formToTextSpace = 5F
                    form = Legend.LegendForm.DEFAULT
                    textColor = ThemeManager.theme.textViewTheme.secondaryTextColor
                    this.xEntrySpace = 20F
                    this.yEntrySpace = 5F
                    this.typeface = TypeFace.getTypeFace(AppearancePreferences.getAppFont(), TypeFace.TypefaceStyle.MEDIUM.style, requireContext())
                    isWordWrapEnabled = true
                    orientation = Legend.LegendOrientation.HORIZONTAL
                    verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                }

                holeRadius = AnalyticsPreferences.getPieHoleRadiusValue()
                setHoleColor(Color.TRANSPARENT)
                setUsePercentValues(false)
                dragDecelerationFrictionCoef = 0.95f
                isHighlightPerTapEnabled = true
                description.isEnabled = false
                setExtraOffsets(chartOffset, chartOffset, chartOffset, chartOffset)
                setDrawCenterText(false)

                // animateXY(1000, 500, Easing.EaseOutCubic)
            }

            installLocationPie.notifyDataSetChanged()
            installLocationPie.notifyDataSetChanged()
            installLocationPie.invalidate()
        }

        settings.setOnClickListener {
            AnalyticsMenu.newInstance()
                .show(childFragmentManager, "analytics_menu")
        }

        search.setOnClickListener {
            clearExitTransition()
            FragmentHelper.openFragment(requireActivity().supportFragmentManager,
                                        Search.newInstance(true),
                                        "preferences_screen")
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            AnalyticsPreferences.sdkValue -> {
                analyticsViewModel.refresh()
            }
            AnalyticsPreferences.pieHoleRadius -> {
                minimumOsPie.apply {
                    holeRadius = AnalyticsPreferences.getPieHoleRadiusValue()
                    invalidate()
                }

                targetOsPie.apply {
                    holeRadius = AnalyticsPreferences.getPieHoleRadiusValue()
                    invalidate()
                }

                installLocationPie.apply {
                    holeRadius = AnalyticsPreferences.getPieHoleRadiusValue()
                    invalidate()
                }
            }
        }
    }

    override fun onThemeChanged(theme: Theme?) {
        super.onThemeChanged(theme)
        minimumOsPie.apply {
            legend.textColor = ThemeManager.theme.textViewTheme.secondaryTextColor
            invalidate()
        }

        targetOsPie.apply {
            legend.textColor = ThemeManager.theme.textViewTheme.secondaryTextColor
            invalidate()
        }

        installLocationPie.apply {
            legend.textColor = ThemeManager.theme.textViewTheme.secondaryTextColor
            invalidate()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ThemeManager.removeListener(this)
    }

    companion object {
        fun newInstance(): Analytics {
            val args = Bundle()
            val fragment = Analytics()
            fragment.arguments = args
            return fragment
        }
    }
}