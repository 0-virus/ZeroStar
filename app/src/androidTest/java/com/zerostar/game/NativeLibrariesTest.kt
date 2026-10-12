package com.zerostar.game

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Load both packaged native dependencies even where framework APIs avoid them. */
@RunWith(AndroidJUnit4::class)
class NativeLibrariesTest {
    @Test fun packagedNativeLibrariesLoadOnDevicePageSize() {
        System.loadLibrary("androidx.graphics.path")
        System.loadLibrary("datastore_shared_counter")
    }
}
