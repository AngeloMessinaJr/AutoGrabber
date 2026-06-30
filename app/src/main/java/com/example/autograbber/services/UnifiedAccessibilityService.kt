package com.example.autograbber.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.autograbber.data.OfferRepository
import com.example.autograbber.data.SettingsRepository
import com.example.autograbber.data.NotificationRepository
import com.example.autograbber.data.db.AppDatabase
import com.example.autograbber.data.models.*
import com.example.autograbber.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.regex.Pattern

class UnifiedAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var offerRepository: OfferRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var notificationHelper: NotificationHelper
    
    private var currentPrefs = FilterPreferences()

    // State management
    private val processedOffers = mutableMapOf<String, Double>()
    private val processedBlocks = mutableSetOf<String>()
    private var flexScrollCount = 0
    private val MAX_FLEX_SCROLLS = 5
    
    private var isProcessing = false
    private var lastFlexScrollTime = 0L
    private var lastFlexClickTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("UnifiedService", "Service Connected")
        settingsRepository = SettingsRepository(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        offerRepository = OfferRepository(db.offerDao())
        notificationRepository = NotificationRepository(db.notificationDao())
        notificationHelper = NotificationHelper(applicationContext)

        // Keep preferences updated in memory for zero-latency access
        serviceScope.launch {
            settingsRepository.filterPreferencesFlow.collectLatest {
                currentPrefs = it
                Log.d("UnifiedService", "Preferences Updated")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return

        // Log events for debugging if needed (can be removed later)
        // Log.d("UnifiedService", "Event from: $packageName")

        // Process based on package and global toggle
        when {
            packageName == "com.instacart.shopper" && currentPrefs.isInstacartEnabled -> {
                handleInstacart(rootNode)
            }
            (packageName.contains("doordash") || packageName.contains("dasher")) && currentPrefs.isDoorDashEnabled -> {
                handleDoorDash(rootNode)
            }
            (packageName.contains("amazon") && (packageName.contains("flex") || packageName.contains("rabbit"))) && currentPrefs.isFlexEnabled -> {
                handleFlex(rootNode)
            }
        }
    }

    // --- FLEX LOGIC ---

    private fun handleFlex(rootNode: AccessibilityNodeInfo) {
        // 0. Global Processing Lock
        if (isProcessing) return

        // 1. Side Menu Check: If side menu is open, click "Offers"
        if (handleFlexSideMenu(rootNode)) return

        // 2. Detection: Are we on the offers screen?
        val isOffersScreen = rootNode.findAccessibilityNodeInfosByText("Refresh").isNotEmpty() || 
                           rootNode.findAccessibilityNodeInfosByText("Filter").isNotEmpty() ||
                           rootNode.findAccessibilityNodeInfosByText("Schedule").isNotEmpty()

        if (isOffersScreen) {
            val allNodes = mutableListOf<AccessibilityNodeInfo>()
            findAllNodes(rootNode, allNodes)
            val payNodes = allNodes.filter { it.text?.toString()?.contains("$") == true }
            
            val filters = currentPrefs.flexFilters

            for (payNode in payNodes) {
                val card = findFlexParentCard(payNode) ?: continue
                val block = extractFlexBlockInfo(card) ?: continue
                val blockId = "${block.station}_${block.timeRange}_${block.pay}"
                
                // If it matches filters, we act
                if (evaluateFlexBlock(block, filters)) {
                    if (filters.autoAccept && !processedBlocks.contains(blockId)) {
                        Log.d("UnifiedService", "FLEX MATCH FOUND: ${block.station} for \$${block.pay}")
                        processedBlocks.add(blockId)
                        
                        serviceScope.launch {
                            offerRepository.addOffer(GigOffer(platform = Platform.FLEX, pay = block.pay, storeName = block.station, action = OfferAction.ACCEPTED, itemCount = "${block.length}h"))
                        }
                        
                        // Perform the click instantly
                        isProcessing = true
                        clickAt(card)
                        lastFlexClickTime = System.currentTimeMillis()
                        
                        // Reset processing flag after a timeout to handle missed transitions
                        Handler(Looper.getMainLooper()).postDelayed({ isProcessing = false }, 1500)
                        return 
                    }
                }
                processedBlocks.add(blockId)
            }

            // 3. Navigation/Refresh Logic
            val now = System.currentTimeMillis()
            if (now - lastFlexScrollTime > 300 && now - lastFlexClickTime > 800) {
                if (flexScrollCount < MAX_FLEX_SCROLLS) {
                    Log.d("UnifiedService", "FLEX SCROLL: Searching more blocks...")
                    scrollDownFlex() 
                    flexScrollCount++
                } else if (currentPrefs.flexFilters.autoRefresh) {
                    Log.d("UnifiedService", "FLEX REFRESH: Reached bottom, refreshing...")
                    if (!clickFlexRefreshButton(rootNode)) {
                        refreshFlex() 
                    }
                    flexScrollCount = 0
                    processedBlocks.clear()
                }
                lastFlexScrollTime = now
            }
        }

        // 4. Confirmation: Handle the "Schedule" page
        val scheduleNodes = rootNode.findAccessibilityNodeInfosByText("Schedule")
        if (scheduleNodes.isNotEmpty()) {
            if (currentPrefs.flexFilters.autoAccept) {
                Log.d("UnifiedService", "FLEX CONFIRM: Clicking Schedule")
                clickAt(scheduleNodes[0])
                lastFlexClickTime = System.currentTimeMillis()
                flexScrollCount = 0 // Reset scroll count after a successful action
            }
        }
    }

    private fun handleFlexSideMenu(rootNode: AccessibilityNodeInfo): Boolean {
        val dashboardNodes = rootNode.findAccessibilityNodeInfosByText("Dashboard")
        val offersNodes = rootNode.findAccessibilityNodeInfosByText("Offers")
        
        if (dashboardNodes.isNotEmpty() && offersNodes.isNotEmpty()) {
            Log.d("UnifiedService", "FLEX MENU: Detected side menu, clicking Offers")
            for (node in offersNodes) {
                if (node.text?.toString() == "Offers") {
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return true
                    }
                    var p = node.parent
                    while (p != null) {
                        if (p.isClickable) {
                            p.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return true
                        }
                        p = p.parent
                    }
                }
            }
        }
        return false
    }

    private fun clickFlexRefreshButton(rootNode: AccessibilityNodeInfo): Boolean {
        val refreshNodes = rootNode.findAccessibilityNodeInfosByText("Refresh")
        for (node in refreshNodes) {
            if (node.text?.toString() == "Refresh" && (node.isClickable || node.parent?.isClickable == true)) {
                if (node.isClickable) node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                else node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    private fun extractFlexBlockInfo(card: AccessibilityNodeInfo): FlexBlock? {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(card, nodes)
        
        var pay: Double? = null
        var timeRange: String? = null
        var lengthText: String? = null
        val texts = mutableListOf<String>()

        for (node in nodes) {
            val text = node.text?.toString()?.trim() ?: continue
            if (text.isEmpty()) continue
            texts.add(text)
            
            if (text.contains("$")) {
                val m = Pattern.compile("\\$(\\d+\\.?\\d*)").matcher(text)
                if (m.find()) pay = m.group(1)?.toDoubleOrNull()
            } else if (text.contains("-") && (text.contains("AM") || text.contains("PM"))) {
                timeRange = text
            } else if (text.contains("hr") || text.contains("min")) {
                lengthText = text
            }
        }
        
        // Station name is usually the node with parentheses or just a long string
        val station = texts.firstOrNull { t ->
            t.isNotEmpty() && !t.contains("$") && !t.contains("hr") && !t.contains("min") &&
            !(t.contains("-") && (t.contains("AM") || t.contains("PM")))
        } ?: "Station"

        if (pay != null && (timeRange != null || lengthText != null)) {
            var length = 0.0
            if (lengthText != null) {
                val hrMatch = Pattern.compile("(\\d+)\\s*hr").matcher(lengthText)
                val minMatch = Pattern.compile("(\\d+)\\s*min").matcher(lengthText)
                val hrs = if (hrMatch.find()) hrMatch.group(1)?.toDoubleOrNull() ?: 0.0 else 0.0
                val mins = if (minMatch.find()) minMatch.group(1)?.toDoubleOrNull() ?: 0.0 else 0.0
                length = hrs + (mins / 60.0)
            }
            return FlexBlock(card, station, pay, timeRange ?: "Unknown Time", length)
        }
        return null
    }

    private fun evaluateFlexBlock(block: FlexBlock, filters: PlatformFilters): Boolean {
        // Pay Check
        if (block.pay < filters.minTotalPay) return false
        
        // Station Check
        if (filters.blockedStores.any { block.station.contains(it, true) }) return false
        
        // Categorized Length Check
        val category = when {
            block.length >= 1.0 && block.length < 2.0 -> "1 hour - 2 hours"
            block.length >= 2.0 && block.length < 3.0 -> "2 hours - 3 hours"
            block.length >= 3.0 && block.length < 4.0 -> "3 hours - 4 hours"
            block.length >= 4.0 -> "4 hours +"
            else -> "Other"
        }
        
        return filters.flexBlockLengths.contains(category)
    }

    private fun scrollDownFlex() {
        val metrics = resources.displayMetrics
        val path = Path().apply { 
            moveTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.8f) // Start at bottom
            lineTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.2f) // End at top
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 200))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun refreshFlex() {
        val metrics = resources.displayMetrics
        val path = Path().apply { 
            moveTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.25f) // Start at top
            lineTo(metrics.widthPixels / 2f, metrics.heightPixels * 0.85f) // Drag down
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 150))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // --- INSTACART & DOORDASH (Simplified for high-speed Unified) ---

    private fun handleInstacart(rootNode: AccessibilityNodeInfo) {
        val allTextNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllTextNodes(rootNode, allTextNodes)
        val payNodes = allTextNodes.filter { it.text?.toString()?.contains("$") == true }
        
        for (payNode in payNodes) {
            val card = findInstacartParentCard(payNode) ?: continue
            val pay = extractPayFromNode(payNode) ?: continue
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            findAllTextNodes(card, nodes)
            
            val details = nodes.firstOrNull { it.text?.toString()?.contains("shop and deliver") == true }?.text?.toString() ?: ""
            val items = extractItemsFromText(details) ?: continue
            val customers = extractCustomersFromText(details)
            val distance = extractDistance(card)
            
            val batchKey = "IC_${pay}_${items}_${customers}"
            if (processedOffers[batchKey] == pay) continue
            processedOffers[batchKey] = pay

            val filters = currentPrefs.instacartFilters
            val worthIt = pay >= filters.minTotalPay && items <= filters.maxItemCount && customers <= filters.maxCustomers
            
            if (!worthIt && filters.autoReject) {
                swipeToHideInstacart(card)
                val title = "Auto Rejected"
                val distText = if (distance != null) ", $distance miles" else ""
                val message = "Instacart: $customers customers, $items items, ${String.format(Locale.US, "$%.2f", pay)}$distText"
                notificationHelper.sendAlertNotification("Auto Grabber", message)
                serviceScope.launch {
                    notificationRepository.addNotification(AppNotification(title = title, message = message, platform = Platform.INSTACART))
                    offerRepository.addOffer(GigOffer(
                        platform = Platform.INSTACART,
                        pay = pay,
                        distance = distance ?: 0.0,
                        payPerMile = if (distance != null && distance > 0) pay / distance else 0.0,
                        storeName = "Instacart Batch",
                        stops = customers,
                        action = OfferAction.REJECTED,
                        reason = "Auto Rejected",
                        itemCount = "$items items"
                    ))
                }
            } else if (worthIt && filters.autoAccept) {
                card.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                val title = "Auto Accepted"
                val distText = if (distance != null) ", $distance miles" else ""
                val message = "Instacart: $customers customers, $items items, ${String.format(Locale.US, "$%.2f", pay)}$distText"
                notificationHelper.sendAlertNotification("Auto Grabber", message)
                serviceScope.launch {
                    notificationRepository.addNotification(AppNotification(title = title, message = message, platform = Platform.INSTACART))
                    offerRepository.addOffer(GigOffer(
                        platform = Platform.INSTACART,
                        pay = pay,
                        distance = distance ?: 0.0,
                        payPerMile = if (distance != null && distance > 0) pay / distance else 0.0,
                        storeName = "Instacart Batch",
                        stops = customers,
                        action = OfferAction.ACCEPTED,
                        reason = "Auto Accepted",
                        itemCount = "$items items"
                    ))
                }
            }
        }
    }

    private fun handleDoorDash(rootNode: AccessibilityNodeInfo) {
        // Check for Step 2: Decline confirmation dialog
        val confirmNodes = rootNode.findAccessibilityNodeInfosByText("Decline offer")
        for (node in confirmNodes) {
            val text = node.text?.toString()
            if (text == "Decline offer") {
                Log.d("UnifiedService", "DD: Confirmation dialog detected, clicking 'Decline offer'")
                clickAt(node)
                return
            }
        }

        val payNodes = rootNode.findAccessibilityNodeInfosByText("$")
        var pay: Double? = null
        for (node in payNodes) {
            pay = extractPayFromNode(node)
            if (pay != null) break
        }
        if (pay == null) return

        val distance = extractDistance(rootNode) ?: return
        val merchant = findMerchant(rootNode) ?: "DoorDash Offer"
        
        val isBlocked = currentPrefs.doorDashFilters.blockedStores.any { merchant.contains(it, ignoreCase = true) }
        
        val offerKey = "DD_${pay}_${distance}"
        if (processedOffers[offerKey] == pay) return
        processedOffers[offerKey] = pay

        val filters = currentPrefs.doorDashFilters
        val worthIt = !isBlocked && pay >= filters.minTotalPay && distance <= filters.maxDistanceMiles
        
        if (!worthIt && filters.autoReject) {
            clickNodeWithText(rootNode, "Decline")
            val title = "Auto Rejected"
            val reason = if (isBlocked) "Blocked Merchant" else "Auto Rejected"
            val message = "$merchant, ${String.format(Locale.US, "$%.2f", pay)}, $distance miles"
            notificationHelper.sendAlertNotification("Auto Grabber", message)
            serviceScope.launch {
                notificationRepository.addNotification(AppNotification(title = title, message = message, platform = Platform.DOORDASH))
                val offer = GigOffer(
                    platform = Platform.DOORDASH,
                    pay = pay,
                    distance = distance,
                    payPerMile = if (distance > 0) pay / distance else 0.0,
                    storeName = merchant,
                    stops = 1,
                    action = OfferAction.REJECTED,
                    reason = reason
                )
                Log.d("UnifiedService", "Adding Rejected DD Offer: ${offer.storeName}, Reason: $reason")
                offerRepository.addOffer(offer)
            }
        } else if (worthIt && filters.autoAccept) {
            clickNodeWithText(rootNode, "Accept")
            val title = "Auto Accepted"
            val message = "$merchant, ${String.format(Locale.US, "$%.2f", pay)}, $distance miles"
            notificationHelper.sendAlertNotification("Auto Grabber", message)
            serviceScope.launch {
                notificationRepository.addNotification(AppNotification(title = title, message = message, platform = Platform.DOORDASH))
                val offer = GigOffer(
                    platform = Platform.DOORDASH,
                    pay = pay,
                    distance = distance,
                    payPerMile = if (distance > 0) pay / distance else 0.0,
                    storeName = merchant,
                    stops = 1,
                    action = OfferAction.ACCEPTED,
                    reason = "Auto Accepted"
                )
                Log.d("UnifiedService", "Adding Accepted DD Offer: ${offer.storeName}")
                offerRepository.addOffer(offer)
            }
        } else {
            // Log as ignored if neither auto-accept nor auto-reject is triggered
            serviceScope.launch {
                offerRepository.addOffer(GigOffer(
                    platform = Platform.DOORDASH,
                    pay = pay,
                    distance = distance,
                    payPerMile = if (distance > 0) pay / distance else 0.0,
                    storeName = merchant,
                    stops = 1,
                    action = OfferAction.IGNORED,
                    reason = "Manual Decision"
                ))
            }
        }
    }

    private fun findMerchant(rootNode: AccessibilityNodeInfo): String? {
        val pickupNodes = rootNode.findAccessibilityNodeInfosByText("Pickup")
        if (pickupNodes.isNotEmpty()) {
            val nodes = mutableListOf<AccessibilityNodeInfo>()
            findAllNodes(rootNode, nodes)
            
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

    // --- HELPERS ---

    private fun findAllNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        list.add(node)
        for (i in 0 until node.childCount) findAllNodes(node.getChild(i), list)
    }

    private fun findAllTextNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.text != null) list.add(node)
        for (i in 0 until node.childCount) findAllTextNodes(node.getChild(i), list)
    }

    private fun findFlexParentCard(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var curr = node; repeat(6) { val p = curr.parent ?: return null; if (p.isClickable) return p; curr = p }
        return null
    }

    private fun findInstacartParentCard(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var curr = node; repeat(8) { val p = curr.parent ?: return null; if (p.isClickable && p.childCount >= 3) return p; curr = p }
        return null
    }

    private fun extractPayFromNode(node: AccessibilityNodeInfo): Double? {
        val m = Pattern.compile("\\$(\\d+(?:\\.\\d+)?)").matcher(node.text?.toString() ?: "")
        return if (m.find()) m.group(1)?.toDoubleOrNull() else null
    }

    private fun extractDistance(node: AccessibilityNodeInfo): Double? {
        val nodes = mutableListOf<AccessibilityNodeInfo>(); findAllTextNodes(node, nodes)
        for (n in nodes) {
            val m = Pattern.compile("(\\d+\\.?\\d*)\\s*mi").matcher(n.text?.toString() ?: "")
            if (m.find()) return m.group(1)?.toDoubleOrNull()
        }
        return null
    }

    private fun extractItemsFromText(text: String): Int? {
        val m = Pattern.compile("(\\d+)\\s*items?").matcher(text)
        return if (m.find()) m.group(1)?.toIntOrNull() else null
    }

    private fun extractCustomersFromText(text: String): Int {
        val m = Pattern.compile("(\\d+)\\s*customers?").matcher(text)
        return if (m.find()) m.group(1)?.toIntOrNull() ?: 1 else 1
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

    private fun clickNodeWithText(root: AccessibilityNodeInfo, text: String) {
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.text?.toString() == text) {
                var curr: AccessibilityNodeInfo? = node
                while (curr != null) {
                    if (curr.isClickable) {
                        clickAt(curr)
                        return
                    }
                    curr = curr.parent
                }
            }
        }
    }

    private fun swipeToHideInstacart(node: AccessibilityNodeInfo) {
        val b = Rect(); node.getBoundsInScreen(b)
        val path = Path().apply { moveTo(b.right - 50f, b.centerY().toFloat()); lineTo(b.left + 100f, b.centerY().toFloat()) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 150)).build(), object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { 
                Handler(Looper.getMainLooper()).postDelayed({
                    val cPath = Path().apply { moveTo(b.right - (b.width() * 0.12f), b.centerY().toFloat()) }
                    dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(cPath, 0, 20)).build(), null, null)
                }, 100)
            }
        }, null)
    }

    private data class FlexBlock(val node: AccessibilityNodeInfo, val station: String, val pay: Double, val timeRange: String, val length: Double)

    override fun onInterrupt() {}
    override fun onDestroy() { super.onDestroy(); serviceJob.cancel() }
}
