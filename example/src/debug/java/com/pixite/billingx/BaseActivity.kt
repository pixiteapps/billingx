package com.pixite.billingx

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pixite.billingx.debug.DebugDrawer
import com.ryanharter.billingx.R

open class BaseActivity : AppCompatActivity() {

  lateinit var drawer: DrawerLayout
  lateinit var debugDrawer: DebugDrawer

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    super.setContentView(R.layout.debug_activity)
    drawer = findViewById(R.id.drawer)
    debugDrawer = supportFragmentManager.findFragmentById(R.id.debug_drawer) as DebugDrawer

    // Peek the drawer to user's know it's there.
    drawer.openDrawer(Gravity.END)
    drawer.postDelayed({
      if (!isFinishing && drawer.isDrawerOpen(Gravity.END)) {
        drawer.closeDrawer(Gravity.END)
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