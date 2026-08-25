package com.danielcioban.routeplanner.data.backup

data class BackupImportOptions(
    val includeLibrary: Boolean = true,
    val includeRoutes: Boolean = true,
    val includeHistory: Boolean = true,
    val includeTemplates: Boolean = true,
    val includeFuel: Boolean = true,
    /** When true, never overwrite an existing remoteId (keep the local copy). */
    val keepLocalOnConflict: Boolean = false,
)

data class BackupPreview(
    val routeAdds: Int = 0,
    val routeOverwrites: Int = 0,
    val routeLocalNewer: Int = 0,
    val libraryAdds: Int = 0,
    val libraryOverwrites: Int = 0,
    val libraryLocalNewer: Int = 0,
    val tripCount: Int = 0,
) {
    val wouldOverwrite: Int get() = routeOverwrites + libraryOverwrites
    val localNewer: Int get() = routeLocalNewer + libraryLocalNewer
}
