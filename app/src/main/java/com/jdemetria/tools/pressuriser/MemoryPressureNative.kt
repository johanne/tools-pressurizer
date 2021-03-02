package com.jdemetria.tools.pressuriser

/**
 * Helper class for making native calls that are memory related.
 */
class MemoryPressureNative {

    companion object {
        // Used to load the 'resource-native-lib' library on application startup.
        init {
            System.loadLibrary("resource-native-lib")
        }

        /**
         * Returns the thread Id given a successful call to lock the [percent] memory.
         */
        external fun lockPercentOfMemory(percent: Float): Long

        /**
         * Returns true if the call to free the memory is successful, false otherwise.
         */
        external fun freeLockedMemory(): Boolean

        /**
         * Returns the system's free memory in bytes.
         */
        external fun getFreeMemory(): Long

        /**
         * Returns the system's used and/or reserved memory in bytes.
         */
        external fun getUsedMemory(): Long

        /**
         * Returns the system's total memory.
         */
        external fun getTotalMemory(): Long
    }
}