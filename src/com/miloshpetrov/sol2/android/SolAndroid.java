
package com.miloshpetrov.sol2.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.miloshpetrov.sol2.SolAppListener;

public class SolAndroid extends AndroidApplication {
  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initialize(new SolAppListener(), false);
  }
}
