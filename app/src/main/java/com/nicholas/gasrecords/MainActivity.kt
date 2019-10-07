package com.nicholas.gasrecords

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.SignInButton
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.drive.DriveScopes
import kotlinx.android.synthetic.main.content_main.*
import com.google.android.gms.common.api.Scope
import android.app.Activity
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.services.drive.Drive
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import java.util.*
import android.util.Log
import com.google.android.gms.tasks.Task
import androidx.annotation.NonNull
import com.google.android.gms.tasks.OnCompleteListener
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T




const val EXTRA_MESSAGE = "com.nicholas.gasrecords.MESSAGE"
const val RC_SIGN_IN = 9001
const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var mDriveServiceHelper: DriveServiceHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        sign_in_button.visibility = View.VISIBLE
        signed_in_textview.visibility = View.GONE
        sign_out_button.visibility = View.GONE

        sign_in_button.setSize(SignInButton.SIZE_STANDARD)

        sign_in_button.setOnClickListener {
            val signInIntent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            val personName          = account.displayName
            val personGivenName     = account.givenName
            val personFamilyName    = account.familyName
            val personEmail         = account.email
            val personId            = account.id
            val personPhoto         = account.photoUrl
            updateUI(account)
        }

        sign_out_button.setOnClickListener {
            //todo, need a dialog fragment here
//            mGoogleSignInClient.revokeAccess()
            mGoogleSignInClient.signOut()
                .addOnCompleteListener(this) {
                    updateUI(null)
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            RC_SIGN_IN -> if (resultCode == Activity.RESULT_OK && resultData != null) {
                handleSignInResult(resultData)
            }

//            REQUEST_CODE_OPEN_DOCUMENT -> if (resultCode == Activity.RESULT_OK && resultData != null) {
//                val uri = resultData.data
//                if (uri != null) {
//                    openFileFromFilePicker(uri)
//                }
//            }
        }

        super.onActivityResult(requestCode, resultCode, resultData)
    }

    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount ->
                // Use the authenticated account to sign in to the Drive service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, Collections.singleton(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
                )
                    .setApplicationName("Gas Records")
                    .build()

                // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                // Its instantiation is required before handling any onClick actions.
                mDriveServiceHelper = DriveServiceHelper(googleDriveService)

                updateUI(googleAccount)
            }
            .addOnFailureListener { exception -> Log.e(TAG, "Unable to sign in.", exception) }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == RC_SIGN_IN) {
//            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
//            handleSignInResult(task)
//        }
//    }
//
//    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
//        try {
//            val account = completedTask.getResult(ApiException::class.java)
//            updateUI(account)
//            val credential = GoogleAccountCredential.usingOAuth2(
//                this, Collections.singleton(DriveScopes.DRIVE_FILE)
//            )
//            credential.selectedAccount = account
//            val googleDriveService = Drive.Builder(
//                AndroidHttp.newCompatibleTransport(),
//                GsonFactory(),
//                credential
//            ).setApplicationName("Gas Records")
//                .build()
//            mDriveServiceHelper = DriveServiceHelper(googleDriveService)
//        } catch (e: ApiException) {
//            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
//            updateUI(null)
//        }
//    }

    private fun updateUI(account: GoogleSignInAccount?) {
        if (account != null) {
            sign_in_button.visibility = View.GONE
            signed_in_textview.visibility = View.VISIBLE
            signed_in_textview.text = resources.getString(R.string.textview_signed_in, account.displayName)
            sign_out_button.visibility = View.VISIBLE
        } else {
            sign_in_button.visibility = View.VISIBLE
            signed_in_textview.visibility = View.GONE
            sign_out_button.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * send gas refill/mileage data to Google Sheets
     */
    fun sendData(view: View) {
        val mileage     = editMileage.text.toString()
        val gallons     = editGallons.text.toString()
        val total       = editTotal.text.toString()
        val pricePer    = editPricePer.text.toString()

        val message = "Data: $mileage $gallons $total $pricePer"

        val intent = Intent(this, DisplayMessageActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(intent)
        // TODO figure out how to make Google API call here
    }
}
