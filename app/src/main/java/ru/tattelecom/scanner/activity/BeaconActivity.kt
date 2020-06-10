package ru.tattelecom.scanner.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_beacon.*
import org.altbeacon.beacon.Beacon
import ru.tattelecom.scanner.R

class BeaconActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BEACON = "beacon"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_beacon)

        val beacon = intent.getParcelableExtra<Beacon>(EXTRA_BEACON)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "${beacon?.id1}"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        textView.text = "${beacon?.id2}, ${beacon?.id3}, ${beacon?.rssi} dBm, ${"%.2f".format(beacon?.distance)} m"
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}