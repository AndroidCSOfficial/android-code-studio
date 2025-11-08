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

package com.tom.rv2ide.templates.android.navigation.bottom

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object BottomNavigationSources {

  fun mainActivityKotlin(packageId: String): String =
      """
      package $packageId
      
      import android.os.Bundle
      import com.google.android.material.bottomnavigation.BottomNavigationView
      import androidx.appcompat.app.AppCompatActivity
      import androidx.navigation.findNavController
      import androidx.navigation.ui.AppBarConfiguration
      import androidx.navigation.ui.setupActionBarWithNavController
      import androidx.navigation.ui.setupWithNavController
      import $packageId.databinding.ActivityMainBinding
      
      class MainActivity : AppCompatActivity() {
      
          private lateinit var binding: ActivityMainBinding
      
          override fun onCreate(savedInstanceState: Bundle?) {
              super.onCreate(savedInstanceState)
      
              binding = ActivityMainBinding.inflate(layoutInflater)
              setContentView(binding.root)
      
              val navView: BottomNavigationView = binding.navView
      
              val navController = findNavController(R.id.nav_host_fragment_activity_main)
              // Passing each menu ID as a set of Ids because each
              // menu should be considered as top level destinations.
              val appBarConfiguration = AppBarConfiguration(
                  setOf(
                      R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
                  )
              )
              setupActionBarWithNavController(navController, appBarConfiguration)
              navView.setupWithNavController(navController)
          }
      }
  """
          .trimIndent()

  fun mainActivityJava(packageId: String): String =
      """
      package $packageId;
      
      import android.os.Bundle;
      
      import com.google.android.material.bottomnavigation.BottomNavigationView;
      
      import androidx.appcompat.app.AppCompatActivity;
      import androidx.navigation.NavController;
      import androidx.navigation.Navigation;
      import androidx.navigation.ui.AppBarConfiguration;
      import androidx.navigation.ui.NavigationUI;
      
      import $packageId.databinding.ActivityMainBinding;
      
      public class MainActivity extends AppCompatActivity {
      
          private ActivityMainBinding binding;
      
          @Override
          protected void onCreate(Bundle savedInstanceState) {
              super.onCreate(savedInstanceState);
      
              binding = ActivityMainBinding.inflate(getLayoutInflater());
              setContentView(binding.getRoot());
      
              BottomNavigationView navView = findViewById(R.id.nav_view);
              // Passing each menu ID as a set of Ids because each
              // menu should be considered as top level destinations.
              AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                      R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                      .build();
              NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
              NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
              NavigationUI.setupWithNavController(binding.navView, navController);
          }
      
      }
  """
          .trimIndent()
}

// Fragments
object Fragment {

  fun templateTypeKt(packageId: String, fragmentName: String): String {
    val nameCap = fragmentName.replaceFirstChar { it.uppercase() }
    val bindingName = "Fragment${nameCap}Binding"
    val textId = "text${nameCap}"

    return """
            package $packageId.ui.$fragmentName

            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import android.widget.TextView
            import androidx.fragment.app.Fragment
            import androidx.lifecycle.ViewModelProvider
            import $packageId.databinding.$bindingName

            class ${nameCap}Fragment : Fragment() {

                private var _binding: $bindingName? = null
                private val binding get() = _binding!!

                override fun onCreateView(
                    inflater: LayoutInflater,
                    container: ViewGroup?,
                    savedInstanceState: Bundle?
                ): View {
                    val ${fragmentName}ViewModel =
                        ViewModelProvider(this).get(${nameCap}ViewModel::class.java)

                    _binding = $bindingName.inflate(inflater, container, false)
                    val root: View = binding.root

                    val textView: TextView = binding.$textId
                    ${fragmentName}ViewModel.text.observe(viewLifecycleOwner) {
                        textView.text = it
                    }
                    return root
                }

                override fun onDestroyView() {
                    super.onDestroyView()
                    _binding = null
                }
            }
        """
        .trimIndent()
  }

  fun templateTypeJava(packageId: String, fragmentName: String): String {
    val nameCap = fragmentName.replaceFirstChar { it.uppercase() }
    val bindingName = "Fragment${nameCap}Binding"
    val viewModelName = "${nameCap}ViewModel"
    val textId = "text${nameCap}"

    return """
            package $packageId.ui.$fragmentName;
            
            import android.os.Bundle;
            import android.view.LayoutInflater;
            import android.view.View;
            import android.view.ViewGroup;
            import android.widget.TextView;
            
            import androidx.annotation.NonNull;
            import androidx.fragment.app.Fragment;
            import androidx.lifecycle.ViewModelProvider;
            
            import $packageId.databinding.$bindingName;
            
            public class ${nameCap}Fragment extends Fragment {
            
                private $bindingName binding;
            
                @Override
                public View onCreateView(@NonNull LayoutInflater inflater,
                                         ViewGroup container, Bundle savedInstanceState) {
                    $viewModelName ${fragmentName}ViewModel =
                            new ViewModelProvider(this).get($viewModelName.class);
            
                    binding = $bindingName.inflate(inflater, container, false);
                    View root = binding.getRoot();
            
                    final TextView textView = binding.$textId;
                    ${fragmentName}ViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
                    return root;
                }
            
                @Override
                public void onDestroyView() {
                    super.onDestroyView();
                    binding = null;
                }
            }
        """
        .trimIndent()
  }
}

object ViewModel {

  fun templateTypeKt(packageId: String, className: String): String =
      """
        package $packageId.ui.$className

        import androidx.lifecycle.LiveData
        import androidx.lifecycle.MutableLiveData
        import androidx.lifecycle.ViewModel

        class ${className.replaceFirstChar { it.uppercase() }}ViewModel : ViewModel() {

            private val _text = MutableLiveData<String>().apply {
                value = "This is ${className.replaceFirstChar { it.uppercase() }} Fragment"
            }
            val text: LiveData<String> = _text
        }
    """
          .trimIndent()

  fun templateTypeJava(packageId: String, className: String): String =
      """
        package $packageId.ui.$className;
        
        import androidx.lifecycle.LiveData;
        import androidx.lifecycle.MutableLiveData;
        import androidx.lifecycle.ViewModel;
        
        public class ${className.replaceFirstChar { it.uppercase() }}ViewModel extends ViewModel {
        
            private final MutableLiveData<String> mText;
        
            public ${className.replaceFirstChar { it.uppercase() }}ViewModel() {
                mText = new MutableLiveData<>();
                mText.setValue("This is ${className.replaceFirstChar { it.uppercase() }} fragment");
            }
        
            public LiveData<String> getText() {
                return mText;
            }
        }
    """
          .trimIndent()
}
