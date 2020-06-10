package ru.tattelecom.scanner.activity

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_scanner.*
import kotlinx.android.synthetic.main.info_no_bluetooth.*
import kotlinx.android.synthetic.main.info_no_permission.*
import org.altbeacon.beacon.*
import ru.tattelecom.scanner.R
import ru.tattelecom.scanner.activity.BeaconActivity.Companion.EXTRA_BEACON
import ru.tattelecom.scanner.adapter.BeaconAdapter
import ru.tattelecom.scanner.adapter.LeDeviceAdapter
import ru.tattelecom.scanner.model.LeDevice
import ru.tattelecom.scanner.model.ScanMode

private const val IBEACON_LAYOUT: String = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"

private const val REQUEST_ENABLE_BT = 1
private const val PERMISSION_REQUEST_FINE_LOCATION = 2

class ScannerActivity : AppCompatActivity(), BeaconConsumer, RangeNotifier {

    private lateinit var beaconManager: BeaconManager

    private val region = Region("AllBeaconsRegion", null, null, null)

    private var leDevices = mutableListOf<LeDevice>()
    private var beacons = mutableListOf<Beacon>()

    private var selectedScanMode = ScanMode.BLE_DEVICES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        // Configure the recycler view.
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Enable bluetooth.
        actionEnableBluetooth.setOnClickListener {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Grant location permission.
        actionGrantLocationPermission.setOnClickListener {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_FINE_LOCATION)
        }

        beaconManager = BeaconManager.getInstanceForApplication(this)
        // Register all types + iBeacon
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.URI_BEACON_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(IBEACON_LAYOUT))
        beaconManager.bind(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.unbind(this)
    }

    override fun onStart() {
        super.onStart()
        startScan(selectedScanMode)
    }

    override fun onStop() {
        super.onStop()
        stopScan()
    }

    private fun startScan(scanMode: ScanMode = ScanMode.BLE_DEVICES) {
        noLocationPermission.visibility = GONE
        bluetoothOff.visibility = GONE
        progressBar.visibility = INVISIBLE

        // Check the Location permission.
        if (!isLocationPermissionsGranted()) {
            noLocationPermission.visibility = VISIBLE
            return
        }

        // Check Bluetooth is enabled.
        bluetoothAdapter?.takeIf { it.isDisabled }?.apply {
            bluetoothOff.visibility = VISIBLE
            return
        }

        progressBar.visibility = VISIBLE

        when (scanMode) {
            ScanMode.BLE_DEVICES -> {
                selectedScanMode = ScanMode.BLE_DEVICES
                recyclerView.adapter = LeDeviceAdapter(leDevices)
                beaconManager.stopRangingBeaconsInRegion(region)
                bluetoothLeScanner.startScan(bleScanCallback)
            }
            ScanMode.BEACONS -> {
                selectedScanMode = ScanMode.BEACONS
                recyclerView.adapter = BeaconAdapter(beacons) { beacon ->
                    val intent = Intent(this, BeaconActivity::class.java)
                    intent.putParcelableExtra(EXTRA_BEACON, beacon)
                    startActivity(intent)
                }
                bluetoothLeScanner.stopScan(bleScanCallback)
                beaconManager.startRangingBeaconsInRegion(region)
            }
        }
    }

    private fun stopScan() {
        beaconManager.stopRangingBeaconsInRegion(region)
        bluetoothLeScanner.stopScan(bleScanCallback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startScan()
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startScan()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_mode, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.scanBleDevices -> {
                startScan(ScanMode.BLE_DEVICES)
                true
            }
            R.id.scanBeacons -> {
                startScan(ScanMode.BEACONS)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val bluetoothLeScanner: BluetoothLeScanner
        get() {
            val bluetoothManager = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            return bluetoothAdapter.bluetoothLeScanner
        }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, scanResult: ScanResult?) {
            if (scanResult != null) {
                // Check if it's a new device.
                val leDevice = leDevices.find { it.device == scanResult.device }
                if (leDevice != null) {
                    leDevice.rssi = scanResult.rssi
                } else {
                    leDevices.add(LeDevice(scanResult.device, scanResult.rssi))
                }
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(this)
    }

    override fun didRangeBeaconsInRegion(beacons: MutableCollection<Beacon>?, region: Region?) {
        if (beacons != null) {
            this.beacons.clear()
            this.beacons.addAll(beacons.toMutableList())
            this.beacons.sortBy { it.distance }
            recyclerView.adapter?.notifyDataSetChanged()
        }
    }
}

fun Activity.isLocationPermissionsGranted() = ContextCompat.checkSelfPermission(this,
    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun Intent.putParcelableExtra(key: String, value: Parcelable) {
    putExtra(key, value)
}