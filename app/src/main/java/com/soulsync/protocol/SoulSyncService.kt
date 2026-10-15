private fun showCalmingMessage() {
    val message = calmingMessages[Random.nextInt(calmingMessages.size)]
    
    // بث للمستخدم في الواجهة
    val broadcastIntent = Intent("com.soulsync.CALMING_MESSAGE")
    broadcastIntent.putExtra("message", message)
    sendBroadcast(broadcastIntent)
    
    // إشعار في شاشة القفل أيضاً
    val intent = Intent(this, MainActivity::class.java)
    val pi = PendingIntent.getActivity(
        this, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notification = NotificationCompat.Builder(this, CALMING_CHANNEL_ID)
        .setContentTitle("SoulSync: تهدئة")
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentIntent(pi)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .build()

    val nm = getSystemService(NotificationManager::class.java)
    nm.notify(CALMING_NOTIF_ID, notification)
    Log.i(TAG, "Calming message shown: $message")
}
