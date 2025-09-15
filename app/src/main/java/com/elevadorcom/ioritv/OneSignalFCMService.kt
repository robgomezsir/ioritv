package com.elevadorcom.ioritv

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.onesignal.OneSignal

class OneSignalFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("OneSignalFCMService", "Novo token FCM: $token")

        if (OneSignal.isInitialized) {
            OneSignal.login(token) // Substitui o setExternalUserId()
        } else {
            Log.e("OneSignalFCMService", "OneSignal não inicializado.")
        }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let { notification ->
            Log.d("OneSignalFCMService", "Mensagem recebida: ${notification.body}")

            // Apenas exibir um log porque OneSignal gerencia notificações automaticamente
            Log.d("OneSignalFCMService", "Notificação do Firebase repassada para OneSignal.")
        }
    }
}
