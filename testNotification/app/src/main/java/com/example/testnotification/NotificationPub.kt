package com.example.testnotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class NotificationPub : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("debug", "Trying rcv notif")

        // Recebe o texto e ID da Intent
        val textoNotificacao = intent.getStringExtra("TEXTO_NOTIFICACAO") ?: "Texto padrão"
        val notifId = intent.getIntExtra("NOTIF_ID", 0)

        // Criar o canal de notificação (necessário para Android 8.0+)
        var channel: NotificationChannel? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = NotificationChannel(
                "br.edu.ufam.testenotification",
                "testeNotification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        }

        // Pegar o gerenciador de notificações
        val nfm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        if (nfm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nfm.createNotificationChannel(channel!!)
                Log.i("debug", "criando canal de notif")
            }
        }

        Log.i("debug", "notificando... #$notifId")

        val channelID = "br.edu.ufam.testenotification"

        // Construir a notificação com BigTextStyle para textos longos
        val builder = NotificationCompat.Builder(context, channelID)
        builder.setSmallIcon(R.drawable.baseline_info_24)
            .setContentTitle("Notificação #${notifId + 1}")
            .setContentText(textoNotificacao)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(textoNotificacao))  // Permite expandir o texto
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Usa ID único para cada notificação (não sobrescreve)
        nfm?.notify(notifId, builder.build())
    }
}