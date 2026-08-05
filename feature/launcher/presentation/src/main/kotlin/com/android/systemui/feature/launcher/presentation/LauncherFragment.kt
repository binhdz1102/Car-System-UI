package com.android.systemui.feature.launcher.presentation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.android.systemui.feature.launcher.domain.LaunchableApp
import com.android.systemui.feature.launcher.domain.LauncherUseCases
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LauncherFragment : Fragment(R.layout.fragment_launcher) {
    @Inject
    lateinit var useCases: LauncherUseCases

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = LauncherAdapter { app -> useCases.launch(app) }
        view.findViewById<RecyclerView>(R.id.launcher_list).adapter = adapter
        val status = view.findViewById<TextView>(R.id.launcher_status)
        view.findViewById<Button>(R.id.launcher_refresh).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { useCases.refresh() }
        }
        view.findViewById<Button>(R.id.launcher_task_view_test).setOnClickListener {
            startActivity(TaskViewTestActivity.intent(requireContext()))
        }
        viewLifecycleOwner.lifecycleScope.launch {
            useCases.observeApps().collectLatest { apps ->
                adapter.submitList(apps)
                status.text = "${apps.size} launcher activities"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch { useCases.refresh() }
    }

    private class LauncherAdapter(
        private val onClick: (LaunchableApp) -> Result<Unit>,
    ) : RecyclerView.Adapter<LauncherViewHolder>() {
        private var items: List<LaunchableApp> = emptyList()

        fun submitList(items: List<LaunchableApp>) {
            this.items = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
            LauncherViewHolder(
                android.view.LayoutInflater.from(parent.context)
                    .inflate(R.layout.launcher_item, parent, false),
                onClick,
            )

        override fun onBindViewHolder(holder: LauncherViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private class LauncherViewHolder(
        view: View,
        private val onClick: (LaunchableApp) -> Result<Unit>,
    ) : RecyclerView.ViewHolder(view) {
        fun bind(app: LaunchableApp) {
            itemView.findViewById<TextView>(R.id.launcher_item_label).text = app.label
            itemView.setOnClickListener {
                onClick(app).onFailure { Timber.tag(TAG).e(it, "Unable to launch %s", app.componentName) }
            }
        }

        private companion object {
            const val TAG = "CarSystemUI.LauncherScreen"
        }
    }
}
