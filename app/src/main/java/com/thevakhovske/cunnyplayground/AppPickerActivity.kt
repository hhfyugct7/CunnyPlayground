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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppPickerActivity : AppCompatActivity() {

    private lateinit var rvApps: RecyclerView
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private val selectedApps = mutableSetOf<String>()
    
    private var allApps = listOf<AppInfo>()
    private var displayApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_app_picker)

        // Handle Window Insets
        val root = findViewById<View>(R.id.rootAppPicker)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        rvApps = findViewById(R.id.rvApps)
        rvApps.layoutManager = LinearLayoutManager(this)
        
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarAppPicker)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Select Apps to Cast"
        toolbar.setNavigationOnClickListener { finish() }
        
        searchView = findViewById(R.id.searchApps)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterApps(newText ?: "")
                return true
            }
        })

        supportActionBar?.title = "Select Apps to Cast"

        val prefs = getSharedPreferences("experimental_prefs", MODE_PRIVATE)
        selectedApps.addAll(prefs.getStringSet("cast_enabled_apps", emptySet()) ?: emptySet())

        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { AppInfo(it.loadLabel(pm).toString(), it.packageName, it.loadIcon(pm)) }
            .sortedBy { it.name.lowercase() }
        
        displayApps.clear()
        displayApps.addAll(allApps)
        rvApps.adapter = AppAdapter(displayApps)
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        
        displayApps.clear()
        displayApps.addAll(filtered)
        rvApps.adapter?.notifyDataSetChanged()
    }

    private fun saveSelection() {
        getSharedPreferences("experimental_prefs", MODE_PRIVATE)
            .edit()
            .putStringSet("cast_enabled_apps", HashSet(selectedApps))
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

            val toggleAction = {
                if (selectedApps.contains(item.packageName)) {
                    selectedApps.remove(item.packageName)
                } else {
                    selectedApps.add(item.packageName)
                }
                saveSelection()
                notifyItemChanged(position)
            }

            holder.itemView.setOnClickListener { toggleAction() }
            holder.cbSelected.setOnClickListener { toggleAction() }
        }

        override fun getItemCount() = items.size
    }

    data class AppInfo(val name: String, val packageName: String, val icon: Drawable)
}
