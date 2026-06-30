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
import com.example.autograbber.data.db.AppDatabase
import com.example.autograbber.data.models.GigOffer
import com.example.autograbber.data.models.OfferAction
import com.example.autograbber.data.models.Platform
import com.example.autograbber.data.models.PlatformFilters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class AmazonFlexAccessibilityService : AccessibilityService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var offerRepository: OfferRepository

    private val processedBlocks = mutableSetOf<String>()
    private var isScrolling = false
    private var lastActionTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("FlexService", "Service Connected")
        settingsRepository = SettingsRepository(applicationContext)
        val db = AppDatabase.getDatabase(applicationContext)
        offerRepository = OfferRepository(db.offerDao())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("amazon") && (packageName.contains("flex") || packageName.contains("rabbit"))) {
            serviceScope.launch {
                try {
                    val prefs = settingsRepository.filterPreferencesFlow.first()
                    // CRITICAL: Stop all logic if toggle is off
                    if (!prefs.isFlexEnabled) {
                        Log.d("FlexService", "Automation Disabled via Toggle")
                        return@launch
                    }

                    val rootNode = rootInActiveWindow ?: return@launch
                    
                    // Check if we are on the offers screen
                    if (isOffersScreen(rootNode)) {
                        processBlocks(rootNode)
                    }
                    
                    // Handle the "Schedule" button on the block details page
                    handleDetailsPage(rootNode)
                } catch (e: Exception) {
                    Log.e("FlexService", "Error in accessibility event", e)
                }
            }
        }
    }

    private fun isOffersScreen(rootNode: AccessibilityNodeInfo): Boolean {
        // Typical text on Flex offers screen
        return rootNode.findAccessibilityNodeInfosByText("Refresh").isNotEmpty() || 
               rootNode.findAccessibilityNodeInfosByText("Filter").isNotEmpty()
    }

    private fun processBlocks(rootNode: AccessibilityNodeInfo) {
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(rootNode, allNodes)

        val blocksFound = mutableListOf<FlexBlock>()
        
        // Find pay nodes as anchors for blocks
        val payNodes = allNodes.filter { it.text?.toString()?.contains("$") == true }
        
        for (payNode in payNodes) {
            val blockCard = findParentBlockCard(payNode) ?: continue
            val block = extractBlockInfo(blockCard)
            if (block != null) {
                blocksFound.add(block)
            }
        }

        if (blocksFound.isEmpty()) {
            autoScroll()
        } else {
            serviceScope.launch {
                val matched = evaluateAndAct(blocksFound)
                if (!matched) {
                    autoScroll()
                }
            }
        }
    }

    private fun extractBlockInfo(card: AccessibilityNodeInfo): FlexBlock? {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(card, nodes)
        
        var pay: Double? = null
        var station: String? = null
        var timeRange: String? = null
        var lengthText: String? = null

        for (node in nodes) {
            val text = node.text?.toString()?.trim() ?: continue
            if (text.isEmpty()) continue
            
            // Pay: $96.50
            if (text.contains("$")) {
                val match = Pattern.compile("\\$(\\d+\\.?\\d*)").matcher(text)
                if (match.find()) pay = match.group(1)?.toDoubleOrNull()
            }
            
            // Time Range: 6:15 AM - 9:45 AM
            if (text.contains("-") && (text.contains("AM") || text.contains("PM"))) {
                timeRange = text
            }
            
            // Length: 3 hr 30 min, 1 hr
            if (text.contains("hr") || text.contains("min") || text.contains("hour", true)) {
                lengthText = text
            }
        }

        // Station name is usually the first meaningful text node that isn't the others
        station = nodes.map { it.text?.toString()?.trim() ?: "" }
            .firstOrNull { it.isNotEmpty() && !it.contains("$") && !it.contains("-") && !it.contains("hr") && !it.contains("min") }

        if (pay != null && (station != null || timeRange != null)) {
            val finalStation = station ?: "Unknown Station"
            val finalTimeRange = timeRange ?: "Unknown Time"
            
            val length = if (lengthText != null) {
                val hrMatch = Pattern.compile("(\\d+)\\s*hr").matcher(lengthText)
                val minMatch = Pattern.compile("(\\d+)\\s*min").matcher(lengthText)
                val hrs = if (hrMatch.find()) hrMatch.group(1)?.toDoubleOrNull() ?: 0.0 else 0.0
                val mins = if (minMatch.find()) minMatch.group(1)?.toDoubleOrNull() ?: 0.0 else 0.0
                hrs + (mins / 60.0)
            } else 0.0
            
            return FlexBlock(card, finalStation, pay, finalTimeRange, length)
        }
        return null
    }

    private suspend fun evaluateAndAct(blocks: List<FlexBlock>): Boolean {
        var anyMatched = false
        val prefs = settingsRepository.filterPreferencesFlow.first()
        if (!prefs.isFlexEnabled) return false

        val filters = prefs.flexFilters
        
        for (block in blocks) {
            val blockId = "${block.station}_${block.timeRange}_${block.pay}"
            if (processedBlocks.contains(blockId)) continue

            val isMatch = evaluateBlock(block, filters)
            if (isMatch) {
                anyMatched = true
                Log.d("FlexService", "MATCH FOUND! Attempting to click: ${block.station} for \$${block.pay}")
                processedBlocks.add(blockId)
                
                offerRepository.addOffer(GigOffer(
                    platform = Platform.FLEX,
                    pay = block.pay,
                    storeName = block.station,
                    itemCount = "${block.length}h",
                    action = OfferAction.ACCEPTED,
                    reason = "Auto-Scheduling Match"
                ))

                clickCard(block.node)
                break 
            } else {
                Log.d("FlexService", "No match for block: ${block.station} (\$${block.pay})")
                processedBlocks.add(blockId)
            }
        }
        return anyMatched
    }

    private fun evaluateBlock(block: FlexBlock, filters: PlatformFilters): Boolean {
        if (block.pay < filters.minTotalPay) {
            Log.d("FlexService", "Block pay \$${block.pay} below minimum \$${filters.minTotalPay}")
            return false
        }
        
        if (filters.blockedStores.any { block.station.contains(it, true) }) {
            Log.d("FlexService", "Station ${block.station} is blocked")
            return false
        }
        
        val lengthCategory = when {
            block.length < 1.0 -> "Less than 1 hour"
            block.length < 2.0 -> "1 hour - 2 hours"
            block.length < 3.0 -> "2 hours - 3 hours"
            block.length < 4.0 -> "3 hours - 4 hours"
            else -> "4 hours +"
        }
        
        if (!filters.flexBlockLengths.contains(lengthCategory)) {
            Log.d("FlexService", "Length ${block.length}h ($lengthCategory) not selected")
            return false
        }
        
        return true
    }

    private fun handleDetailsPage(rootNode: AccessibilityNodeInfo) {
        val scheduleButtons = mutableListOf<AccessibilityNodeInfo>()
        findNodesByText(rootNode, listOf("Schedule", "ACCEPT"), scheduleButtons)
        
        for (node in scheduleButtons) {
            if (node.isClickable || node.parent?.isClickable == true) {
                Log.d("FlexService", "FAST ACCEPT: Clicking button")
                
                // Use gesture for faster response than standard click action
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val path = Path().apply { moveTo(bounds.centerX().toFloat(), bounds.centerY().toFloat()) }
                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
                    .build()
                
                dispatchGesture(gesture, null, null)
                return
            }
        }
    }

    private fun findNodesByText(root: AccessibilityNodeInfo, texts: List<String>, list: MutableList<AccessibilityNodeInfo>) {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        findAllNodes(root, nodes)
        for (n in nodes) {
            val t = n.text?.toString() ?: ""
            if (texts.any { t.contains(it, true) }) {
                list.add(n)
            }
        }
    }

    private fun clickCard(node: AccessibilityNodeInfo) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val x = bounds.centerX().toFloat()
        val y = bounds.centerY().toFloat()

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10)) // Instant click
            .build()
        
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                lastActionTime = System.currentTimeMillis()
            }
        }, null)
    }

    private fun autoScroll() {
        if (isScrolling) return
        
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 300) return // Throttling to prevent over-scrolling

        isScrolling = true
        serviceScope.launch {
            val prefs = settingsRepository.filterPreferencesFlow.first()
            if (!prefs.isFlexEnabled) {
                isScrolling = false
                return@launch
            }

            // High-speed refresh gesture
            val displayMetrics = resources.displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            val path = Path().apply {
                moveTo(width / 2f, height * 0.4f) // Shorter, faster swipe
                lineTo(width / 2f, height * 0.8f) // Swiping DOWN to refresh as seen in many high-speed bots
            }

            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 100)) // Ultra-fast (100ms)
                .build()

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    isScrolling = false
                    lastActionTime = System.currentTimeMillis()
                }
            }, null)
        }
    }

    private fun findParentBlockCard(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current = node
        repeat(6) {
            val parent = current.parent ?: return null
            if (parent.isClickable) return parent
            current = parent
        }
        return null
    }

    private fun findAllNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        list.add(node)
        for (i in 0 until node.childCount) {
            findAllNodes(node.getChild(i), list)
        }
    }

    private data class FlexBlock(
        val node: AccessibilityNodeInfo,
        val station: String,
        val pay: Double,
        val timeRange: String,
        val length: Double
    )

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
