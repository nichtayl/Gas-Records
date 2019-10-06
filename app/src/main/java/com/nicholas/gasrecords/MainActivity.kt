package com.nicholas.gasrecords

import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText

import kotlinx.android.synthetic.main.activity_main.*

const val EXTRA_MESSAGE = "com.nicholas.gasrecords.MESSAGE"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
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
        val editMileageText     = findViewById<EditText>(R.id.editMileage)
        val editGallonsText     = findViewById<EditText>(R.id.editGallons)
        val editTotalText       = findViewById<EditText>(R.id.editTotal)
        val editPricePerText    = findViewById<EditText>(R.id.editPricePer)

        val mileage     = editMileageText.text.toString()
        val gallons     = editGallonsText.text.toString()
        val total       = editTotalText.text.toString()
        val pricePer    = editPricePerText.text.toString()

        val message = "Data: $mileage $gallons $total $pricePer"

        val intent = Intent(this, DisplayMessageActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE, message)
        }
        startActivity(intent)
        // TODO figure out how to make Google API call here
    }
}
