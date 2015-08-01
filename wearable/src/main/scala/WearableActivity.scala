package citywasp.android.wearable

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class WearableActivity extends Activity {

    override def onCreate(savedInstanceState: Bundle) = {
        super.onCreate(savedInstanceState)
        val tv = new TextView(this)
        tv.setText("Hello, Android Wearable!!")
        setContentView(tv)
    }
}
