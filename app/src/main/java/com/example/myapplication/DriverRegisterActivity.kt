package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.models.ApiResponse
import com.example.myapplication.models.RegisterRequest
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.utils.CustomNotification
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DriverRegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: com.google.android.material.textfield.TextInputEditText
    private lateinit var etConfirmPassword: com.google.android.material.textfield.TextInputEditText
    private lateinit var tilPassword: com.google.android.material.textfield.TextInputLayout
    private lateinit var tilConfirmPassword: com.google.android.material.textfield.TextInputLayout
    private lateinit var etFullName: EditText
    private lateinit var etLicenseNumber: EditText
    private lateinit var etContactNumber: EditText
    private lateinit var etTruckAssignment: EditText
    private lateinit var btnSubmit: View
    private lateinit var pbRegister: ProgressBar
    private lateinit var tvSubmitText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var validationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_driver_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.driver_register_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupFieldLocking()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }
        btnSubmit.setOnClickListener { registerDriver() }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etFullName = findViewById(R.id.et_full_name)
        etLicenseNumber = findViewById(R.id.et_license_number)
        etContactNumber = findViewById(R.id.et_contact_number)
        etTruckAssignment = findViewById<EditText>(R.id.et_truck_assignment)
        btnSubmit = findViewById(R.id.btn_submit)
        pbRegister = findViewById<ProgressBar>(R.id.pb_register)
        tvSubmitText = findViewById<TextView>(R.id.tv_submit_text)
    }

    private fun setupFieldLocking() {
        val allFields = listOf(etUsername, etEmail, etPassword, etConfirmPassword, etFullName, etLicenseNumber, etContactNumber, etTruckAssignment, btnSubmit)
        
        lockFields(allFields.drop(1))

        // 1. Username
        etUsername.addValidationWatcher(
            validator = { it.length >= 3 && it.matches("^[a-zA-Z ]+$".toRegex()) },
            errorMsg = "Username must be at least 3 letters",
            onValid = { updateFieldState(etEmail, true) },
            onInvalid = { updateFieldState(etEmail, false) }
        )

        // 2. Email
        etEmail.addValidationWatcher(
            validator = { android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() },
            errorMsg = "Invalid email format (e.g., user@email.com)",
            onValid = { 
                checkEmailInDb(etEmail.text.toString()) { exists ->
                    if (exists) {
                        etEmail.error = "Email is already registered"
                        updateFieldState(etPassword, false)
                    } else {
                        etEmail.error = null
                        updateFieldState(etPassword, true)
                    }
                }
            },
            onInvalid = { 
                etEmail.error = "Invalid email format"
                updateFieldState(etPassword, false) 
            }
        )

        // 3. Password
        val passPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$".toRegex()
        etPassword.addValidationWatcher(
            validator = { it.matches(passPattern) },
            errorMsg = "Password must be at least 8 characters with upper, lower, number, and symbol",
            onValid = { 
                tilPassword.error = null
                if (etConfirmPassword.text?.isNotEmpty() == true) {
                    validateConfirmPassword()
                } else {
                    updateFieldState(etConfirmPassword, true) 
                }
            },
            onInvalid = { 
                if (etPassword.text?.isNotEmpty() == true) {
                    tilPassword.error = "Password must be at least 8 characters with upper, lower, number, and symbol"
                }
                updateFieldState(etConfirmPassword, false) 
            }
        )

        // 4. Confirm Password
        etConfirmPassword.addValidationWatcher(
            validator = { it == etPassword.text.toString() && it.isNotEmpty() },
            errorMsg = "Passwords do not match",
            onValid = { 
                tilConfirmPassword.error = null
                updateFieldState(etFullName, true) 
            },
            onInvalid = { 
                if (etConfirmPassword.text?.isNotEmpty() == true) {
                    tilConfirmPassword.error = "Passwords do not match"
                }
                updateFieldState(etFullName, false) 
            }
        )

        // 5. Full Name
        etFullName.addValidationWatcher(
            validator = { it.length >= 2 && it.matches("^[a-zA-Z ]+$".toRegex()) },
            errorMsg = "Full Name must contain letters only",
            onValid = { updateFieldState(etLicenseNumber, true) },
            onInvalid = { updateFieldState(etLicenseNumber, false) }
        )

        // 6. License Number
        etLicenseNumber.addValidationWatcher(
            validator = { it.length >= 5 && it.all { c -> c.isDigit() || c == '-' } },
            errorMsg = "Please enter a valid license number (numbers and hyphens only)",
            onValid = { 
                checkLicenseInDb(etLicenseNumber.text.toString()) { exists ->
                    if (exists) {
                        etLicenseNumber.error = "License number already exists"
                        updateFieldState(etContactNumber, false)
                    } else {
                        etLicenseNumber.error = null
                        updateFieldState(etContactNumber, true)
                    }
                }
            },
            onInvalid = { 
                etLicenseNumber.error = "Invalid license number format"
                updateFieldState(etContactNumber, false) 
            }
        )

        // 7. Contact Number
        etContactNumber.addValidationWatcher(
            validator = { it.length == 11 && it.all { c -> c.isDigit() } },
            errorMsg = "Contact number must be exactly 11 digits",
            onValid = { 
                checkPhoneInDb(etContactNumber.text.toString()) { exists ->
                    if (exists) {
                        etContactNumber.error = "Contact number already exists"
                        updateFieldState(etTruckAssignment, false)
                    } else {
                        etContactNumber.error = null
                        updateFieldState(etTruckAssignment, true)
                    }
                }
            },
            onInvalid = { 
                etContactNumber.error = "Contact number must be exactly 11 digits"
                updateFieldState(etTruckAssignment, false)
            }
        )

        // 8. Preferred Truck
        etTruckAssignment.addValidationWatcher(
            validator = { it.isEmpty() || it.matches("^[a-zA-Z0-9-]+$".toRegex()) },
            errorMsg = "Letters, numbers, and hyphens only",
            onValid = {
                val truckId = etTruckAssignment.text.toString().trim()
                if (truckId.isNotEmpty()) {
                    checkTruckInDb(truckId) { exists ->
                        if (exists) {
                            etTruckAssignment.error = "Truck ID already assigned or exists"
                            updateFieldState(btnSubmit, false)
                        } else {
                            etTruckAssignment.error = null
                            updateFieldState(btnSubmit, true)
                        }
                    }
                } else {
                    etTruckAssignment.error = null
                    updateFieldState(btnSubmit, true)
                }
            },
            onInvalid = { 
                etTruckAssignment.error = "Letters, numbers, and hyphens only"
                updateFieldState(btnSubmit, false) 
            }
        )

        allFields.forEachIndexed { index, view ->
            if (index > 0) {
                view.setOnTouchListener { v, _ ->
                    if (!v.isEnabled) {
                        CustomNotification.showTopNotification(this, "Please complete the previous field correctly first")
                    }
                    false
                }
            }
        }
    }

    private fun validateConfirmPassword() {
        if (etConfirmPassword.text.toString() == etPassword.text.toString()) {
            tilConfirmPassword.error = null
            updateFieldState(etFullName, true)
        } else {
            tilConfirmPassword.error = "Passwords do not match"
            updateFieldState(etFullName, false)
        }
    }

    private fun checkEmailInDb(email: String, callback: (Boolean) -> Unit) {
        RetrofitClient.instance.checkEmail(email).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                callback(response.isSuccessful && response.body()?.success == true)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) { callback(false) }
        })
    }

    private fun checkPhoneInDb(phone: String, callback: (Boolean) -> Unit) {
        RetrofitClient.instance.checkPhone(phone).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                callback(response.isSuccessful && response.body()?.success == true)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) { callback(false) }
        })
    }

    private fun checkLicenseInDb(license: String, callback: (Boolean) -> Unit) {
        RetrofitClient.instance.checkLicense(license).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                callback(response.isSuccessful && response.body()?.success == true)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) { callback(false) }
        })
    }

    private fun checkTruckInDb(truckId: String, callback: (Boolean) -> Unit) {
        RetrofitClient.instance.checkTruck(truckId).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                callback(response.isSuccessful && response.body()?.success == true)
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) { callback(false) }
        })
    }

    private fun updateFieldState(view: View, isValid: Boolean) {
        if (isValid) {
            view.isEnabled = true
            view.alpha = 1.0f
        } else {
            view.isEnabled = false
            view.alpha = 0.5f
            resetSubsequentFields(view)
        }
    }

    private fun resetSubsequentFields(currentView: View) {
        val allFields = listOf(etUsername, etEmail, etPassword, etConfirmPassword, etFullName, etLicenseNumber, etContactNumber, etTruckAssignment, btnSubmit)
        val index = allFields.indexOf(currentView)
        if (index != -1) {
            for (i in index + 1 until allFields.size) {
                allFields[i].isEnabled = false
                allFields[i].alpha = 0.5f
            }
        }
    }

    private fun lockFields(views: List<View>) {
        views.forEach { 
            it.isEnabled = false
            it.alpha = 0.5f
        }
    }

    private fun EditText.addValidationWatcher(
        validator: (String) -> Boolean,
        errorMsg: String,
        onValid: () -> Unit,
        onInvalid: () -> Unit
    ) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                validationRunnable?.let { handler.removeCallbacks(it) }
                
                if (validator(input)) {
                    this@addValidationWatcher.error = null
                    onValid()
                } else {
                    onInvalid()
                    if (input.isNotEmpty()) {
                        validationRunnable = Runnable { 
                            this@addValidationWatcher.error = errorMsg
                        }
                        handler.postDelayed(validationRunnable!!, 2000)
                    }
                }
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    private fun registerDriver() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val contactNumber = etContactNumber.text.toString().trim()
        val licenseNumber = etLicenseNumber.text.toString().trim()
        val truckAssignment = etTruckAssignment.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || fullName.isEmpty() || contactNumber.isEmpty() || licenseNumber.isEmpty()) {
            CustomNotification.showTopNotification(this, "Please fill in all required fields")
            return
        }

        val request = RegisterRequest(
            username = username,
            name = fullName,
            email = email,
            password = password,
            role = "driver",
            phone = contactNumber,
            licenseNumber = licenseNumber,
            preferredTruck = truckAssignment.ifEmpty { null }
        )

        showLoading(true)
        RetrofitClient.instance.register(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    CustomNotification.showTopNotification(this@DriverRegisterActivity, "Registration Submitted Successfully", false)
                    handler.postDelayed({ finish() }, 1500)
                } else {
                    val errorMsg = response.body()?.message ?: "Registration failed"
                    CustomNotification.showTopNotification(this@DriverRegisterActivity, errorMsg)
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showLoading(false)
                CustomNotification.showTopNotification(this@DriverRegisterActivity, "Error: ${t.message}")
            }
        })
    }

    private fun showLoading(show: Boolean) {
        pbRegister.visibility = if (show) View.VISIBLE else View.GONE
        tvSubmitText.text = if (show) "Registering..." else "Register as Driver"
        btnSubmit.isEnabled = !show
        btnSubmit.alpha = if (show) 0.7f else 1.0f
    }
}
