/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.templates.android.basic

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object BasicSources {

  fun mainActivityKotlin(packageId: String): String =
      """
    package $packageId
    
    import android.os.Bundle
    import androidx.appcompat.app.AppCompatActivity
    import $packageId.databinding.ActivityMainBinding
    
    class MainActivity : AppCompatActivity() {
        
        private var _binding: ActivityMainBinding? = null
        private val binding: ActivityMainBinding
            get() = checkNotNull(_binding) { "Activity has been destroyed" }
        
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            binding.textView.text = "Hello, Basic Activity!"
        }
        
        override fun onDestroy() {
            super.onDestroy()
            _binding = null
        }
    }
  """
          .trimIndent()

  fun mainActivityJava(packageId: String): String =
      """
    package $packageId;
    
    import android.os.Bundle;
    import androidx.appcompat.app.AppCompatActivity;
    import $packageId.databinding.ActivityMainBinding;
    
    public class MainActivity extends AppCompatActivity {
        private ActivityMainBinding binding;
        
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            binding.textView.setText("Hello, Basic Activity!");
        }
        
        @Override
        protected void onDestroy() {
            super.onDestroy();
            binding = null;
        }
    }
  """
          .trimIndent()
}
