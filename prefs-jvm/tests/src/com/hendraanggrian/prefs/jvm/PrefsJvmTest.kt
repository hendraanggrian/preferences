package com.hendraanggrian.prefs.jvm

import com.hendraanggrian.prefs.Prefs
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PrefsJvmTest {
    private lateinit var propertiesPrefs: PropertiesPrefs
    private lateinit var jvmPrefs: JvmPrefs

    @BeforeTest fun createTest() {
        Prefs.setLogger(Prefs.Logger.System)
        propertiesPrefs = Prefs.of(File("test.properties").apply { if (exists()) delete() })
        jvmPrefs = Prefs.userNode<Prefs>()
    }

    @Test fun properties() {
        propertiesPrefs["name"] = "Hendra"
        propertiesPrefs.save()
        assertEquals("Hendra", propertiesPrefs["name"])
    }

    @Test fun jvm() {
        jvmPrefs["name"] = "Hendra"
        jvmPrefs.save()
        assertEquals("Hendra", jvmPrefs["name"])
    }
}