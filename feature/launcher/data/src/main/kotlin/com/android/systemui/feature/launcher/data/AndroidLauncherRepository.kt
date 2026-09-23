package com.android.systemui.feature.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.android.systemui.feature.launcher.domain.LaunchableApp
import com.android.systemui.feature.launcher.domain.LauncherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

/** PackageManager boundary for the XML app grid and the bottom-bar launcher action. */
@Singleton
class AndroidLauncherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : LauncherRepository {
    private val mutableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())

    override val apps: StateFlow<List<LaunchableApp>> = mutableApps.asStateFlow()

    override suspend fun refresh() {
        withContext(Dispatchers.Default) {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = context.packageManager.queryIntentActivities(launcherIntent, 0)
                .mapNotNull { info ->
                    val activity = info.activityInfo ?: return@mapNotNull null
                    val component = ComponentName(activity.packageName, activity.name)
                    LaunchableApp(
                        packageName = activity.packageName,
                        componentName = component.flattenToString(),
                        label = info.loadLabel(context.packageManager).toString(),
                    )
                }
                .distinctBy(LaunchableApp::componentName)
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            mutableApps.value = resolved
            Timber.tag(TAG).d("Launcher entries refreshed: %s", resolved.size)
        }
    }

    override fun launch(app: LaunchableApp): Result<Unit> = runCatching {
        val component = ComponentName.unflattenFromString(app.componentName)
            ?: error("Invalid launcher component ${app.componentName}")
        context.startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
        Timber.tag(TAG).i("Launched %s", app.componentName)
    }.onFailure { Timber.tag(TAG).e(it, "Unable to launch %s", app.componentName) }

    private companion object {
        const val TAG = "CarSystemUI.Launcher"
    }
}
