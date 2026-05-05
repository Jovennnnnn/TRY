package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvDriverName: TextView
    private lateinit var tvTruckId: TextView
    private lateinit var tvCurrentStatus: TextView
    private lateinit var btnStart: android.view.View
    private lateinit var btnPause: android.view.View
    private lateinit var btnFinish: android.view.View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_dashboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.driver_dashboard_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)
        tvDriverName = findViewById(R.id.tvDriverName)
        tvTruckId = findViewById(R.id.tvTruckId)
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus)
        btnStart = findViewById(R.id.btn_start)
        btnPause = findViewById(R.id.btn_pause)
        btnFinish = findViewById(R.id.btn_finish)

        val user = sessionManager.getUser()
        tvDriverName.text = user?.name ?: "Pedro Santos"
        tvTruckId.text = "Truck: GT-001"

        findViewById<android.view.View>(R.id.btn_view_full_route).setOnClickListener {
            showFullRouteDialog()
        }

        findViewById<android.view.View>(R.id.cardNavigation).setOnClickListener {
            showNavigationBottomSheet()
        }

        findViewById<android.view.View>(R.id.cardCollectionLog).setOnClickListener {
            showCollectionLogDialog()
        }

        setupStatusControls()
        setupBottomNavigation()
        setupLogout()
    }

    private fun showFullRouteDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_full_route, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<android.view.View>(R.id.btn_close).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<android.view.View>(R.id.btn_done).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showNavigationBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_navigation, null)
        bottomSheetDialog.setContentView(view)

        view.findViewById<android.view.View>(R.id.btn_next_purok).setOnClickListener {
            com.example.myapplication.utils.CustomNotification.showTopNotification(this, "Navigating to Purok 3...", false)
            bottomSheetDialog.dismiss()
        }

        view.findViewById<android.view.View>(R.id.btn_waste_facility).setOnClickListener {
            com.example.myapplication.utils.CustomNotification.showTopNotification(this, "Navigating to Waste Facility...", false)
            bottomSheetDialog.dismiss()
        }

        view.findViewById<android.view.View>(R.id.btn_base).setOnClickListener {
            com.example.myapplication.utils.CustomNotification.showTopNotification(this, "Navigating back to Base...", false)
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showCollectionLogDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_collection_log, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<android.view.View>(R.id.btn_close).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun setupStatusControls() {
        btnStart.setOnClickListener {
            tvCurrentStatus.text = "ACTIVE & TRACKING"
            tvCurrentStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
        }
        btnPause.setOnClickListener {
            tvCurrentStatus.text = "ON BREAK / PAUSED"
            tvCurrentStatus.setTextColor(android.graphics.Color.parseColor("#F9A825"))
        }
        btnFinish.setOnClickListener {
            tvCurrentStatus.text = "MISSION COMPLETED"
            tvCurrentStatus.setTextColor(android.graphics.Color.parseColor("#1565C0"))
        }
    }

    private fun setupLogout() {
        findViewById<android.view.View>(R.id.btn_logout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showLogoutConfirmation() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_logout_confirmation, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_confirm_logout).setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        alertDialog.show()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_dashboard

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, DriverSettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}