package com.danielcioban.routeplanner.util

import java.util.UUID

/** Stable cross-device identifier for future sync / import merge. */
fun newRemoteId(): String = UUID.randomUUID().toString()
