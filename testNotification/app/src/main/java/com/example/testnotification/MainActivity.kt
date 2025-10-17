package com.example.testnotification

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var contadorNotificacao = 0  // Contador para IDs únicos

    // Lista de textos grandes diferentes
    private val textosGrandes = listOf(
        "Esta é a primeira notificação com um texto bem longo para demonstrar como o Android lida com notificações extensas. O sistema pode expandir automaticamente para mostrar todo o conteúdo quando o usuário arrasta a notificação para baixo.",

        "Segunda notificação chegando! Aqui temos outro texto diferente e extenso sobre tecnologia. O Android permite que você visualize textos longos usando o estilo BigTextStyle, que é perfeito para mensagens detalhadas e informações importantes que precisam ser lidas completamente.",

        "Terceira notificação ativada! Este texto fala sobre a importância de notificações bem estruturadas em aplicativos mobile. Usuários apreciam quando recebem informações claras e completas sem precisar abrir o aplicativo. Isso melhora significativamente a experiência do usuário.",

        "Quarta notificação disponível! Agora vamos falar sobre desenvolvimento Android. Criar notificações eficientes requer entender os diferentes estilos disponíveis, como BigTextStyle, BigPictureStyle e InboxStyle. Cada um serve para um propósito específico e melhora a comunicação com o usuário.",

        "Quinta notificação no ar! Este é um texto sobre boas práticas em notificações push. É importante não bombardear o usuário com muitas notificações, respeitar as preferências de silencioso, e sempre fornecer um conteúdo relevante que agregue valor à experiência do usuário no seu aplicativo."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verificar permissão de notificação
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        } else {
            Log.i("debug", "Permissão garantida já!")
        }

        // Configurar o botão
        val button = findViewById<Button>(R.id.notificacao)
        button.setOnClickListener { view ->
            // Pega o texto baseado no contador (circular)
            val textoAtual = textosGrandes[contadorNotificacao % textosGrandes.size]

            Toast.makeText(
                view.context,
                "Criando notificação ${contadorNotificacao + 1} em 1 segundo..",
                Toast.LENGTH_SHORT
            ).show()

            // Agenda a notificação passando o texto e ID
            agendaNotificacao2(1000, textoAtual, contadorNotificacao)

            contadorNotificacao++  // Incrementa para próxima notificação
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("debug", "Permissão concedida pelo usuário!")
            } else {
                Log.i("debug", "Permissão negada pelo usuário")
            }
        }
    }

    private fun agendaNotificacao2(delay: Int, texto: String, notifId: Int) {
        val intent = Intent(applicationContext, NotificationPub::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Passa o texto e o ID pela Intent
        intent.putExtra("TEXTO_NOTIFICACAO", texto)
        intent.putExtra("NOTIF_ID", notifId)

        Log.i("debug", "agendando notificacao #$notifId")

        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notifId,  // Usa ID único para cada notificação
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            SystemClock.elapsedRealtime() + delay.toLong(),
            pendingIntent
        )
    }
}