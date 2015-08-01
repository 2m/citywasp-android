package citywasp.android.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class PhoneActivity extends Activity {

    override def onCreate(savedInstanceState: Bundle) = {
        super.onCreate(savedInstanceState)
        val tv = new TextView(this)
        tv.setText("Hello, Android phone!!!")
        setContentView(tv)
    }
}
