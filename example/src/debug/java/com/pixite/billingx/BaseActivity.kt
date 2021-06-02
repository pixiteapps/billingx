package com.pixite.billingx

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.pixite.billingx.debug.DebugDrawer

open class BaseActivity : AppCompatActivity() {

  lateinit var drawer: DrawerLayout
  lateinit var debugDrawer: DebugDrawer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    super.setContentView(R.layout.debug_activity)
    drawer = findViewById(R.id.drawer)
    debugDrawer = supportFragmentManager.findFragmentById(R.id.debug_drawer) as DebugDrawer

    // Peek the drawer to user's know it's there.
    drawer.openDrawer(GravityCompat.END)
    drawer.postDelayed({
      if (!isFinishing && drawer.isDrawerOpen(GravityCompat.END)) {
        drawer.closeDrawer(GravityCompat.END)
      }
    }, 800)
  }

  override fun setContentView(@LayoutRes layoutResID: Int) {
    val contentParent = findViewById<ViewGroup>(R.id.content_frame)
    contentParent.removeAllViews()
    LayoutInflater.from(this).inflate(layoutResID, contentParent)
  }

  override fun setContentView(view: View?) {
    val contentParent = findViewById<ViewGroup>(R.id.content_frame)
    contentParent.removeAllViews()
    contentParent.addView(view)
  }

  override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
    val contentParent = findViewById<ViewGroup>(R.id.content_frame)
    contentParent.removeAllViews()
    contentParent.addView(view, params)
  }

}