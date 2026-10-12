package com.zerostar.game

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment
import com.zerostar.game.ads.ageTreatment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdsSdkContractTest {
    @Test fun shippedSdkResolvesEachAgeConstantWithoutEnumReflection() {
        assertEquals(AgeRestrictedTreatment.CHILD, ageTreatment("CHILD"))
        assertEquals(AgeRestrictedTreatment.TEEN, ageTreatment("TEEN"))
        assertEquals(AgeRestrictedTreatment.UNSPECIFIED, ageTreatment("UNSPECIFIED"))
    }
}
