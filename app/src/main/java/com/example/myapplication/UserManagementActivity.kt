package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.models.UserData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout

class UserManagementActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var containerUsers: LinearLayout
    private lateinit var tvEmptyState: TextView
    private lateinit var tabLayout: TabLayout

    private var allResidents = mutableListOf<UserData>()
    private var allDrivers = mutableListOf<UserData>()
    private var allRequests = mutableListOf<UserData>()
    
    private var currentFilteredList = mutableListOf<UserData>()
    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_management)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_management_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeViews()
        loadMockData()
        setupTabLayout()
        setupSearch()
        setupBottomNavigation()

        // Handle initial tab
        val tabIndex = intent.getIntExtra("TAB_INDEX", 0)
        tabLayout.getTabAt(tabIndex)?.select()
        updateList(tabIndex)

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun initializeViews() {
        etSearch = findViewById(R.id.et_search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        containerUsers = findViewById(R.id.container_users)
        tvEmptyState = findViewById(R.id.tv_empty_state)
        tabLayout = findViewById(R.id.tab_layout)
    }

    private fun loadMockData() {
        // Mock Residents
        allResidents.add(UserData(1, "juan_d", "Juan Dela Cruz", "juan@example.com", "resident", "09123456789", "Purok 1"))
        allResidents.add(UserData(2, "maria_s", "Maria Santos", "maria@example.com", "resident", "09234567890", "Purok 3"))
        allResidents.add(UserData(3, "pedro_p", "Pedro Penduko", "pedro@example.com", "resident", "09345678901", "Purok 2"))

        // Mock Drivers
        allDrivers.add(UserData(101, "driver_rex", "Rex Driver", "rex@truck.com", "driver", "09112223333", null, null, "LIC-12345", "Truck-001"))
        allDrivers.add(UserData(102, "driver_lisa", "Lisa Montero", "lisa@truck.com", "driver", "09445556666", null, null, "LIC-67890", "Truck-002"))

        // Mock Requests
        allRequests.add(UserData(201, "new_user", "New Applicant", "new@pending.com", "resident", "09778889999", "Purok 4"))
    }

    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                etSearch.text.clear()
                updateSearchHint()
                updateList(currentTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateSearchHint() {
        etSearch.hint = when (currentTab) {
            0 -> getString(R.string.search_residents)
            1 -> getString(R.string.search_drivers)
            2 -> getString(R.string.search_requests)
            else -> getString(R.string.search)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterList(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
        }
    }

    private fun filterList(query: String) {
        val sourceList = when (currentTab) {
            0 -> allResidents
            1 -> allDrivers
            2 -> allRequests
            else -> allResidents
        }

        currentFilteredList = if (query.isEmpty()) {
            sourceList.toMutableList()
        } else {
            sourceList.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true) ||
                (it.phone?.contains(query) ?: false)
            }.toMutableList()
        }

        displayUsers(currentFilteredList)
    }

    private fun updateList(tabIndex: Int) {
        val listToShow = when (tabIndex) {
            0 -> allResidents
            1 -> allDrivers
            2 -> allRequests
            else -> allResidents
        }
        currentFilteredList = listToShow.toMutableList()
        displayUsers(currentFilteredList)
    }

    private fun displayUsers(users: List<UserData>) {
        containerUsers.removeAllViews()
        
        if (users.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            containerUsers.addView(tvEmptyState)
        } else {
            tvEmptyState.visibility = View.GONE
            val inflater = LayoutInflater.from(this)
            
            for (user in users) {
                val view = inflater.inflate(R.layout.item_user_card, containerUsers, false)
                
                view.findViewById<TextView>(R.id.tv_user_name).text = user.name
                view.findViewById<TextView>(R.id.tv_user_email).text = user.email
                
                val detailText = when (user.role) {
                    "driver" -> "License: ${user.licenseNumber ?: "N/A"} | Truck: ${user.preferredTruck ?: "N/A"}"
                    else -> "Address: ${user.purok ?: ""}, Balintawak"
                }
                view.findViewById<TextView>(R.id.tv_user_detail).text = detailText
                
                val indicator = view.findViewById<View>(R.id.view_role_indicator)
                if (user.role == "driver") {
                    indicator.setBackgroundResource(R.drawable.driver_icon_bg)
                } else {
                    indicator.setBackgroundResource(R.drawable.resident_icon_bg)
                }

                view.setOnClickListener {
                    // Show user details or actions
                }
                
                containerUsers.addView(view)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_users

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_analytics -> {
                    startActivity(Intent(this, AnalyticsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_complaints -> {
                    startActivity(Intent(this, ComplaintsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_users -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, AdminSettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}