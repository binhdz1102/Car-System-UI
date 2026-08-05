package com.android.systemui.feature.notifications.presentation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.systemui.feature.notifications.domain.NotificationItem
import com.android.systemui.feature.notifications.domain.NotificationUseCases
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@ServiceScoped
class NotificationPanelBinder @Inject constructor(
    private val useCases: NotificationUseCases,
) {
    fun createView(context: Context, onClose: () -> Unit): View =
        LayoutInflater.from(context).inflate(R.layout.notification_panel, null, false).also { view ->
            view.findViewById<ImageButton>(R.id.notification_close).setOnClickListener { onClose() }
            view.findViewById<Button>(R.id.notification_clear_all).setOnClickListener { useCases.clearAll() }
        }

    fun bind(view: View, scope: CoroutineScope) {
        val adapter = NotificationAdapter { key -> useCases.dismiss(key) }
        view.findViewById<RecyclerView>(R.id.notification_list).adapter = adapter
        scope.launch {
            useCases.observe().collectLatest { items ->
                adapter.submitList(items)
                view.findViewById<TextView>(R.id.notification_empty).visibility =
                    if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private class NotificationAdapter(
        private val onDismiss: (String) -> Unit,
    ) : RecyclerView.Adapter<NotificationViewHolder>() {
        private var items: List<NotificationItem> = emptyList()

        fun submitList(newItems: List<NotificationItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder =
            NotificationViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.notification_item, parent, false),
                onDismiss,
            )

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size
    }

    private class NotificationViewHolder(
        view: View,
        private val onDismiss: (String) -> Unit,
    ) : RecyclerView.ViewHolder(view) {
        fun bind(item: NotificationItem) {
            itemView.findViewById<TextView>(R.id.notification_app).text = item.appLabel
            itemView.findViewById<TextView>(R.id.notification_title).text =
                item.title.ifBlank { item.packageName }
            itemView.findViewById<TextView>(R.id.notification_text).text = item.text
            itemView.setOnLongClickListener {
                onDismiss(item.key)
                Timber.tag(TAG).d("Dismissed notification key=%s", item.key)
                true
            }
        }

        private companion object {
            const val TAG = "CarSystemUI.NotificationPanel"
        }
    }
}
