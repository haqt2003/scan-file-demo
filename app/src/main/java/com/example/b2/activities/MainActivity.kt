package com.example.b2.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.b2.R
import com.example.b2.adapters.FileAdapter
import com.example.b2.databinding.ActivityMainBinding
import com.example.b2.models.FileData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var adapter: FileAdapter
    private val items = mutableListOf<FileData>()
    private val itemsCheck = mutableListOf<FileData>()

    private var isScanned = false

    private var job: Job? = null

    private val storageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                startScan()
            } else {
                Toast.makeText(this, "Quyền chưa được cấp - N", Toast.LENGTH_SHORT).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = FileAdapter(items)
        binding.rvFiles.adapter = adapter
        binding.rvFiles.layoutManager = LinearLayoutManager(this@MainActivity)

        val resultPermission = checkPermission()
        if (resultPermission) {
            startScan()
            startScanCheck()
            binding.btPause.isEnabled = true
            binding.btScan.isEnabled = false
            binding.btScan.setBackgroundColor(getColor(R.color.grey))
            binding.btPause.setBackgroundColor(getColor(R.color.red))
        } else {
            binding.btPause.isEnabled = false
            binding.btScan.isEnabled = true
            binding.btScan.setBackgroundColor(getColor(R.color.green))
            binding.btPause.setBackgroundColor(getColor(R.color.grey))
            requestPermission()
        }

        with(binding) {
            btPause.setOnClickListener {
                btPause.isEnabled = false
                btScan.isEnabled = true
                btScan.setBackgroundColor(getColor(R.color.green))
                btPause.setBackgroundColor(getColor(R.color.grey))
                stopScan()
            }

            btScan.setOnClickListener {
                if (checkPermission()) {
                    btPause.isEnabled = true
                    btScan.isEnabled = false
                    btScan.setBackgroundColor(getColor(R.color.grey))
                    btPause.setBackgroundColor(getColor(R.color.red))
                    if (isScanned) {
                        btScan.text = "Continue Scanning"
                        items.clear()
                        adapter.notifyDataSetChanged()
                        itemsCheck.clear()
                        isScanned = false
                        startScanCheck()
                    }
                    startScan()
                } else {
                    requestPermission()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startScan() {
        job = CoroutineScope(Dispatchers.IO).launch {
            getStorageInfo()
            val rootDir = File(Environment.getExternalStorageDirectory().absolutePath)
            scanFiles(rootDir)
        }
    }

    private fun stopScan() {
        job?.cancel()
    }

    private fun startScanCheck() {
        CoroutineScope(Dispatchers.IO).launch {
            val rootDir = File(Environment.getExternalStorageDirectory().absolutePath)
            scanFilesCheck(rootDir)
        }
    }

    private suspend fun scanFiles(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanFiles(file)
            } else {
                val fileData = FileData(
                    name = file.name,
                    path = file.path,
                    type = file.extension,
                    size = file.length().toString()
                )
                if (items.none { it.path == fileData.path }) {
                    withContext(Dispatchers.Main) {
                        items.add(0, fileData)
                        adapter.notifyItemInserted(0)
                        binding.rvFiles.scrollToPosition(0)
                        binding.tvCount.text = "File scanned: ${items.size}"
                    }
                    delay(1000L)
                }
            }
        }
        if (items.size == itemsCheck.size) {
            withContext(Dispatchers.Main) {
                isScanned = true
                binding.btPause.isEnabled = false
                binding.btScan.isEnabled = true
                binding.btScan.setBackgroundColor(getColor(R.color.green))
                binding.btScan.text = "Restart Scanning"
                binding.btPause.setBackgroundColor(getColor(R.color.grey))
            }
        }
    }

    private suspend fun scanFilesCheck(directory: File) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                scanFilesCheck(file)
            } else {
                val fileData = FileData(
                    name = file.name,
                    path = file.path,
                    type = file.extension,
                    size = file.length().toString()
                )
                if (itemsCheck.none { it.path == fileData.path }) {
                    withContext(Dispatchers.Main) {
                        itemsCheck.add(0, fileData)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun getStorageInfo() {
        val freeSpace = Environment.getExternalStorageDirectory().freeSpace
        val totalSpace = Environment.getExternalStorageDirectory().totalSpace
        withContext(Dispatchers.Main) {
            binding.tvFree.text = "Free: ${formatSize(freeSpace)}"
            binding.tvMemory.text = "Memory: ${formatSize(totalSpace)}"
        }
    }

    private fun formatSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> String.format("%d B", size)
        }
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        } else {
            val write =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = android.net.Uri.fromParts("package", packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            }.onFailure {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan()
                binding.btPause.isEnabled = true
                binding.btScan.isEnabled = false
                binding.btScan.setBackgroundColor(getColor(R.color.grey))
                binding.btPause.setBackgroundColor(getColor(R.color.red))
            } else {
                Toast.makeText(this, "Quyền chưa được cấp - O", Toast.LENGTH_SHORT).show()
            }
        }
    }

}