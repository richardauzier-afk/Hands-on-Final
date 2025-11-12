package com.example.notificationapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSendNotification: Button
    
    private val CHANNEL_ID = "notification_channel"
    private val NOTIFICATION_PERMISSION_CODE = 123
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Inicializar views
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSendNotification = findViewById(R.id.buttonSendNotification)
        
        // Criar canal de notificação
        createNotificationChannel()
        
        // Configurar botão
        buttonSendNotification.setOnClickListener {
            val message = editTextMessage.text.toString()
            
            if (message.isNotEmpty()) {
                // Verificar permissão para Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        sendNotification(message)
                    } else {
                        // Solicitar permissão
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            NOTIFICATION_PERMISSION_CODE
                        )
                    }
                } else {
                    // Para versões anteriores ao Android 13
                    sendNotification(message)
                }
            } else {
                Toast.makeText(this, "Por favor, digite uma mensagem", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun createNotificationChannel() {
        // Criar canal de notificação (necessário para Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Canal de Notificações"
            val descriptionText = "Canal para notificações do aplicativo"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun sendNotification(message: String) {
        // Criar notificação
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Nova Mensagem")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        
        with(NotificationManagerCompat.from(this)) {
            // Usar timestamp como ID único para cada notificação
            val notificationId = System.currentTimeMillis().toInt()
            
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notify(notificationId, builder.build())
                Toast.makeText(
                    this@MainActivity,
                    "Notificação enviada!",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Limpar o campo de texto
                editTextMessage.text.clear()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permissão concedida, enviar notificação
                    val message = editTextMessage.text.toString()
                    if (message.isNotEmpty()) {
                        sendNotification(message)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Permissão de notificação negada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }
}
