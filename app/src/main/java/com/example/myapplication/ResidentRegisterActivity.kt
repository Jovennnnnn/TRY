package com.example.myapplication

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
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

class ResidentRegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: com.google.android.material.textfield.TextInputEditText
    private lateinit var etConfirmPassword: com.google.android.material.textfield.TextInputEditText
    private lateinit var tilPassword: com.google.android.material.textfield.TextInputLayout
    private lateinit var tilConfirmPassword: com.google.android.material.textfield.TextInputLayout
    private lateinit var etFullName: EditText
    private lateinit var etContactNumber: EditText
    private lateinit var spinnerPurok: Spinner
    private lateinit var containerPurok: View
    private lateinit var etAddress: EditText
    private lateinit var btnSubmit: View
    private lateinit var pbRegister: ProgressBar
    private lateinit var tvSubmitText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var validationRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_resident_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.resident_register_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupPurokSpinner()
        setupFieldLocking()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }
        btnSubmit.setOnClickListener { performRegistration() }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        tilPassword = findViewById(R.id.til_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etFullName = findViewById(R.id.et_full_name)
        etContactNumber = findViewById(R.id.et_contact_number)
        spinnerPurok = findViewById(R.id.spinner_purok)
        containerPurok = findViewById(R.id.container_purok)
        etAddress = findViewById(R.id.et_address)
        btnSubmit = findViewById(R.id.btn_submit)
        pbRegister = findViewById<ProgressBar>(R.id.pb_register)
        tvSubmitText = findViewById<TextView>(R.id.tv_submit_text)
    }

    private fun setupPurokSpinner() {
        val puroks = arrayOf("Choose Purok...", "Purok 1", "Purok 2", "Purok 3", "Purok 4", "Purok 5", "Purok 6", "Purok 7")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, puroks)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPurok.adapter = adapter
    }

    private fun setupFieldLocking() {
        val allFields = listOf(etUsername, etEmail, etPassword, etConfirmPassword, etFullName, etContactNumber, containerPurok, etAddress, btnSubmit)
        
        lockFields(allFields.drop(1))

        // 1. Username
        etUsername.addValidationWatcher(
            validator = { it.length >= 3 && it.matches("^[a-zA-Z ]+$".toRegex()) },
            errorMsg = "Username must be at least 3 letters",
            onValid = { updateFieldState(etEmail, true) },
            onInvalid = { updateFieldState(etEmail, false) }
        )

        // 2. Email (Format + DB check)
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
            errorMsg = "",
            showError = false,
            onValid = { 
                tilPassword.error = null
                if (etConfirmPassword.text?.isNotEmpty() == true) {
                    validateConfirmPassword()
                } else {
                    updateFieldState(etConfirmPassword, true) 
                }
            },
            onInvalid = { 
                updateFieldState(etConfirmPassword, false) 
            }
        )

        // 4. Confirm Password
        etConfirmPassword.addValidationWatcher(
            validator = { it == etPassword.text.toString() && it.isNotEmpty() },
            errorMsg = "",
            showError = false,
            onValid = { 
                tilConfirmPassword.error = null
                updateFieldState(etFullName, true) 
            },
            onInvalid = { 
                updateFieldState(etFullName, false) 
            }
        )

        // 5. Full Name
        etFullName.addValidationWatcher(
            validator = { it.length >= 2 && it.matches("^[a-zA-Z ]+$".toRegex()) },
            errorMsg = "Full Name must contain letters only",
            onValid = { updateFieldState(etContactNumber, true) },
            onInvalid = { updateFieldState(etContactNumber, false) }
        )

        // 6. Contact Number (11 digits + DB check)
        etContactNumber.addValidationWatcher(
            validator = { it.length == 11 && it.all { c -> c.isDigit() } },
            errorMsg = "Contact number must be exactly 11 digits",
            onValid = {
                checkPhoneInDb(etContactNumber.text.toString()) { exists ->
                    if (exists) {
                        etContactNumber.error = "Number already exists"
                        updateFieldState(containerPurok, false)
                    } else {
                        etContactNumber.error = null
                        updateFieldState(containerPurok, true)
                    }
                }
            },
            onInvalid = { 
                etContactNumber.error = "Contact number must be exactly 11 digits"
                updateFieldState(containerPurok, false) 
            }
        )

        // 7. Purok
        spinnerPurok.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                if (pos > 0) updateFieldState(etAddress, true) else updateFieldState(etAddress, false)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // 8. Address
        etAddress.addValidationWatcher(
            validator = { it.length >= 5 },
            errorMsg = "Complete address is required",
            onValid = { updateFieldState(btnSubmit, true) },
            onInvalid = { updateFieldState(btnSubmit, false) }
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

    private fun updateFieldState(view: View, isValid: Boolean) {
        if (isValid) {
            view.isEnabled = true
            view.alpha = 1.0f
            if (view == containerPurok) {
                spinnerPurok.isEnabled = true
                spinnerPurok.isClickable = true
                spinnerPurok.alpha = 1.0f
            }
        } else {
            view.isEnabled = false
            view.alpha = 0.5f
            if (view == containerPurok) {
                spinnerPurok.isEnabled = false
                spinnerPurok.isClickable = false
                spinnerPurok.alpha = 0.5f
            }
            resetSubsequentFields(view)
        }
    }

    private fun resetSubsequentFields(currentView: View) {
        val allFields = listOf(etUsername, etEmail, etPassword, etConfirmPassword, etFullName, etContactNumber, containerPurok, etAddress, btnSubmit)
        val index = allFields.indexOf(currentView)
        if (index != -1) {
            for (i in index + 1 until allFields.size) {
                allFields[i].isEnabled = false
                allFields[i].alpha = 0.5f
                if (allFields[i] == containerPurok) {
                    spinnerPurok.isEnabled = false
                }
            }
        }
    }

    private fun lockFields(views: List<View>) {
        views.forEach { 
            it.isEnabled = false
            it.alpha = 0.5f
            if (it == containerPurok) {
                spinnerPurok.isEnabled = false
                spinnerPurok.isClickable = false
                spinnerPurok.alpha = 0.5f
            }
        }
    }

    private fun EditText.addValidationWatcher(
        validator: (String) -> Boolean,
        errorMsg: String,
        showError: Boolean = true,
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
                    if (input.isNotEmpty() && showError) {
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

    private fun performRegistration() {
        val request = RegisterRequest(
            username = etUsername.text.toString().trim(),
            name = etFullName.text.toString().trim(),
            email = etEmail.text.toString().trim(),
            password = etPassword.text.toString(),
            role = "resident",
            phone = etContactNumber.text.toString().trim(),
            purok = spinnerPurok.selectedItem.toString(),
            completeAddress = etAddress.text.toString().trim()
        )

        showLoading(true)
        RetrofitClient.instance.register(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                showLoading(false)
                if (response.isSuccessful && response.body()?.success == true) {
                    CustomNotification.showTopNotification(this@ResidentRegisterActivity, "Registration Submitted Successfully", false)
                    handler.postDelayed({ finish() }, 1500)
                } else {
                    val msg = response.body()?.message ?: "Registration Failed"
                    CustomNotification.showTopNotification(this@ResidentRegisterActivity, msg)
                }
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                showLoading(false)
                CustomNotification.showTopNotification(this@ResidentRegisterActivity, "Error: ${t.message}")
            }
        })
    }

    private fun showLoading(show: Boolean) {
        pbRegister.visibility = if (show) View.VISIBLE else View.GONE
        tvSubmitText.text = if (show) "Registering..." else "Submit Registration"
        btnSubmit.isEnabled = !show
        btnSubmit.alpha = if (show) 0.7f else 1.0f
    }
}
