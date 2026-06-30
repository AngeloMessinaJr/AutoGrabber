package com.example.autograbber.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.autograbber.data.OfferRepository
import com.example.autograbber.data.SettingsRepository
import com.example.autograbber.data.NotificationRepository
import com.example.autograbber.data.db.AppDatabase
import com.example.autograbber.data.models.GigOffer
import com.example.autograbber.data.models.OfferAction
import com.example.autograbber.data.models.Platform
import com.example.autograbber.data.models.PlatformFilters
import com.example.autograbber.data.models.AppNotification
import com.example.autograbber.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class DoorDashAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var offerRepository: OfferRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var notificationHelper: NotificationHelper

    private val processedOffers = mutableMapOf<String, Double>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("DDService", "Service Connected")
        settingsRepository = SettingsRepository(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        offerRepository = OfferRepository(db.offerDao())
        notificationRepository = NotificationRepository(db.notificationDao())
        notificationHelper = NotificationHelper(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == "com.doordash.driverapp" || 
            event.packageName == "com.dd.dasher") {
            
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                
                val rootNode = rootInActiveWindow ?: return
                
                // Check for decline dialog first (Step 2)
                if (handleDeclineDialog(rootNode)) return

                // Check for new offer (Step 1)
                handleNewOffer(rootNode)
            }
        }
    }

    private fun handleNewOffer(rootNode: AccessibilityNodeInfo) {
        val pay = findPay(rootNode)
        val distance = findDistance(rootNode)
        val merchant = findMerchant(rootNode)

        if (pay != null && distance != null && merchant != null) {
            val offerKey = "${merchant}_${distance}"
            val lastPay = processedOffers[offerKey]

            // Process if new OR pay increased
            if (lastPay == null || pay > lastPay) {
                Log.d("DDService", "Offer Detected: $merchant, $pay, $distance mi")
                processedOffers[offerKey] = pay

                serviceScope.launch {
                    try {
                        val prefs = settingsRepository.filterPreferencesFlow.first()
                        if (!prefs.isDoorDashEnabled) return@launch

                        val filters = prefs.doorDashFilters
                        val isBlocked = filters.blockedStores.any { merchant.contains(it, ignoreCase = true) }
                        val isWorthit = !isBlocked && evaluateOffer(pay, distance, filters)

                        if (!isWorthit) {
                            if (filters.autoReject) {
                                Log.d("DDService", "Rejecting offer...")
                                val reason = if (isBlocked) "Blocked Merchant" else "Auto Rejected"
                                offerRepository.addOffer(GigOffer(
                                    platform = Platform.DOORDASH,
                                    pay = pay,
                                    distance = distance,
                                    payPerMile = if (distance > 0) pay / distance else 0.0,
                                    storeName = merchant,
                                    stops = 1,
                                    action = OfferAction.REJECTED,
                                    reason = reason
                                ))
                                clickDecline(rootNode)
                                val title = "Auto Rejected"
                                val message = "$merchant, ${String.format(java.util.Locale.US, "$%.2f", pay)}, $distance miles"
                                notificationHelper.sendAlertNotification("Auto Grabber", message)
                                serviceScope.launch {
                                    notificationRepository.addNotification(AppNotification(title = title, message = message, platform = Platform.DOORDASH))
                                }
                            }
                        } else {
                            Log.d("DDService", "Offer is good!")
                            if (filters.autoAccept) {
                                Log.d("DDService", "Auto-Accepting offer...")
                                offerRepository.addOffer(GigOffer(
                                    platform = Platform.DOORDASH,
                                    pay = pay,
                                    distance = distance,
                                    payPerMile = if (distance > 0) pay / distance else 0.0,
                                    storeName = merchant,
                                    stops = 1,
                                    action = OfferAction.ACCEPTED,
                                    reason = "Auto Accepted"
                                ))
                                clickAccept(rootNode)
                                val title = "Auto Accepted"
                                val message = "$merchant, ${String.format(java.util.Locale.US, "$%.2f", pay)}, $distance miles"
                                notificationHelper.sendAlertNotification("Auto Grabber", message)
                                serviceScope.launch {
                                    notificationRepository.addNotification(AppNotification(title = title, message = message, platform = Platform.DOORDASH))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DDService", "Error during offer processing", e)
                    }
                }
            }
        }
    }

    private fun clickAt(node: AccessibilityNodeInfo) {
        val bounds = Rect(); node.getBoundsInScreen(bounds)
        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat()
        if (x <= 0 || y <= 0) return
        
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun evaluateOffer(pay: Double, distance: Double, filters: PlatformFilters): Boolean {
        if (pay < filters.minTotalPay) return false
        if (distance > filters.maxDistanceMiles) return false
        val payPerMile = if (distance > 0) pay / distance else 0.0
        if (payPerMile < filters.minPayPerMile) return false
        return true
    }

    private fun findPay(rootNode: AccessibilityNodeInfo): Double? {
        val payNodes = rootNode.findAccessibilityNodeInfosByText("$")
        for (node in payNodes) {
            val text = node.text?.toString() ?: continue
            val match = Pattern.compile("\\$(\\d+(?:\\.\\d+)?)").matcher(text)
            if (match.find()) {
                return match.group(1)?.toDoubleOrNull()
            }
        }
        return null
    }

    private fun findDistance(rootNode: AccessibilityNodeInfo): Double? {
        val miNodes = rootNode.findAccessibilityNodeInfosByText(" mi")
        for (node in miNodes) {
            val text = node.text?.toString() ?: continue
            val match = Pattern.compile("(\\d+\\.?\\d*)\\s*mi").matcher(text)
            if (match.find()) {
                return match.group(1)?.toDoubleOrNull()
            }
        }
        return null
    }

    private fun findMerchant(rootNode: AccessibilityNodeInfo): String? {
        val pickupNodes = rootNode.findAccessibilityNodeInfosByText("Pickup")
        if (pickupNodes.isNotEmpty()) {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            getAllNodes(rootNode, nodes)
            
            var pickupFound = false
            for (node in nodes) {
                val text = node.text?.toString() ?: continue
                if (text == "Pickup") {
                    pickupFound = true
                    continue
                }
                if (pickupFound && text.isNotEmpty() && !text.contains("$") && !text.contains("mi") && !text.contains("Deliver by")) {
                    return text
                }
            }
        }
        return null
    }

    private fun getAllNodes(node: AccessibilityNodeInfo, list: MutableList<AccessibilityNodeInfo>) {
        list.add(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) getAllNodes(child, list)
        }
    }

    private fun clickDecline(rootNode: AccessibilityNodeInfo) {
        clickNodeWithText(rootNode, "Decline")
    }

    private fun clickAccept(rootNode: AccessibilityNodeInfo) {
        clickNodeWithText(rootNode, "Accept")
    }

    private fun clickNodeWithText(rootNode: AccessibilityNodeInfo, text: String) {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.text?.toString() == text) {
                if (node.isClickable) {
                    clickAt(node)
                    return
                }
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        clickAt(parent)
                        return
                    }
                    parent = parent.parent
                }
            }
        }
    }

    private fun handleDeclineDialog(rootNode: AccessibilityNodeInfo): Boolean {
        val confirmDeclineNodes = rootNode.findAccessibilityNodeInfosByText("Decline offer")
        if (confirmDeclineNodes.isNotEmpty()) {
            Log.d("DDService", "Confirming decline...")
            for (node in confirmDeclineNodes) {
                if (node.text?.toString() == "Decline offer") {
                    clickAt(node)
                    return true
                }
            }
        }
        return false
    }

    override fun onInterrupt() {
        Log.d("DDService", "Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        processedOffers.clear()
    }
}
