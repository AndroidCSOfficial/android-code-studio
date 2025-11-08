/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.templates.impl.withCpp

import com.tom.rv2ide.templates.base.AndroidModuleTemplateBuilder
import com.tom.rv2ide.templates.impl.base.baseLayoutContentMain

internal fun emptyLayoutSrc() = baseLayoutContentMain()

internal fun AndroidModuleTemplateBuilder.withCppSrcKt(): String {
  return """
package ${data.packageName}

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import ${data.packageName}.databinding.ActivityMainBinding

public class MainActivity : AppCompatActivity() {

    companion object {
        init {
            System.loadLibrary("tomaslib")
        }
    }
    
    private var _binding: ActivityMainBinding? = null
    
    private val binding: ActivityMainBinding
      get() = checkNotNull(_binding) { "Activity has been destroyed" }
    
    // Native method declaration
    external fun sayHello(): String
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate and get instance of binding
        _binding = ActivityMainBinding.inflate(layoutInflater)

        // set content view to binding's root
        setContentView(binding.root)
        
        // Call native method and show toast
        val message = sayHello()
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
"""
}

internal fun AndroidModuleTemplateBuilder.withCppSrcJava(): String {
  return """
package ${data.packageName};

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import ${data.packageName}.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("tomaslib");
    }

    private ActivityMainBinding binding;
    
    // Native method declaration
    public native String sayHello();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate and get instance of binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        // set content view to binding's root
        setContentView(binding.getRoot());
        
        // Call native method and show toast
        String message = sayHello();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.binding = null;
    }
}
"""
}
