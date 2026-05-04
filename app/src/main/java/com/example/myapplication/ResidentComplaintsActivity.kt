package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.models.Complaint
import com.example.myapplication.models.ComplaintsResponse
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResidentComplaintsActivity : AppCompatActivity() {

    private lateinit var complaintsContainer: LinearLayout
    private lateinit var tvTotalComplaints: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvInProgressCount: TextView
    private lateinit var tvResolvedCount: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resident_complaints)

        sessionManager = SessionManager(this)
        complaintsContainer = findViewById(R.id.complaints_container)
        tvTotalComplaints = findViewById(R.id.tv_total_complaints)
        tvPendingCount = findViewById(R.id.tv_pending_count)
        tvInProgressCount = findViewById(R.id.tv_in_progress_count)
        tvResolvedCount = findViewById(R.id.tv_resolved_count)

        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        findViewById<View>(R.id.btn_new_complaint).setOnClickListener {
            startActivity(Intent(this, FileComplaintActivity::class.java))
        }

        setupBottomNavigation()
        fetchMyComplaints()
    }

    override fun onResume() {
        super.onResume()
        fetchMyComplaints()
    }

    private fun fetchMyComplaints() {
        RetrofitClient.instance.getComplaints().enqueue(object : Callback<ComplaintsResponse> {
            override fun onResponse(call: Call<ComplaintsResponse>, response: Response<ComplaintsResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val complaints = response.body()?.data ?: emptyList()
                    val currentUserId = sessionManager.getUser()?.userId ?: -1
                    val myComplaints = complaints.filter { it.userId == currentUserId }
                    updateUI(myComplaints)
                } else {
                    Toast.makeText(this@ResidentComplaintsActivity, "Failed to load complaints", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ComplaintsResponse>, t: Throwable) {
                Toast.makeText(this@ResidentComplaintsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(complaints: List<Complaint>) {
        tvTotalComplaints.text = "${complaints.size} total"
        complaintsContainer.removeAllViews()

        var pendingCount = 0
        var inProgressCount = 0
        var resolvedCount = 0

        for (complaint in complaints) {
            when (complaint.status.uppercase()) {
                "PENDING" -> pendingCount++
                "IN PROGRESS" -> inProgressCount++
                "RESOLVED" -> resolvedCount++
            }

            val cardView = LayoutInflater.from(this).inflate(R.layout.item_complaint, complaintsContainer, false)
            
            val tvCategory = cardView.findViewById<TextView>(R.id.tvCategory)
            val tvStatus = cardView.findViewById<TextView>(R.id.tvStatus)
            val tvResidentName = cardView.findViewById<TextView>(R.id.tvResidentName)
            val tvDescription = cardView.findViewById<TextView>(R.id.tvDescription)
            val tvDate = cardView.findViewById<TextView>(R.id.tvDate)
            val layoutActions = cardView.findViewById<LinearLayout>(R.id.layoutActions)
            val layoutAdminResponse = cardView.findViewById<LinearLayout>(R.id.layoutAdminResponse)
            val tvAdminResponse = cardView.findViewById<TextView>(R.id.tvAdminResponse)
            val layoutResolvedDate = cardView.findViewById<LinearLayout>(R.id.layoutResolvedDate)
            val tvResolvedDate = cardView.findViewById<TextView>(R.id.tvResolvedDate)

            tvCategory.text = complaint.category
            tvStatus.text = complaint.status.uppercase()
            tvResidentName.visibility = View.GONE // Hide name for resident's own view
            tvDescription.text = complaint.description
            tvDate.text = complaint.createdAt

            layoutActions.visibility = View.GONE // Residents cannot change status

            when (complaint.status.uppercase()) {
                "PENDING" -> {
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#F9A825"))
                    tvStatus.background.setTint(android.graphics.Color.parseColor("#FFF9C4"))
                    layoutAdminResponse.visibility = View.GONE
                    layoutResolvedDate.visibility = View.GONE
                }
                "IN PROGRESS" -> {
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#1E88E5"))
                    tvStatus.background.setTint(android.graphics.Color.parseColor("#E3F2FD"))
                    layoutAdminResponse.visibility = View.GONE
                    layoutResolvedDate.visibility = View.GONE
                }
                "RESOLVED" -> {
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#43A047"))
                    tvStatus.background.setTint(android.graphics.Color.parseColor("#E8F5E9"))
                    layoutAdminResponse.visibility = View.VISIBLE
                    tvAdminResponse.text = complaint.adminResponse ?: "No response provided."
                    layoutResolvedDate.visibility = View.VISIBLE
                    tvResolvedDate.text = "Resolved: ${complaint.updatedAt ?: complaint.createdAt}"
                }
            }

            complaintsContainer.addView(cardView)
        }

        tvPendingCount.text = pendingCount.toString()
        tvInProgressCount.text = inProgressCount.toString()
        tvResolvedCount.text = resolvedCount.toString()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_complaints

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, ResidentDashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_track -> {
                    startActivity(Intent(this, TrackTrucksActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_complaints -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, ResidentSettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}