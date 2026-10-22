package com.example.session

import org.json.JSONObject

/**
 * A sensitive device action waiting for the user to confirm on screen.
 * `resume` runs the rest of a multi-step plan after this action is confirmed.
 */
data class PendingDeviceAction(
    val id: String,
    val toolName: String,
    val summary: String,
    val args: JSONObject,
    val resume: (() -> String)? = null
)
