package org.mywill.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.mywill.client.ApiClient
import org.mywill.client.AppController
import org.mywill.client.WillDto

class MainActivity : AppCompatActivity() {
    private val controller = AppController(ApiClient("http://10.0.2.2:8080"))
    private var currentWillId: Long? = null

    private lateinit var authView: View
    private lateinit var listView: View
    private lateinit var editorView: View
    private lateinit var bottomNavigation: View
    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private lateinit var willsRecyclerView: RecyclerView
    private lateinit var adapter: WillsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        authView = findViewById(R.id.authView)
        listView = findViewById(R.id.listView)
        editorView = findViewById(R.id.editorView)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)
        willsRecyclerView = findViewById(R.id.willsRecyclerView)

        setupAuth()
        setupNavigation()
        setupEditor()
        setupRecyclerView()

        showSection("auth")
    }

    private fun showSection(section: String) {
        authView.visibility = if (section == "auth") View.VISIBLE else View.GONE
        listView.visibility = if (section == "list") View.VISIBLE else View.GONE
        editorView.visibility = if (section == "editor") View.VISIBLE else View.GONE
        bottomNavigation.visibility = if (section != "auth") View.VISIBLE else View.GONE
    }

    private fun showStatus(message: String, isError: Boolean = false) {
        statusTextView.text = message
        statusTextView.visibility = View.VISIBLE
        statusTextView.postDelayed({
            statusTextView.visibility = View.GONE
        }, 3000)
    }

    private fun setupAuth() {
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                val res = controller.login(email, password)
                progressBar.visibility = View.GONE
                if (res.success) {
                    showSection("list")
                    loadMyWills()
                } else {
                    showStatus(res.message, true)
                }
            }
        }

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                val res = controller.register(email, password)
                progressBar.visibility = View.GONE
                showStatus(res.message, !res.success)
            }
        }
    }

    private fun setupNavigation() {
        findViewById<Button>(R.id.navMyWills).setOnClickListener {
            lifecycleScope.launch { loadMyWills() }
        }
        findViewById<Button>(R.id.navSharedWills).setOnClickListener {
            lifecycleScope.launch { loadSharedWills() }
        }
        findViewById<Button>(R.id.navNewWill).setOnClickListener {
            currentWillId = null
            findViewById<EditText>(R.id.willTitleEditText).setText("")
            findViewById<EditText>(R.id.willContentEditText).setText("")
            findViewById<View>(R.id.accessLayout).visibility = View.GONE
            showSection("editor")
        }
    }

    private fun setupEditor() {
        val titleEdit = findViewById<EditText>(R.id.willTitleEditText)
        val contentEdit = findViewById<EditText>(R.id.willContentEditText)
        val saveBtn = findViewById<Button>(R.id.saveWillButton)
        val accessEmailEdit = findViewById<EditText>(R.id.accessEmailEditText)
        val addAccessBtn = findViewById<Button>(R.id.addAccessButton)
        val allowedEmailsText = findViewById<TextView>(R.id.allowedEmailsTextView)

        saveBtn.setOnClickListener {
            val title = titleEdit.text.toString()
            val content = contentEdit.text.toString()
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                val res = if (currentWillId == null) {
                    controller.createWill(title, content)
                } else {
                    controller.updateWill(currentWillId!!, title, content)
                }
                progressBar.visibility = View.GONE
                if (res != null) {
                    showStatus("Saved")
                    currentWillId = res.id
                    findViewById<View>(R.id.accessLayout).visibility = View.VISIBLE
                    allowedEmailsText.text = "Access: ${res.allowedEmails.joinToString()}"
                } else {
                    showStatus("Error saving", true)
                }
            }
        }

        addAccessBtn.setOnClickListener {
            val email = accessEmailEdit.text.toString()
            val id = currentWillId
            if (id != null && email.isNotBlank()) {
                lifecycleScope.launch {
                    progressBar.visibility = View.VISIBLE
                    val res = controller.addAccess(id, email)
                    progressBar.visibility = View.GONE
                    if (res != null) {
                        showStatus("Access granted")
                        allowedEmailsText.text = "Access: ${res.allowedEmails.joinToString()}"
                        accessEmailEdit.setText("")
                    } else {
                        showStatus("Error adding access", true)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = WillsAdapter { will ->
            currentWillId = will.id
            findViewById<EditText>(R.id.willTitleEditText).setText(will.title)
            findViewById<EditText>(R.id.willContentEditText).setText(will.content)
            
            val isMyWill = controller.state.myWills.any { it.id == will.id }
            findViewById<View>(R.id.accessLayout).visibility = if (isMyWill) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.allowedEmailsTextView).text = "Access: ${will.allowedEmails.joinToString()}"
            findViewById<Button>(R.id.saveWillButton).visibility = if (isMyWill) View.VISIBLE else View.GONE
            
            showSection("editor")
        }
        willsRecyclerView.layoutManager = LinearLayoutManager(this)
        willsRecyclerView.adapter = adapter
    }

    private suspend fun loadMyWills() {
        showSection("list")
        findViewById<TextView>(R.id.listTitleTextView).text = "My Wills"
        progressBar.visibility = View.VISIBLE
        val wills = controller.loadMyWills()
        progressBar.visibility = View.GONE
        adapter.submitList(wills)
    }

    private suspend fun loadSharedWills() {
        showSection("list")
        findViewById<TextView>(R.id.listTitleTextView).text = "Shared Wills"
        progressBar.visibility = View.VISIBLE
        val wills = controller.loadSharedWills()
        progressBar.visibility = View.GONE
        adapter.submitList(wills)
    }
}

class WillsAdapter(private val onClick: (WillDto) -> Unit) : RecyclerView.Adapter<WillsAdapter.ViewHolder>() {
    private var items = listOf<WillDto>()

    fun submitList(newList: List<WillDto>) {
        items = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.text1.text = item.title
        holder.text2.text = item.content.take(50)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text1: TextView = view.findViewById(android.R.id.text1)
        val text2: TextView = view.findViewById(android.R.id.text2)
    }
}
