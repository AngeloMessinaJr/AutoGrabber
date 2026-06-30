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
import com.example.autograbber.data.db.AppDatabase
import com.example.autograbber.data.models.GigOffer
import com.example.autograbber.data.models.OfferAction
import com.example.autograbber.data.models.Platform
import com.example.autograbber.data.models.PlatformFilters
import com.example.autograbber.data.models.StoreDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class InstacartAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var offerRepository: OfferRepository

    // Map to track processed batches and their last known pay
    private val processedBatches = mutableMapOf<String, Double>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("InstacartService", "Service Connected")
        settingsRepository = SettingsRepository(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        offerRepository = OfferRepository(db.offerDao())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.packageName == "com.instacart.shopper") {
            val rootNode = rootInActiveWindow ?: return
            
            Log.d("InstacartService", "Event received from Instacart. EventType: ${AccessibilityEvent.eventTypeToString(event.eventType)}")

            // Check if we are specifically inside the "Hidden batches" sub-menu
            val headerNodes = rootNode.findAccessibilityNodeInfosByText("Hidden batches")
            val isHiddenMenu = headerNodes.any { it.text?.toString()?.equals("Hidden batches", true) == true }
            if (isHiddenMenu) {
                Log.d("InstacartService", "In Hidden batches menu, skipping...")
                headerNodes.forEach { it.recycle() }
                return
            }
            headerNodes.forEach { it.recycle() }

            processBatches(rootNode)
        }
    }

    private fun processBatches(rootNode: AccessibilityNodeInfo) {
        val allTextNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllTextNodes(rootNode, allTextNodes)

        val payNodes = allTextNodes.filter { it.text?.toString()?.contains("$") == true }
        Log.d("InstacartService", "Found ${payNodes.size} potential pay nodes")
        
        // Find all batches that NEED action
        val batchesToProcess = mutableListOf<BatchAction>()

        for (payNode in payNodes) {
            val batchCardNode = findParentBatchCard(payNode)
            if (batchCardNode == null) {
                Log.d("InstacartService", "Could not find parent card for pay node: ${payNode.text}")
                continue
            }

            val pay = extractPayFromNode(payNode)
            val batchDetailsText = findBatchDetailsText(batchCardNode)
            val itemsCountText = extractItemsAndUnitsText(batchDetailsText)
            val items = extractItemsFromText(batchDetailsText)
            val customers = extractCustomersFromText(batchDetailsText)
            val distance = extractDistance(batchCardNode)
            val storeDetails = extractStoreDetails(batchCardNode)
            val stores = storeDetails.map { it.name }
            val retailerName = if (stores.isNotEmpty()) stores.joinToString(" + ") else "Instacart Retailer"

            Log.d("InstacartService", "Detected Batch: $retailerName, Pay: $pay, Items: $items, Dist: $distance, Cust: $customers")

            if (pay != null && items != null) {
                val batchKey = "${retailerName}_${distance ?: 0.0}_${customers}"
                val lastPay = processedBatches[batchKey]

                val isNew = lastPay == null
                val payIncreased = lastPay != null && pay > lastPay
                
                Log.d("InstacartService", "Batch Key: $batchKey. lastPay: $lastPay. isNew: $isNew, payIncreased: $payIncreased")

                if (isNew || payIncreased) {
                    batchesToProcess.add(BatchAction(
                        node = batchCardNode,
                        pay = pay,
                        items = items,
                        distance = distance,
                        retailerName = retailerName,
                        storeDetails = storeDetails,
                        customers = customers,
                        batchKey = batchKey,
                        itemsCountText = itemsCountText,
                        isNew = isNew,
                        payIncreased = payIncreased
                    ))
                }
            }
        }

        Log.d("InstacartService", "Batches pending action: ${batchesToProcess.size}")

        // Process the first one found. The rest will be handled in the next event trigger.
        if (batchesToProcess.isNotEmpty()) {
            val action = batchesToProcess[0]
            
            // Mark as processed immediately
            processedBatches[action.batchKey] = action.pay
            
            serviceScope.launch {
                try {
                    val prefs = settingsRepository.filterPreferencesFlow.first()
                    if (!prefs.isInstacartEnabled) return@launch

                    val filters = prefs.instacartFilters
                    val stores = action.storeDetails.map { it.name }
                    val hideResult = evaluateBatch(action.pay, action.items, action.distance, stores, action.customers, filters)

                    if (hideResult.shouldHide) {
                        if (filters.autoReject) {
                            Log.d("InstacartService", "Hiding Batch: ${action.pay}")
                            offerRepository.addOffer(GigOffer(
                                platform = Platform.INSTACART,
                                pay = action.pay,
                                distance = action.distance ?: 0.0,
                                payPerMile = if (action.distance != null && action.distance > 0) action.pay / action.distance else 0.0,
                                storeName = action.retailerName,
                                storeDetails = action.storeDetails,
                                stops = action.customers,
                                action = OfferAction.HIDDEN,
                                reason = if (action.payIncreased) "Batch Hidden (Boosted)" else "Batch Hidden",
                                itemCount = action.itemsCountText
                            ))
                            hideBatchGesture(action.node)
                        }
                    } else if (filters.autoAccept) {
                        Log.d("InstacartService", "Accepting Batch: ${action.pay}")
                        offerRepository.addOffer(GigOffer(
                            platform = Platform.INSTACART,
                            pay = action.pay,
                            distance = action.distance ?: 0.0,
                            payPerMile = if (action.distance != null && action.distance > 0) action.pay / action.distance else 0.0,
                            storeName = action.retailerName,
                            storeDetails = action.storeDetails,
                            stops = action.customers,
                            action = OfferAction.ACCEPTED,
                            reason = "Auto-Accepted",
                            itemCount = action.itemsCountText
                        ))
                        action.node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                } catch (e: Exception) {
                    Log.e("InstacartService", "Error", e)
                }
            }
        }
    }

    private data class BatchAction(
        val node: AccessibilityNodeInfo,
        val pay: Double,
        val items: Int,
        val distance: Double?,
        val retailerName: String,
        val storeDetails: List<StoreDetail>,
        val customers: Int,
        val batchKey: String,
        val itemsCountText: String,
        val isNew: Boolean,
        val payIncreased: Boolean
    )

    private fun findParentBatchCard(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current = node
        repeat(8) {
            val parent = current.parent ?: return null
            if (parent.isClickable && parent.childCount >= 3) return parent
            current = parent
        }
        return null
    }

    private fun extractPayFromNode(node: AccessibilityNodeInfo): Double? {
        val text = node.text?.toString() ?: ""
        val match = Pattern.compile("\\$(\\d+\\.\\d+)").matcher(text)
        if (match.find()) return match.group(1)?.toDoubleOrNull()
        return null
    }

    private fun findBatchDetailsText(card: AccessibilityNodeInfo): String {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findAllTextNodes(card, nodes)
        return nodes.firstOrNull { it.text?.toString()?.contains("shop and deliver") == true }?.text?.toString() ?: ""
    }

    private fun extractItemsAndUnitsText(text: String): String {
        val match = Pattern.compile("(\\d+\\s*items?\\s*\\(\\d+\\s*units?\\))").matcher(text)
        if (match.find()) return match.group(1) ?: ""
        val itemMatch = Pattern.compile("(\\d+\\s*items?)").matcher(text)
        if (itemMatch.find()) return itemMatch.group(1) ?: ""
        return ""
    }

    private fun extractItemsFromText(text: String): Int? {
        val match = Pattern.compile("(\\d+)\\s*items?").matcher(text)
        if (match.find()) return match.group(1)?.toIntOrNull()
        return null
    }

    private fun extractCustomersFromText(text: String): Int {
        val custMatch = Pattern.compile("(\\d+)\\s*customers?").matcher(text)
        if (custMatch.find()) return custMatch.group(1)?.toIntOrNull() ?: 1
        
        val shopMatch = Pattern.compile("(\\d+)\\s*shop and deliver").matcher(text)
        if (shopMatch.find()) return shopMatch.group(1)?.toIntOrNull() ?: 1

        return 1
    }

    private fun extractDistance(card: AccessibilityNodeInfo): Double? {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findAllTextNodes(card, nodes)
        for (n in nodes) {
            val text = n.text?.toString() ?: ""
            val match = Pattern.compile("(\\d+\\.?\\d*)\\s*mi").matcher(text)
            if (match.find()) return match.group(1)?.toDoubleOrNull()
        }
        return null
    }

    private fun extractStoreDetails(card: AccessibilityNodeInfo): List<StoreDetail> {
        val details = mutableListOf<StoreDetail>()
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findAllTextNodes(card, nodes)

        var currentStoreName: String? = null
        
        for (n in nodes) {
            val text = n.text?.toString()?.trim() ?: continue
            if (text.isBlank()) continue
            
            if (isRetailerName(text)) {
                if (currentStoreName != null) {
                    details.add(StoreDetail(currentStoreName))
                }
                currentStoreName = text
            } else if (currentStoreName != null && isPossibleAddress(text)) {
                details.add(StoreDetail(currentStoreName, text))
                currentStoreName = null
            }
        }
        
        if (currentStoreName != null) {
            details.add(StoreDetail(currentStoreName))
        }

        return details
    }

    private fun isPossibleAddress(text: String): Boolean {
        val clean = text.trim()
        if (Pattern.compile("^\\d+").matcher(clean).find()) return true
        if (Pattern.compile("\\b(Rd|Ave|Blvd|St|Dr|Way|Lane|Ct|Street|Road|Avenue)\\b", Pattern.CASE_INSENSITIVE).matcher(clean).find()) return true
        return false
    }

    private fun isRetailerName(text: String): Boolean {
        if (text.length < 2) return false
        val clean = text.trim()
        if (clean.contains("$") || clean.contains(" mi", true) || clean.contains("items", true) ||
            clean.contains("units", true) || clean.contains("shop and deliver", true) ||
            clean.contains("batch", true) || clean.contains("Hide", true) ||
            clean.equals("Instacart", true)) return false
        if (Pattern.compile("^\\d+").matcher(clean).find()) return false
        if (Pattern.compile("\\b(Rd|Ave|Blvd|St|Dr|Way|Lane|Ct|Street|Road|Avenue)\\b", Pattern.CASE_INSENSITIVE).matcher(clean).find()) return false
        return true
    }

    private fun hideBatchGesture(cardNode: AccessibilityNodeInfo) {
        val bounds = Rect()
        cardNode.getBoundsInScreen(bounds)
        
        val startX = bounds.right - 50f
        val endX = bounds.left + 100f
        val y = bounds.centerY().toFloat()

        val swipePath = Path().apply {
            moveTo(startX, y)
            lineTo(endX, y)
        }

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(swipePath, 0, 150)) // Fast swipe

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                // Smallest possible delay for animation
                Handler(Looper.getMainLooper()).postDelayed({
                    clickRightSideOfCard(bounds)
                }, 100)
            }
        }, null)
    }

    private fun clickRightSideOfCard(cardBounds: Rect) {
        val clickX = cardBounds.right - (cardBounds.width() * 0.12f)
        val clickY = cardBounds.centerY().toFloat()

        val clickPath = Path().apply {
            moveTo(clickX, clickY)
        }

        val clickGesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(clickPath, 0, 20)) // Extremely fast click
            .build()

        dispatchGesture(clickGesture, null, null)
    }

    private data class EvaluationResult(val shouldHide: Boolean, val reason: String = "")

    private fun evaluateBatch(pay: Double, items: Int, distance: Double?, stores: List<String>, customers: Int, filters: PlatformFilters): EvaluationResult {
        if (stores.any { s -> filters.blockedStores.any { b -> s.contains(b, true) } })
            return EvaluationResult(true, "Blocked Store")
        if (filters.autoHideMultiRetailer && stores.size > 1)
            return EvaluationResult(true, "Multi-Store")
        if (customers > filters.maxCustomers)
            return EvaluationResult(true, "Too many customers")
        if (items > filters.maxItemCount)
            return EvaluationResult(true, "Too many items")
        if (pay < filters.minTotalPay)
            return EvaluationResult(true, "Low Pay")
        if (distance != null && distance > filters.maxDistanceMiles)
            return EvaluationResult(true, "Distance too high")
        if (distance != null && distance > 0 && (pay / distance) < filters.minPayPerMile)
            return EvaluationResult(true, "Low Pay/Mile")
        return EvaluationResult(false)
    }

    private fun findAllTextNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (node.text != null) list.add(node)
        for (i in 0 until node.childCount) {
            findAllTextNodes(node.getChild(i), list)
        }
    }

    override fun onInterrupt() {}

    private fun String.contains(other: String, ignoreCase: Boolean): Boolean {
        return this.indexOf(other, 0, ignoreCase) != -1
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        processedBatches.clear()
    }
}
