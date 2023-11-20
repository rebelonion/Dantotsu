package ani.dantotsu.download

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import ani.dantotsu.R
import ani.dantotsu.themes.ThemeManager

class DownloadContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager(this).applyTheme()
        setContentView(R.layout.activity_container)

        val fragmentClassName = intent.getStringExtra("FRAGMENT_CLASS_NAME")
        val fragment = Class.forName(fragmentClassName).newInstance() as Fragment

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}