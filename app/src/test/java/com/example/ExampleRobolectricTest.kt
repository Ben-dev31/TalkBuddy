package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BuddyCatalog
import com.example.model.TopicCatalog
import com.example.model.UserLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TalkBuddy", appName)
  }

  @Test
  fun `verify buddies catalog initialized properly`() {
    val buddies = BuddyCatalog.buddies
    assertEquals(3, buddies.size)
    assertNotNull(BuddyCatalog.getById("alex"))
    assertNotNull(BuddyCatalog.getById("sam"))
    assertNotNull(BuddyCatalog.getById("chloe"))
  }

  @Test
  fun `verify daily topics count`() {
    val topics = TopicCatalog.topics
    assertEquals(6, topics.size)
  }
}

