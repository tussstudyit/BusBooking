package com.example.busbooking.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.busbooking.R
import com.example.busbooking.data.session.ISessionManager
import com.example.busbooking.data.session.UserRole
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

// ════════════════════════════════════════════════════════════════════════════════
// SPLASHFRAGMENT - APP STARTUP LOGIC (Role-Based Navigation)
// ════════════════════════════════════════════════════════════════════════════════

/**
 * SplashFragment is the first screen shown after app launch.
 *
 * Responsibilities:
 *   1. Check SessionManager for active session
 *   2. If NOT logged in → Navigate to LoginFlow
 *   3. If logged in → Check user role:
 *      ├─ If USER → Navigate to UserFlow
 *      └─ If ADMIN → Navigate to AdminFlow
 *
 * Important:
 *   - Always runs on app startup (set as startDestination)
 *   - Enforces multi-layer protection
 *   - No back button allowed
 */
class SplashFragment : Fragment() {

    // Inject SessionManager dependency
    private val sessionManager: ISessionManager by inject()

    private lateinit var navController: androidx.navigation.NavController
    private lateinit var progressBar: ProgressBar
    private lateinit var logoView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = findNavController()
        progressBar = view.findViewById(R.id.splash_progress_bar)
        logoView = view.findViewById(R.id.splash_logo)

        // Show splash animation (2 seconds)
        view.postDelayed({
            checkSessionAndNavigate()
        }, 2000)  // 2 second splash duration
    }

    private fun checkSessionAndNavigate() {
        lifecycleScope.launch {
            try {
                // 🔍 STEP 1: Check if user is logged in
                val isLoggedIn = sessionManager.isLoggedIn()

                if (!isLoggedIn) {
                    // Not authenticated → Go to Login Flow
                    navigateToLogin()
                    return@launch
                }

                // 🔍 STEP 2: Get user role
                val role = sessionManager.getCurrentUserRole()

                // 🔍 STEP 3: Route based on role (Guard enforcement)
                when (role) {
                    UserRole.USER -> {
                        navigateToUserFlow()
                    }
                    UserRole.ADMIN -> {
                        navigateToAdminFlow()
                    }
                }
            } catch (e: Exception) {
                // Error reading session → Force login
                navigateToLogin()
            }
        }
    }

    private fun navigateToLogin() {
        val action = SplashFragmentDirections.actionSplashToLogin()
        navController.navigate(action)
    }

    private fun navigateToUserFlow() {
        val action = SplashFragmentDirections.actionSplashToUser()
        navController.navigate(action)
    }

    private fun navigateToAdminFlow() {
        val action = SplashFragmentDirections.actionSplashToAdmin()
        navController.navigate(action)
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// ADMINDASHBOARDFRAGMENT - EXAMPLE WITH ROLE GUARD
// ════════════════════════════════════════════════════════════════════════════════

/**
 * AdminDashboardFragment is ONLY accessible by users with ADMIN role.
 *
 * Multi-layer protection:
 *   Layer 1: Navigation Graph (only accessible from AdminFlow)
 *   Layer 2: Fragment Guard (check role in onViewCreated)
 *   Layer 3: ViewModel/Repository (validate role before DB ops)
 */
class AdminDashboardFragment : Fragment() {

    private val sessionManager: ISessionManager by inject()
    private val navController: androidx.navigation.NavController by lazy {
        findNavController()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            // 🔒 GUARD: Check user role (Layer 2 - Fragment level)
            if (!sessionManager.isAdminUser()) {
                // User is trying to access admin screen without admin role
                showUnauthorizedDialog()
                return@launch
            }

            // ✅ User has admin role, continue loading
            setupUI()
            setupViewModelObservers()
        }
    }

    private fun showUnauthorizedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Access Denied")
            .setMessage("You don't have permission to access this screen.")
            .setPositiveButton("OK") { _, _ ->
                // Pop this fragment and go back to user dashboard
                navController.popBackStack(R.id.userDashboardFragment, false)
            }
            .setCancelable(false)
            .show()
    }

    private fun setupUI() {
        // Load admin dashboard content
    }

    private fun setupViewModelObservers() {
        // Observe ViewModel state
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// GENERIC FRAGMENT GUARD PATTERN (Copy for other protected fragments)
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Base class or pattern for protected fragments
 */
abstract class RoleProtectedFragment(
    @Suppress("unused") val requiredRole: UserRole
) : Fragment() {

    protected val sessionManager: ISessionManager by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔒 GUARD: Validate role
        validateRoleOrPopBack()
    }

    private fun validateRoleOrPopBack() {
        lifecycleScope.launch {
            val currentRole = sessionManager.getCurrentUserRole()

            if (currentRole != requiredRole) {
                // Unauthorized access
                findNavController().popBackStack()
                return@launch
            }

            // Authorized, continue setup
            onRoleValidated()
        }
    }

    // Override in subclasses
    protected abstract fun onRoleValidated()
}

// ════════════════════════════════════════════════════════════════════════════════
// LOGINFRAGMENT - EXAMPLE WITH SESSION SAVE
// ════════════════════════════════════════════════════════════════════════════════

/**
 * LoginFragment demonstrates:
 *   - Calling repository to login
 *   - Saving session to SessionManager
 *   - Navigation to splash (which checks role)
 */
class LoginFragment : Fragment() {

    private val sessionManager: ISessionManager by inject()

    // TODO: Inject AuthRepository
    // private val authRepository: AuthRepository by inject()

    fun doLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // Call repository to login
                // val result = authRepository.login(email, password)

                // if (result is Result.Success) {
                //     // Create UserSession
                //     val session = UserSession(
                //         userId = result.data.id,
                //         email = result.data.email,
                //         name = result.data.name,
                //         role = UserRole.valueOf(result.data.role),
                //         loginTime = System.currentTimeMillis()
                //     )
                //
                //     // 💾 Save to SessionManager
                //     sessionManager.saveSession(session)
                //
                //     // Navigate to splash → splash will check role → route to correct flow
                //     findNavController().navigate(
                //         LoginFragmentDirections.actionLoginSuccess()
                //     )
                // } else {
                //     showError("Login failed")
                // }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// LOGOUTPATTERN - How to clear session
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Example logout flow from dashboard
 */
fun handleLogout(
    sessionManager: ISessionManager,
    navController: androidx.navigation.NavController,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        // 1. Clear session
        sessionManager.clearSession()

        // 2. Navigate to login flow
        // Action definition removes all back stack entries
        navController.navigate(
            R.id.actionUserDashboardLogout  // or actionAdminDashboardLogout
        )
    }
}

