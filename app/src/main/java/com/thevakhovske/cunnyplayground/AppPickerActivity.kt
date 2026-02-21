package com.thevakhovske.cunnyplayground

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppPickerActivity : AppCompatActivity() {

    private lateinit var rvApps: RecyclerView
    private val selectedApps = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic layout or simple one
        rvApps = RecyclerView(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(this@AppPickerActivity)
        }
        setContentView(rvApps)
        supportActionBar?.title = "Select Apps to Cast"

        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        selectedApps.addAll(prefs.getStringSet("cast_enabled_apps", emptySet()) ?: emptySet())

        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { AppInfo(it.loadLabel(pm).toString(), it.packageName, it.loadIcon(pm)) }
            .sortedBy { it.name.lowercase() }

        rvApps.adapter = AppAdapter(apps)
    }

    private fun saveSelection() {
        getSharedPreferences("experimental_prefs", MODE_PRIVATE)
            .edit()
            .putStringSet("cast_enabled_apps", selectedApps)
            .apply()
    }

    inner class AppAdapter(private val items: List<AppInfo>) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
            val tvName: TextView = view.findViewById(R.id.tvAppName)
            val tvPackage: TextView = view.findViewById(R.id.tvPackageName)
            val cbSelected: CheckBox = view.findViewById(R.id.cbAppSelected)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivIcon.setImageDrawable(item.icon)
            holder.tvName.text = item.name
            holder.tvPackage.text = item.packageName
            holder.cbSelected.isChecked = selectedApps.contains(item.packageName)

            holder.itemView.setOnClickListener {
                if (selectedApps.contains(item.packageName)) {
                    selectedApps.remove(item.packageName)
                } else {
                    selectedApps.add(item.packageName)
                }
                saveSelection()
                notifyItemChanged(position)
            }
        }

        override fun getItemCount() = items.size
    }

    data class AppInfo(val name: String, val packageName: String, val icon: Drawable)
}
